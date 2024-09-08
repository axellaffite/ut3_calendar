package com.edt.ut3.backend.requests.authentication_services

import android.util.Log
import com.edt.ut3.R
import com.edt.ut3.backend.requests.getClient
import io.ktor.client.*
import io.ktor.client.plugins.cookies.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.TimeoutException
import kotlin.collections.HashMap
import kotlin.collections.Map
import kotlin.collections.any
import kotlin.collections.joinToString
import kotlin.collections.set
import kotlin.collections.slice

class AuthenticatorUT3(
    val client: HttpClient,
    baseUrl: String
) : Authenticator(baseUrl) {

    @Throws(AuthenticationException::class)
    override suspend fun connect(credentials: Credentials?) {
        try {
            if (credentials == null) {
                throw AuthenticationException(R.string.error_missing_credentials)
            }

            ensureAuthentication(credentials)
        } catch (e: IOException) {
            throw AuthenticationException(R.string.error_check_internet)
        } catch (e: TimeoutException) {
            throw AuthenticationException(R.string.error_check_internet)
        }
    }

    @Throws(AuthenticationException::class)
    override suspend fun checkCredentials(credentials: Credentials) {
        ensureAuthentication(credentials, getClient())
    }


    @Throws(IOException::class, TimeoutException::class, AuthenticationException::class)
    override suspend fun ensureAuthentication(credentials: Credentials) {
        ensureAuthentication(credentials, client)
    }

    private suspend fun isConnected(targetClient: HttpClient) : Boolean{
        Log.d("Auth", "Checking if connected")
        if (targetClient.cookies("https://edt.univ-tlse3.fr").any { cookie -> cookie.name == "saml-session" }) {
            val response1: HttpResponse = targetClient.get("https://edt.univ-tlse3.fr/calendar", )
            if(response1.request.url.fullPath != "/idp/profile/SAML2/Redirect/SSO" && response1.status == HttpStatusCode.OK){
                Log.d("Auth", "Client already connected")
                return true
            }

        }
        return false
    }


    @Throws(IOException::class, TimeoutException::class, AuthenticationException::class)
    private suspend fun ensureAuthentication(credentials: Credentials, targetClient: HttpClient) {
        Log.d(this::class.simpleName, "Checking authentication..")
        if(!isConnected(targetClient)){
            authenticate(credentials, targetClient)
        }
    }


    @Throws(IOException::class, TimeoutException::class, AuthenticationException::class)
    override suspend fun authenticate(credentials: Credentials) = authenticate(credentials, client)

    @Throws(IOException::class, TimeoutException::class, AuthenticationException::class)
    private suspend fun authenticate(credentials: Credentials, targetClient: HttpClient) {
        if(isConnected(targetClient)) {
            Log.d("Auth", "Client already authentified, skipping...")
            return
        }
        // ex: edt.univ-tlse3.fr/calendar
        val baseURL = URL(baseUrl)
        val urlencodedBaseURL = withContext(Dispatchers.IO) {
            URLEncoder.encode(baseUrl, "utf-8")
        }
        // ex : edt.univ-tlse3.fr
        val baseHost = baseURL.host
        // ex : ["edt", "univ-tlse3", "fr"]
        val hostDomains = baseHost.split(".")
        // ex : univ-tlse3.fr
        val primaryDomain = hostDomains.slice(hostDomains.size - 2 until hostDomains.size).joinToString(".")

        val response1: HttpResponse = targetClient.get("https://${baseHost}/calendar")
        val execution = response1.request.url.parameters["execution"]

        // On signale qu'on souhaite une nouvelle connection, pas de localStorage
        val response2: HttpResponse = targetClient.submitForm(
            url = "https://idp.${primaryDomain}/idp/profile/SAML2/Redirect/SSO",
            formParameters  = Parameters.build {
                append("_eventId_proceed","")
                append("shib_idp_ls_exception.shib_idp_persistent_ss", "")
                append("shib_idp_ls_exception.shib_idp_session_ss", "")
                append("shib_idp_ls_success.shib_idp_persistent_ss", "true")
                append("shib_idp_ls_success.shib_idp_session_ss", "true")
                append("shib_idp_ls_supported", "true")
                append("shib_idp_ls_value.shib_idp_persistent_ss", "")
                append("shib_idp_ls_value.shib_idp_session_ss", "")
            }
        ) {
            parameter("execution", "e1s1")
        }
        val txt: String = response2.bodyAsText()
        val token = extract_execution_from_ut3_login_page(txt)
            ?: throw AuthenticationException(R.string.error_during_authentication)

        // On peut transmettre les credentials à cAS
        val response3 = targetClient.submitForm("https://cas.${primaryDomain}/cas/login", formParameters = Parameters.build {
            append("username", credentials.username)
            append("password", credentials.password)
            append("execution", token)
            append("_eventId", "submit")
            append("geolocation", "")
        }) {
            parameter("entityId", urlencodedBaseURL)
            parameter("service", "https%3A%2F%2Fidp.${primaryDomain}%2Fidp%2FAuthn%2FExternal%3Fconversation%3De1s2")
        }

        if(response3.status == HttpStatusCode.Unauthorized){
            throw AuthenticationException(R.string.error_wrong_credentials)
        }

        // Attribution des droits d'accès à CELCAT (normalement one time only, mais ça marche jamais)
        val response4 = targetClient.submitForm("https://idp.${primaryDomain}/idp/profile/SAML2/Redirect/SSO", formParameters = Parameters.build {
            append("_eventId_proceed", "Accepter")
            append("_shib_idp_consentIds", "displayName")
            append("_shib_idp_consentIds", "eduPersonPrincipalName")
            append("_shib_idp_consentIds", "mail")
            append("_shib_idp_consentIds", "uid")
            append("_shib_idp_consentOptions", "_shib_idp_globalConsent")
        }){
            parameter("execution", "e1s3")
        }

        val localStorage = extract_local_storage_function_calls(response4.bodyAsText())

        val response5 = targetClient.submitForm("https://idp.${primaryDomain}/idp/profile/SAML2/Redirect/SSO", formParameters = Parameters.build {
            append("_eventId_proceed", "")
            append("shib_idp_ls_exception.shib_idp_persistent_ss", "")
            append("shib_idp_ls_success.shib_idp_persistent_ss", "true")
            append("shib_idp_ls_success.shib_idp_session_ss", "true")
        }) {
            parameter("execution", "e1s4")
        }

        val samlResponse =  extract_saml_response(response5.bodyAsText())
        if(samlResponse == null){
            Log.d("Auth", "No SAML response found")
            throw AuthenticationException(R.string.error_during_authentication)
        }
        val response6 = targetClient.submitForm("${baseUrl}/Saml/AssertionConsumerService", formParameters = Parameters.build{
            append("SAMLResponse", samlResponse)
        }) {
            accept(ContentType.Text.Html)
            accept(ContentType.Application.Xml)
            accept(ContentType.Image.Any)
        }

        if(needs_disambiguation(response6)){
            val body = response6.bodyAsText()
            val requestToken = disambiguation_extract_request_token(body)
            val res7_token = disambiguation_extract_token(body)
            if(res7_token == null || requestToken == null || credentials.disambiguationIdentity == null){
                Log.d("Auth","No disambiguation token found, or credentials do not specify disambiguation information")
                Log.d("Auth","Request token : $requestToken")
                Log.d("Auth","Token : $token")
                Log.d("Auth","Disambiguation identity : ${credentials.disambiguationIdentity}")
                throw AuthenticationException(R.string.error_during_authentication)
            }
            val response7 = targetClient.submitForm("${baseUrl}/Disambiguate/Disambiguated", formParameters = Parameters.build {
                append("Token", res7_token)
                append("__RequestVerificationToken", requestToken)
                append("submit", credentials.disambiguationIdentity)
            })
        }


        Log.d("Auth", "End of authentication process, last request status code : " + response5.status.toString())
    }


    private fun needs_disambiguation(response: HttpResponse): Boolean{
        return response.request.url.fullPath.contains("Disambiguate")
    }

    private fun disambiguation_extract_request_token(page: String): String? {
        val reg = Regex("name=\"__RequestVerificationToken\" .*? value=\"(.*?)\"")
        return reg.find(page)?.groups?.get(1)?.value
    }

    private fun disambiguation_extract_token(page: String): String? {
        val reg = Regex("name=\"Token\" value=\"(.*?)\"")
        return reg.find(page)?.groups?.get(1)?.value
    }



    private fun extract_execution_from_ut3_login_page(page: String): String?{
        val reg = Regex("name=\"execution\" value=\"(.*?)\"")
        return reg.find(page)?.groups?.get(1)?.value
    }

    private fun extract_local_storage_function_calls(page: String): Map<String, String>{
        val reg = Regex("""writeLocalStorage\("(.*?)", "(.*?)"\);""")
        val matches = HashMap<String, String>()
        for(match in reg.findAll(page)){
            matches[match.groups[1]?.value!!] = match.groups[2]?.value!!
        }
        return matches
    }

    private fun extract_saml_response(page: String): String? {
        return Regex("name=\"SAMLResponse\" value=\"(.*?)\"").find(page)?.groups?.get(1)?.value
    }
}
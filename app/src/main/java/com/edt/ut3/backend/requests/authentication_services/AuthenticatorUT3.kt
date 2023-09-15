package com.edt.ut3.backend.requests.authentication_services

import android.util.Log
import com.edt.ut3.R
import com.edt.ut3.backend.requests.getClient
import io.ktor.client.*
import io.ktor.client.call.receive
import io.ktor.client.plugins.cookies.*
import io.ktor.client.request.get
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.*
import io.ktor.http.*
import java.io.IOException
import java.util.Dictionary
import java.util.concurrent.TimeoutException
import io.ktor.client.request.forms.*

class AuthenticatorUT3(
    val client: HttpClient,
    host: String = "https://edt.univ-tlse3.fr/calendar2"
) : Authenticator(host) {

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


    @Throws(IOException::class, TimeoutException::class, AuthenticationException::class)
    private suspend fun ensureAuthentication(credentials: Credentials, targetClient: HttpClient) {
        Log.d(this::class.simpleName, "Checking authentication..")
        authenticate(credentials)
    }


    @Throws(IOException::class, TimeoutException::class, AuthenticationException::class)
    override suspend fun authenticate(credentials: Credentials) = authenticate(credentials, client)
    override suspend fun login(tokens: Map<String, String>) {
        login(tokens, client)
    }

    private suspend fun login(tokens: Map<String, String>, targetClient: HttpClient){

    }


    @Throws(IOException::class, TimeoutException::class, AuthenticationException::class)
    private suspend fun authenticate(credentials: Credentials, targetClient: HttpClient) {

        val response1: HttpResponse = client.get("https://edt.univ-tlse3.fr/calendar2", )
        Log.d("Auth", response1.request.url.toString())
        val execution = response1.request.url.parameters["execution"]
        Log.d("Auth", "Mode d'execution : " + execution)

        // On signale qu'on souhaite une nouvelle connection, pas de localStorage
        val response2: HttpResponse = client.submitForm(
            url = "https://idp.univ-tlse3.fr/idp/profile/SAML2/Redirect/SSO",
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
        val response3 = targetClient.submitForm("https://cas.univ-tlse3.fr/cas/login", formParameters = Parameters.build {
            append("username", credentials.username)
            append("password", credentials.password)
            append("execution", token)
            append("_eventId", "submit")
            append("geolocation", "")
        }) {
            parameter("entityId", "https%3A%2F%2Fedt.univ-tlse3.fr%2Fcalendar2")
            parameter("service", "https%3A%2F%2Fidp.univ-tlse3.fr%2Fidp%2FAuthn%2FExternal%3Fconversation%3De1s2")
        }

        if(response3.status == HttpStatusCode.Unauthorized){
            throw AuthenticationException(R.string.error_wrong_credentials)
        }

        // Attribution des droits d'accès à CELCAT (normalement one time only, mais ça marche jamais)
        val response4 = targetClient.submitForm("https://idp.univ-tlse3.fr/idp/profile/SAML2/Redirect/SSO", formParameters = Parameters.build {
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

        val response5 = targetClient.submitForm("https://idp.univ-tlse3.fr/idp/profile/SAML2/Redirect/SSO", formParameters = Parameters.build {
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
        val response6 = targetClient.submitForm("https://edt.univ-tlse3.fr/calendar2/Saml/AssertionConsumerService", formParameters = Parameters.build{
            append("SAMLResponse", samlResponse)
        })
        Log.d("Auth", "End of authentication process, last request status code : " + response5.status.toString())
        Log.d("Cookies : ", targetClient.cookies("https://edt.univ-tlse3.fr/").toString())
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
package com.edt.ut3.backend.requests.authentication_services

import android.util.Log
import com.edt.ut3.R
import com.edt.ut3.backend.requests.getClient
import io.ktor.client.*
import io.ktor.client.features.cookies.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import java.io.IOException
import java.util.concurrent.TimeoutException

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
        // Here we look for authentication cookies
        // If there are no cookie available for authentication
        // the resulting cookie is null.
        val registeredCookies = targetClient.cookies(host)


        // The authentication is considered successful
        // if there is a cookie that is persistent
        val authenticated = containsValidAuthenticationCookie(registeredCookies)


        // If we're not logged in,
        // we try to authenticate
        Log.d(this::class.simpleName, "Already authenticated: $authenticated")
        if (!authenticated) {
            authenticate(credentials)
        }
    }


    @Throws(IOException::class, TimeoutException::class, AuthenticationException::class)
    override suspend fun authenticate(credentials: Credentials) = authenticate(credentials, client)


    @Throws(IOException::class, TimeoutException::class, AuthenticationException::class)
    private suspend fun authenticate(credentials: Credentials, targetClient: HttpClient) {
        Log.d(this@AuthenticatorUT3::class.simpleName, "Trying authentication to https://$host")

        val response = targetClient.get<String>("$host/LdapLogin")

        val token = getTokenFromResponse(response)
        if (token == null) {
            throw AuthenticationException(R.string.error_unable_to_treat_server_response)
        }


        val authenticationSuccessful = token.let { token ->
            // We launch the authentication
            // The request doesn't return cookies as they
            // are handled by the CookieProvider.
            targetClient.submitForm<Unit>(
                url = "$host/LdapLogin/Logon",
                formParameters = Parameters.build {
                    append("Name", credentials.username)
                    append("Password", credentials.password)
                    append("__RequestVerificationToken", token)
                }
            )

            containsValidAuthenticationCookie(targetClient.cookies(host))
        }

        Log.d(this@AuthenticatorUT3::class.simpleName, "Authentication successful: $authenticationSuccessful")
        if (!authenticationSuccessful) {
            throw AuthenticationException(R.string.error_wrong_credentials)
        }
    }


    @Throws(IOException::class)
    private fun getTokenFromResponse(body: String) = body.run {
        val reg =
            Regex("<input name=\"__RequestVerificationToken\" type=\"hidden\" value=\"(.*)\" />")

        reg.find(body)?.groups?.get(1)?.value
    }


    private fun containsValidAuthenticationCookie(cookies: List<Cookie>) : Boolean {
        return cookies.find { it.name.matches(Regex(".Calendar.Cookies")) } != null
    }

}
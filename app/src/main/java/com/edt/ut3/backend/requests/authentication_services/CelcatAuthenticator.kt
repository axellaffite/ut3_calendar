package com.edt.ut3.backend.requests.authentication_services

import android.content.Context
import com.edt.ut3.backend.requests.CookieProvider
import com.edt.ut3.backend.requests.HttpClientProvider
import com.edt.ut3.backend.requests.withAuthentication
import okhttp3.HttpUrl
import okhttp3.Request
import java.io.IOException
import java.net.SocketTimeoutException

class CelcatAuthenticator {
    private val cookieProvider = CookieProvider.getInstance()

    @Throws(SocketTimeoutException::class, IOException::class, Authenticator.InvalidCredentialsException::class)
    suspend fun connect(url: HttpUrl, credentials: Authenticator.Credentials?) {
        credentials?.let {
            getAuthenticator(url.host)
                ?.ensureAuthentication(
                    url.host,
                    cookieProvider,
                    credentials
                )
        } ?: throw Authenticator.InvalidCredentialsException("Credentials are null")
    }

    private fun getAuthenticator(host: String): Authenticator? = when (host) {
        "edt.univ-tlse3.fr" -> AuthenticatorUT3()
        else -> AuthenticatorUT3()
    }


    @Throws(SocketTimeoutException::class, IOException::class, Authenticator.InvalidCredentialsException::class)
    suspend fun checkCredentials(context: Context, credentials: Authenticator.Credentials) {
        val client = HttpClientProvider.generateNewClient()
        val url = "https://edt.univ-tlse3.fr"
        val request = Request.Builder().url(url).build()
        client.withAuthentication(context, request.url, CelcatAuthenticator(), credentials) {}
    }

    fun disconnect(url: HttpUrl) {
        cookieProvider.removeCookiesFor(url)
    }
}
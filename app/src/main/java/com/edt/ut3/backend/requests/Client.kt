package com.edt.ut3.backend.requests

import android.content.Context
import com.edt.ut3.backend.credentials.CredentialsManager
import com.edt.ut3.backend.requests.authentication_services.Authenticator
import com.edt.ut3.misc.RedirectFixerPlugin
import io.ktor.client.engine.cio.*
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpRedirect
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.plugins.logging.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.client.plugins.cookies.AcceptAllCookiesStorage
import io.ktor.client.plugins.contentnegotiation.*

import kotlinx.serialization.json.Json as JsonSerializerBase

val JsonSerializer = JsonSerializerBase {
    encodeDefaults = true
    isLenient = true
    ignoreUnknownKeys = true
}

fun getClient() = HttpClient(CIO) {
    install(HttpCookies) {
        storage = AcceptAllCookiesStorage()
    }

    install(ContentNegotiation) {
        json(JsonSerializer)
    }

    install(Logging) {
        logger = Logger.DEFAULT
        level = LogLevel.ALL
    }

    install(HttpRedirect) {
        checkHttpMethod = false

    }
    install(RedirectFixerPlugin){}

}

suspend fun HttpClient.authenticateIfNeeded(
    context: Context,
    authenticator: Authenticator
): HttpClient {
    if(authenticator.needsAuthentication){
        val credentials = CredentialsManager.getInstance(context).getCredentials()
        if (credentials != null) {
            authenticator.authenticate(credentials)
        }
    }

    return this
}
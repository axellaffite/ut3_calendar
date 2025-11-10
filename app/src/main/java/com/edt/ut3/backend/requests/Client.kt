package com.edt.ut3.backend.requests

import android.content.Context
import com.edt.ut3.backend.credentials.CredentialsManager
import com.edt.ut3.backend.requests.authentication_services.Authenticator
import com.edt.ut3.misc.RedirectFixerPlugin
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpRedirect
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.cookies.AcceptAllCookiesStorage
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.plugins.logging.DEFAULT
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.jackson.jackson

val objectMapper = ObjectMapper().setup()

fun ObjectMapper.setup(): ObjectMapper {
    registerKotlinModule()
    configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
    return this
}

fun getClient() = HttpClient(CIO) {
    install(HttpCookies) {
        storage = AcceptAllCookiesStorage()
    }

    install(ContentNegotiation) {
        jackson {
            setup()
        }
    }

    install(Logging) {
        logger = Logger.DEFAULT
        level = LogLevel.ALL
    }

    install(HttpRedirect) {
        checkHttpMethod = false

    }
    install(RedirectFixerPlugin) {}

}

suspend fun HttpClient.authenticateIfNeeded(
    context: Context,
    authenticator: Authenticator
): HttpClient {
    if (authenticator.needsAuthentication) {
        val credentials = CredentialsManager.getInstance(context).getCredentials()
        if (credentials != null) {
            authenticator.authenticate(credentials)
        }
    }

    return this
}
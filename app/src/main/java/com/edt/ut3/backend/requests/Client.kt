package com.edt.ut3.backend.requests

import android.content.Context
import com.edt.ut3.backend.credentials.CredentialsManager
import com.edt.ut3.backend.requests.authentication_services.Authenticator
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.*
import io.ktor.client.features.cookies.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.features.logging.*
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

    install(JsonFeature) {
        serializer = KotlinxSerializer(JsonSerializer)
    }

    install(Logging) {
        logger = Logger.DEFAULT
        level = LogLevel.ALL
    }

    install(HttpRedirect) {
        checkHttpMethod = false
    }
}

suspend fun HttpClient.authenticateIfNeeded(
    context: Context,
    authenticator: Authenticator
): HttpClient {
    val credentials = CredentialsManager.getInstance(context).getCredentials()
    if (credentials != null) {
        authenticator.authenticate(credentials)
    }

    return this
}
package com.edt.ut3.backend.requests

import com.edt.ut3.refactored.models.repositories.CredentialsRepository
import com.edt.ut3.refactored.models.services.authentication.AbstractAuthenticator
import com.edt.ut3.refactored.injected
import io.ktor.client.*
import kotlinx.serialization.json.Json as JsonSerializerBase

val JsonSerializer = JsonSerializerBase {
    encodeDefaults = true
    isLenient = true
    ignoreUnknownKeys = true
}

suspend fun HttpClient.authenticateIfNeeded(
    authenticator: AbstractAuthenticator,
    credentialsRepository: CredentialsRepository = injected()
): HttpClient {
    val credentials = credentialsRepository.getCredentials()
    if (credentials != null) {
        authenticator.authenticate(credentials)
    }

    return this
}
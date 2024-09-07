package com.edt.ut3.backend.requests.authentication_services

import com.edt.ut3.backend.formation_choice.AuthenticationMethod
import io.ktor.client.HttpClient

fun getAuthenticator(
    method: AuthenticationMethod,
    client: HttpClient,
    host: String
): Authenticator =
    when (method) {
        AuthenticationMethod.NONE -> AuthenticatorNone(host)
        AuthenticationMethod.UT3_FSI -> AuthenticatorUT3(client, host)
    }
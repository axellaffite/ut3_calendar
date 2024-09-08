package com.edt.ut3.backend.requests.authentication_services

class AuthenticatorNone(baseUrl: String) : Authenticator(baseUrl) {
    override val needsAuthentication = false
    override suspend fun connect(credentials: Credentials?) {

    }

    override suspend fun checkCredentials(credentials: Credentials) {

    }

    override suspend fun ensureAuthentication(credentials: Credentials) {

    }

    override suspend fun authenticate(credentials: Credentials) {

    }

}
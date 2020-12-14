package com.edt.ut3.backend.requests.authentication_services

import com.edt.ut3.backend.requests.CookieProvider
import java.io.IOException
import java.net.SocketTimeoutException

abstract class Authenticator {

    /**
     * This function takes a request and ensure
     * that we're logged in for the current
     * host provided.
     *
     * If the user isn't logged in it will try to
     * log in with the provided credentials.
     *
     * @throws SocketTimeoutException If the server can't be reached
     * @throws IOException If something fails during the request
     * @throws InvalidCredentialsException If the credentials are not valid
     *
     * @param host The host to authenticate with
     * @param provider The cookie provider that stores all the cookies
     * @param credentials The user's credentials
     */
    @Throws(SocketTimeoutException::class, IOException::class, InvalidCredentialsException::class)
    abstract suspend fun ensureAuthentication(host: String, provider: CookieProvider, credentials: Credentials)

    /**
     * This function will try to log
     * the user into the given host.
     *
     * @throws SocketTimeoutException If the server can't be reached
     * @throws IOException If something fails during the request
     * @throws InvalidCredentialsException If the credentials are not valid
     *
     * @param host The host to authenticate with
     * @param provider The cookie provider that stores all the cookies
     * @param credentials The user's credentials
     */
    @Throws(SocketTimeoutException::class, IOException::class, InvalidCredentialsException::class)
    abstract suspend fun authenticate(host: String, provider: CookieProvider, credentials: Credentials)


    class InvalidCredentialsException(reason: String? = null): IllegalStateException(reason)

    data class Credentials (
        val username: String,
        val password: String
    ) {
        companion object {
            fun from(username: String?, password: String?): Credentials? {
                return if (username != null && password != null) {
                    Credentials(username, password)
                } else {
                    null
                }
            }
        }
    }
}
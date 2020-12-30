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


    data class Credentials (
        val username: String,
        val password: String
    ) {

        companion object {
            /**
             * Returns the credentials created from the provided
             * [username] and [password].
             *
             * If one of these two parameters is null, the result of
             * this function will be null too.
             *
             * @param username The credential's username
             * @param password The credential's password
             * @return The built credentials or null if the
             * username or the password is null.
             */
            fun from(username: String?, password: String?): Credentials? {
                return when {
                    username == null -> null
                    password == null -> null
                    else -> Credentials(username, password)
                }
            }

        }

    }


    /**
     * This [Exception] is used to throw an error
     * when there is an Authentication problem.
     *
     * @param reason Why the exception has been thrown.
     */
    class InvalidCredentialsException(reason: String? = null): IllegalStateException(reason)

}
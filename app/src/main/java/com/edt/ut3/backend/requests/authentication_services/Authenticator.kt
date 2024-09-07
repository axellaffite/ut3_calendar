package com.edt.ut3.backend.requests.authentication_services

import android.content.Context
import androidx.annotation.StringRes
import java.util.Dictionary

abstract class Authenticator(val baseUrl: String) {
    open val needsAuthentication = true
    /**
     * Connects the client to the [host] provided in the constructor.
     *
     * @param credentials The credentials to use in order to get connected to the host.
     */
    @Throws(AuthenticationException::class)
    abstract suspend fun connect(credentials: Credentials?)


    /**
     * Check whether the credentials are valid or not.
     * The cookies shouldn't be saved in the same Cookie jar as
     * the client used for classic connections.
     *
     * @param credentials The credentials to use in order to get connected to the host.
     */
    @Throws(AuthenticationException::class)
    abstract suspend fun checkCredentials(credentials: Credentials)


    /**
     * This function takes a request and ensure
     * that we're logged in for the current
     * host provided.
     *
     * If the user isn't logged in it will try to
     * log in with the provided credentials.
     *
     * @throws AuthenticationException If an error occurs during credentials
     * exception or if credentials are invalid
     *
     * @param credentials The user's credentials
     */
    @Throws(AuthenticationException::class)
    abstract suspend fun ensureAuthentication(credentials: Credentials)

    /**
     * This function will try to log
     * the user into the given host.
     *
     * @throws AuthenticationException If an error occurs during credentials
     * exception or if credentials are invalid
     *
     * @param credentials The user's credentials
     */
    @Throws(AuthenticationException::class)
    abstract suspend fun authenticate(credentials: Credentials)

}


class AuthenticationException(
    @StringRes val resource: Int,
    val builder: AuthenticationException.(context: Context) -> String = ::defaultResourceResolver
) : Exception() {

    fun getMessage(context: Context) = builder(this, context)

    companion object {
        fun defaultResourceResolver(exception: AuthenticationException, context: Context): String {
            return context.getString(exception.resource)
        }
    }

}


data class Credentials(
    val username: String,
    val password: String,
    val disambiguationIdentity: String?
) {
    companion object {
        fun from(username: String?, password: String?, disambiguationIdentity: String?): Credentials? {
            return if (username != null && password != null) {
                Credentials(username, password, disambiguationIdentity)
            } else {
                null
            }
        }
    }
}
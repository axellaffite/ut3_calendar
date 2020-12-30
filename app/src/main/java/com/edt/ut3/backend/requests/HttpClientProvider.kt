package com.edt.ut3.backend.requests

import com.edt.ut3.backend.requests.authentication_services.Authenticator
import com.edt.ut3.backend.requests.authentication_services.CelcatAuthenticator
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit

/**
 * This class provides a builder for
 * a unified OkHttpClient over the application.
 *
 */
class HttpClientProvider {
    companion object {

        private var client: OkHttpClient? = null

        /**
         * This function provides a builder for an
         * unified OkHttpClient. It must be used all
         * over the application.
         * It sets multiple timeouts such as :
         *  - Connection timeout : 10s
         *  - Write timeout : 10s
         *  - Read timeout : 10s
         *
         * @return The OkHttpClient
         */
        fun generateNewClient(): OkHttpClient = synchronized(this) {
            if (client == null) {
                client = OkHttpClient.Builder()
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .writeTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(10, TimeUnit.SECONDS)
                    .cookieJar(CookieProvider)
                    .build()
            }

            client!!
        }


    }
}

val authMutex = Mutex()

/**
 * This function executes the provided [block] of code
 * authenticated with the provided [authenticator].
 * By default, the [authenticator] is a [CelcatAuthenticator].
 *
 * If the provided credentials are null the authentication will
 * not be performed.
 *
 * @param host The host to which the request must be authenticated.
 * @param authenticator The authenticator used to perform the authentication.
 * @param credentials The credentials used to perform the authentication.
 * @param block The block of code (the request) to execute while authenticated.
 * @return The result of the execution of the provided [block of code][block].
 */
@Throws(SocketTimeoutException::class, IOException::class, Authenticator.InvalidCredentialsException::class)
suspend fun<T> OkHttpClient.withAuthentication(
    host: HttpUrl,
    authenticator: CelcatAuthenticator = CelcatAuthenticator(),
    credentials: Authenticator.Credentials?,
    block: OkHttpClient.() -> T
): T {
    return authMutex.withLock {
        try {
            credentials?.let { authenticator.connect(host, credentials) }

            block(this)
        } finally {
            authenticator.disconnect(host)
        }
    }
}
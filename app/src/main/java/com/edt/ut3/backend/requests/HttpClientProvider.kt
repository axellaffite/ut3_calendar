package com.edt.ut3.backend.requests

import android.content.Context
import com.edt.ut3.backend.credentials.CredentialsManager
import com.edt.ut3.backend.requests.authentication_services.Authenticator
import com.edt.ut3.backend.requests.authentication_services.CelcatAuthenticator
import com.edt.ut3.misc.extensions.isNotNull
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
@Throws(SocketTimeoutException::class, IOException::class, Authenticator.InvalidCredentialsException::class)
suspend fun<T> OkHttpClient.withAuthentication(
    context: Context,
    host: HttpUrl,
    auth: CelcatAuthenticator = CelcatAuthenticator(),
    credentials: Authenticator.Credentials? = null,
    block: OkHttpClient.() -> T
): T {
    return authMutex.withLock {
        var err: Exception? = null
        var result: T? = null
        try {
            val cred = credentials ?: CredentialsManager.getInstance(context).getCredentials()
            if (cred.isNotNull()) {
                auth.connect(host, cred)
            }
            result = block(this)
        } catch (e: Exception) {
            err = e
        } finally {
            auth.disconnect(host)
        }

        if (err != null) {
            throw err
        }

        result!!
    }
}
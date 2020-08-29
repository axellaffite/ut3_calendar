package com.edt.ut3.backend.requests

import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * This class provides a builder for
 * a unified OkHttpClient over the application.
 *
 */
class HttpClientProvider {
    companion object {
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
        fun generateNewClient(): OkHttpClient = OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build()
    }
}
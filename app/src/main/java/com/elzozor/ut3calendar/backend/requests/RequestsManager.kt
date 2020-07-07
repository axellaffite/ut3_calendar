package com.elzozor.ut3calendar.backend.requests

import android.content.Context
import com.elzozor.ut3calendar.R
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory

class RequestsManager(private val context: Context) {
    private var httpClient = okhttp3.OkHttpClient()

    private fun retrofit() = Retrofit.Builder()
        .client(httpClient)
        .baseUrl(context.resources.getString(R.string.addr_celcat))
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    fun celcatService(): CelcatService = retrofit().create(CelcatService::class.java)
}
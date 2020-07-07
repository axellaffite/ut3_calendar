package com.edt.ut3.backend.requests

import android.content.Context
import com.edt.ut3.R
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class RequestsManager(private val context: Context) {
    private var httpClient = okhttp3.OkHttpClient()

    private fun retrofit() = Retrofit.Builder()
        .client(httpClient)
        .baseUrl(context.resources.getString(R.string.addr_celcat))
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    fun celcatService(): CelcatService = retrofit().create(CelcatService::class.java)
}
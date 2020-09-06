package com.edt.ut3.backend.requests

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ServiceBuilder {
    private val client = HttpClientProvider.generateNewClient()

    private val baseService = Retrofit.Builder()
        .addConverterFactory(GsonConverterFactory.create())
        .client(client)

    private val roomFinderService = baseService
        .baseUrl("https://rooms-finder.api.goulin.fr/")
        .build()

    fun buildRoomFinderService(): RoomFinderRequest =
        roomFinderService.create(RoomFinderRequest::class.java)
}
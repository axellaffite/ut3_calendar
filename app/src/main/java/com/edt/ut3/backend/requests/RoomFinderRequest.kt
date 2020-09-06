package com.edt.ut3.backend.requests

import com.edt.ut3.backend.goulin_room_finder.Building
import com.edt.ut3.backend.goulin_room_finder.Room
import retrofit2.http.GET
import retrofit2.http.Query
import java.io.IOException

interface RoomFinderRequest {

    @GET("/buildings")
    @Throws(IOException::class)
    suspend fun getBuildings(): List<Building>

    @GET("/")
    @Throws(IOException::class)
    suspend fun getFreeRooms(@Query("place") place: String) : List<Room>

}
package com.edt.ut3.backend.requests.room_finder

import com.edt.ut3.backend.goulin_room_finder.Building
import com.edt.ut3.backend.goulin_room_finder.Room
import com.edt.ut3.backend.requests.getClient
import io.ktor.client.*
import io.ktor.client.request.*

class RoomFinderService(val client: HttpClient = getClient()) {

    companion object {
        private const val SERVICE_URL = "https://rooms-finder-api.goulin.fr"
    }

    suspend fun getBuildings(): List<Building> {
        return client.get("$SERVICE_URL/buildings")
    }

    suspend fun getFreeRooms(place: String): List<Room> {
        return client.get("$SERVICE_URL/") {
            parameter("place", place)
        }
    }

}
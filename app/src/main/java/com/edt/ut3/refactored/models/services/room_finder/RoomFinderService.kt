package com.edt.ut3.refactored.models.services.room_finder

import com.edt.ut3.refactored.models.domain.room_finder.Building
import com.edt.ut3.refactored.models.domain.room_finder.Room
import com.edt.ut3.refactored.injected
import io.ktor.client.*
import io.ktor.client.request.*

private const val SERVICE_URL = "https://rooms-finder.api.goulin.fr"

class RoomFinderService(val client: HttpClient = injected()) {
    suspend fun getBuildings(): List<Building> {
        return client.get("$SERVICE_URL/buildings")
    }

    suspend fun getFreeRooms(place: String): List<Room> {
        return client.get("$SERVICE_URL/") {
            parameter("place", place)
        }
    }
}
package com.edt.ut3.ui.room_finder

import androidx.lifecycle.ViewModel
import com.edt.ut3.backend.goulin_room_finder.Building
import com.edt.ut3.backend.requests.ServiceBuilder
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext
import java.io.IOException

class RoomFinderViewModel : ViewModel() {

    private val service = ServiceBuilder.buildRoomFinderService()
    private  val buildings = mutableListOf<Building>()

    @Throws(IOException::class)
    suspend fun getBuildings(forceRefresh: Boolean = false) = withContext(IO) {
        if (buildings.isEmpty() || forceRefresh) {
            val newBuildings = service.getBuildings()
            buildings.clear()
            buildings.addAll(newBuildings)
        }

        buildings
    }

    @Throws(IOException::class)
    suspend fun getFreeRooms(building: String) = withContext(IO) { service.getFreeRooms(building) }

}
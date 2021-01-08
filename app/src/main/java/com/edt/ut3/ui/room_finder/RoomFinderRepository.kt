package com.edt.ut3.ui.room_finder

import com.edt.ut3.backend.goulin_room_finder.Building
import com.edt.ut3.backend.goulin_room_finder.Room
import com.edt.ut3.backend.requests.ServiceBuilder
import com.edt.ut3.misc.extensions.add
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.*

class RoomFinderRepository {

    private val service = ServiceBuilder.buildRoomFinderService()

    private val buildingMutex = Mutex()
    private val roomMutex = Mutex()

    private var _buildings: Set<Building>? = null

    private var selectedBuilding: Building? = null
    private var _freeRooms: List<Room>? = null
    private var nextRoomUpdate = Date()


    @Throws(IOException::class, SocketTimeoutException::class)
    suspend fun getBuildings(forceUpdate: Boolean) : Set<Building> = buildingMutex.withLock {
        if (_buildings.isNullOrEmpty() || forceUpdate) {
            _buildings = withContext(IO) {
                service.getBuildings().toSet()
            }
        }


        return _buildings!!
    }


    /**
     * TODO
     *
     * @param building
     * @return
     */
    @Throws(IOException::class, SocketTimeoutException::class)
    suspend fun getFreeRooms(building: Building): List<Room> = roomMutex.withLock {
        if (_freeRooms == null || selectedBuilding != building || nextRoomUpdate <= Date()) {
            _freeRooms = withContext(IO) {
                service.getFreeRooms(building.name)
            }

            selectedBuilding = building
            nextRoomUpdate = Date().add(Calendar.MINUTE, 2)
        }

        return _freeRooms!!
    }

}
package com.edt.ut3.ui.room_finder

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.edt.ut3.backend.goulin_room_finder.Building
import com.edt.ut3.backend.goulin_room_finder.Room
import com.edt.ut3.backend.requests.ServiceBuilder
import com.edt.ut3.misc.extensions.isNullOrFalse
import com.edt.ut3.ui.room_finder.RoomFinderState.Presentation
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException


class RoomFinderViewModel : ViewModel() {

    private var downloadJob: Job? = null
    private var searchJob: Job? = null

    private var rooms = listOf<Room>()
    private val service = ServiceBuilder.buildRoomFinderService()

    private val _buildings = MutableLiveData<Set<Building>>()
    val buildings : LiveData<Set<Building>>
        get() = _buildings

    val state = MutableLiveData<RoomFinderState>(Presentation)

    private val _error = MutableLiveData<RoomFinderFailure?>(null)
    val error : LiveData<RoomFinderFailure?>
        get() = _error

    private val _searchResult = MutableLiveData(listOf<Room>())
    val searchResult: LiveData<List<Room>>
        get() = _searchResult

    private val _searchBarText = MutableLiveData("")
    val searchBarText: LiveData<String>
        get() = _searchBarText

    var ready = false

    private val activatedFilters = hashSetOf<(List<Room>) -> List<Room>>()

    fun addFilter(filter: (List<Room>) -> List<Room>) {
        if (activatedFilters.add(filter)) {
            updateSearchResults()
        }
    }

    fun removeFilter(filter: (List<Room>) -> List<Room>) {
        if (activatedFilters.remove(filter)) {
            updateSearchResults()
        }
    }

    fun updateBarText(text: String) {
        if (text != _searchBarText.value) {
            _searchBarText.value = text
        }
    }

    fun selectBuilding(building: String) {
        updateBarText(building)
        updateSearchResults(true)
    }

    fun updateSearchResults(forceRefresh: Boolean = false) {
        val text = searchBarText.value
        if (text.isNullOrEmpty()) {
            return
        }

        state.value = RoomFinderState.Searching

        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            downloadJob?.join()

            try {
                val rooms = filterResult(getFreeRooms(searchBarText.value!!, forceRefresh))
                _searchResult.value = rooms
                state.value = RoomFinderState.Result
            } catch (e: IOException) {
                _error.value = RoomFinderFailure.SearchFailure
            }
        }
    }

    private fun filterResult(result: List<Room>) =
        activatedFilters.fold(result) { acc, roomFilter ->
            roomFilter(acc)
        }

    fun updateBuildingsData(forceRefresh: Boolean = false) = synchronized(this) {
        if (downloadJob?.isActive.isNullOrFalse()) {
            state.value = RoomFinderState.Downloading

            downloadJob = viewModelScope.launch {
                println("launching download")
                launchBuildingsDownload(forceRefresh, CoroutineExceptionHandler { _, _ ->
                    _error.value = RoomFinderFailure.UpdateBuildingFailure
                })?.join()
                state.value = Presentation
                ready = true
            }
        }
    }

    @Throws(IOException::class)
    suspend fun getFreeRooms(building: String, forceRefresh: Boolean = false) = withContext(IO) {
        if (rooms.isEmpty() || forceRefresh) {
            rooms = service.getFreeRooms(building)
        }

        rooms
    }

    private fun launchBuildingsDownload(
        forceRefresh: Boolean = false,
        handler: CoroutineExceptionHandler
    ): Job? {
        synchronized(this) {
            val dataShouldBeUpdated = (_buildings.value.isNullOrEmpty() || forceRefresh)
            val downloadNotActive = downloadJob?.isActive.isNullOrFalse()
            if (dataShouldBeUpdated && downloadNotActive) {
                downloadJob = viewModelScope.launch(handler) {
                    val newBuildings = service.getBuildings()
                    _buildings.value = newBuildings.toSet()
                }
            }

            return downloadJob
        }
    }

}
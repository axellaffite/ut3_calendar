package com.edt.ut3.ui.room_finder

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.edt.ut3.backend.goulin_room_finder.Building
import com.edt.ut3.backend.goulin_room_finder.Room
import com.edt.ut3.ui.room_finder.RoomFinderState.Presentation
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.IOException


class RoomFinderViewModel(private val repository: RoomFinderRepository) : ViewModel() {

    private var selectJob: Job? = null

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

    var ready = false

    private val activatedFilters = hashSetOf<(List<Room>) -> List<Room>>()

    private var selection: Building? = null

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

    private fun filterResult(result: List<Room>) =
        activatedFilters.fold(result) { acc, roomFilter ->
            roomFilter(acc)
        }


    fun updateSearchResults() {
        selection?.let { selection ->
            selectBuilding(selection)
        }
    }


    fun selectBuilding(building: Building) {
        selectJob?.cancel()
        selectJob = viewModelScope.launch {
            state.value = RoomFinderState.Searching

            try {
                val newRooms = repository.getFreeRooms(building)
                val filtered = filterResult(newRooms)
                selection = building

                _searchResult.value = filtered
                state.value = RoomFinderState.Result
            } catch (e: IOException) {
                e.printStackTrace()
                _error.value = RoomFinderFailure.SearchFailure
            }
        }
    }


    fun updateBuildingsData(forceUpdate: Boolean = false) = synchronized(this) {
        viewModelScope.launch {
            state.value = RoomFinderState.Downloading
            try {
                _buildings.value = repository.getBuildings(forceUpdate)
                state.value = Presentation
            } catch (e: Exception) {
                e.printStackTrace()
                _error.value = RoomFinderFailure.UpdateBuildingFailure
            }
        }
    }

}
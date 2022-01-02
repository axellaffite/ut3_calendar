package com.edt.ut3.refactored.viewmodels

import androidx.lifecycle.ViewModel
import com.edt.ut3.refactored.models.domain.maps.Place
import com.edt.ut3.refactored.models.repositories.database.AppDatabase
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext

class PlaceViewModel(database: AppDatabase): ViewModel() {

    private val dao = database.placeDao()

    suspend fun insert(place: Place) = dao.insert(place)

    suspend fun insertAll(places: List<Place>) = dao.insertAll(places)

    suspend fun delete(vararg place: Place) = dao.delete(*place)

    suspend fun selectAll() = dao.selectAll()

    fun selectAllLD() = dao.selectAllLD()

    suspend fun hasPlaces() = withContext(IO) { dao.hasPlaces() }

}
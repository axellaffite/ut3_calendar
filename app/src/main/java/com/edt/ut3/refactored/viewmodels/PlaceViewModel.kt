package com.edt.ut3.refactored.viewmodels

import androidx.lifecycle.ViewModel
import com.edt.ut3.refactored.models.domain.maps.Place
import com.edt.ut3.refactored.models.repositories.database.AppDatabase

class PlaceViewModel(database: AppDatabase): ViewModel() {

    private val dao = database.placeDao()

    suspend fun insert(vararg place: Place) = dao.insert(*place)

    suspend fun delete(vararg place: Place) = dao.delete(*place)

    suspend fun selectAll() = dao.selectAll()

    fun selectAllLD() = dao.selectAllLD()

}
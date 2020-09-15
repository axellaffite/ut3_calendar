package com.edt.ut3.backend.database.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import com.edt.ut3.backend.database.AppDatabase
import com.edt.ut3.backend.maps.Place

class PlaceViewModel(context: Context): ViewModel() {

    private val dao = AppDatabase.getInstance(context).placeDao()

    suspend fun insert(vararg place: Place) = dao.insert(*place)

    suspend fun delete(vararg place: Place) = dao.delete(*place)

    suspend fun selectAll() = dao.selectAll()

    fun selectAllLD() = dao.selectAllLD()

}
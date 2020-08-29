package com.edt.ut3.backend.database.viewmodels

import android.content.Context
import com.edt.ut3.backend.database.AppDatabase

class EventViewModel(context: Context) {

    private val dao = AppDatabase.getInstance(context).eventDao()

    suspend fun getEventsByIDs(vararg ids: String) = dao.selectByIDs(*ids)

    suspend fun getEvents() = dao.selectAll()

    fun getEventLD() = dao.selectAllLD()

}
package com.elzozor.ut3calendar.backend.database.viewmodels

import android.content.Context
import com.elzozor.ut3calendar.backend.database.AppDatabase

class EventViewModel(context: Context) {

    private val dao = AppDatabase.getInstance(context).eventDao()

    suspend fun getEventsByIDs(vararg ids: String) = dao.selectByIDs(*ids)

    suspend fun getEvents() = dao.selectAll()
}
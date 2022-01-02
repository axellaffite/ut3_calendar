package com.edt.ut3.refactored.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.edt.ut3.backend.note.Note
import com.edt.ut3.refactored.models.domain.EventWithNote
import com.edt.ut3.refactored.models.domain.celcat.Event
import com.edt.ut3.refactored.models.repositories.database.AppDatabase

class EventViewModel(database: AppDatabase): ViewModel() {

    private val dao = database.eventDao()

    suspend fun getEventsByIDs(vararg ids: String) = dao.selectByIDs(*ids)

    suspend fun getEvents() = dao.selectAll()

    fun getEventLD() = dao.selectAllLD()

    suspend fun insert(vararg events: Event) = dao.insert(*events)

    suspend fun delete(vararg events: Event) = dao.delete(*events)

    suspend fun deleteID(eventID: String) = dao.deleteID(eventID)

    suspend fun update(vararg events: Event) = dao.update(*events)


    fun listenToEventID(eventID: String?) = dao.listenToEventID(eventID)

    fun listenToEventWithNote(eventID: String?): LiveData<EventWithNote> = dao.listenToEventWithNote(eventID)

    suspend fun eventExists(eventID: String?) = dao.eventExists(eventID)

}
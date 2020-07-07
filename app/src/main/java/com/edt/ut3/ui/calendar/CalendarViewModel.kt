package com.edt.ut3.ui.calendar

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import com.edt.ut3.backend.celcat.Event
import com.edt.ut3.backend.database.viewmodels.EventViewModel

class CalendarViewModel : ViewModel() {
    private lateinit var events: LiveData<List<Event>>

    @Synchronized
    fun getEvents(context: Context) : LiveData<List<Event>> {
        if (!this::events.isInitialized) {
            events = liveData {
                val events = EventViewModel(context).getEvents()
                emit(events)
            }
        }

        return events
    }
}
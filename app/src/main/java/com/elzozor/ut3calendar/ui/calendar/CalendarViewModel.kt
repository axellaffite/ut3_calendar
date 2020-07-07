package com.elzozor.ut3calendar.ui.calendar

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import com.elzozor.ut3calendar.backend.celcat.Event
import com.elzozor.ut3calendar.backend.database.viewmodels.EventViewModel

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
package com.edt.ut3.ui.calendar

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import com.edt.ut3.backend.celcat.Event
import com.edt.ut3.backend.database.viewmodels.EventViewModel
import kotlinx.coroutines.Dispatchers.IO

class CalendarViewModel : ViewModel() {
    private lateinit var events: LiveData<List<Event>>

    @Synchronized
    fun getEvents(context: Context) : LiveData<List<Event>> {
        if (!this::events.isInitialized) {
            events = liveData(IO) {
                val evts = EventViewModel(context).getEvents()
                emit(evts)
            }
        }

        return events
    }
}
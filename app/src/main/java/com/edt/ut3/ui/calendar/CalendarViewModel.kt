package com.edt.ut3.ui.calendar

import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.edt.ut3.backend.celcat.Event
import com.edt.ut3.backend.database.viewmodels.EventViewModel
import com.edt.ut3.misc.timeCleaned
import java.util.*

class CalendarViewModel : ViewModel() {
    var selectedEvent: Event? = null
        set(value) {
            if (value == null) {
                selectedEventPictures.clear()
            }

            field = value
        }

    var selectedEventPictures = mutableListOf<Bitmap>()

    private lateinit var events: LiveData<List<Event>>
    var selectedDate = Date().timeCleaned()

    @Synchronized
    fun getEvents(context: Context) : LiveData<List<Event>> {
        if (!this::events.isInitialized) {
            events = EventViewModel(context).getEventLD()
        }

        return events
    }
}
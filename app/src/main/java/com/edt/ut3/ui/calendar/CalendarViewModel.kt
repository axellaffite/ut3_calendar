package com.edt.ut3.ui.calendar

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.edt.ut3.backend.celcat.Course
import com.edt.ut3.backend.celcat.Event
import com.edt.ut3.backend.database.viewmodels.CoursesViewModel
import com.edt.ut3.backend.database.viewmodels.EventViewModel
import com.edt.ut3.backend.note.Note
import com.edt.ut3.misc.timeCleaned
import java.lang.ref.WeakReference
import java.util.*

class CalendarViewModel : ViewModel() {

    var calendarMode = MutableLiveData(CalendarMode.DAY)
    var selectedEvent: Event? = null
    var selectedEventNote: Note? = null
    var selectedDate = MutableLiveData(Date().timeCleaned())
    var lastPosition = MutableLiveData(Int.MAX_VALUE / 2)
    val positions = mutableMapOf<Int, WeakReference<CalendarViewerFragment>>()

    private lateinit var coursesVisibility : LiveData<List<Course>>
    private lateinit var events: LiveData<List<Event>>

    @Synchronized
    fun getEvents(context: Context) : LiveData<List<Event>> {
        if (!this::events.isInitialized) {
            events = EventViewModel(context).getEventLD()
        }

        return events
    }

    @Synchronized
    fun getCoursesVisibility(context: Context): LiveData<List<Course>> {
        if (!this::coursesVisibility.isInitialized) {
            coursesVisibility = CoursesViewModel(context).getCoursesLD()
        }

        return coursesVisibility
    }
}
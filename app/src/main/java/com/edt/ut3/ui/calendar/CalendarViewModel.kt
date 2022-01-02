package com.edt.ut3.ui.calendar

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.edt.ut3.refactored.models.domain.celcat.Course
import com.edt.ut3.refactored.models.domain.celcat.Event
import com.edt.ut3.refactored.viewmodels.CoursesViewModel
import com.edt.ut3.refactored.viewmodels.EventViewModel
import com.edt.ut3.backend.note.Note
import com.edt.ut3.misc.extensions.timeCleaned
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.*

class CalendarViewModel : ViewModel(), KoinComponent {
    private val eventViewModel: EventViewModel by inject()
    private val coursesViewModel: CoursesViewModel by inject()

    var selectedEvent = MutableLiveData<Event>(null)
    var selectedEventNote: Note? = null
    var selectedDate = MutableLiveData(Date().timeCleaned())
    var lastPosition = MutableLiveData(Int.MAX_VALUE / 2)

    private lateinit var coursesVisibility : LiveData<List<Course>>
    private lateinit var events: LiveData<List<Event>>

    @Synchronized
    fun getEvents(): LiveData<List<Event>> {
        if (!this::events.isInitialized) {
            events = eventViewModel.getEventLD()
        }

        return events
    }

    @Synchronized
    fun getCoursesVisibility(): LiveData<List<Course>> {
        if (!this::coursesVisibility.isInitialized) {
            coursesVisibility = coursesViewModel.getCoursesLD()
        }

        return coursesVisibility
    }
}
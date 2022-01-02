package com.edt.ut3.refactored.viewmodels.event_details

import androidx.lifecycle.MutableLiveData

class EventDetailsCalendarSharedViewModel : IEventDetailsSharedViewModel() {
    override val event = MutableLiveData<String>()
    override var isSubFragmentShown: Boolean = false
}
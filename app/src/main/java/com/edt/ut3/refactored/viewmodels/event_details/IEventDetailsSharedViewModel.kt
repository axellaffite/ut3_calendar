package com.edt.ut3.refactored.viewmodels.event_details

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

abstract class IEventDetailsSharedViewModel: ViewModel() {
    abstract val event: MutableLiveData<String>
    abstract var isSubFragmentShown: Boolean
}
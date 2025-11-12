package com.edt.ut3.ui.notes

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.edt.ut3.backend.celcat.Event
import com.edt.ut3.backend.database.AppDatabase
import com.edt.ut3.backend.note.Note
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.stateIn
import kotlin.time.Duration.Companion.seconds

class FragmentNotesViewModel(application: Application) : AndroidViewModel(application) {
    private val noteDao = AppDatabase.getInstance(application).noteDao()

    val selectedEvent = MutableLiveData<Event>(null)
    var selectedNote = MutableLiveData<Note>(null)


    // LiveData from the DAO for observing notes
    val notesLD: StateFlow<List<Note>> = noteDao.getAllFlow().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5.seconds),
        initialValue = emptyList()
    )

    fun clearState() {
        selectedNote.value = null
        selectedEvent.value = null
    }
}
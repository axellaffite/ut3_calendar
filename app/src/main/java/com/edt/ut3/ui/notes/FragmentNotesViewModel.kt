package com.edt.ut3.ui.notes

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.edt.ut3.refactored.models.domain.celcat.Event
import com.edt.ut3.refactored.viewmodels.NotesViewModel
import com.edt.ut3.backend.note.Note
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class FragmentNotesViewModel : ViewModel(), KoinComponent {
    private val notesViewModel: NotesViewModel by inject()

    val selectedEvent = MutableLiveData<Event>(null)
    var selectedNote = MutableLiveData<Note>(null)
    private lateinit var notes: LiveData<List<Note>>

    fun getNotes(context: Context) : LiveData<List<Note>> {
        if (!this::notes.isInitialized) {
            notes = notesViewModel.getNotesLD()
        }

        return notes
    }

}
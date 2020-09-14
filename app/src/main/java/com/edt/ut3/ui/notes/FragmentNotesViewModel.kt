package com.edt.ut3.ui.notes

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.edt.ut3.backend.database.viewmodels.NotesViewModel
import com.edt.ut3.backend.note.Note

class FragmentNotesViewModel : ViewModel() {

    private lateinit var notes: LiveData<List<Note>>

    fun getNotes(context: Context) : LiveData<List<Note>> {
        if (!this::notes.isInitialized) {
            notes = NotesViewModel(context).getNotesLD()
        }

        return notes
    }

}
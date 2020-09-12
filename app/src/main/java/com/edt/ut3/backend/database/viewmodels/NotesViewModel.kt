package com.edt.ut3.backend.database.viewmodels

import android.content.Context
import com.edt.ut3.backend.database.AppDatabase
import com.edt.ut3.backend.note.Note

class NotesViewModel(context: Context) {

    private val dao = AppDatabase.getInstance(context).noteDao()

    fun getNotesLD() = dao.selectAllLD()

    suspend fun save(note: Note) {
        val ids = dao.insert(note)

        if (note.id == 0L) {
            note.id = ids[0]
        }
    }

    suspend fun delete(note: Note) {
        dao.delete(note)
        note.clearPictures()
    }

}
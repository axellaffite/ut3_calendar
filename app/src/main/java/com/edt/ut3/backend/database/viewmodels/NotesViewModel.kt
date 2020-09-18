package com.edt.ut3.backend.database.viewmodels

import android.content.Context
import com.edt.ut3.backend.database.AppDatabase
import com.edt.ut3.backend.note.Note
import com.edt.ut3.backend.notification.NotificationManager

class NotesViewModel(private val context: Context) {

    private val dao = AppDatabase.getInstance(context).noteDao()

    fun getNotesLD() = dao.selectAllLD()

    suspend fun save(note: Note) {
        if (note.isEmpty()) {
            delete(note)
        } else {
            val ids = dao.insert(note)

            if (note.id == 0L) {
                note.id = ids[0]
            }

            NotificationManager.getInstance(context).run {
                if (note.reminder.isActive()) {
                    create(note)
                } else {
                    delete(note)
                }
            }
        }
    }

    suspend fun delete(note: Note) {
        dao.delete(note)
        note.clearPictures()
        note.clearNotifications(context)
    }

}
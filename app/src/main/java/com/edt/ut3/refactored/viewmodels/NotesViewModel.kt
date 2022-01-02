package com.edt.ut3.refactored.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import com.edt.ut3.backend.note.Note
import com.edt.ut3.refactored.models.services.notifications.NotificationManagerService
import com.edt.ut3.refactored.injected
import com.edt.ut3.refactored.models.repositories.database.AppDatabase
import org.koin.core.component.KoinComponent

class NotesViewModel(database: AppDatabase): ViewModel(), KoinComponent {

    private val dao = database.noteDao()

    fun getNotesLD() = dao.selectAllLD()

    suspend fun getNotesByEventIDs(vararg eventIDs: String) = dao.selectByEventIDs(*eventIDs)

    fun getNoteByEventIDLD(eventID: String) = dao.selectByEventIDLD(eventID)

    suspend fun getNoteByID(id: Long) = dao.selectByID(id)

    suspend fun save(note: Note, notificationManagerService: NotificationManagerService = injected()) {
        if (note.isEmpty()) {
            Log.d(this::class.simpleName, "Note is empty")
            delete(note)
        } else {
            Log.d(this::class.simpleName, "Note isn't empty")

            val ids = dao.insert(note)

            if (note.id == 0L) {
                note.id = ids[0]
            }

            notificationManagerService.run {
                if (note.reminder.isActive()) {
                    this.createNoteSchedule(note)
                } else {
                    this.removeNoteSchedule(note)
                }
            }
        }
    }

    suspend fun delete(note: Note, notificationManagerService: NotificationManagerService = injected()) {
        dao.delete(note)
        note.clearPictures()
        note.clearNotifications(notificationManagerService)
    }

}
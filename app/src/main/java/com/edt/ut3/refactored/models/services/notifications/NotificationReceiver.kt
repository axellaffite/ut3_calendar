package com.edt.ut3.refactored.models.services.notifications

import android.app.Notification
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.edt.ut3.refactored.injected
import com.edt.ut3.refactored.viewmodels.NotesViewModel
import kotlinx.coroutines.runBlocking

class NotificationReceiver : BroadcastReceiver() {

    companion object {
        var NOTIFICATION_ID = "notification_id"
        var NOTIFICATION = "notification"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(this::class.simpleName, "Notification received")
        intent.getParcelableExtra<Notification>(NOTIFICATION)?.let { notification ->
            val notificationId = intent.getIntExtra(NOTIFICATION_ID, 0)
            val managerService: NotificationManagerService = injected()
            managerService.displayNoteNotification(notificationId, notification)

            runBlocking {
                val notesViewModel = injected<NotesViewModel>()
                notesViewModel.run {
                    val note = getNoteByID(notificationId.toLong())
                    note?.also {
                        it.reminder.disable()
                        save(it)
                    }
                }
            }
        } ?: run {
            Log.e(this::class.simpleName, "Received a notification without information")
        }
    }
}
package com.edt.ut3.backend.notification

import android.app.Notification
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import com.edt.ut3.backend.database.viewmodels.NotesViewModel
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class NotificationReceiver : BroadcastReceiver() {

    companion object {
        var NOTIFICATION_ID = "notification_id"
        var NOTIFICATION = "notification"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(this::class.simpleName, "Notification received")
        intent.getParcelableExtra<Notification>(NOTIFICATION)?.let { notification ->
            val notificationId = intent.getIntExtra(NOTIFICATION_ID, 0)
            NotificationManagerCompat.from(context).notify(notificationId, notification)

            GlobalScope.launch {
                NotesViewModel(context).run {
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
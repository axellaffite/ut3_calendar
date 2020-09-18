package com.edt.ut3.backend.notification

import android.app.Notification
import android.app.NotificationChannel
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.edt.ut3.R
import com.edt.ut3.backend.celcat.Event
import fr.anto.notificationscheduler.NotificationScheduler
import java.util.*


class NotificationManager private constructor(val context: Context) {
    companion object {

        @Volatile
        private var INSTANCE: NotificationManager? = null


        @Synchronized fun getInstance(context: Context): NotificationManager {
            if (INSTANCE == null) {
                INSTANCE = NotificationManager(context)
            }
            return INSTANCE!!
        }
    }

    private fun createUpdateNotificationChannel()
    {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            val name = "Changement de cours"
            val descriptionChannel = "Notification de changement dan l'emploi du temps"
            val channel : NotificationChannel = NotificationChannel(
                "UPDATE_EDT",
                name,
                android.app.NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = descriptionChannel
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }
            context.getSystemService(android.app.NotificationManager::class.java).createNotificationChannel(
                channel
            )
        }
    }

    fun createNewEventsNotification(events: List<Event>)
    {
        createUpdateNotificationChannel()
        events.map {
            val notification: Notification = NotificationCompat.Builder(context, "UPDATE_EDT")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(context.getString(R.string.new_event_added, it.courseName))
                .setContentText(   context.getString(
                    R.string.new_event_added_full, android.text.format.DateFormat.format(
                        "EEE dd MMM",
                        it.start
                    ), android.text.format.DateFormat.format("hh:mm", it.start)
                ))
                .setGroup("added_course")
                .build()
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.notify(Random().nextInt(), notification)
        }
    }
    fun createDeletedEventsNotification(events: List<Event>)
    {
        createUpdateNotificationChannel()
        events.map {
            val notification: Notification = NotificationCompat.Builder(context, "UPDATE_EDT")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(context.getString(R.string.event_deleted, it.courseName))
                .setContentText( context.getString(
                    R.string.new_event_added_full, android.text.format.DateFormat.format(
                        "EEE dd MMM",
                        it.start
                    ), android.text.format.DateFormat.format("hh:mm", it.start)
                ))
                .setGroup("removed_course")
                .build()
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.notify(Random().nextInt(), notification)
        }
    }
    fun createUpdatedEventsNotification(events: List<Event>)
    {
        createUpdateNotificationChannel()
        events.map {
            val notification: Notification = NotificationCompat.Builder(context, "UPDATE_EDT")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(context.getString(R.string.event_updated, it.courseName))
                .setContentText( context.getString(
                    R.string.event_updated_full, android.text.format.DateFormat.format(
                        "EEE dd MMM hh:mm",
                        it.start
                    )
                ))
                .setGroup("updated_course")
                .build()
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.notify(Random().nextInt(), notification)
        }

    }
}
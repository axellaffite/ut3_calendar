package com.edt.ut3.backend.notification

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.edt.ut3.MainActivity
import com.edt.ut3.R
import com.edt.ut3.backend.celcat.Event
import com.edt.ut3.backend.note.Note
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

    private fun createUpdateNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Changement de cours"
            val descriptionChannel = "Notification de changement dans l'emploi du temps"
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

    fun create(events: List<Event>, type: EventChange.Type) {
        createUpdateNotificationChannel()

        val notifications = events.map { event ->
            NotificationCompat.Builder(context, "UPDATE_EDT")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(generateEventNotificationTitle(event, type))
                .setContentText(generateEventNotificationText(event, type))
                .setGroup("UPDATE_EDT")
                .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_SUMMARY)
                .setGroupSummary(false)
                .build()
        }

        NotificationManagerCompat.from(context).apply {
            notifications.forEach {
                notify(Random().nextInt(), it)
            }
        }
    }

    fun displayUpdateGroup(added: Int, removed: Int, updated: Int) {
        if (added + removed + updated == 0) return

        val contents = StringBuilder()
        if (added > 0) contents.append(context.getString(R.string.new_event_added).format(added))
        if (removed > 0) contents.append(context.getString(R.string.event_deleted).format(removed))
        if (updated > 0) contents.append(context.getString(R.string.event_updated).format(updated))

        NotificationManagerCompat.from(context).apply {
            val summary = NotificationCompat.Builder(context, "UPDATE_EDT")
                .setContentTitle(context.getString(R.string.calendar_updated))
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentText(contents.toString())
                .setStyle(
                    NotificationCompat.InboxStyle()
                        .setBigContentTitle(context.getString(R.string.calendar_updated))
                )
                .setGroup("UPDATE_EDT")
                .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_SUMMARY)
                .setGroupSummary(true)
                .build()

            notify("SUMMARY".hashCode(), summary)
        }
    }

    private fun generateEventNotificationTitle(event: Event, type: EventChange.Type) = when (type) {
        EventChange.Type.ADDED -> context.getString(
            R.string.new_event_added, event.courseOrCategory(
                context
            )
        )
        EventChange.Type.REMOVED -> context.getString(
            R.string.event_deleted, event.courseOrCategory(
                context
            )
        )
        EventChange.Type.UPDATED -> context.getString(
            R.string.event_updated, event.courseOrCategory(
                context
            )
        )
    }

    private fun generateEventNotificationText(event: Event, type: EventChange.Type) = when(type) {
        EventChange.Type.ADDED -> context.getString(
            R.string.new_event_added_full, android.text.format.DateFormat.format(
                "EEE dd MMM",
                event.start
            ), android.text.format.DateFormat.format("HH:mm", event.start)
        )

        EventChange.Type.REMOVED -> context.getString(
            R.string.event_deleted_full, android.text.format.DateFormat.format(
                "EEE dd MMM",
                event.start
            ), android.text.format.DateFormat.format("HH:mm", event.start)
        )

        EventChange.Type.UPDATED -> context.getString(
            R.string.event_updated_full, android.text.format.DateFormat.format(
                "EEE dd MMM",
                event.start
            ), android.text.format.DateFormat.format("HH:mm", event.start)
        )
    }

    fun createNoteSchedule(note: Note) {
        if (!note.reminder.isActive()) {
            return
        }

        Log.d(this::class.simpleName, "Scheduling notification")
        scheduleNotification(note)
    }

    fun removeNoteSchedule(note: Note) {
        if (note.reminder.isActive()) {
            return
        }

        // TODO remove the note schedule
    }

    private fun scheduleNotification(note: Note) {
        val notificationId = note.id.toInt()

        val intent = Intent(context, MainActivity::class.java)
        val activity = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, "NOTE_NOTIFICATION")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(note.title)
            .setContentText(note.contents)
            .setAutoCancel(true)
            .setGroup("NOTE_NOTIFICATION")
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
            .setContentIntent(activity)
            .build()

        val notificationIntent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra(NotificationReceiver.NOTIFICATION_ID, notificationId)
            putExtra(NotificationReceiver.NOTIFICATION, notification)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId,
            notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        val time = note.reminder.getReminderDate()!!.time
        val secondsBeforeFiring = (note.reminder.getReminderDate()!!.time - System.currentTimeMillis()) / 1000

        Log.d(this::class.simpleName, "schedule: ${note.reminder.getReminderDate()}")
        Log.d(this::class.simpleName, "Second before firing: $secondsBeforeFiring")
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pendingIntent);
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, time, pendingIntent);
        }
    }
}
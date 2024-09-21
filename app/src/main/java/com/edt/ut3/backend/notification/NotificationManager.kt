package com.edt.ut3.backend.notification

import android.app.AlarmManager
import android.app.Notification
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
import com.google.firebase.messaging.RemoteMessage
import java.util.*


class NotificationManager private constructor(val context: Context) {

    companion object {
        private var INSTANCE: NotificationManager? = null

        fun getInstance(context: Context): NotificationManager = synchronized(this) {
            if (INSTANCE == null) {
                INSTANCE = NotificationManager(context)
            }

            INSTANCE!!
        }
    }

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    init {
        createReminderChannel()
        createUpdateChannel()
        createFirebaseChannel()
    }

    /**
     * This function ensure that the notification channel
     * has been created.
     * It's called when the [NotificationManager]
     * is created.
     *
     * @param info The channel information
     */
    private fun createUpdateNotificationChannel(info: NotificationChannelInformation) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                info.id,
                info.getTitle(context),
                info.importance
            )

            channel.description = info.getDescription(context)
            channel.lockscreenVisibility = info.visibility

            context.getSystemService(android.app.NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }


    /**
     * Ensure that the event updates
     * channel is created.
     */
    private fun createUpdateChannel() = createUpdateNotificationChannel(NotificationChannelInformation.UpdateChannel)


    /**
     * Ensure that the reminder
     * channel is created.
     */
    private fun createReminderChannel() = createUpdateNotificationChannel(NotificationChannelInformation.ReminderChannel)


    /**
     * Ensure that the firebase
     * channel is created.
     */
    private fun createFirebaseChannel() = createUpdateNotificationChannel(NotificationChannelInformation.FirebaseChannel)


    /**
     * Creates notifications for each [events] passed
     * to the function.
     *
     * @param events The events to notify
     * @param type The [type] of the events
     */
    private fun create(events: List<Event>, type: EventChange.Type) {
        val channel = NotificationChannelInformation.UpdateChannel

        val notifications = events.map { event ->
            NotificationCompat.Builder(context, channel.id)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(generateEventNotificationTitle(event, type))
                .setContentText(generateEventNotificationText(event, type))
                .setGroup(channel.id)
                .build()
        }

        NotificationManagerCompat.from(context).apply {
            notifications.forEach {
                notify(Random().nextInt(), it)
            }
        }
    }

    /**
     * Display the group notification.
     * Must be called before the notifications are displayed.
     *
     * @param added The new events
     * @param removed The removed events
     * @param updated The updated events
     */
    private fun displayUpdateGroup(added: Int, removed: Int, updated: Int) {
        val channel = NotificationChannelInformation.UpdateChannel
        if (added + removed + updated == 0) return

        val contents = StringBuilder()
        if (added > 0) contents.append(context.getString(R.string.new_event_added).format(added))
        if (removed > 0) contents.append(context.getString(R.string.event_deleted).format(removed))
        if (updated > 0) contents.append(context.getString(R.string.event_updated).format(updated))

        NotificationManagerCompat.from(context).apply {
            val summary = NotificationCompat.Builder(context, channel.id)
                .setContentTitle(context.getString(R.string.calendar_updated))
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentText(contents.toString())
                .setStyle(
                    NotificationCompat.InboxStyle()
                        .setBigContentTitle(context.getString(R.string.calendar_updated))
                )
                .setGroup(channel.id)
                .setGroupSummary(true)
                .build()

            notify(channel.summaryID, summary)
        }
    }


    fun notifyUpdates(added: List<Event>, removed: List<Event>, updated: List<Event>) {
        displayUpdateGroup(added.size, removed.size, updated.size)

        val modifications = listOf(
            added to EventChange.Type.ADDED,
            removed to EventChange.Type.REMOVED,
            updated to EventChange.Type.UPDATED
        )

        for ((events, type) in modifications) {
            if (events.isNotEmpty()) {
                create(events, type)
            }
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

        val notificationIntent = createNoteNotificationIntent()
        val notificationPendingIntent = createNoteNotificationPendingIntent(note, notificationIntent)


        alarmManager.cancel(notificationPendingIntent)
    }

    /**
     * Schedule a notification from
     * a note.
     *
     * @param note The note to schedule
     */
    private fun scheduleNotification(note: Note) {
        val channel = NotificationChannelInformation.ReminderChannel
        val notificationId = note.id.toInt()

        val intent = Intent(context, MainActivity::class.java)
        val activity = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channel.id)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(note.title)
            .setContentText(note.contents)
            .setAutoCancel(true)
            .setGroup(channel.id)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
            .setContentIntent(activity)
            .build()

        val notificationIntent = createNoteNotificationIntent().apply {
            putExtra(NotificationReceiver.NOTIFICATION_ID, notificationId)
            putExtra(NotificationReceiver.NOTIFICATION, notification)
        }

        val pendingIntent = createNoteNotificationPendingIntent(note, notificationIntent)

        val reminderDate = note.reminder.getReminderDate()!!
        val time = reminderDate.time
        val secondsBeforeFiring = (reminderDate.time - System.currentTimeMillis()) / 1000

        Log.d(this::class.simpleName, "schedule: ${note.reminder.getReminderDate()}")
        Log.d(this::class.simpleName, "Second before firing: $secondsBeforeFiring")

        when {
            Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms() -> {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pendingIntent)
                Log.d("NotificationManager", "Notification scheduled for $reminderDate")
            }

            else -> {
                Log.e("NotificationManager", "Notification not scheduled, no permission to do it")
            }
        }
    }

    private fun createNoteNotificationIntent() = Intent(context, NotificationReceiver::class.java)

    private fun createNoteNotificationPendingIntent(note: Note, notificationIntent: Intent): PendingIntent {
        return PendingIntent.getBroadcast(
            context,
            note.id.toInt(),
            notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun displayNoteNotification(id: Int, notification: Notification) {
        val channel = NotificationChannelInformation.ReminderChannel

        NotificationManagerCompat.from(context).run {
            val summary = NotificationCompat.Builder(context, channel.id)
                .setContentTitle(context.getString(R.string.personnal_note))
                .setSmallIcon(R.mipmap.ic_launcher)
                .setGroup(channel.id)
                .setGroupSummary(true)
                .build()

            notify(channel.summaryID, summary)
            notify(id, notification)
        }
    }

    fun displayFirebaseNotification(firebaseNotification: RemoteMessage.Notification) {
        val channel = NotificationChannelInformation.FirebaseChannel

        NotificationManagerCompat.from(context).run {
            val notification = NotificationCompat.Builder(context, channel.id)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(firebaseNotification.title)
                .setContentText(firebaseNotification.body)
                .setGroup(channel.id)
                .build()


            notify(Objects.hash(firebaseNotification.body, firebaseNotification.title), notification)
        }
    }
}
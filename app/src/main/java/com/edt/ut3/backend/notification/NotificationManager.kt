package com.edt.ut3.backend.notification

import android.content.Context
import android.text.format.DateFormat
import com.edt.ut3.R
import com.edt.ut3.backend.celcat.Event
import fr.anto.notificationscheduler.NotificationScheduler


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

    fun createNewEventsNotification(events: List<Event>)
    {
        events.map {
            NotificationScheduler.Builder(context).
            title(context.getString(R.string.new_event_added, it.courseName)).
            content(context.getString(R.string.new_event_added_full,android.text.format.DateFormat.format("EEE dd MMM",it.start),android.text.format.DateFormat.format("hh:mm",it.start))).
            large_icon(R.mipmap.ic_launcher).
            small_icon(R.mipmap.ic_launcher).
            group("added_course").
            build()
        }
    }
    fun createDeletedEventsNotification(events: List<Event>)
    {
        events.map {
            NotificationScheduler.Builder(context).
            title(context.getString(R.string.event_deleted, it.courseName)).
            content(context.getString(R.string.new_event_added_full,android.text.format.DateFormat.format("EEE dd MMM",it.start),android.text.format.DateFormat.format("hh:mm",it.start))).
            large_icon(R.mipmap.ic_launcher).
            small_icon(R.mipmap.ic_launcher).
            group("removed_course").
            build()
        }
    }
    fun createUpdatedEventsNotification(events: List<Event>)
    {
        events.map {
            NotificationScheduler.Builder(context).
            title(context.getString(R.string.event_updated, it.courseName)).
            content(context.getString(R.string.event_updated_full,android.text.format.DateFormat.format("EEE dd MMM hh:mm",it.start))).
            large_icon(R.mipmap.ic_launcher).
            small_icon(R.mipmap.ic_launcher).
            group("updated_course").
            build()
        }
    }
}
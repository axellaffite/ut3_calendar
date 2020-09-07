package com.edt.ut3.backend.notification

import android.content.Context
import com.edt.ut3.backend.celcat.Event

class NotificationManager private constructor(context : Context) {
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
    fun createNewEventsNotification(events : List<Event>)
    {
        //TODO Make notif apear
    }
    fun createDeletedEventsNotification(events : List<Event>)
    {
        //TODO Make notif apear
    }
    fun createUpdatedEventsNotification(events : List<Event>)
    {
        //TODO Make notif apear
    }
}
package com.edt.ut3.backend.firebase_services

import android.content.Context
import android.util.Log
import com.edt.ut3.backend.notification.NotificationManager
import com.edt.ut3.backend.preferences.PreferencesManager
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class FirebaseMessagingHandler: FirebaseMessagingService() {

    companion object {
        /**
         * Ensure that the device has been
         * registered into the proper groups.
         */
        fun ensureGroupRegistration(context: Context) {
            FirebaseMessaging.getInstance().run {
                val preferences = PreferencesManager.getInstance(context)
                val toSubscribe = preferences.groups
                val toUnsubscribe = preferences.oldGroups

                subscribeToTopic("everyone")

                toSubscribe?.forEach { group ->
                    subscribeToTopic(group)
                    Log.d("FIREBASE_NOTIFICATIONS", "$group subscribed !")
                }

                toUnsubscribe?.forEach { group ->
                    unsubscribeFromTopic(group).addOnSuccessListener {
                        synchronized(this) {
                            preferences.oldGroups = (preferences.oldGroups ?: listOf()) - group
                            Log.d("FIREBASE_NOTIFICATIONS", "$group unsubscribed !")
                        }
                    }
                }
            }
        }
    }

    /**
     * Display a notification when a message
     * os received from the firebase server.
     *
     * @param remoteMessage The incoming message from the
     * firebase server.
     */
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        remoteMessage.notification?.let { notification ->
            NotificationManager.getInstance(applicationContext)
                .displayFirebaseNotification(notification)
        }
    }

    /**
     * Used to handle a new token.
     * As it is not actually used, this
     * function simply returns Unit.
     *
     * @param token
     */
    override fun onNewToken(token: String) = Unit
}
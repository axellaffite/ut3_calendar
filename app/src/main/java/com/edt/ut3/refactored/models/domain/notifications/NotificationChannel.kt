package com.edt.ut3.refactored.models.domain.notifications

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat.IMPORTANCE_DEFAULT
import androidx.core.app.NotificationManagerCompat.IMPORTANCE_HIGH
import com.edt.ut3.R

sealed class NotificationChannelInformation (
    val id: String,
    val summaryID: Int,
    val importance: Int,
    val visibility: Int,
    private val title: Int,
    private val description: Int
) {

    fun getTitle(context: Context) = context.getString(title)
    fun getDescription(context: Context) = context.getString(description)

    object UpdateChannel : NotificationChannelInformation(
        id = "UPDATE_EDT",
        summaryID = 0,
        importance = IMPORTANCE_DEFAULT,
        visibility = NotificationCompat.VISIBILITY_PUBLIC,
        title = R.string.channel_course_title,
        description = R.string.channel_course_description
    )

    object ReminderChannel : NotificationChannelInformation(
        id = "REMINDER",
        summaryID = 1,
        importance = IMPORTANCE_HIGH,
        visibility = NotificationCompat.VISIBILITY_PUBLIC,
        title = R.string.channel_reminder_title,
        description = R.string.channel_reminder_description
    )

    object FirebaseChannel : NotificationChannelInformation(
        id = "FIREBASE",
        summaryID = 2,
        importance = IMPORTANCE_DEFAULT,
        visibility = NotificationCompat.VISIBILITY_PUBLIC,
        title = R.string.channel_firebase_title,
        description = R.string.channel_firebase_description
    )

}
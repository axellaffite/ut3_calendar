package com.edt.ut3.backend.notification

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
        "UPDATE_EDT",
        0,
        IMPORTANCE_DEFAULT,
        NotificationCompat.VISIBILITY_PUBLIC,
        R.string.channel_course_title,
        R.string.channel_course_description
    )

    object ReminderChannel : NotificationChannelInformation (
        "REMINDER",
        1,
        IMPORTANCE_HIGH,
        NotificationCompat.VISIBILITY_PUBLIC,
        R.string.channel_reminder_title,
        R.string.channel_reminder_description
    )

}
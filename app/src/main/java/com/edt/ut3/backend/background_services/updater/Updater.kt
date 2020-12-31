package com.edt.ut3.backend.background_services.updater

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.work.ListenableWorker
import com.edt.ut3.backend.preferences.PreferencesManager

interface Updater {

    fun getProgression(): LiveData<Int>

    fun getUpdateNotificationTitle(context: Context): String

    @Throws(Failure::class)
    suspend fun doUpdate(parameters: Parameters): ListenableWorker.Result

    data class Parameters(
        val firstUpdate: Boolean,
        val context: Context,
        val preferences: PreferencesManager
    )

    class Failure(
        reason: Int,
        val shouldBeNotified: Boolean
    ): Exception() {
        private val _reason: Int = reason

        fun getReason() = _reason

        fun getReasonMessage(context: Context) = context.getString(_reason)

    }

}
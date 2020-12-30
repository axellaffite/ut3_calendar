package com.edt.ut3.backend.request_refactoring.updater

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.work.ListenableWorker
import com.edt.ut3.backend.preferences.PreferencesManager

interface Updater {

    @Throws(Failure::class)
    suspend fun doUpdate(parameters: Parameters): ListenableWorker.Result

    fun getProgression(): LiveData<Int>

    data class Parameters(
        val firstUpdate: Boolean,
        val context: Context,
        val preferences: PreferencesManager
    )

    class Failure(reason: Int): Exception() {
        private val _reason: Int = reason

        fun getReason() = _reason

        fun getReasonMessage(context: Context) = context.getString(_reason)

    }

}
package com.edt.ut3.backend.background_services


import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.coroutineScope
import kotlin.time.ExperimentalTime

class Updater(appContext: Context, workerParams: WorkerParameters):
    CoroutineWorker(appContext, workerParams) {

    companion object {
        enum class Status {Nothing, Running, Success, Failure}
        var status = MutableLiveData<Status>(Status.Nothing)
    }

    private fun updateStatus(stat: Status) {
        status.postValue(stat)
    }

    @ExperimentalTime
    override suspend fun doWork(): Result = coroutineScope {
//        updateStatus(Status.Running)
//
//        val service = RequestsManager(applicationContext).celcatService()
//        val offset = (31*6).days // +- 6 months
//        val events = service.getEventsCelcatBody(
//            Date() + offset,
//            Date() - offset,
//            PreferencesManager(applicationContext).getGroups().toList()
//        )

        TODO("Put events into the database")
//        AppDatabase.getInstance(applicationContext).eventDao().insert(*events.toTypedArray())

        updateStatus(Status.Success)
        Result.success()
    }
}
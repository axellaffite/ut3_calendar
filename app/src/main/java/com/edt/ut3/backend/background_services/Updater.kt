package com.edt.ut3.backend.background_services


import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.edt.ut3.backend.celcat.Event
import com.edt.ut3.backend.database.AppDatabase
import com.edt.ut3.backend.requests.CelcatService
import com.edt.ut3.misc.toList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import kotlin.time.ExperimentalTime

class Updater(appContext: Context, workerParams: WorkerParameters):
    CoroutineWorker(appContext, workerParams) {

    companion object {
        const val Progress = "progress"
    }

    @ExperimentalTime
    override suspend fun doWork(): Result = coroutineScope {
        setProgress(workDataOf(Progress to 0))

        var result = Result.success()
        try {
            val groups = listOf("LINF6CMA") //PreferencesManager(applicationContext).getGroups().toList<String>()

            val events = withContext(Dispatchers.IO) {
                CelcatService().let { service ->
                    service.getEvents(groups).body()?.string() ?: throw IOException()
                }
            }

            Log.d("EVENTS", "Updater count1 : ${events.length}")

            setProgress(workDataOf(Progress to 50))

            val eventsArray = withContext(Dispatchers.Default) {
                JSONArray(events).toList<JSONObject>()
            }

            setProgress(workDataOf(Progress to 80))

            AppDatabase.getInstance(applicationContext).eventDao().insert(
                *eventsArray.map { Event.fromJSON(it) }.toTypedArray()
            )

            Log.d("EVENTS", "Updater count: ${eventsArray.size}")
        } catch (e: Exception) {
            TODO("Catch exceptions properly")
            when (e) {
                is IOException -> {}
                is JSONException -> {}
                else -> {}
            }

            e.printStackTrace()

            result = Result.failure()
        }

        setProgress(workDataOf(Progress to 100))
        result
    }
}
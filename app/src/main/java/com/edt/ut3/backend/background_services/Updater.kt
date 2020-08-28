package com.edt.ut3.backend.background_services


import android.content.Context
import android.util.Log
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.work.*
import com.edt.ut3.backend.celcat.Event
import com.edt.ut3.backend.database.AppDatabase
import com.edt.ut3.backend.preferences.PreferencesManager
import com.edt.ut3.backend.requests.CelcatService
import com.edt.ut3.misc.fromHTML
import com.edt.ut3.misc.map
import com.edt.ut3.misc.toList
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.time.ExperimentalTime

class Updater(appContext: Context, workerParams: WorkerParameters):
    CoroutineWorker(appContext, workerParams) {

    companion object {
        const val Progress = "progress"

        fun scheduleUpdate(context: Context) {
            val worker = PeriodicWorkRequestBuilder<Updater>(1, TimeUnit.HOURS).build()
            WorkManager.getInstance(context).let {
                it.enqueueUniquePeriodicWork("event_update", ExistingPeriodicWorkPolicy.KEEP, worker)
            }
        }

        fun forceUpdate(context: Context, viewLifecycleOwner: LifecycleOwner? = null, observer: Observer<WorkInfo>? = null) {
            val worker = OneTimeWorkRequestBuilder<Updater>().build()
            WorkManager.getInstance(context).let {
                it.enqueueUniqueWork("event_update_force", ExistingWorkPolicy.KEEP, worker)

                if (viewLifecycleOwner != null && observer != null) {
                    it.getWorkInfoByIdLiveData(worker.id).observe(viewLifecycleOwner, observer)
                }
            }
        }
    }

    @ExperimentalTime
    override suspend fun doWork(): Result = coroutineScope {
        setProgress(workDataOf(Progress to 0))

        var result = Result.success()
        try {
            val groups = PreferencesManager(applicationContext).getGroups().toList<String>()
            Log.d("UPDATER", "Downloading events for theses groups: $groups")

            val classes = getClasses().toHashSet()
            val courses = getCourses().toHashSet()

            println(classes)
            println(courses)


            val eventsJSONArray = withContext(IO) {
                JSONArray(
                CelcatService()
                        .getEvents(groups)
                        .body()
                        ?.string()
                        ?: throw IOException()
                )
            }

            setProgress(workDataOf(Progress to 50))

            val eventsArray = withContext(Default) {
                eventsJSONArray.map {
                    Event.fromJSON(it as JSONObject, classes, courses)
                }
            }
            Log.d("UPDATER", "Events count : ${eventsArray.size}")


            setProgress(workDataOf(Progress to 80))

            AppDatabase.getInstance(applicationContext).eventDao().insert(
                *eventsArray.toTypedArray()
            )
        } catch (e: Exception) {
            e.printStackTrace()
//            TODO("Catch exceptions properly")
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

    @Throws(IOException::class)
    private suspend fun getClasses() = withContext(IO) {
        val data = CelcatService().getClasses().body()?.string() ?: throw IOException()

        JSONObject(data).getJSONArray("results").map {
            (it as JSONObject).run { getString("id").fromHTML().trim() }
        }.also {
            println("Classes count : ${it.size}")
        }
    }

    private suspend fun getCourses() = withContext(IO) {
        val data = CelcatService().getCoursesNames().body()?.string() ?: throw IOException()

        JSONObject(data).getJSONArray("results").map {
            (it as JSONObject).run {
                val text = getString("text").fromHTML().trim()
                val id = getString("id").fromHTML().trim()
                "$text [$id]"
            }
        }.also {
            println("Courses count: ${it.size}")
        }
    }
}
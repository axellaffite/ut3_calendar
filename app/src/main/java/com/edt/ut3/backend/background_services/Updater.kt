package com.edt.ut3.backend.background_services


import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.edt.ut3.backend.celcat.Event
import com.edt.ut3.backend.database.AppDatabase
import com.edt.ut3.backend.requests.CelcatService
import com.edt.ut3.misc.fromHTML
import com.edt.ut3.misc.map
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.Dispatchers.IO
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
            (it as JSONObject).run { getString("text").fromHTML().trim() }
        }.also {
            println("Courses count: ${it.size}")
        }
    }
}
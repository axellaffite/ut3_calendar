package com.edt.ut3.backend.background_services


import android.content.Context
import android.util.Log
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.work.*
import androidx.work.impl.utils.PreferenceUtils
import com.edt.ut3.backend.celcat.Event
import com.edt.ut3.backend.database.AppDatabase
import com.edt.ut3.backend.notification.NotificationManager
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
                it.enqueueUniquePeriodicWork(
                    "event_update",
                    ExistingPeriodicWorkPolicy.KEEP,
                    worker
                )
            }
        }

        fun forceUpdate(
            context: Context,
            viewLifecycleOwner: LifecycleOwner? = null,
            observer: Observer<WorkInfo>? = null
        ) {
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

            val receivedEvent = withContext(Default) {
                eventsJSONArray.map {
                    Event.fromJSON(it as JSONObject, classes, courses)
                }
            }
            Log.d("UPDATER", "Events count : ${receivedEvent.size}")


            setProgress(workDataOf(Progress to 80))

            AppDatabase.getInstance(applicationContext).run{
                /* get all the event and their id before the update */
                val oldEvent: List<Event> = eventDao().selectAll()
                val oldEventID : List<String> = oldEvent.map { it -> it.id }
                /* get id of received events */
                val receivedEventID : List<String> = receivedEvent.map { it -> it.id }

                /*  Compute all events ID changes since last update */
                val newEventsID = receivedEventID.toList().toHashSet().apply{ removeAll(oldEventID) }
                val removedEventsID = oldEventID.toList().toHashSet().apply{ removeAll(
                    receivedEventID
                )}
                val updatedEventsID = receivedEventID.toList().toHashSet().apply { retainAll(
                    oldEventID
                ) }

                /* retrieve corresponding events from their id */
                val newEvents = receivedEvent.filter { newEventsID.contains(it.id) }
                val removedEvent = oldEvent.filter {removedEventsID.contains(it.id)}
                val updatedEvent = receivedEvent.filter { updatedEventsID.contains(it.id) }.toHashSet().apply{
                    removeAll(oldEvent)
                }.toList()

                /* write changes to database */
                eventDao().insert(*newEvents.toTypedArray())
                eventDao().delete(*removedEvent.toTypedArray())
                eventDao().update(*updatedEvent.toTypedArray())

                //TODO Also check if this is the first update
                if (PreferencesManager(applicationContext).isNotificationEnabled()) {
                    if(removedEvent.isNotEmpty()) {
                        NotificationManager.createDeletedEventsNotification(removedEvent)
                    }
                    if(newEvents.isNotEmpty()) {
                        NotificationManager.createNewEventsNotification(newEvents)
                    }
                    if(updatedEvent.isNotEmpty()) {
                        NotificationManager.createUpdatedEventsNotification(updatedEvent)
                    }
                }

                //TODO Mark the first update as made

            }
        } catch (e: Exception) {
            e.printStackTrace()
//            TODO("Catch exceptions properly")
            when (e) {
                is IOException -> {
                }
                is JSONException -> {
                }
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
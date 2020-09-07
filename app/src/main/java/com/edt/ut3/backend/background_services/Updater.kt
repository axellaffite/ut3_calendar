package com.edt.ut3.backend.background_services


import android.content.Context
import android.util.Log
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.work.*
import androidx.work.impl.utils.PreferenceUtils
import com.edt.ut3.backend.celcat.Course
import com.edt.ut3.backend.celcat.Event
import com.edt.ut3.backend.database.AppDatabase
import com.edt.ut3.backend.notification.NotificationManager
import com.edt.ut3.backend.database.viewmodels.CoursesViewModel
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


/**
 * Used to update the Calendar data in background.
 *
 * @param appContext The application context
 * @param workerParams The worker's parameters
 */
class Updater(appContext: Context, workerParams: WorkerParameters):
    CoroutineWorker(appContext, workerParams) {

    companion object {
        // Used to provide a progress value outside
        // of the Worker.
        const val Progress = "progress"

        /**
         * Schedule a periodic update.
         * If the periodic update is already launched
         * it is not replaced.
         *
         * @param context A valid context
         */
        fun scheduleUpdate(context: Context) {
            val worker = PeriodicWorkRequestBuilder<Updater>(1, TimeUnit.HOURS).build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork("event_update", ExistingPeriodicWorkPolicy.KEEP, worker)
        }

        /**
         * Force an update that will execute
         * almost directly after the function call.
         *
         * @param context A valid context
         * @param viewLifecycleOwner The fragment's viewLifeCycleOwner which will observe the Worker *optional*
         * @param observer An observer *optional*
         */
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
                        NotificationManager.getInstance(applicationContext).createDeletedEventsNotification(removedEvent)
                    }
                    if(newEvents.isNotEmpty()) {
                        NotificationManager.getInstance(applicationContext).createNewEventsNotification(newEvents)
                    }
                    if(updatedEvent.isNotEmpty()) {
                        NotificationManager.getInstance(applicationContext).createUpdatedEventsNotification(updatedEvent)
                    }
                }

                //TODO Mark the first update as made

            }
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

    /**
     * Returns the classes parsed properly.
     */
    @Throws(IOException::class)
    private suspend fun getClasses() = withContext(IO) {
        val data = CelcatService().getClasses().body()?.string() ?: throw IOException()

        JSONObject(data).getJSONArray("results").map {
            (it as JSONObject).run { getString("id").fromHTML().trim() }
        }
    }


    /**
     * Returns the courses parsed properly.
     */
    private suspend fun getCourses() = withContext(IO) {
        val data = CelcatService().getCoursesNames().body()?.string() ?: throw IOException()

        JSONObject(data).getJSONArray("results").map {
            (it as JSONObject).run {
                val text = getString("text").fromHTML().trim()
                val id = getString("id").fromHTML().trim()
                "$text [$id]"
            }
        }
    }

    /**
     * Update the course table which
     * defines which courses are visible
     * on the calendar.
     *
     * @param events The new event list
     */
    private suspend fun insertCoursesVisibility(events: List<Event>) {
        CoursesViewModel(applicationContext).run {
            val old = getCoursesVisibility().toMutableSet()
            val new = events.map { it.courseName }
                .toHashSet()
                .filterNotNull()
                .map { Course(it) }

            println(new)

            val titleToRemove = mutableListOf<String>()
            old.forEach { course ->
                new.find { it.title == course.title }?.let {
                    it.visible = course.visible
                } ?: run {
                    titleToRemove.add(course.title)
                }
            }

            println("Remove: $titleToRemove")
            println("Insert: $new")

            remove(*titleToRemove.toTypedArray())
            insert(*new.toTypedArray())
        }
    }
}
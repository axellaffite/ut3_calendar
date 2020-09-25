package com.edt.ut3.backend.background_services


import android.content.Context
import android.util.Log
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.work.*
import com.edt.ut3.backend.celcat.Course
import com.edt.ut3.backend.celcat.Event
import com.edt.ut3.backend.database.viewmodels.CoursesViewModel
import com.edt.ut3.backend.database.viewmodels.EventViewModel
import com.edt.ut3.backend.notification.EventChange
import com.edt.ut3.backend.notification.NotificationManager
import com.edt.ut3.backend.preferences.PreferencesManager
import com.edt.ut3.backend.requests.CelcatService
import com.edt.ut3.misc.fromHTML
import com.edt.ut3.misc.map
import com.edt.ut3.misc.timeCleaned
import com.edt.ut3.misc.toList
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.util.*
import java.util.concurrent.TimeUnit


/**
 * Used to update the Calendar data in background.
 *
 * @param appContext The application context
 * @param workerParams The worker's parameters
 */
@Suppress("BlockingMethodInNonBlockingContext")
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
        fun forceUpdate(context: Context, firstUpdate : Boolean = false, viewLifecycleOwner: LifecycleOwner? = null, observer: Observer<WorkInfo>? = null) {
            val inputData = Data.Builder().putBoolean("firstUpdate", firstUpdate).build()

            val worker = OneTimeWorkRequestBuilder<Updater>().setInputData(inputData).build()
            WorkManager.getInstance(context).let {
                it.enqueueUniqueWork("event_update_force", ExistingWorkPolicy.KEEP, worker)

                if (viewLifecycleOwner != null && observer != null) {
                    it.getWorkInfoByIdLiveData(worker.id).observe(viewLifecycleOwner, observer)
                }
            }
        }
    }

    private val prefManager = PreferencesManager.getInstance(applicationContext)


    override suspend fun doWork(): Result = coroutineScope {
        setProgress(workDataOf(Progress to 0))

        val firstUpdate = inputData.getBoolean("firstUpdate", false)
        val today = Date().timeCleaned()

        var result = Result.success()
        try {
            val groups = JSONArray(prefManager.groups).toList<String>()
            Log.d("UPDATER", "Downloading events for theses groups: $groups")

            val link = prefManager.link
                ?: throw IllegalStateException("Link must be set")

            val classes = getClasses(link).toHashSet()
            val courses = getCourses(link)

            Log.d(this::class.simpleName, classes.toString())
            Log.d(this::class.simpleName, courses.toString())


            val eventsJSONArray = withContext(IO) {
                JSONArray(
                CelcatService()
                        .getEvents(firstUpdate, link, groups)
                        .body
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


            EventViewModel(applicationContext).run {
                /* get all the event and their id before the update */
                val oldEvent: List<Event> = getEvents()
                val oldEventID = oldEvent.map { it.id }.toHashSet()
                val oldEventMap = oldEvent.map { it.id to it }.toMap()

                /* get id of received events */
                val receivedEventID = receivedEvent.map { it.id }.toHashSet()

                /*  Compute all events ID changes since last update */
                val newEventsID = receivedEventID - oldEventID
                val removedEventsID = oldEventID - receivedEventID
                val updatedEventsID = receivedEventID - newEventsID

                /* retrieve corresponding events from their id */
                val newEvents = receivedEvent.filter { newEventsID.contains(it.id) }
                val removedEvent = oldEvent.filter {
                    removedEventsID.contains(it.id)
                    && (firstUpdate || it.start > today)
                } + listOf(oldEvent.random(), oldEvent.random())

                val updatedEvent = receivedEvent.filter {
                    updatedEventsID.contains(it.id)
                    && it != oldEventMap[it.id]
                } + listOf(oldEvent.random(), oldEvent.random())

                /* write changes to database */
                insert(*newEvents.toTypedArray())
                delete(*removedEvent.toTypedArray())
                update(*updatedEvent.toTypedArray())

                val notificationEnabled = prefManager.notification
                val shouldDisplayNotifications = notificationEnabled && !firstUpdate
                if (shouldDisplayNotifications) {
                    val notificationManager = NotificationManager.getInstance(applicationContext)

                    if(removedEvent.isNotEmpty()) {
                        notificationManager.create(removedEvent, EventChange.Type.REMOVED)
                    }

                    if(newEvents.isNotEmpty()) {
                        notificationManager.create(newEvents, EventChange.Type.ADDED)
                    }

                    if(updatedEvent.isNotEmpty()) {
                        notificationManager.create(updatedEvent, EventChange.Type.UPDATED)
                    }

                    notificationManager.displayUpdateGroup(newEvents.size, removedEvent.size, updatedEvent.size)
                }

                insertCoursesVisibility(receivedEvent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
//            TODO("Catch exceptions properly")
            when (e) {
                is IOException -> {}
                is JSONException -> {}
                is IllegalStateException -> {}
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
    private suspend fun getClasses(link: String) = withContext(IO) {
        val data = CelcatService().getClasses(link).body?.string() ?: throw IOException()
        JSONObject(data).getJSONArray("results").map {
            (it as JSONObject).run { getString("id").fromHTML().trim() }
        }
    }


    /**
     * Returns the courses parsed properly.
     */
    @Throws(IOException::class)
    private suspend fun getCourses(link: String) = withContext(IO) {
        val data = CelcatService().getCoursesNames(link).body?.string() ?: throw IOException()


        JSONObject(data).getJSONArray("results").map {
            (it as JSONObject).run {
                val text = getString("text").fromHTML().trim()
                val id = getString("id").fromHTML().trim()
                listOf(id to text, text to text)
            }
        }.flatten().toMap()
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

            Log.d(this::class.simpleName, new.toString())

            val titleToRemove = mutableListOf<String>()
            old.forEach { oldCourse ->
                new.find { it.title == oldCourse.title }?.let { it.visible = oldCourse.visible }
                    ?: run { titleToRemove.add(oldCourse.title) }
            }

            Log.d(this::class.simpleName, "Remove: $titleToRemove")
            Log.d(this::class.simpleName, "Insert: $new")

            remove(*titleToRemove.toTypedArray())
            insert(*new.toTypedArray())
        }
    }
}
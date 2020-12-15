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
import com.edt.ut3.backend.formation_choice.School
import com.edt.ut3.backend.notification.NotificationManager
import com.edt.ut3.backend.preferences.PreferencesManager
import com.edt.ut3.backend.requests.authentication_services.Authenticator
import com.edt.ut3.backend.requests.celcat.CelcatService
import com.edt.ut3.misc.extensions.timeCleaned
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import org.json.JSONException
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.properties.Delegates


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
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "event_update",
                ExistingPeriodicWorkPolicy.KEEP,
                worker
            )
        }

        /**
         * Force an update that will execute
         * almost directly after the function call.
         *
         * @param context A valid context
         * @param viewLifecycleOwner The fragment's viewLifeCycleOwner
         * which will observe the Worker *optional*
         * @param observer An observer *optional*
         */
        fun forceUpdate(
            context: Context,
            firstUpdate : Boolean = false,
            viewLifecycleOwner: LifecycleOwner? = null,
            observer: Observer<WorkInfo>? = null
        ) {
            val inputData = Data.Builder().putBoolean("firstUpdate", firstUpdate).build()

            val worker = OneTimeWorkRequestBuilder<Updater>().setInputData(inputData).build()
            WorkManager.getInstance(context).run {
                enqueueUniqueWork("event_update_force", ExistingWorkPolicy.KEEP, worker)

                if (viewLifecycleOwner != null && observer != null) {
                    getWorkInfoByIdLiveData(worker.id).observe(viewLifecycleOwner, observer)
                }
            }
        }
    }

    private val prefManager = PreferencesManager.getInstance(applicationContext)
    private val eventViewModel = EventViewModel(applicationContext)
    private var firstUpdate by Delegates.notNull<Boolean>()
    private val today = Date().timeCleaned()

    override suspend fun doWork(): Result = coroutineScope {
        setProgress(workDataOf(Progress to 0))

        firstUpdate = inputData.getBoolean("firstUpdate", false)

        var result = Result.success()
        try {
            val groups = prefManager.groups ?: throw IllegalStateException("Groups must be set")
            val link = prefManager.link ?: throw IllegalStateException("Link must be set")

            val classes = getClasses(link.rooms).toHashSet()
            val courses = getCourses(link.courses)
            val incomingEvents = getEvents(link, groups, classes, courses)

            Log.d("Updater", courses.toString())

            val changes = computeEventUpdate(incomingEvents)

            updateDatabaseContents(changes)
            displayUpdateNotifications(changes)
            insertCoursesVisibility(eventViewModel.getEvents())
        } catch (e: Exception) {
            e.printStackTrace()
            //TODO ("Catch exceptions properly")
            when (e) {
                is IOException -> {}
                is SerializationException -> {}
                is Authenticator.InvalidCredentialsException -> {}
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
     * Retrieves the incoming events
     * and parse them into a list of [Event].
     *
     * @param link The link from which the events should be downloaded
     * @param groups The groups to download
     * @param classes All the classes available
     * @param courses All the courses available
     */
    @Throws(
        JSONException::class,
        SocketTimeoutException::class,
        IOException::class,
        Authenticator.InvalidCredentialsException::class
    )
    private suspend fun getEvents(
        link: School.Info,
        groups: List<String>,
        classes: HashSet<String>,
        courses: Map<String, String>
    ) = withContext(Default) {
        val eventsJSONArray = withContext(IO) {
            CelcatService
                .getEvents(applicationContext, firstUpdate, link.url, groups)
                .body
                ?.string()
        } ?: throw IOException()

        Json.parseToJsonElement(eventsJSONArray).jsonArray.fold(listOf<Event>()) { acc, e ->
            try {
                acc + Event.fromJSON(e.jsonObject, classes, courses)
            } catch (e: Exception) {
                e.printStackTrace()
                acc
            }
        }
    }

    /**
     * Compute the differences between the local database
     * and the incoming [events][Event].
     *
     * @param incomingEvents
     * @return A class containing all the update computed
     * from the incoming events.
     */
    private suspend fun computeEventUpdate(
        incomingEvents: List<Event>
    ) : EventChanges = withContext(IO) {
        /* get all the event and their id before the update */
        val oldEvent: List<Event> = eventViewModel.getEvents()
        val oldEventID = oldEvent.map { it.id }.toHashSet()
        val oldEventMap = oldEvent.map { it.id to it }.toMap()

        /* get id of received events */
        val receivedEventID = incomingEvents.map { it.id }.toHashSet()

        /*  Compute all events ID changes since last update */
        val newEventsID = receivedEventID - oldEventID
        val removedEventsID = oldEventID - receivedEventID
        val updatedEventsID = receivedEventID - newEventsID

        /* retrieve corresponding events from their id */
        val newEvents = incomingEvents.filter { newEventsID.contains(it.id) }
        val removedEvent = oldEvent.filter {
            removedEventsID.contains(it.id)
                    && (firstUpdate || it.start > today)
        }

        val updatedEvent = incomingEvents.filter {
            updatedEventsID.contains(it.id)
                    && it != oldEventMap[it.id]
        }

        EventChanges(newEvents, updatedEvent, removedEvent)
    }

    /**
     * Update the current database with the new data.
     *
     * @param changes The computed changes
     */
    private suspend fun updateDatabaseContents(changes: EventChanges) = eventViewModel.run {
        insert(*changes.new.toTypedArray())
        delete(*changes.deleted.toTypedArray())
        update(*changes.updated.toTypedArray())
    }

    private fun displayUpdateNotifications(changes: EventChanges) {
        val notificationEnabled = prefManager.notification
        val shouldDisplayNotifications = notificationEnabled && !firstUpdate
        if (shouldDisplayNotifications) {
            val notificationManager = NotificationManager.getInstance(applicationContext)

            notificationManager.notifyUpdates(
                added = changes.new,
                removed = changes.deleted,
                updated = changes.updated
            )
        }
    }

    /**
     * Returns the classes parsed properly.
     */
    @Throws(IOException::class)
    private suspend fun getClasses(link: String) = withContext(IO) {
        CelcatService.getClasses(applicationContext, link)
    }


    /**
     * Returns the courses parsed properly.
     */
    @Throws(IOException::class)
    private suspend fun getCourses(link: String): Map<String, String> = withContext(IO) {
        CelcatService.getCoursesNames(applicationContext, link)
    }

    /**
     * Update the course table which
     * defines which courses are visible
     * on the calendar.
     *
     * @param events The new event list
     */
    private suspend fun insertCoursesVisibility(events: List<Event>) {
        val vm = CoursesViewModel(applicationContext)

        val old = vm.getCoursesVisibility().toMutableSet()
        val new = events.map { it.courseName }
            .toHashSet()
            .filterNotNull()
            .map { Course(it) }

        Log.d(this::class.simpleName, new.toString())

        val titleToRemove = mutableListOf<String>()
        old.forEach { oldCourse ->
            new.find { it.title == oldCourse.title }
                ?.let { it.visible = oldCourse.visible }
                ?: run { titleToRemove.add(oldCourse.title) }
        }

        vm.run {
            remove(*titleToRemove.toTypedArray())
            insert(*new.toTypedArray())
        }
    }


    private data class EventChanges (
        val new: List<Event>,
        val updated: List<Event>,
        val deleted: List<Event>
    )
}
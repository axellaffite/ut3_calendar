package com.edt.ut3.backend.background_services.updater.celcat

import android.content.Context
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.work.ListenableWorker
import com.edt.ut3.R
import com.edt.ut3.backend.celcat.Course
import com.edt.ut3.backend.celcat.Event
import com.edt.ut3.backend.database.viewmodels.CoursesViewModel
import com.edt.ut3.backend.database.viewmodels.EventViewModel
import com.edt.ut3.backend.formation_choice.School
import com.edt.ut3.backend.notification.NotificationManager
import com.edt.ut3.backend.preferences.PreferencesManager
import com.edt.ut3.backend.background_services.updater.Updater
import com.edt.ut3.backend.requests.authentication_services.Authenticator
import com.edt.ut3.backend.requests.celcat.CelcatService
import com.edt.ut3.misc.extensions.timeCleaned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.*

class CelcatUpdater: Updater {

    private val _progression = MutableLiveData(0)

    @Throws(Updater.Failure::class)
    override suspend fun doUpdate(parameters: Updater.Parameters): ListenableWorker.Result {
        val (firstUpdate, context, preferences) = parameters

        val groups = parameters.preferences.groups
            ?: throw Updater.Failure(R.string.error_groups_not_configured)

        val link = parameters.preferences.link
            ?: throw Updater.Failure(R.string.error_link_not_configured)

        val classes = getClasses(context, link.rooms)
        val courses = getCourses(context, link.courses)
        val incomingEvents = getEvents(
            firstUpdate = firstUpdate,
            link = link,
            groups = groups,
            classes = classes,
            courses = courses
        )

        val changes = computeEventUpdate(
            incomingEvents = incomingEvents,
            firstUpdate = firstUpdate,
            context = context
        )

        updateDatabaseContents(context, changes)
        displayUpdateNotifications(preferences, firstUpdate, context, changes)
        insertCoursesVisibilities(context, EventViewModel(context).getEvents())

        return ListenableWorker.Result.success()
    }

    override fun getProgression() = _progression

    /**
     * Returns the classes parsed properly.
     */
    @Throws(Updater.Failure::class)
    private suspend fun getClasses(context: Context, link: String): HashSet<String> {
        return try {
            val classes = withContext(Dispatchers.IO) {
                CelcatService.getClasses(link).toHashSet()
            }

            classes
        } catch (e: IOException) {
            throw Updater.Failure(R.string.error_check_internet)
        } catch (e: Exception) {
            throw Updater.Failure(R.string.error_get_classes)
        }
    }

    /**
     * Returns the courses parsed properly.
     */
    @Throws(IOException::class)
    private suspend fun getCourses(context: Context, link: String): Map<String, String> {
        return try {
            val coursesNames = withContext(Dispatchers.IO) {
                CelcatService.getCoursesNames(link)
            }

            coursesNames
        } catch (e: IOException) {
            throw Updater.Failure(R.string.error_check_internet)
        } catch (e: Exception) {
            throw Updater.Failure(R.string.error_get_courses)
        }
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
    @Throws(Updater.Failure::class)
    private suspend fun getEvents(
        firstUpdate: Boolean,
        link: School.Info,
        groups: List<String>,
        classes: HashSet<String>,
        courses: Map<String, String>,
    ) : List<Event> {
        return try {
            val eventsJSONArray = withContext(Dispatchers.IO) {
                CelcatService.run {
                    getEvents(firstUpdate, link.url, groups).body
                }?.string()
            } ?: throw IOException()

            val parsedEvent = withContext(Dispatchers.Default) {
                Json.parseToJsonElement(eventsJSONArray).jsonArray.fold(listOf<Event>()) { acc, e ->
                    try {
                        acc + Event.fromJSON(e.jsonObject, classes, courses)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        acc
                    }
                }
            }

            parsedEvent
        } catch (e: SocketTimeoutException) {
            throw Updater.Failure(R.string.error_check_internet)
        } catch (e: SerializationException) {
            throw Updater.Failure(R.string.error_parse_events)
        } catch (e: Authenticator.InvalidCredentialsException) {
            throw Updater.Failure(R.string.error_wrong_credentials)
        } catch (e: Exception) {
            throw Updater.Failure(R.string.error_updater_unknown)
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
        incomingEvents: List<Event>,
        firstUpdate: Boolean,
        context: Context
    ) : EventChanges = withContext(Dispatchers.IO) {
        /* get all the event and their id before the update */
        val oldEvent: List<Event> = EventViewModel(context).getEvents()
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
        val today = Date().timeCleaned()
        val removedEvent = oldEvent.filter { event ->
            removedEventsID.contains(event.id)
                && (firstUpdate || event.start > today)
        }

        val updatedEvent = incomingEvents.filter { event ->
            updatedEventsID.contains(event.id)
                && event != oldEventMap[event.id]
        }

        EventChanges(newEvents, updatedEvent, removedEvent)
    }

    /**
     * Update the current database with the new data.
     *
     * @param changes The computed changes
     */
    private suspend fun updateDatabaseContents(context: Context, changes: EventChanges) {
        EventViewModel(context).run {
            insert(*changes.new.toTypedArray())
            delete(*changes.deleted.toTypedArray())
            update(*changes.updated.toTypedArray())
        }
    }

    private fun displayUpdateNotifications(
        preferencesManager: PreferencesManager,
        firstUpdate: Boolean,
        context: Context,
        changes: EventChanges
    ) {
        val notificationEnabled = preferencesManager.notification
        val shouldDisplayNotifications = notificationEnabled && !firstUpdate
        if (shouldDisplayNotifications) {
            val notificationManager = NotificationManager.getInstance(context)

            notificationManager.notifyUpdates(
                added = changes.new,
                removed = changes.deleted,
                updated = changes.updated
            )
        }
    }

    /**
     * Update the course table which
     * defines which courses are visible
     * on the calendar.
     *
     * @param events The new event list
     */
    private suspend fun insertCoursesVisibilities(context: Context, events: List<Event>) {
        val eventViewModel = CoursesViewModel(context)

        val oldVisibilities = eventViewModel.getCoursesVisibility().toMutableSet()
        val new = events
            .map { it.courseName }
            .toHashSet()
            .filterNotNull()
            .map { Course(it) }

        Log.d(this::class.simpleName, new.toString())

        val titleToRemove = mutableListOf<String>()
        for (old in oldVisibilities) {
            val corresponding = new.find { it.title == old.title }

            if (corresponding != null) {
                corresponding.visible = old.visible
            } else {
                titleToRemove.add(old.title)
            }
        }

        eventViewModel.run {
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
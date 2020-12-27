package com.edt.ut3.backend.calendar

import android.content.Context
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.edt.ut3.backend.celcat.Course
import com.edt.ut3.backend.celcat.CourseStatusData
import com.edt.ut3.backend.celcat.Event
import com.edt.ut3.backend.preferences.PreferencesManager
import com.edt.ut3.misc.extensions.add
import com.edt.ut3.misc.extensions.timeCleaned
import com.elzozor.yoda.Day
import com.elzozor.yoda.Week
import com.elzozor.yoda.events.EventWrapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*

/**
 * Used to build the [Day] view given
 * a bunch of events and parameters.
 *
 * @property eventList The event list to display
 * @property context A valid context to build the views
 * @property currentDate The actual date to which the Calendar is set.
 * @property currentPosition The current pager position
 * @property dayPosition The position of the [Day] view that will be built
 * @property container The Layout that will contain the day
 * @property courseVisibility The [courses][Course] visibilities
 * @property dayBuilder The function to build a [Day] view
 * @property emptyDayBuilder The function to build an empty [Day] view
 * @property allDayBuilder The function to build an all [Day] view
 */
class DayBuilder(
    private val eventList: List<Event>?,
    private val context: Context,
    private val currentDate: Date,
    private val currentPosition: Int,
    private val dayPosition: Int,
    private val container: FrameLayout,
    private val courseVisibility: List<CourseStatusData>,
    private val dayBuilder: (context: Context, eventWrapper: EventWrapper, x: Int, y: Int, w:Int, h: Int) -> Pair<Boolean, View>,
    private val emptyDayBuilder: () -> View,
    private val allDayBuilder: (events: List<EventWrapper>) -> View,
) {

    private val preferences = PreferencesManager.getInstance(context)

    /**
     * This function handle when a new bunch of
     * events are available.
     * It calls the [filterEvents] function which
     * will filter the events and then call the
     * [buildDayView] function that display them.
     */
    suspend fun build(height: Int, width: Int) {
        if (eventList == null) {
            Log.d("Builder", "event list is null, aborting")
            return
        }

        val calendarMode = preferences.calendarMode
        when {
            calendarMode.isAgenda() ->
                buildDayView(container, eventList, height, width)
            else ->
                buildWeekView(container, eventList, height, width)
        }
    }


    /**
     * Build a day view for the
     * [AGENDA][CalendarMode.Mode.AGENDA] mode.
     *
     * @param container The layout that will contain the resulting view
     * @param eventList The event list to display
     * @param height The resulting view height
     * @param width The resulting view width
     */
    private suspend fun buildDayView(container: FrameLayout, eventList: List<Event>, height: Int, width: Int) = withContext(
        Dispatchers.Default
    ) {
        val selectedDate = currentDate.add(Calendar.DAY_OF_YEAR, dayPosition - currentPosition)
        val filter = { it: Event -> it.start > selectedDate && it.start < selectedDate.add(Calendar.DAY_OF_YEAR, 1) }
        val filtered = filterEvents(eventList, filter)


        val eventContainer = withContext(Dispatchers.Main) {
            buildEventContainer(container, { Day(context, null) }) {
                it.apply {
                    dayBuilder = this@DayBuilder.dayBuilder
                    allDayBuilder = this@DayBuilder.allDayBuilder
                    emptyDayBuilder = this@DayBuilder.emptyDayBuilder

                    hoursMode = Day.HoursMode.COMPLETE_H
                    fit = Day.Fit.BOUNDS_ADAPTIVE
                    displayMode = Day.Display.FIT_TO_CONTAINER
                    start = 7
                    end = 20

                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            }
        }

        eventContainer.setEvents(filtered, height, width)
        withContext(Dispatchers.Main) {
            container.post {
                if (eventContainer.parent == null) {
                    container.addView(eventContainer)
                }
            }
        }
    }


    /**
     * Build a day view for the
     * [AGENDA][CalendarMode.Mode.WEEK] mode.
     *
     * @param container The layout that will contain the resulting view
     * @param eventList The event list to display
     * @param height The resulting view height
     * @param width The resulting view width
     */
    private suspend fun buildWeekView(container: FrameLayout, eventList: List<Event>, height: Int, width: Int) = withContext(
        Dispatchers.Default
    ) {
        Log.d("DayBuilder", "Constructing week view")

        val date = currentDate.add(Calendar.DAY_OF_YEAR, (dayPosition - currentPosition) * 7)

        val selectedDate = Calendar.getInstance().run {
            time = date.timeCleaned()
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            time
        }

        val filter = { it: Event ->
            it.start > selectedDate
            && it.start < selectedDate.add(Calendar.WEEK_OF_YEAR, 1)
        }

        val filtered = filterEvents(eventList, filter)

        val eventContainer = withContext(Dispatchers.Main) {
            buildEventContainer(container, { Week(context, null) }) {
                it.apply {
                    dayBuilder = this@DayBuilder.dayBuilder
                    allDayBuilder = this@DayBuilder.allDayBuilder
                    emptyDayBuilder = this@DayBuilder.emptyDayBuilder

                    hoursMode = Day.HoursMode.SIMPLE
                    displayMode = Day.Display.FIT_TO_CONTAINER
                    start = 7
                    end = 20

                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            }
        }

        eventContainer.setEvents(selectedDate, filtered, height, width)
        withContext(Dispatchers.Main) {
            container.post {
                container.removeAllViews()
                container.addView(eventContainer)
                container.requestLayout()
            }
        }
    }

    /**
     * Generic function to build the Event container.
     * It will remove all the views into the container
     * before calling the [initializer] function
     *
     * @param container The container layout
     * @param builder The builder
     * @param initializer
     */
    private suspend inline fun<reified T: View> buildEventContainer(
        container: FrameLayout,
        crossinline builder: () -> T,
        crossinline initializer: (T) -> T
    ) = withContext(Dispatchers.Main) {
        container.removeAllViews()
        initializer(builder())
    }


    /**
     * This function filter the events given
     * the courses visibilities and the [dateFilter].
     *
     *
     * @param eventList The event list to filter
     * @param dateFilter A filter that must filter the event
     * with the given date
     */
    private suspend fun filterEvents(eventList: List<Event>, dateFilter: (Event) -> Boolean) = withContext(
        Dispatchers.Default
    ) {
        val hiddenCourses = courseVisibility
            .filter { !it.visible }
            .map { it.title }
            .toHashSet()

        eventList.filter { ev ->
            dateFilter(ev)
            && ev.courseName !in hiddenCourses
        }.map { ev -> Event.Wrapper(ev) }
    }

}
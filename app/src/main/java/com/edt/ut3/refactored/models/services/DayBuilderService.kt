package com.edt.ut3.refactored.models.services

import android.content.Context
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.edt.ut3.refactored.models.repositories.preferences.PreferencesManager
import com.edt.ut3.misc.extensions.add
import com.edt.ut3.misc.extensions.timeCleaned
import com.edt.ut3.refactored.injected
import com.edt.ut3.refactored.models.domain.celcat.Event
import com.edt.ut3.refactored.models.domain.daybuilder.DayBuilderConfig
import com.elzozor.yoda.Day
import com.elzozor.yoda.Week
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*

class DayBuilderService {
    private val preferences = PreferencesManager.getInstance(injected())

    /**
     * This function handle when a new bunch of
     * events are available.
     * It calls the [filterEvents] function which
     * will filter the events and then call the
     * [buildDayView] function that display them.
     */
    suspend fun build(context: Context, config: DayBuilderConfig) {
        val calendarMode = preferences.calendarMode
        when {
            calendarMode.isAgenda() ->
                buildDayView(config, context)
            else ->
                buildWeekView(config, context)
        }
    }


    /**
     * Build a day view for the
     * [AGENDA][CalendarMode.Mode.AGENDA] mode.
     *
     * @param config The config that'll be used to build the day view
     * @param context Android context
     */
    private suspend fun buildDayView(config: DayBuilderConfig, context: Context) =
        withContext(Dispatchers.Default) {
            val selectedDate = config.currentDate.add(Calendar.DAY_OF_YEAR, config.dayPosition - config.currentPosition)
            val filter = { it: Event -> it.start > selectedDate && it.start < selectedDate.add(Calendar.DAY_OF_YEAR, 1) }
            val filtered = filterEvents(config, filter)


            val eventContainer = withContext(Dispatchers.Main) {
                buildEventContainer(config.container, { Day(context, null) }) {
                    it.apply {
                        dayBuilder = config.dayBuilder
                        allDayBuilder = config.allDayBuilder
                        emptyDayBuilder = config.emptyDayBuilder

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

            eventContainer.setEvents(filtered, config.height, config.width)
            withContext(Dispatchers.Main) {
                config.container.post {
                    if (eventContainer.parent == null) {
                        config.container.addView(eventContainer)
                    }
                }
            }
        }


    /**
     * Build a day view for the
     * [AGENDA][CalendarMode.Mode.WEEK] mode.
     *
     * @param config The config that'll be used to build the day view
     * @param context Android context
     */
    private suspend fun buildWeekView(config: DayBuilderConfig, context: Context) = withContext(
        Dispatchers.Default
    ) {
        Log.d("DayBuilder", "Constructing week view")

        val date = config.currentDate.add(Calendar.DAY_OF_YEAR, (config.dayPosition - config.currentPosition) * 7)

        val selectedDate = Calendar.getInstance().run {
            time = date.timeCleaned()
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            time
        }

        val filter = { it: Event ->
            it.start > selectedDate
                && it.start < selectedDate.add(Calendar.WEEK_OF_YEAR, 1)
        }

        val filtered = filterEvents(config, filter)

        val eventContainer = withContext(Dispatchers.Main) {
            buildEventContainer(config.container, { Week(context, null) }) {
                it.apply {
                    dayBuilder = config.dayBuilder
                    allDayBuilder = config.allDayBuilder
                    emptyDayBuilder = config.emptyDayBuilder

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

        eventContainer.setEvents(selectedDate, filtered, config.height, config.width)
        withContext(Dispatchers.Main) {
            config.container.post {
                config.container.removeAllViews()
                config.container.addView(eventContainer)
                config.container.requestLayout()
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
    private suspend fun filterEvents(config: DayBuilderConfig, dateFilter: (Event) -> Boolean) =
        withContext(Dispatchers.Default) {
            val hiddenCourses = config.courseVisibility
                .filter { !it.visible }
                .map { it.title }
                .toHashSet()

            config.eventList.filter { ev ->
                dateFilter(ev) && ev.courseName !in hiddenCourses
            }.map { ev -> com.edt.ut3.refactored.models.domain.celcat.EventWrapper(ev) }
        }
}
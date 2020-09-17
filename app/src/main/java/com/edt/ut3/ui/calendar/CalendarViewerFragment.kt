package com.edt.ut3.ui.calendar

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import com.edt.ut3.R
import com.edt.ut3.backend.calendar.CalendarMode
import com.edt.ut3.backend.celcat.Event
import com.edt.ut3.backend.preferences.PreferencesManager
import com.edt.ut3.backend.preferences.PreferencesManager.Preference
import com.edt.ut3.misc.Emoji
import com.edt.ut3.misc.add
import com.edt.ut3.misc.timeCleaned
import com.edt.ut3.misc.toDp
import com.edt.ut3.ui.calendar.view_builders.EventView
import com.edt.ut3.ui.calendar.view_builders.LayoutAllDay
import com.elzozor.yoda.Day
import com.elzozor.yoda.Week
import com.elzozor.yoda.events.EventWrapper
import kotlinx.android.synthetic.main.fragment_calendar.*
import kotlinx.android.synthetic.main.fragment_calendar_viewer.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext
import java.util.*

class CalendarViewerFragment: Fragment() {

    companion object {
        fun newInstance(thisIndex: Int) =
            CalendarViewerFragment().apply {
                position = thisIndex
            }
    }

    private val viewModel : CalendarViewModel by activityViewModels()
    private lateinit var preferences: PreferencesManager
    var date: Date = Date()
    var job: Job? = null
    var position = 0
    var mode = CalendarMode.default()
    var getHeight : () -> Int = {0}

    val calendarModeObserver =
        SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
            if (key == Preference.CALENDAR.value) {
                context?.let {
                    val newMode = preferences.get(Preference.CALENDAR) as CalendarMode

                    if (mode != newMode) {
                        mode = newMode
                        refreshDate(viewModel.selectedDate.value!!, false)
                        handleEventsChange(viewModel.getEvents(it).value)
                    }
                }
            }
        }

    /**
     * We need to save the last position and date in order to keep
     * them after the screen rotation.
     *
     * @param outState
     */
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putInt("position", position)
        outState.putLong("date", date.time)
    }

    /**
     * The local variables are backed up here.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        preferences = PreferencesManager(requireContext())
        mode = preferences.get(Preference.CALENDAR) as CalendarMode

        savedInstanceState?.run {
            position = getInt("position")
            date = Date(getLong("date"))
        }
    }

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View?
    {
        return inflater.inflate(R.layout.fragment_calendar_viewer, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupListeners()
    }

    private fun setupListeners() {
        // This listener refresh the current view when the events are updated.
        viewModel.getEvents(requireContext()).observe(viewLifecycleOwner) {
            handleEventsChange(it)
        }

        viewModel.getCoursesVisibility(requireContext()).observe(viewLifecycleOwner) {
            handleEventsChange(viewModel.getEvents(requireContext()).value)
        }

        viewModel.selectedDate.observe(viewLifecycleOwner) {
            refreshDate(it)
        }

        viewModel.lastPosition.observe(viewLifecycleOwner) {
            if (position == it && lifecycle.currentState == Lifecycle.State.STARTED) {
                viewModel.selectedDate.value = date
            }
        }

        preferences.observe(calendarModeObserver)
    }

    private fun refreshDate(up: Date, refresh: Boolean = true) {
        val coeff = when (preferences.get(Preference.CALENDAR)) {
            CalendarMode.default() -> 1
            else -> 7
        }

        val newDate = up.add(Calendar.DAY_OF_YEAR, (position - viewModel.lastPosition.value!!) * coeff)
        if (date != newDate) {
            date = newDate

            if (refresh) {
                handleEventsChange(viewModel.getEvents(requireContext()).value)
            }
        }
    }


    /**
     * This function handle when a new bunch of
     * events are available.
     * It calls the filterEvents function which
     * will filter the events and then call the
     * day_view function that display them.
     *
     * @param eventList The event list
     */

    fun handleEventsChange(eventList: List<Event>?) {
        if (eventList == null) return

        context?.let {
            requireView().post {
                job?.cancel()
                job = lifecycleScope.launchWhenCreated {
                    val height = (parentFragment as Fragment).view?.findViewById<NestedScrollView>(R.id.scroll_view)?.height ?: 0
                    when (preferences.get(Preference.CALENDAR)) {
                        CalendarMode.default() ->
                            buildDayView(calendar_container, eventList, height, requireView().width)
                        else ->
                            buildWeekView(calendar_container, eventList, height, requireView().width)
                    }
                }
            }
        }
    }


    private suspend fun buildDayView(container: FrameLayout, eventList: List<Event>, height: Int, width: Int) = withContext(
        Dispatchers.Default
    ) {
        val selectedDate = date.timeCleaned()
        val filter = { it: Event -> it.start > selectedDate && it.start < selectedDate.add(Calendar.DAY_OF_YEAR, 1) }
        val filtered = filterEvents(eventList, filter)


        val eventContainer = withContext(Dispatchers.Main) {
            buildEventContainer(container, { Day(requireContext(), null) }) {
                it.apply {
                    dayBuilder = this@CalendarViewerFragment::buildEventView
                    allDayBuilder = this@CalendarViewerFragment::buildAllDayView
                    emptyDayBuilder = this@CalendarViewerFragment::buildEmptyDayView

                    hoursMode = Day.HoursMode.COMPLETE_H
                    fit = Day.Fit.BOUNDS_ADAPTIVE
                    displayMode = Day.Display.FIT_TO_CONTAINER
                    start = 7
                    end = 20

                    layoutParams = FrameLayout.LayoutParams(
                        MATCH_PARENT,
                        MATCH_PARENT
                    )
                }
            }
        }

        eventContainer.setEvents(filtered, height, width)
        withContext(Dispatchers.Main) {
            view?.post {
                if (eventContainer.parent == null) {
                    container.addView(eventContainer)
                }
            }
        }
    }


    private suspend fun buildWeekView(container: FrameLayout, eventList: List<Event>, height: Int, width: Int) = withContext(
        Dispatchers.Default
    ) {
        val selectedDate = Calendar.getInstance().run {
            time = date.timeCleaned()
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            time
        }

        val filter = { it: Event -> it.start > selectedDate && it.start < selectedDate.add(Calendar.WEEK_OF_YEAR, 1) }
        val filtered = filterEvents(eventList, filter)

        val eventContainer = withContext(Dispatchers.Main) {
            buildEventContainer(container, { Week(requireContext(), null) }) {
                it.apply {
                    dayBuilder = this@CalendarViewerFragment::buildEventView
                    allDayBuilder = this@CalendarViewerFragment::buildAllDayView
                    emptyDayBuilder = this@CalendarViewerFragment::buildEmptyDayView

                    hoursMode = Day.HoursMode.SIMPLE
                    fit = Day.Fit.BOUNDS_ADAPTIVE
                    displayMode = Day.Display.FIT_TO_CONTAINER
                    start = 7
                    end = 20

                    layoutParams = FrameLayout.LayoutParams(
                        MATCH_PARENT,
                        MATCH_PARENT
                    )
                }
            }
        }

        eventContainer.setEvents(selectedDate, filtered, height, width)
        withContext(Dispatchers.Main) {
            view?.post {
                container.removeAllViews()
                container.addView(eventContainer)
                container.requestLayout()
            }
        }
    }

    private suspend inline fun<reified T:View> buildEventContainer(container: FrameLayout,
                                                                   crossinline builder: () -> T,
                                                                   crossinline initializer: (T) -> T
    ) = withContext(Dispatchers.Main) {
        container.removeAllViews()
        initializer(builder())
    }



    private suspend fun filterEvents(eventList: List<Event>, dateFilter: (Event) -> Boolean) = withContext(
        Dispatchers.Default
    ) {
        val hiddenCourses = viewModel.getCoursesVisibility(requireContext()).value
            ?.filter { !it.visible }
            ?.map { it.title }?.toHashSet()
            ?: hashSetOf()

        eventList.filter { ev ->
            dateFilter(ev)
            && ev.courseName !in hiddenCourses
        }.map { ev -> Event.Wrapper(ev) }
    }

    /**
     * This function builds a view for an Event.
     *
     * @param context A valid context
     * @param eventWrapper The event encapsulated into an EventWrapper
     * @param x The x position of this event
     * @param y The y position of this event
     * @param w The width of this event
     * @param h The height of this event
     * @return The builded view
     */
    private fun buildEventView(context: Context, eventWrapper: EventWrapper, x: Int, y: Int, w:Int, h: Int)
            : Pair<Boolean, View> {
        return Pair(true, EventView(context, eventWrapper as Event.Wrapper).apply {
            val spacing = context.resources.getDimension(R.dimen.event_spacing).toInt()
            val positionAdder = {x:Int -> x+spacing}
            val sizeChanger = {x:Int -> x-spacing}

            layoutParams = ConstraintLayout.LayoutParams(sizeChanger(w), sizeChanger(h)).apply {
                leftMargin = positionAdder(x)
                topMargin = positionAdder(y)
            }

            setOnClickListener { openEventDetailsView(event) }
        })
    }

    private fun openEventDetailsView(event: Event) {
        viewModel.selectedEvent.value = event
        with (parentFragment as CalendarFragment) {
            event_details_container?.also {
                bottomSheetManager.setVisibleSheet(event_details_container)
            }
        }
    }

    private fun buildAllDayView(events: List<EventWrapper>): View {
        val builder = { event: EventWrapper ->
            buildEventView(requireContext(), event, 0, 0, 0, 0).run {
                (second as EventView).apply {
                    padding = 16.toDp(context).toInt()

                    layoutParams = ViewGroup.LayoutParams(
                        MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                }
            }
        }

        return LayoutAllDay(requireContext()).apply {
            setEvents(events, builder)
        }
    }

    private fun buildEmptyDayView(): View {
        return TextView(requireContext()).apply {
            layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
            gravity = Gravity.CENTER
            text = getString(R.string.empty_day).format(Emoji.happy())
        }
    }


}
package com.edt.ut3.ui.calendar

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.CalendarView
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.children
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.work.WorkInfo
import com.edt.ut3.MainActivity
import com.edt.ut3.R
import com.edt.ut3.backend.background_services.Updater
import com.edt.ut3.backend.background_services.Updater.Companion.Progress
import com.edt.ut3.backend.celcat.Event
import com.edt.ut3.misc.add
import com.edt.ut3.misc.set
import com.edt.ut3.misc.timeCleaned
import com.edt.ut3.misc.toDp
import com.edt.ut3.ui.calendar.options.CalendarOptionsFragment
import com.edt.ut3.ui.custom_views.overlay_layout.OverlayBehavior
import com.elzozor.yoda.Day
import com.elzozor.yoda.Week
import com.elzozor.yoda.events.EventWrapper
import com.elzozor.yoda.utils.DateExtensions.get
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.fragment_calendar.*
import kotlinx.android.synthetic.main.fragment_calendar.view.*
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import kotlin.time.ExperimentalTime

class CalendarFragment : Fragment() {

    enum class Status { IDLE, UPDATING }
    enum class CalendarMode { DAY, WEEK }

    private val calendarViewModel: CalendarViewModel by activityViewModels()

    private var job : Job? = null

    private var shouldBlockScroll = false
    private var canBlockScroll = false
    private var status = Status.IDLE
    private var calendarMode = CalendarMode.DAY

    @ExperimentalTime
    private val calendarActionBar : ActionBarLayout by lazy {
        ActionBarLayout(requireContext(), calendarView).apply {
            fun callCalendarListener(date: Date) {
                date.run {
                    val year = date.get(Calendar.YEAR)
                    val month = date.get(Calendar.MONTH)
                    val day = date.get(Calendar.DAY_OF_MONTH)

                    onCalendarDateChange(year, month, day)
                }
            }

            fun setNewDate(date: Date) {
                calendar.date = date.time

                callCalendarListener(date)
            }

            fun addProperAmountToDate(date: Date, amount: Int): Date {
                val field = when (calendarMode) {
                    CalendarMode.DAY -> Calendar.DAY_OF_YEAR
                    else -> Calendar.WEEK_OF_YEAR
                }

                return date.add(field, amount)
            }

            setOnPreviousClickListener {
                val newDate = addProperAmountToDate(Date(calendar.date), -1)
                setNewDate(newDate)
            }

            setOnNextClickListener {
                val newDate = addProperAmountToDate(Date(calendar.date), +1)

                setNewDate(newDate)
            }
        }
    }


    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_calendar, container, false)
    }


    @ExperimentalTime
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Updater.scheduleUpdate(requireContext())

        setupListeners()

        calendarView.setDate(calendarViewModel.selectedDate.time, false, false)
        calendarViewModel.getEvents(requireContext()).value?.let {
            handleEventsChange(requireView(), it)
        }

        activity?.run {
            supportFragmentManager.run {
                beginTransaction()
                    .replace(R.id.settings, CalendarOptionsFragment())
                    .commit()

                beginTransaction()
                    .add(R.id.news, CalendarNews())
                    .commit()
            }
        }
    }

    @ExperimentalTime
    @SuppressLint("ClickableViewAccessibility")

    private fun setupListeners() {
        calendarViewModel.getEvents(requireContext()).observe(viewLifecycleOwner, { evLi ->
            handleEventsChange(requireView(), evLi)
        })

        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            onCalendarDateChange(year, month, dayOfMonth)
        }

        app_bar.addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener { appBarLayout, verticalOffset ->
            checkScrollAvailability(appBarLayout, verticalOffset)
            hideNavBarIfNecessary(appBarLayout, verticalOffset)
        })

        app_bar.addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener { _, verticalOffset ->
            hideRefreshWhenNecessary(verticalOffset)
        })

        day_scroll.setOnScrollChangeListener { scrollView: NestedScrollView?, _, scrollY, _, oldScrollY ->
            blockScrollIfNecessary(scrollY, oldScrollY)
        }

        refresh_button.setOnClickListener {
            refresh_button.hide()
            status = Status.UPDATING

            forceUpdate()
        }

        settings_button.setOnClickListener {
            findNavController().navigate(R.id.action_navigation_calendar_to_preferencesFragment)
        }

        calendarViewModel.getCoursesVisibility(requireContext()).observe(viewLifecycleOwner) {
            println("changed calendar")
            handleEventsChange(requireView(), calendarViewModel.getEvents(requireContext()).value)
        }

        val calendar = calendarView!!
        (activity as MainActivity?)?.run {
            view?.post { setActionViewContent(calendarActionBar) }
        }

        // This part handles the behavior of the front layout
        // on different triggers such as move and swipe actions.
        // Basically the behavior is to hide and show specific
        // views depending on the layout position and its behavior
        // status and elevating it while moving to let the user
        // see the difference between it and the background views.
        OverlayBehavior.from(front_layout).apply {

            // Action triggered when the view is actually swiping
            // on the left.
            // Swiping on the left means that the view is currently
            // moving to the left letting appear the right side view.
            onSwipingLeftListeners.add {
                settings?.visibility = VISIBLE
                news?.visibility = GONE
            }

            // Action triggered when the view is actually swiping
            // on the right.
            // Swiping on the left means that the view is currently
            // moving to the right letting appear the left side view.
            onSwipingRightListeners.add {
                news?.visibility = VISIBLE
                settings?.visibility = GONE
            }

            // Action triggered when the view is moving no matter
            // on which direction it is moving.
            // Note that this is triggered when the view is moving
            // and not only when the view is moving left or right
            // when it's on the IDLE status.
            // The goal here is to elevate the view to let the user
            // see the difference between this one and the views
            // on background as they have the same color.
            onMoveListeners.add {
                front_layout?.elevation = 8.toDp(requireContext())
            }

            // Action triggered when the view's status change.
            // It is trigger no matter if the old one is different
            // from the new one.
            // The goal here is to reset the view's elevation to prevent
            // unwanted shadows.
            onStatusChangeListeners.add {
                front_layout?.elevation = 0f
            }
        }
    }


    @ExperimentalTime
    private fun onCalendarDateChange(year: Int, month: Int, dayOfMonth: Int) {
        calendarViewModel.getEvents(requireContext()).value?.let {
            calendarViewModel.selectedDate = Date().set(year, month, dayOfMonth).timeCleaned()
            handleEventsChange(requireView(), it)
        }
    }

    /**
     * Hide the navbar to allow the user to use
     * bottom bar actions.
     *
     */
    private fun hideNavBarIfNecessary(appBarLayout: AppBarLayout, scrollY: Int) {
        (activity as MainActivity?)?.let {
            if (scrollY + appBarLayout.totalScrollRange == 0) {
                it.setActionViewVisibility(VISIBLE)
            } else {
                it.setActionViewVisibility(GONE)
            }
        }
    }

    private fun forceUpdate() {
        Updater.forceUpdate(requireContext(), viewLifecycleOwner, {
            it?.let { workInfo ->
                val progress = workInfo.progress
                val value = progress.getInt(Progress, 0)

                when (workInfo.state) {
                    WorkInfo.State.FAILED -> {
                        Snackbar.make(front_layout, R.string.update_failed, Snackbar.LENGTH_INDEFINITE)
                            .setAction(R.string.action_retry) {
                                forceUpdate()
                            }
                            .show()
                    }

                    WorkInfo.State.SUCCEEDED -> {
                        Snackbar.make(front_layout, R.string.update_succeeded, Snackbar.LENGTH_LONG)
                            .addCallback(object: BaseTransientBottomBar.BaseCallback<Snackbar>() {
                                override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                                    super.onDismissed(transientBottomBar, event)

                                    refresh_button.show()
                                    status = Status.IDLE
                                }
                            })
                            .show()
                    }

                    else -> {}
                }

                Log.i("PROGRESS", value.toString())
            }
        })
    }

    /**
     * This function check whether the scroll must
     * be blocked or not.
     *
     *
     * @param appBarLayout
     * @param verticalOffset
     */
    private fun checkScrollAvailability(appBarLayout: AppBarLayout, verticalOffset: Int) {
        val canSwipe = (verticalOffset + appBarLayout.totalScrollRange == 0)
        shouldBlockScroll = canBlockScroll && canSwipe
        canBlockScroll = (verticalOffset + appBarLayout.totalScrollRange > 0)

        val behavior = OverlayBehavior.from(front_layout)
        behavior.canSwipe = canSwipe
    }


    /**
     * This function blocks the scroll
     * if the checkScrollAvailability function
     * has decided that it needed to be blocked.
     * It then sets the blocking variable to false.
     *
     * @param scrollY
     * @param oldScrollY
     */
    private fun blockScrollIfNecessary(scrollY: Int, oldScrollY: Int) {
        println("oldScrollY: $oldScrollY scrollY: $scrollY")
        when {
            shouldBlockScroll -> {
                day_scroll.scrollTo(0,0)
                shouldBlockScroll = false

                val behavior = OverlayBehavior.from(front_layout)
                behavior.canSwipe = true
            }
        }
    }

    /**
     * This function hides the refresh button
     * depending on the app bar vertical offset.
     *
     * @param verticalOffset The vertical offset of the action bar
     */
    private fun hideRefreshWhenNecessary(verticalOffset: Int) {
        if (status == Status.UPDATING) {
            return
        }

        if (verticalOffset == 0) {
            refresh_button.show()
            settings_button.show()
        } else {
            settings_button.hide()
            refresh_button.hide()
        }
    }

    /**
     * This function handle when a new bunch of
     * events are available.
     * It calls the filterEvents function which
     * will filter the events and then call the
     * day_view function that display them.
     *
     * @param root The root view
     * @param eventList The event list
     */

    @ExperimentalTime
    private fun handleEventsChange(root: View, eventList: List<Event>?) {
        if (eventList == null) return

        context?.let {
            root.post {
                job?.cancel()
                job = lifecycleScope.launchWhenResumed {
                    when (resources.configuration.orientation) {
                        Configuration.ORIENTATION_LANDSCAPE ->
                            buildWeekView(root.calendar_container, eventList, root.height, root.width)
                        else ->
                            buildDayView(root.calendar_container, eventList, root.height, root.width)
                    }
                }
            }
        }
    }


    @ExperimentalTime
    private suspend fun buildDayView(container: FrameLayout, eventList: List<Event>, height: Int, width: Int) = withContext(Default) {
        val selectedDate = calendarViewModel.selectedDate.timeCleaned()
        val filter = { it: Event -> it.start > selectedDate && it.start < selectedDate.add(Calendar.DAY_OF_YEAR, 1) }
        val filtered = filterEvents(eventList, filter)


        val eventContainer = withContext(Main) {
            buildEventContainer(container, { Day(requireContext(), null) }) {
                it.apply {
                    dayBuilder = this@CalendarFragment::buildEventView
                    allDayBuilder = this@CalendarFragment::buildAllDayView
                    emptyDayBuilder = this@CalendarFragment::buildEmptyDayView

                    hoursMode = Day.HoursMode.COMPLETE_H
                    fit = Day.Fit.BOUNDS_ADAPTIVE
                    displayMode = Day.Display.FIT_TO_CONTAINER
                    start = 7
                    end = 20

                    layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
                }
            }
        }

        eventContainer.setEvents(filtered, height, width)
        withContext(Main) {
            if (eventContainer.parent == null) {
                container.addView(eventContainer)
            }

            calendarActionBar.updateBarText(selectedDate, false)
            calendarMode = CalendarMode.DAY
        }
    }


    @ExperimentalTime
    private suspend fun buildWeekView(container: FrameLayout, eventList: List<Event>, height: Int, width: Int) = withContext(Default) {
        val selectedDate = Calendar.getInstance().run {
            time = calendarViewModel.selectedDate.timeCleaned()
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            time
        }

        val filter = { it: Event -> it.start > selectedDate && it.start < selectedDate.add(Calendar.WEEK_OF_YEAR, 1) }
        val filtered = filterEvents(eventList, filter)

        val eventContainer = withContext(Main) {
            buildEventContainer(container, { Week(requireContext(), null) }) {
                it.apply {
                    dayBuilder = this@CalendarFragment::buildEventView
                    allDayBuilder = this@CalendarFragment::buildAllDayView
                    emptyDayBuilder = this@CalendarFragment::buildEmptyDayView

                    hoursMode = Day.HoursMode.SIMPLE
                    fit = Day.Fit.BOUNDS_ADAPTIVE
                    displayMode = Day.Display.FIT_TO_CONTAINER
                    start = 7
                    end = 20

                    layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
                }
            }
        }

        eventContainer.setEvents(selectedDate, filtered, height, width)
        withContext(Main) {
            container.removeAllViews()
            container.addView(eventContainer)
            container.requestLayout()

            calendarActionBar.updateBarText(selectedDate, true, eventContainer.children.filter { it is Day }.count() - 1)
            calendarMode = CalendarMode.WEEK
        }
    }

    private suspend inline fun<reified T:View> buildEventContainer(container: FrameLayout,
                                                                   crossinline builder: () -> T,
                                                                   crossinline initializer: (T) -> T
    ) = withContext(Main) {
        container.removeAllViews()
        initializer(builder())
    }



    private suspend fun filterEvents(eventList: List<Event>, dateFilter: (Event) -> Boolean) = withContext(Default) {
        val hiddenCourses = calendarViewModel.getCoursesVisibility(requireContext()).value
            ?.filter { !it.visible }
            ?.map { it.title }?.toHashSet() ?: hashSetOf()

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
        calendarViewModel.selectedEvent = event
        findNavController().navigate(R.id.action_navigation_calendar_to_fragmentEventDetails)
    }

    private fun buildAllDayView(events: List<EventWrapper>): View {
        val builder = { event: EventWrapper ->
            buildEventView(requireContext(), event, 0, 0, 0, 0).run {
                (second as EventView).apply {
                    padding = 16.toDp(context).toInt()

                    layoutParams = ViewGroup.LayoutParams(
                        MATCH_PARENT, WRAP_CONTENT
                    )
                }
            }
        }

        return LayoutAllDay(requireContext()).apply {
            setEvents(events, builder)
        }
    }

    private fun buildEmptyDayView(): View {
        return View(requireContext())
    }

    class ActionBarLayout(context: Context, val calendar: CalendarView) : ConstraintLayout(context) {
        init {
            inflate(context, R.layout.calendar_action, this)
        }

        fun setOnPreviousClickListener(listener: (View) -> Unit) {
            findViewById<ImageButton>(R.id.previous).setOnClickListener {
                listener(it)
            }
        }

        fun setOnNextClickListener(listener: (View) -> Unit) {
            findViewById<ImageButton>(R.id.next).setOnClickListener {
                listener(it)
            }
        }

        fun updateBarText(date: Date, week: Boolean, dayCount: Int = 1) {
            println(dayCount)
            findViewById<TextView>(R.id.text).apply {
                text = if (week) {
                    val start = SimpleDateFormat("EEE dd/MM/yyyy", Locale.getDefault()).format(date)
                    val end = SimpleDateFormat("EEE dd/MM/yyyy", Locale.getDefault()).format(date.add(Calendar.DAY_OF_YEAR, dayCount))

                    "$start - $end"
                } else {
                    SimpleDateFormat("EEE dd/MM/yyyy", Locale.getDefault()).format(date)
                }
            }
        }
    }
}
package com.edt.ut3.ui.calendar

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.work.WorkInfo
import com.edt.ut3.R
import com.edt.ut3.backend.background_services.Updater
import com.edt.ut3.backend.background_services.Updater.Companion.Progress
import com.edt.ut3.backend.celcat.Event
import com.edt.ut3.misc.plus
import com.edt.ut3.misc.set
import com.edt.ut3.misc.timeCleaned
import com.edt.ut3.misc.toDp
import com.edt.ut3.ui.calendar.event_details.FragmentEventDetails
import com.edt.ut3.ui.calendar.options.CalendarOptionsFragment
import com.edt.ut3.ui.custom_views.overlay_layout.OverlayBehavior
import com.edt.ut3.ui.preferences.PreferencesFragment
import com.elzozor.yoda.events.EventWrapper
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.fragment_calendar.*
import kotlinx.android.synthetic.main.fragment_calendar.view.*
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext
import java.util.*
import kotlin.time.ExperimentalTime
import kotlin.time.days

class CalendarFragment : Fragment() {

    enum class Status { IDLE, UPDATING }

    private val calendarViewModel: CalendarViewModel by activityViewModels()

    private var job : Job? = null

    private var shouldBlockScroll = false
    private var canBlockScroll = false
    private var status = Status.IDLE

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

        requireActivity().supportFragmentManager.run {
            beginTransaction()
                .replace(R.id.settings, CalendarOptionsFragment())
                .commit()

            beginTransaction()
                .add(R.id.news, CalendarNews())
                .commit()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @ExperimentalTime
    private fun setupListeners() {
        calendarViewModel.getEvents(requireContext()).observe(viewLifecycleOwner, { evLi ->
            handleEventsChange(requireView(), evLi)
        })

        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            calendarViewModel.getEvents(requireContext()).value?.let {
                calendarViewModel.selectedDate = Date().set(year, month, dayOfMonth).timeCleaned()
                handleEventsChange(requireView(), it)
            }
        }

        app_bar.addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener { appBarLayout, verticalOffset ->
            checkScrollAvailability(appBarLayout, verticalOffset)
        })

        app_bar.addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener { _, verticalOffset ->
            hideRefreshWhenNecessary(verticalOffset)
        })

        day_scroll.setOnScrollChangeListener { _: NestedScrollView?, _, scrollY, _, oldScrollY ->
            blockScrollIfNecessary(scrollY, oldScrollY)
        }

        refresh_button.setOnClickListener {
            refresh_button.hide()
            status = Status.UPDATING

            forceUpdate()
        }

        settings_button.setOnClickListener {
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.nav_host_fragment, PreferencesFragment())
                .addToBackStack(null)
                .commitAllowingStateLoss()
        }

        calendarViewModel.getCoursesVisibility(requireContext()).observe(viewLifecycleOwner) {
            println("changed calendar")
            handleEventsChange(requireView(), calendarViewModel.getEvents(requireContext()).value)
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

    private fun forceUpdate() {
        Updater.forceUpdate(requireContext(), viewLifecycleOwner, {
            it?.let {
                val progress = it.progress
                val value = progress.getInt(Progress, 0)
                println("DEBUG: ${it.state}")
                when (it.state) {
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

        root.day_view.dayBuilder = this::buildEventView
        root.day_view.allDayBuilder = this::buildAllDayView
        root.day_view.emptyDayBuilder = this::buildEmptyDayView

        filterEvents(root, eventList)
    }


    /**
     * This function filter the event and display them.
     *
     * @param root The root view
     * @param eventList The event list
     */
    @ExperimentalTime
    private fun filterEvents(root: View, eventList: List<Event>) {
        val selectedDate = calendarViewModel.selectedDate
        val hiddenCourses = calendarViewModel.getCoursesVisibility(requireContext()).value
            ?.filter { !it.visible }
            ?.map { it.title }?.toHashSet() ?: hashSetOf()


        job?.cancel()
        job = lifecycleScope.launchWhenResumed {
            println(selectedDate.toString())
            withContext(IO) {
                val events = withContext(Default) {
                    eventList.filter { ev ->
                        ev.start >= selectedDate
                        && ev.start <= selectedDate + 1.days
                        && ev.courseName !in hiddenCourses
                    }.map { ev -> Event.Wrapper(ev) }
                }

                root.day_view.post {
                    lifecycleScope.launchWhenStarted {
                        root.day_view.setEvents(events, day_scroll.height)
                        
                        withContext(Main) {
                            root.day_view.requestLayout()
                        }
                    }
                }
            }
        }
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
        requireActivity().supportFragmentManager.beginTransaction()
            .setCustomAnimations(
                R.anim.slide_in,
                R.anim.fade_out,
                R.anim.fade_in,
                R.anim.slide_out
            )
            .replace(R.id.nav_host_fragment, FragmentEventDetails(event))
            .addToBackStack(null)
            .commitAllowingStateLoss()
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
}
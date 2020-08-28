package com.edt.ut3.ui.calendar

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.edt.ut3.R
import com.edt.ut3.backend.background_services.Updater
import com.edt.ut3.backend.background_services.Updater.Companion.Progress
import com.edt.ut3.backend.celcat.Event
import com.edt.ut3.misc.plus
import com.edt.ut3.misc.set
import com.edt.ut3.misc.timeCleaned
import com.edt.ut3.misc.toDp
import com.edt.ut3.ui.custom_views.overlay_layout.OverlayBehavior
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.time.ExperimentalTime
import kotlin.time.days

class CalendarFragment : Fragment() {

    enum class Status { IDLE, UPDATING }

    private val calendarViewModel by viewModels<CalendarViewModel> { defaultViewModelProviderFactory }

    private var job : Job? = null

    private var shouldBlockScroll = false
    private var canBlockScroll = false
    private var refreshInitialized = false
    private var refreshButtonY = 0f

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

        refresh_button.post {
            refreshButtonY = refresh_button.y
            refreshInitialized = true
        }

        Updater.scheduleUpdate(requireContext())

        setupListeners()

        calendarView.setDate(calendarViewModel.selectedDate.time, false, false)
        calendarViewModel.getEvents(requireContext()).value?.let {
            handleEventsChange(requireView(), it)
        }

        requireActivity().supportFragmentManager.run {
            beginTransaction()
                .replace(R.id.settings, CalendarSettingsFragment())
                .commit()

            beginTransaction()
                .add(R.id.news, CalendarNews())
                .commit()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @ExperimentalTime
    private fun setupListeners() {
        calendarViewModel.getEvents(requireContext()).observe(viewLifecycleOwner, Observer { evLi ->
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

        app_bar.addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener { appBarLayout, verticalOffset ->
            moveRefreshButton(appBarLayout, verticalOffset)
        })

        day_scroll.setOnScrollChangeListener { _: NestedScrollView?, _, scrollY, _, oldScrollY ->
            blockScrollIfNecessary(scrollY, oldScrollY)
        }

        refresh_button.setOnClickListener {
            val transY = PropertyValuesHolder.ofFloat("translationY", refreshButtonY + refreshButtonTotalHeight())
            val rotation = PropertyValuesHolder.ofFloat("rotationX", 180f, 0f)
            ObjectAnimator.ofPropertyValuesHolder(refresh_button, transY, rotation).apply {
                duration = 800L
                doOnStart {
                    status = Status.UPDATING
                }

                start()
            }

            forceUpdate()
        }

        getOverlayBehavior(front_layout).onSwipingLeft = {
            settings.visibility = VISIBLE
            news.visibility = GONE
        }

        getOverlayBehavior(front_layout).onSwipingRight = {
            news.visibility = VISIBLE
            settings.visibility = GONE
        }
    }

    private fun forceUpdate() {
        Updater.forceUpdate(requireContext(), viewLifecycleOwner, Observer {
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

                                    val transY = PropertyValuesHolder.ofFloat("translationY", refreshButtonY)
                                    val rotation = PropertyValuesHolder.ofFloat("rotation", 180f, 0f)
                                    ObjectAnimator.ofPropertyValuesHolder(refresh_button, transY, rotation).apply {
                                        duration = 300L
                                        doOnStart {
                                            status = Status.IDLE
                                        }
                                        start()
                                    }
                                }
                            })
                            .show()
                    }
                }

                Log.i("PROGRESS", value.toString())
            }
        })
    }

    private fun getOverlayBehavior(view: View): OverlayBehavior<View> {
        val params = view.layoutParams as CoordinatorLayout.LayoutParams
        return params.behavior as OverlayBehavior
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

        val behavior = getOverlayBehavior(front_layout)
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

                val behavior = getOverlayBehavior(front_layout)
                behavior.canSwipe = true
            }
        }
    }

    /**
     * This function moves and change the
     * opacity of the refresh button in order
     * to avoid visibility problems when
     * the events are displayed on the screen.
     *
     * When the calendar is unfolded, the button
     * is fully visible and at its highest position
     * while when the calendar is folded the button
     * isn't visible and at its lowest position.
     *
     * All of this is interpolated in this function
     * to display a smooth animation.
     *
     * @param appBarLayout The action bar
     * @param verticalOffset The vertical offset of the action bar
     */
    private fun moveRefreshButton(appBarLayout: AppBarLayout, verticalOffset: Int) {
        println("Status=$status")
        if (!refreshInitialized || status == Status.UPDATING) {
            return
        }

        val total = appBarLayout.totalScrollRange.toFloat()
        val offset = (total + verticalOffset)
        val amount = (offset / total).coerceIn(0f, 1f)
        val totalHeight = refreshButtonTotalHeight()

        val opacity = (amount * 255f).toInt()
        val translationAmount = refreshButtonY + totalHeight * (1f - amount)


        refresh_button.apply {
            background.alpha = opacity
            y = translationAmount
        }
    }

    private fun refreshButtonTotalHeight() = refresh_button.height + 16.toDp(requireContext())

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

        root.day_view.setViewBuilder(this::buildEventView)
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

        job?.cancel()
        job = lifecycleScope.launch {
            println(selectedDate.toString())
            withContext(IO) {
                val events = withContext(Default) {
                    eventList.filter {
                            ev -> ev.start >= selectedDate
                            && ev.start <= selectedDate + 1.days
                    }.map { ev -> Event.Wrapper(ev) }
                }

                root.day_view.autoFitHours = true
                root.day_view.setEvents(events)

                withContext(Main) {
                    root.day_view.requestLayout()
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
        return Pair(false, EventView(context, eventWrapper as Event.Wrapper).apply {
            setOnClickListener {
//                PopupMenu(context, this).apply {
//                    inflate(R.menu.event_menu)
//                    setOnMenuItemClickListener {
//                        when (it.itemId) {
//                            R.id.add_note -> {
//                                BottomSheetBehavior.from<CardView>(requireView().bottomNav).setState(STATE_EXPANDED)/*.state = STATE_EXPANDED*/
//                            }
//                            else -> println("wtf")
//                        }
//
//                        false
//                    }
//                }.show()
            }
        })
    }
}
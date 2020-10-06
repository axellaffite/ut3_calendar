package com.edt.ut3.ui.calendar

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration.ORIENTATION_PORTRAIT
import android.os.Bundle
import android.util.Log
import android.view.*
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.addCallback
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import androidx.work.WorkInfo
import com.edt.ut3.R
import com.edt.ut3.backend.background_services.Updater
import com.edt.ut3.backend.calendar.CalendarMode
import com.edt.ut3.backend.calendar.DayBuilder
import com.edt.ut3.backend.celcat.Event
import com.edt.ut3.backend.preferences.PreferencesManager
import com.edt.ut3.misc.*
import com.edt.ut3.ui.calendar.event_details.FragmentEventDetails
import com.edt.ut3.ui.calendar.view_builders.EventView
import com.edt.ut3.ui.calendar.view_builders.LayoutAllDay
import com.elzozor.yoda.events.EventWrapper
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_COLLAPSED
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.fragment_calendar.*
import kotlinx.android.synthetic.main.fragment_calendar.view.*
import kotlinx.coroutines.Job
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs


class CalendarFragment : BottomSheetFragment(),
    androidx.appcompat.widget.Toolbar.OnMenuItemClickListener {

    enum class Status { IDLE, UPDATING }

    private val calendarViewModel: CalendarViewModel by activityViewModels()

    private var status = Status.IDLE

    private lateinit var preferences: PreferencesManager

    private val preferenceChangeListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _: SharedPreferences, key: String ->
            when (key) {
                PreferencesManager.PreferenceKeys.CALENDAR_MODE.key -> {
                    val newPreference = CalendarMode.fromJson(preferences.calendarMode)
                    updateBarText(calendarViewModel.selectedDate.value!!, newPreference)

                    pager?.notifyDataSetChanged()
                }
            }
        }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View?
    {
        preferences = PreferencesManager.getInstance(requireContext())

        // Schedule the periodic update in order to
        // keep the Calendar up to date.
        Updater.scheduleUpdate(requireContext())

        return inflater.inflate(R.layout.fragment_calendar, container, false)
    }

    /**
     * This function is used to update the calendar mode
     * which is sets in the ViewModel.
     */
    private fun updateCalendarMode() {
        val lastValue = CalendarMode.fromJson(preferences.calendarMode)
        val newPreference = when (context?.resources?.configuration?.orientation) {
            ORIENTATION_PORTRAIT ->
                lastValue.withAgendaMode()
            else ->
                lastValue.withWeekMode()
        }

        preferences.calendarMode = newPreference.toJSON()

        view?.action_view?.menu?.findItem(R.id.change_view)?.let {
            it.isEnabled = newPreference.mode == CalendarMode.Mode.AGENDA
        }
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        updateCalendarMode()


        setupBottomSheetManager()
        setupBackButtonListener()

        view.run {
            action_view?.menu?.findItem(R.id.change_view)?.let {
                updateViewIcon(it)
            }

            post {
                setupViewPager()

                setupListeners()

                if (PreferencesManager.getInstance(requireContext()).link == null) {
                    findNavController().navigate(R.id.fragmentFormationChoice)
                }
            }
        }
    }

    private fun setupBottomSheetManager() {
        bottomSheetManager.add(options_container, event_details_container)
    }

    /**
     * Setup a listener that's in charge to
     * handle the back button press depending
     * on the current view state.
     *
     * If a BottomSheet is expanded, we must close it.
     * Otherwise, we disable the listener and call the
     * activity onBackPressedFunction().
     */
    private fun setupBackButtonListener() {
        activity?.onBackPressedDispatcher?.addCallback(viewLifecycleOwner) {
            if (bottomSheetManager.hasVisibleSheet()) {
                bottomSheetManager.setVisibleSheet(null)
            } else {
                activity?.let {
                    isEnabled = false
                    it.onBackPressed()
                }
            }
        }
    }

    /**
     * Setup the ViewPager, construct its
     * pager, set the recycling limit and
     * the animation.
     */
    private fun setupViewPager() {
        pager?.apply {
            // Creates the pager and assign it to
            // the ViewPager2.
            val pagerAdapter = DaySlider(this@CalendarFragment)
            adapter = pagerAdapter

            // The offscreen page limit is set
            // to 1 to optimize rendering and
            // ram consumption.
            offscreenPageLimit = 1

            // This animation is taken from the
            // official documentation.
            // It animate the layout with a zoom animation.
            setPageTransformer(ZoomOutPageTransformer())

            // We reset the current item to the last registered
            // position.
            setCurrentItem(calendarViewModel.lastPosition.value!!, false)
        }
    }

    private fun setupListeners() {
        // This listener is in charge to listen to the
        // AppBar offset in order to hide things when
        // necessary ( such as the refresh buttons and the action bar ).
        view?.scroll_view?.onScrollChangeListeners?.add { _: Int, y: Int, _: Int, _: Int ->
            hideRefreshWhenNecessary(y)
        }

        // Force the Updater to perform an update
        // and hides the refresh button.
        view?.refresh_button?.setOnClickListener {
            refresh_button.hide()
            status = Status.UPDATING

            forceUpdate()
        }

        // Launch the setting fragment
        // when clicked.
        view?.settings_button?.setOnClickListener {
            findNavController().navigate(R.id.action_navigation_calendar_to_preferencesFragment)
        }

        preferences.observe(preferenceChangeListener)

        view?.scroll_view?.post {
            scroll_view?.let {
                pager?.apply {
                    layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, it.height)
                }
            }
        }

        view?.action_view?.setOnMenuItemClickListener(this)

        view?.event_details_container?.let {
            BottomSheetBehavior.from(it).addBottomSheetCallback(object:
                BottomSheetBehavior.BottomSheetCallback() {
                override fun onStateChanged(bottomSheet: View, newState: Int) {
                    if (newState == STATE_COLLAPSED) {
                        calendarViewModel.selectedEvent.value = null
                    }
                }

                override fun onSlide(bottomSheet: View, slideOffset: Float) {
                    // DO NOTHING
                }

            })
        }

        setupCalendarListeners()
    }

    /**
     * This function setup the listeners which will
     * update the calendar ui, the day ui and anything
     * related to them.
     */
    private fun setupCalendarListeners() {
        calendarViewModel.getEvents(requireContext()).observe(viewLifecycleOwner) {
            pager?.notifyDataSetChanged()
        }

        // This listener allows the CalendarViewerFragments to keep
        // up to date their contents by listening to the
        // view model's selectedDate variable.
        view?.calendarView?.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val newDate = Date().set(year, month, dayOfMonth).timeCleaned()

            calendarViewModel.selectedDate.value = newDate
            pager?.notifyDataSetChanged()
        }

        calendarViewModel.selectedDate.observe(viewLifecycleOwner) { selectedDate ->
            view?.calendarView?.run {
                if (selectedDate.time != date) {
                    setDate(selectedDate.time, false, false)
                }
            }

            updateBarText(selectedDate, CalendarMode.fromJson(preferences.calendarMode))

//            pager?.adapter?.notifyDataSetChanged()
        }


        // This callback is in charge to update the
        // ViewModel current index.
        //
        // This is done to give information to the
        // CalendarViewerFragments in order to keep
        // them up to date.
        view?.pager?.registerOnPageChangeCallback(object: ViewPager2.OnPageChangeCallback() {
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
                super.onPageScrolled(position, positionOffset, positionOffsetPixels)

                calendarViewModel.run {
                    val oldPosition = lastPosition.value
                    if (oldPosition != position && abs(positionOffset) < 10f) {
                        lastPosition.value = position

                        if (oldPosition == null) {
                            return
                        }

                        val mode = CalendarMode.fromJson(preferences.calendarMode)
                        val mult = if (mode.isAgenda()) 1 else 7
                        selectedDate.value =
                            selectedDate.value!!.add(Calendar.DAY_OF_YEAR, (position - oldPosition) * mult)
                    }
                }
            }
        })

        calendarViewModel.getCoursesVisibility(requireContext()).observe(viewLifecycleOwner) {
            pager?.notifyDataSetChanged()
        }




        val childFragment = childFragmentManager.findFragmentById(R.id.event_details)
        if (childFragment is FragmentEventDetails) {
            childFragment.onReady = {
                bottomSheetManager.setVisibleSheet(event_details_container)
            }

            childFragment.listenTo = calendarViewModel.selectedEvent
        }
    }



    /**
     * This function performs an update
     * and listen to it in order to display
     * success/error message.
     *
     */
    private fun forceUpdate() {
        Updater.forceUpdate(requireContext(), false, viewLifecycleOwner, { workInfo ->
            workInfo?.run {
                when (state) {
                    WorkInfo.State.FAILED -> {
                        Snackbar.make(
                            front_layout,
                            R.string.update_failed,
                            Snackbar.LENGTH_INDEFINITE
                        )
                            .setAction(R.string.action_retry) {
                                forceUpdate()
                            }
                            .show()
                    }

                    WorkInfo.State.SUCCEEDED -> {
                        Snackbar.make(front_layout, R.string.update_succeeded, Snackbar.LENGTH_LONG)
                            .addCallback(object : BaseTransientBottomBar.BaseCallback<Snackbar>() {
                                override fun onDismissed(
                                    transientBottomBar: Snackbar?,
                                    event: Int
                                ) {
                                    super.onDismissed(transientBottomBar, event)


                                    scroll_view?.scrollY?.let {
                                        hideRefreshWhenNecessary(it)
                                    }
                                    status = Status.IDLE
                                }
                            })
                            .show()
                    }

                    else -> {
                    }
                }
            }
        })
    }

    /**
     * This function hides the refresh button
     * depending on the app bar vertical offset.
     *
     * @param verticalOffset The vertical offset of the action bar
     */
    private fun hideRefreshWhenNecessary(verticalOffset: Int) {
        if (verticalOffset < 100) {
            if (status != Status.UPDATING) {
                refresh_button.show()
            }
            settings_button.show()
        } else {
            settings_button.hide()
            refresh_button.hide()
        }
    }

    override fun onMenuItemClick(item: MenuItem?): Boolean {
        return when (item?.itemId) {
            R.id.change_view -> onChangeViewClick(item)
            R.id.visibility -> {
                onVisibilityClick()
            }
            else -> false
        }
    }

    private fun onVisibilityClick(): Boolean {
        return view?.let { root ->
            BottomSheetBehavior.from(root.options_container).apply {
                when (state) {
                    STATE_COLLAPSED -> {
                        bottomSheetManager.setVisibleSheet(root.options_container)
                    }

                    else -> {
                        state = STATE_COLLAPSED
                    }
                }
            }

            true
        } ?: false
    }

    private fun onChangeViewClick(item: MenuItem): Boolean {
        val mode = CalendarMode.fromJson(preferences.calendarMode)
        val newMode = mode.invertForceWeek()
        Log.d(this::class.simpleName, "Mode: $mode | NewMode: $newMode")
        preferences.calendarMode = newMode.toJSON()

        updateViewIcon(item)

        return true
    }

    private fun updateViewIcon(item: MenuItem) {
        val mode = CalendarMode.fromJson(preferences.calendarMode)
        val icon = when (mode) {
            CalendarMode.default() -> R.drawable.ic_week_view
            else -> R.drawable.ic_agenda_view
        }

        item.setIcon(icon)
    }

    private fun updateBarText(date: Date, mode: CalendarMode?, dayCount: Int = 5) {
        val beginDate = Calendar.getInstance().apply {
            time = date
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        }.time

        action_view?.apply {
            val newTitle = when (mode) {
                CalendarMode.default() -> {
                    SimpleDateFormat("EEE dd/MM/yyyy", Locale.getDefault()).format(date)
                }

                else -> {
                    val start = SimpleDateFormat("EEE dd/MM/yyyy", Locale.getDefault()).format(
                        beginDate
                    )
                    val end = SimpleDateFormat("EEE dd/MM/yyyy", Locale.getDefault()).format(
                        beginDate.add(
                            Calendar.DAY_OF_YEAR,
                            dayCount - 1
                        )
                    )

                    "$start - $end"
                }
            }

            title = newTitle
        }
    }


    /**
     *  Used to display the calendar.
     *
     * @param fragment The fragment which contains the viewpager.
     */
    private inner class DaySlider(fragment: Fragment) : RecyclerView.Adapter<DayViewHolder>() {

        override fun getItemCount() = Int.MAX_VALUE

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayViewHolder {
            Log.d("CalendarFragment", "inflating")
            val v = LayoutInflater.from(parent.context).inflate(R.layout.day_viewer, parent, false) as FrameLayout

            return DayViewHolder(v)
        }

        override fun onBindViewHolder(holder: DayViewHolder, position: Int) {
            holder.day.post {
                holder.job?.cancel()
                holder.job = lifecycleScope.launchWhenCreated {
                    DayBuilder(
                        calendarViewModel.getEvents(requireContext()).value,
                        requireContext(),
                        calendarViewModel.selectedDate.value!!,
                        calendarViewModel.lastPosition.value!!,
                        position,
                        holder.day,
                        calendarViewModel.getCoursesVisibility(requireContext()).value ?: listOf(),
                        this@CalendarFragment::buildEventView,
                        this@CalendarFragment::buildEmptyDayView,
                        this@CalendarFragment::buildAllDayView
                    ).build(pager.height, pager.width)
                }
            }
        }
    }

    class DayViewHolder(val day: FrameLayout, var job: Job? = null): RecyclerView.ViewHolder(day)


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
    fun buildEventView(context: Context, eventWrapper: EventWrapper, x: Int, y: Int, w:Int, h: Int)
            : Pair<Boolean, View> {
        return Pair(true, EventView(context, eventWrapper as Event.Wrapper).apply {
            val spacing = context.resources.getDimension(R.dimen.event_spacing).toInt()
            val positionAdder = {x:Int -> x+spacing}
            val sizeChanger = {x:Int -> x-spacing}

            layoutParams = ConstraintLayout.LayoutParams(sizeChanger(w), sizeChanger(h)).apply {
                leftMargin = positionAdder(x)
                topMargin = positionAdder(y)
            }

            setOnClickListener { setSelectedEvent(event) }
        })
    }

    private fun setSelectedEvent(event: Event) {
        calendarViewModel.selectedEvent.value = event
    }

    fun buildAllDayView(events: List<EventWrapper>): View {
        val builder = { event: EventWrapper ->
            buildEventView(requireContext(), event, 0, 0, 0, 0).run {
                (second as EventView).apply {
                    padding = 16.toDp(context).toInt()

                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                }
            }
        }

        return LayoutAllDay(requireContext()).apply {
            setEvents(events, builder)
        }
    }

    fun buildEmptyDayView(): View {
        return TextView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            gravity = Gravity.CENTER
            text = context.getString(R.string.empty_day).format(Emoji.happy())
        }
    }
}
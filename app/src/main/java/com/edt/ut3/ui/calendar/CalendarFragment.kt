package com.edt.ut3.ui.calendar

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration.ORIENTATION_PORTRAIT
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.CalendarView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import androidx.work.WorkInfo
import com.edt.ut3.MainActivity
import com.edt.ut3.R
import com.edt.ut3.backend.background_services.Updater
import com.edt.ut3.misc.add
import com.edt.ut3.misc.set
import com.edt.ut3.misc.timeCleaned
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_COLLAPSED
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.calendar_action.view.*
import kotlinx.android.synthetic.main.fragment_calendar.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.time.ExperimentalTime

class CalendarFragment : Fragment(), LifecycleObserver {

    enum class Status { IDLE, UPDATING }

    private val calendarViewModel: CalendarViewModel by activityViewModels()

    private var status = Status.IDLE

    lateinit var calendarActionBar : CalendarActionBar



    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?): View?
    {
        setupLifecycleListener()
        updateCalendarMode()

        // Schedule the periodic update in order to
        // keep the Calendar up to date.
        Updater.scheduleUpdate(requireContext())

        return inflater.inflate(R.layout.fragment_calendar, container, false)
    }

    /**
     * This function is used to register an
     * observer an the activity's lifecycle.
     *
     * It's used to setup the action bar
     * when the activity's lifecycle is
     * at least in "OnCreate" state.
     */
    private fun setupLifecycleListener() {
        lifecycle.addObserver(this)
    }

    /**
     * This function is used to update the calendar mode
     * which is sets in the ViewModel.
     */
    private fun updateCalendarMode() {
        when (context?.resources?.configuration?.orientation) {
            ORIENTATION_PORTRAIT ->
                calendarViewModel.calendarMode.value = CalendarMode.DAY
            else ->
                calendarViewModel.calendarMode.value = CalendarMode.WEEK
        }
    }


    @ExperimentalTime
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViewPager()
        calendarView.date = calendarViewModel.selectedDate.value!!.time


    }

    /**
     * Setup the ViewPager, construct its
     * pager, set the recycling limit and
     * the animation.
     */
    private fun setupViewPager() {
        pager.apply {
            val pagerAdapter = DaySlider(this@CalendarFragment)
            adapter = pagerAdapter
            pagerAdapter.notifyDataSetChanged()

            setCurrentItem(calendarViewModel.lastPosition, false)
            offscreenPageLimit = 1

            setPageTransformer(ZoomOutPageTransformer())
        }
    }

    @ExperimentalTime
    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    private fun setupActionBar() {
        val mActivity = activity
        if (mActivity is MainActivity) {
            val calendar_view = calendarView ?: return

            calendarActionBar = CalendarActionBar(requireContext(), calendar_view).apply {
                setOnViewChangeClickListener {
                    calendarViewModel.calendarMode.run {
                        when (value) {
                            CalendarMode.DAY -> {
                                value = CalendarMode.WEEK
                                setAgendaIcon()
                            }

                            else -> {
                                value = CalendarMode.DAY
                                setWeekIcon()
                            }
                        }
                    }
                }

                setOnVisibilityClickListener {
                    BottomSheetBehavior.from(options_container).apply {
                        state = when (state) {
                            STATE_COLLAPSED -> STATE_EXPANDED
                            else -> STATE_COLLAPSED
                        }
                    }
                }
            }

            mActivity.setActionViewContent(calendarActionBar)
        }

        setupListeners()
    }

    @ExperimentalTime
    private fun setupListeners() {
        // This listener allows the CalendarViewerFragments to keep
        // up to date their contents by listening to the
        // view model's selectedDate variable.
        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val newDate = Date().set(year, month, dayOfMonth).timeCleaned()
            calendarViewModel.selectedDate.value = newDate
        }

        // This callback is in charge to update the
        // ViewModel variables such as the current date,
        // the current index in the viewpager and so on.
        //
        // This is done to give information to the
        // CalendarViewerFragments in order to keep
        // them up to date.
        pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)

                calendarViewModel.run {
                    // Get the old date to calculate the new one.
                    val oldDate = selectedDate.value!!

                    // The day offset that needs to be added to
                    // the current date is the offset between the
                    // current position and the last one that has
                    // been registered in the ViewModel.
                    var dayAddAmount = position - lastPosition

                    // If the CalendarMode is sets to WEEK
                    // we need to multiply the offset by 7
                    // to get the proper date on the side fragments
                    if (calendarMode.value == CalendarMode.WEEK) {
                        dayAddAmount *= 7
                    }

                    // We can now add the computed offset to the
                    // old date in order to compute the new one.
                    val newDate = oldDate.add(Calendar.DAY_OF_YEAR, dayAddAmount)

                    // Finally, we update all the variables which store
                    // the current "state".
                    lastPosition = position
                    selectedDate.value = newDate

                    // The calendar view needs to be updated too.
                    calendarView.date = newDate.time
                }
            }
        })

        // This listener is in charge to listen to the
        // AppBar offset in order to hide things when
        // necessary ( such as the refresh buttons and the action bar ).
        app_bar.addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener { appBarLayout, verticalOffset ->
            hideRefreshWhenNecessary(verticalOffset)
            (activity as MainActivity?)?.run {
                setActionViewVisibility(if (verticalOffset + appBarLayout.totalScrollRange == 0) VISIBLE else GONE)
            }
        })

        // Force the Updater to perform an update
        // and hides the refresh button.
        refresh_button.setOnClickListener {
            refresh_button.hide()
            status = Status.UPDATING

            forceUpdate()
        }

        // Launch the setting fragment
        // when clicked.
        settings_button.setOnClickListener {
            findNavController().navigate(R.id.action_navigation_calendar_to_preferencesFragment)
        }

        // Update the action bar text when the selected date
        // is updated in the ViewModel.
        calendarViewModel.selectedDate.observe(viewLifecycleOwner) {
            calendarActionBar.updateBarText(it, calendarViewModel.calendarMode.value)
        }

        calendarViewModel.calendarMode.observe(viewLifecycleOwner) {
            calendarActionBar.updateBarText(calendarViewModel.selectedDate.value!!, it)
        }
    }


    /**
     * This function performs an update
     * and listen to it in order to display
     * success/error message.
     *
     */
    private fun forceUpdate() {
        Updater.forceUpdate(requireContext(), viewLifecycleOwner, { workInfo ->
            workInfo?.run {
                when (state) {
                    WorkInfo.State.FAILED -> {
                        Snackbar.make(
                            front_layout,
                            R.string.update_failed,
                            Snackbar.LENGTH_INDEFINITE)
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
                                    event: Int)
                                {
                                    super.onDismissed(transientBottomBar, event)

                                    refresh_button.show()
                                    status = Status.IDLE
                                }
                            })
                            .show()
                    }

                    else -> { }
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
     *  Used to display the calendar.
     *
     * @param fragment The fragment which contains the viewpager.
     */
    private inner class DaySlider(fragment: Fragment) : FragmentStateAdapter(fragment) {

        override fun getItemCount() = Int.MAX_VALUE

        override fun createFragment(position: Int) =
            CalendarViewerFragment.newInstance(
                baseDate = calendarViewModel.selectedDate.value!!,
                currentIndex = calendarViewModel.lastPosition,
                thisIndex = position,
                calendarViewModel.calendarMode.value!!,
                front_layout
            )

        override fun getItemId(position: Int) = position.toLong()
    }


    @SuppressLint("ViewConstructor")
    class CalendarActionBar(context: Context, val calendar: CalendarView) : ConstraintLayout(context) {
        init {
            inflate(context, R.layout.calendar_action, this)
        }

        fun updateBarText(date: Date, mode: CalendarMode?, dayCount: Int = 5) {
            val beginDate = Calendar.getInstance().apply {
                time = date
                set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            }.time

            findViewById<TextView>(R.id.text)?.apply {
                text = when (mode) {
                    CalendarMode.WEEK -> {
                        val start = SimpleDateFormat("EEE dd/MM/yyyy", Locale.getDefault()).format(beginDate)
                        val end = SimpleDateFormat("EEE dd/MM/yyyy", Locale.getDefault()).format(
                            beginDate.add(
                                Calendar.DAY_OF_YEAR,
                                dayCount - 1
                            )
                        )

                        "$start - $end"
                    }

                    else -> {
                        SimpleDateFormat("EEE dd/MM/yyyy", Locale.getDefault()).format(date)
                    }
                }
            }
        }

        fun setOnViewChangeClickListener(listener: (View) -> Unit) {
            layout_switch_button.setOnClickListener(listener)
        }

        fun setOnVisibilityClickListener(listener: (View) -> Unit) {
            visibility_button.setOnClickListener(listener)
        }

        fun setWeekIcon() {
            layout_switch_button.setImageResource(R.drawable.ic_week_view)
        }

        fun setAgendaIcon() {
            layout_switch_button.setImageResource(R.drawable.ic_agenda_view)
        }
    }




    class ZoomOutPageTransformer : ViewPager2.PageTransformer {

        private val MIN_SCALE = 0.85f
        private val MIN_ALPHA = 0.5f

        override fun transformPage(view: View, position: Float) {
            view.apply {
                val pageWidth = width
                val pageHeight = height
                when {
                    position < -1 -> { // [-Infinity,-1)
                        // This page is way off-screen to the left.
                        alpha = 0f
                    }
                    position <= 1 -> { // [-1,1]
                        // Modify the default slide transition to shrink the page as well
                        val scaleFactor = Math.max(MIN_SCALE, 1 - Math.abs(position))
                        val vertMargin = pageHeight * (1 - scaleFactor) / 2
                        val horzMargin = pageWidth * (1 - scaleFactor) / 2
                        translationX = if (position < 0) {
                            horzMargin - vertMargin / 2
                        } else {
                            horzMargin + vertMargin / 2
                        }

                        // Scale the page down (between MIN_SCALE and 1)
                        scaleX = scaleFactor
                        scaleY = scaleFactor

                        // Fade the page relative to its size.
                        alpha = (MIN_ALPHA +
                                (((scaleFactor - MIN_SCALE) / (1 - MIN_SCALE)) * (1 - MIN_ALPHA)))
                    }
                    else -> { // (1,+Infinity]
                        // This page is way off-screen to the right.
                        alpha = 0f
                    }
                }
            }
        }
    }
}
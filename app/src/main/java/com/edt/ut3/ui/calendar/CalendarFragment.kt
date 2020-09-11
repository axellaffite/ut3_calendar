package com.edt.ut3.ui.calendar

import android.content.Context
import android.content.res.Configuration.ORIENTATION_PORTRAIT
import android.os.Bundle
import android.util.Log
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
import com.edt.ut3.backend.background_services.Updater.Companion.Progress
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

    lateinit var actionBarLayout : ActionBarLayout


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View?
    {
        when (context?.resources?.configuration?.orientation) {
            ORIENTATION_PORTRAIT -> calendarViewModel.calendarMode.value =
                CalendarViewerFragment.CalendarMode.DAY
            else -> calendarViewModel.calendarMode.value = CalendarViewerFragment.CalendarMode.WEEK
        }

        lifecycle.addObserver(this)

        return inflater.inflate(R.layout.fragment_calendar, container, false)
    }


    @ExperimentalTime
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Updater.scheduleUpdate(requireContext())

        setupListeners()

        calendarView.date = calendarViewModel.selectedDate.value!!.time

        pager.adapter = FragmentSlider(this)
        pager.adapter?.notifyDataSetChanged()
        pager.setCurrentItem(calendarViewModel.lastPosition, false)
        pager.offscreenPageLimit = 1

        pager.setPageTransformer(ZoomOutPageTransformer())
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    private fun setupActionBar() {
        with(activity as MainActivity?) {
            actionBarLayout = ActionBarLayout(requireContext(), calendarView).apply {
                setOnViewChangeClickListener {
                    calendarViewModel.calendarMode.run {
                        when (value) {
                            CalendarViewerFragment.CalendarMode.DAY -> {
                                value = CalendarViewerFragment.CalendarMode.WEEK
                                setAgendaIcon()
                            }

                            else -> {
                                value = CalendarViewerFragment.CalendarMode.DAY
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

            (activity as MainActivity?)?.run {
                setActionViewContent(actionBarLayout)
            }

            calendarViewModel.selectedDate.observe(viewLifecycleOwner) {
                actionBarLayout.updateBarText(it, false)
            }
        }
    }

    @ExperimentalTime
    private fun setupListeners() {

        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val newDate = Date().set(year, month, dayOfMonth).timeCleaned()
            calendarViewModel.selectedDate.value = newDate
        }


        pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)

                val oldDate = calendarViewModel.selectedDate.value!!
                var dayAddAmount = position - calendarViewModel.lastPosition

                if (calendarViewModel.calendarMode.value == CalendarViewerFragment.CalendarMode.WEEK) {
                    dayAddAmount *= 7
                }

                val newDate = oldDate.add(Calendar.DAY_OF_YEAR, dayAddAmount)

                calendarViewModel.lastPosition = position
                calendarViewModel.selectedDate.value = newDate
                calendarView.date = newDate.time


            }
        })

        app_bar.addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener { appBarLayout, verticalOffset ->
            hideRefreshWhenNecessary(verticalOffset)
            (activity as MainActivity?)?.run {
                setActionViewVisibility(if (verticalOffset + appBarLayout.totalScrollRange == 0) VISIBLE else GONE)
            }
        })

        refresh_button.setOnClickListener {
            refresh_button.hide()
            status = Status.UPDATING

            forceUpdate()
        }

        settings_button.setOnClickListener {
            findNavController().navigate(R.id.action_navigation_calendar_to_preferencesFragment)
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

                                    refresh_button.show()
                                    status = Status.IDLE
                                }
                            })
                            .show()
                    }

                    else -> {
                    }
                }

                Log.i("PROGRESS", value.toString())
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


    private inner class FragmentSlider(fragment: Fragment) : FragmentStateAdapter(fragment) {

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


    class ActionBarLayout(context: Context, val calendar: CalendarView) : ConstraintLayout(context) {
        init {
            inflate(context, R.layout.calendar_action, this)
        }

        fun updateBarText(date: Date, week: Boolean, dayCount: Int = 1) {
            println(dayCount)
            findViewById<TextView>(R.id.text).apply {
                text = if (week) {
                    val start = SimpleDateFormat("EEE dd/MM/yyyy", Locale.getDefault()).format(date)
                    val end = SimpleDateFormat("EEE dd/MM/yyyy", Locale.getDefault()).format(
                        date.add(
                            Calendar.DAY_OF_YEAR,
                            dayCount
                        )
                    )

                    "$start - $end"
                } else {
                    SimpleDateFormat("EEE dd/MM/yyyy", Locale.getDefault()).format(date)
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
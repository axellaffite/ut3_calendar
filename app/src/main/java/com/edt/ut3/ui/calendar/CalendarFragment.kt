package com.edt.ut3.ui.calendar

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.work.WorkInfo
import com.edt.ut3.R
import com.edt.ut3.backend.background_services.Updater
import com.edt.ut3.backend.background_services.Updater.Companion.Progress
import com.edt.ut3.misc.set
import com.edt.ut3.misc.timeCleaned
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.fragment_calendar.*
import java.util.*
import kotlin.time.ExperimentalTime

class CalendarFragment : Fragment() {

    enum class Status { IDLE, UPDATING }

    private val calendarViewModel: CalendarViewModel by activityViewModels()

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

        calendarView.date = calendarViewModel.selectedDate.value!!.time
    }

    @SuppressLint("ClickableViewAccessibility")
    @ExperimentalTime
    private fun setupListeners() {

        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            calendarViewModel.getEvents(requireContext()).value?.let {
                calendarViewModel.selectedDate.value = Date().set(year, month, dayOfMonth).timeCleaned()
            }
        }

        app_bar.addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener { appBarLayout, verticalOffset ->
            hideRefreshWhenNecessary(verticalOffset)
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


    private inner class FragmentSlider() : FragmentStateAdapter(this) {
        override fun getItemCount() = Int.MAX_VALUE

        override fun createFragment(position: Int) = CalendarViewerFragment()
    }
}
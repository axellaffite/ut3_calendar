package com.edt.ut3.ui.calendar

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.edt.ut3.R
import com.edt.ut3.backend.background_services.Updater
import com.edt.ut3.backend.background_services.Updater.Companion.Progress
import com.edt.ut3.backend.celcat.Event
import com.edt.ut3.misc.set
import com.elzozor.yoda.events.EventWrapper
import kotlinx.android.synthetic.main.fragment_calendar.view.*
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
import java.util.concurrent.TimeUnit

class CalendarFragment : Fragment() {

    private val calendarViewModel by viewModels<CalendarViewModel> { defaultViewModelProviderFactory }

    private var job : Job? = null

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_calendar, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        calendarViewModel.getEvents(requireContext()).observe(viewLifecycleOwner, Observer { evLi ->
            println("Event database has been updated")
            handleEventsChange(view, evLi)
        })

        val worker = PeriodicWorkRequestBuilder<Updater>(1, TimeUnit.HOURS).build()
        WorkManager.getInstance(requireContext()).let {
            it.enqueueUniquePeriodicWork("event_update", ExistingPeriodicWorkPolicy.REPLACE, worker)
            it.getWorkInfoByIdLiveData(worker.id).observe(viewLifecycleOwner, Observer { workInfo ->
                workInfo?.let {
                    val progress = workInfo.progress
                    val value = progress.getInt(Progress, 0)

                    Log.i("PROGRESS", value.toString())
                }
            })
        }
    }

    private fun handleEventsChange(root: View, eventList: List<Event>?) {
        if (eventList == null) return

        root.day_view.setViewBuilder(this::buildEventView)
        filterEvents(root, eventList)
    }

    private fun buildEventView(context: Context, eventWrapper: EventWrapper, x: Int, y: Int, w:Int, h: Int)
            : Pair<Boolean, View> {
        return Pair(false, CardView(context).apply {
            addView(
                TextView(context).apply {
                    (eventWrapper as Event.Wrapper).let { ev ->
                        text = generateCardContents(ev.event)
                        setBackgroundColor(Color.parseColor("#FF" + ev.event.backGroundColor?.substring(1)))
                        setTextColor(Color.parseColor("#FF" + ev.event.textColor?.substring(1)))
                    }

                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.MATCH_PARENT
                    )

                    gravity = Gravity.CENTER
                }
            )

            radius = context.resources.getDimension(R.dimen.event_radius)
        })
    }

    private fun filterEvents(root: View, eventList: List<Event>) {
        job?.cancel()
        job = lifecycleScope.launch {
            withContext(IO) {
                val events = withContext(Default) {
                    eventList.filter {
                            ev -> ev.start >= Date().set(2020, Calendar.JANUARY, 13)
                            && ev.start <= Date().set(2020, Calendar.JANUARY, 14)
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

    private fun generateCardContents(event: Event) : String {
        val description = StringBuilder()
        if (event.locations.size == 1) {
            description.append(event.locations.first())
        }

        if (event.courseName != null) {
            if (description.isNotEmpty()) {
                description.append("\n")
            }

            description.append(event.courseName)
        }

        if (description.isEmpty()) {
            description.append(event.description)
        }

        return description.toString()
    }
}
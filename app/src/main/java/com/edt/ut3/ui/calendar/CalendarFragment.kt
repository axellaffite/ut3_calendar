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

        return inflater.inflate(R.layout.fragment_calendar, container, false).also { root ->
            calendarViewModel.getEvents(requireContext()).observe(viewLifecycleOwner, Observer { evLi ->
                root.day_view.setViewBuilder { context: Context, eventWrapper: EventWrapper, x, y, w, h ->
                    Log.d("EVENT", "x: $x, y: $y, w: $w, h: $h")

                    Pair(false, CardView(context).apply {
                        addView(
                            TextView(context).apply {
                                (eventWrapper as Event.Wrapper).let { ev ->
                                    text = ev.event.courseName
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

                evLi?.let { li ->
                    Log.d("EVENTS", "Count: ${evLi.size}")
                    job?.cancel()
                    job = lifecycleScope.launch {
                        withContext(IO) {
                            val events = withContext(Default) {
                                li.sortedBy { it.start }.filter { ev -> ev.start >= Date().set(2020, Calendar.JANUARY, 13)
                                        && ev.start <= Date().set(2020, Calendar.JANUARY, 14)
                                }.map { ev -> Event.Wrapper(ev) }
                            }

                            events.forEach {
                                println(it.begin())
                            }

                            root.day_view.setEvents(events)

                            withContext(Main) {
                                root.day_view.requestLayout()
                                Log.d("EVENTS", "Event count: ${events.size}")
                            }
                        }
                    }
                }
            })
        }
    }
}
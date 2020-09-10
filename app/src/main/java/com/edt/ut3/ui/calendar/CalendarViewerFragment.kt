package com.edt.ut3.ui.calendar

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.edt.ut3.R
import com.edt.ut3.backend.celcat.Event
import com.edt.ut3.misc.add
import com.edt.ut3.misc.toDp
import com.elzozor.yoda.Day
import com.elzozor.yoda.events.EventWrapper
import kotlinx.android.synthetic.main.fragment_calendar_viewer.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext
import java.util.*
import kotlin.time.ExperimentalTime

class CalendarViewerFragment: Fragment() {

    companion object {
        fun newInstance(baseDate: Date, currentIndex: Int, thisIndex: Int) = CalendarViewerFragment().apply {
            date = baseDate.add(Calendar.DAY_OF_YEAR, thisIndex - currentIndex)
        }
    }

    val viewModel : CalendarViewModel by activityViewModels()
    var date: Date = Date()
    var job: Job? = null

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View?
    {
        return inflater.inflate(R.layout.fragment_calendar_viewer, container, false).also {
            setupCalendarView(it.findViewById(R.id.day_view))
        }
    }

    @ExperimentalTime
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupListeners()
    }

    @ExperimentalTime
    private fun setupListeners() {
        viewModel.getEvents(requireContext()).observe(viewLifecycleOwner) {
            handleEventsChange(it)
        }

        viewModel.selectedDate.observe(viewLifecycleOwner) {
            date = it
            handleEventsChange(viewModel.getEvents(requireContext()).value)
        }

        viewModel.getCoursesVisibility(requireContext()).observe(viewLifecycleOwner) {
            handleEventsChange(viewModel.getEvents(requireContext()).value)
        }
    }

    private fun setupCalendarView(day: Day) {
        day.
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
    @ExperimentalTime
    private fun handleEventsChange(eventList: List<Event>?) {
        if (eventList == null) return

        day_view.dayBuilder = this::buildEventView
        day_view.allDayBuilder = this::buildAllDayView
        day_view.emptyDayBuilder = this::buildEmptyDayView

        filterEvents(eventList)
    }


    /**
     * This function filter the event and display them.
     *
     * @param eventList The event list
     */
    @ExperimentalTime
    private fun filterEvents(eventList: List<Event>) {
        val selectedDate = date
        val hiddenCourses = viewModel.getCoursesVisibility(requireContext()).value
            ?.filter { !it.visible }
            ?.map { it.title }?.toHashSet() ?: hashSetOf()


        job?.cancel()
        job = lifecycleScope.launchWhenResumed {
            println(selectedDate.toString())
            withContext(Dispatchers.IO) {
                val events = withContext(Dispatchers.Default) {
                    eventList.filter { ev ->
                        ev.start >= selectedDate
                        && ev.start <= selectedDate.add(Calendar.DAY_OF_YEAR, 1)
                        && ev.courseName !in hiddenCourses
                    }.map { ev -> Event.Wrapper(ev) }
                }

                day_view.post {
                    lifecycleScope.launchWhenStarted {
                        day_view.setEvents(events, requireView().height, requireView().width)

                        withContext(Dispatchers.Main) {
                            day_view.requestLayout()
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
        viewModel.selectedEvent = event
        findNavController().navigate(R.id.action_navigation_calendar_to_fragmentEventDetails)
    }

    private fun buildAllDayView(events: List<EventWrapper>): View {
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

    private fun buildEmptyDayView(): View {
        return View(requireContext())
    }


}
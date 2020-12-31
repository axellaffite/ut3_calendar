package com.edt.ut3.ui.calendar.courses_visibility

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.edt.ut3.R
import com.edt.ut3.backend.celcat.Course
import com.edt.ut3.backend.celcat.CourseStatusData
import com.edt.ut3.backend.database.viewmodels.CoursesViewModel
import com.edt.ut3.ui.calendar.CalendarViewModel
import kotlinx.android.synthetic.main.fragment_calendar_options.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class CoursesVisibilityFragment: Fragment() {

    /**
     * Used to communicate between the [CalendarFragment][com.edt.ut3.ui.calendar.CalendarFragment]
     * and this fragment.
     */
    private val viewModel: CalendarViewModel by activityViewModels()

    /**
     * The adapter that will store the
     * Courses list. It must be passed
     * to the ListView in charge to display
     * the data.
     */
    private val coursesAdapter = CourseAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_calendar_options, container, false)


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        CoursesStatusList.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        CoursesStatusList.adapter = coursesAdapter
        CoursesStatusList.setHasFixedSize(false)

        viewModel.getCoursesVisibility(view.context).observe(viewLifecycleOwner, coursesAdapter::dataSet::set)
    }


    /**
     * [CourseAdapter] is used to manage
     * and display a list of [CourseStatusData]
     * in a ListView.
     *
     * The adapter dataSet can be updated simply by
     * assigning a new value to the [dataSet] field.
     * When it's done, the dataSet call the function
     * [BaseAdapter.notifyDataSetChanged] by itself
     * to update the displayed data.
     */
    class CourseAdapter : RecyclerView.Adapter<CourseViewHolder>() {
        var dataSet: List<CourseStatusData> = listOf()
            set(value) {
                field = value
                notifyDataSetChanged()
            }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CourseViewHolder {
            return CourseViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(
                        R.layout.layout_course_status_view_holder,
                        parent,
                        false
                    ) as CourseStatus
            )
        }

        override fun onBindViewHolder(holder: CourseViewHolder, position: Int) {
            holder.course.apply {
                val course = dataSet[position]

                setCourse(course) { _, visible ->
                    GlobalScope.launch {
                        CoursesViewModel(context).insert(
                            Course(course.title, visible)
                        )
                    }
                }
            }
        }

        override fun getItemCount() = dataSet.size
    }

    class CourseViewHolder(val course: CourseStatus): RecyclerView.ViewHolder(course)

}


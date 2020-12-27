package com.edt.ut3.ui.calendar.courses_visibility

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
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

        CoursesStatusList.adapter = coursesAdapter

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
    class CourseAdapter : BaseAdapter() {
        var dataSet: List<CourseStatusData> = listOf()
            set(value) {
                field = value
                notifyDataSetChanged()
            }

        override fun getCount() = dataSet.size

        override fun getItem(position: Int) = dataSet[position]

        override fun getItemId(position: Int) = 0L

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val root = (convertView as? CourseStatus) ?: CourseStatus(parent!!.context)

            return root.apply {
                val course = getItem(position)

                setCourse(course) { _, visible ->
                    GlobalScope.launch {
                        CoursesViewModel(context).insert(
                            Course(course.title, visible)
                        )
                    }
                }
            }
        }

    }

}


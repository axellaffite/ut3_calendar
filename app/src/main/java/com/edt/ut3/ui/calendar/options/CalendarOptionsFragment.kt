package com.edt.ut3.ui.calendar.options

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.edt.ut3.R
import com.edt.ut3.backend.celcat.Course
import com.edt.ut3.backend.database.viewmodels.CoursesViewModel
import com.edt.ut3.backend.preferences.PreferencesManager
import com.edt.ut3.misc.toDp
import com.edt.ut3.ui.calendar.CalendarViewModel
import com.edt.ut3.ui.preferences.Theme
import kotlinx.android.synthetic.main.fragment_calendar_options.*
import kotlinx.coroutines.launch

class CalendarOptionsFragment: Fragment() {

    private val viewModel: CalendarViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View?
    {
        return inflater.inflate(R.layout.fragment_calendar_options, container, false).also {
            viewModel.getCoursesVisibility(requireContext()).observe(viewLifecycleOwner) {
                generateCoursesChips(it)
            }
        }
    }

    private fun generateCoursesChips(courses: List<Course>) {
        courses.forEachIndexed { index, course ->
            if (group_list.childCount <= index) {
                group_list.addView(CourseButton(requireContext(), course))
            }

            with(group_list.getChildAt(index) as CourseButton) {
                this.course = course
                this.text = course.title
                setOnClickListener {
                    this.course.visible = !this.course.visible
                    lifecycleScope.launch {
                        CoursesViewModel(context).insert(course)
                    }
                }


                setCompoundDrawablesWithIntrinsicBounds(
                    generateCheckedResource(course.visible), null, null, null
                )
            }
        }

        if (courses.size < group_list.childCount) {
            group_list.removeViews(courses.size, group_list.childCount - courses.size)
        }
    }


    private fun generateCheckedResource(visible: Boolean) = run {
        val icon = if (visible) {
            R.drawable.ic_checked_round
        } else R.drawable.ic_checked_round_empty

        ContextCompat.getDrawable(requireContext(), icon)?.apply {
            val color = when (PreferencesManager(requireContext()).get(PreferencesManager.Preference.THEME)) {
                Theme.LIGHT -> Color.BLACK
                else -> Color.WHITE
            }

            DrawableCompat.setTint(this, color)
        }
    }




    private class CourseButton(context: Context, var course: Course)
        : AppCompatButton(context) {

        val padding = 8.toDp(context).toInt()

        init {
            setPadding(padding, padding, padding, padding)
            compoundDrawablePadding = padding
            setBackgroundColor(Color.TRANSPARENT)
        }

    }
}
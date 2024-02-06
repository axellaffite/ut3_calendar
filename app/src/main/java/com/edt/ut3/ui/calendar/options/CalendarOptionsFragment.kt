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
import com.edt.ut3.databinding.FragmentCalendarOptionsBinding
import com.edt.ut3.misc.extensions.toDp
import com.edt.ut3.ui.calendar.CalendarViewModel
import com.edt.ut3.ui.preferences.Theme
import kotlinx.coroutines.launch

class CalendarOptionsFragment: Fragment() {

    private lateinit var binding: FragmentCalendarOptionsBinding
    private val viewModel: CalendarViewModel by activityViewModels()
    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View?
    {
        binding = FragmentCalendarOptionsBinding.inflate(layoutInflater)
        return binding.root.also {
            viewModel.getCoursesVisibility(requireContext()).observe(viewLifecycleOwner, ::generateCoursesChips)
        }
    }

    private fun generateCoursesChips(courses: List<Course>) {
        courses.forEachIndexed { index, course ->
            if (binding.groupList.childCount <= index) {
                binding.groupList.addView(CourseButton(requireContext(), course))
            }

            with(binding.groupList.getChildAt(index) as CourseButton) {
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

        if (courses.size < binding.groupList.childCount) {
            binding.groupList.removeViews(courses.size, binding.groupList.childCount - courses.size)
        }
    }


    private fun generateCheckedResource(visible: Boolean) = run {
        val icon =
            if (visible) { R.drawable.ic_checked_round }
            else { R.drawable.ic_checked_round_empty }

        ContextCompat.getDrawable(requireContext(), icon)?.apply {
            val phoneTheme = PreferencesManager.getInstance(requireContext()).currentTheme()
            val color = if (phoneTheme == Theme.LIGHT) { Color.BLACK } else { Color.WHITE }

            DrawableCompat.setTint(this, color)
        }
    }




    private class CourseButton(context: Context, var course: Course) : AppCompatButton(context) {
        val padding = 8.toDp(context).toInt()

        init {
            setPadding(padding, 0, padding, 0)
            compoundDrawablePadding = padding
            setBackgroundColor(Color.TRANSPARENT)
            isAllCaps = false
        }

    }
}
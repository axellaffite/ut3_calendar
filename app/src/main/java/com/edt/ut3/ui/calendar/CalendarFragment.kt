package com.edt.ut3.ui.calendar

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.edt.ut3.R

class CalendarFragment : Fragment() {

    private val calendarViewModel by viewModels<CalendarViewModel> { defaultViewModelProviderFactory }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_calendar, container, false).also {

        }
    }
}
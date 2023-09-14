package com.edt.ut3.ui.calendar.view_builders

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import com.edt.ut3.R
import com.edt.ut3.backend.celcat.Event
import com.edt.ut3.databinding.LayoutAllDayBinding
import com.elzozor.yoda.events.EventWrapper

class LayoutAllDay(context: Context, attributeSet: AttributeSet?): LinearLayout(context, attributeSet) {

    constructor(context: Context) : this(context, null)
    var binding: LayoutAllDayBinding? = null
    init {
        binding = LayoutAllDayBinding.bind(this)
    }

    fun setEvents(events: List<EventWrapper>, builder: (EventWrapper) -> View) {
        binding!!.list.adapter = AllDayAdapter(context, events.map { it as Event.Wrapper }.toTypedArray(), builder)
    }

}
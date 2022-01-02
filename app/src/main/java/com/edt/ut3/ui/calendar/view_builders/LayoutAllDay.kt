package com.edt.ut3.ui.calendar.view_builders

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import com.edt.ut3.R
import com.elzozor.yoda.events.EventWrapper
import kotlinx.android.synthetic.main.layout_all_day.view.*

class LayoutAllDay(context: Context, attributeSet: AttributeSet?): LinearLayout(context, attributeSet) {

    constructor(context: Context) : this(context, null)

    init {
        inflate(context, R.layout.layout_all_day, this)
    }

    fun setEvents(events: List<EventWrapper>, builder: (EventWrapper) -> View) {
        list.adapter = AllDayAdapter(context, events.map { it as com.edt.ut3.refactored.models.domain.celcat.EventWrapper }.toTypedArray(), builder)
    }

}
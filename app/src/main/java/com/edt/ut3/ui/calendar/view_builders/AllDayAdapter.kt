package com.edt.ut3.ui.calendar.view_builders

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import com.elzozor.yoda.events.EventWrapper

class AllDayAdapter(context: Context, private val values: Array<com.edt.ut3.refactored.models.domain.celcat.EventWrapper>, private val builder: (EventWrapper) -> View) :
    ArrayAdapter<com.edt.ut3.refactored.models.domain.celcat.EventWrapper>(context, -1, values) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val event = values[position]

        return if (convertView is EventView) {
            convertView.apply {
                setEvent(event)
            }
        } else {
            builder(event)
        }
    }

}
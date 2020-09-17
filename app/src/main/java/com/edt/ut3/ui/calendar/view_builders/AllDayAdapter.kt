package com.edt.ut3.ui.calendar.view_builders

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import com.edt.ut3.backend.celcat.Event
import com.elzozor.yoda.events.EventWrapper

class AllDayAdapter(context: Context, private val values: Array<Event.Wrapper>, private val builder: (EventWrapper) -> View) :
    ArrayAdapter<Event.Wrapper>(context, -1, values) {

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
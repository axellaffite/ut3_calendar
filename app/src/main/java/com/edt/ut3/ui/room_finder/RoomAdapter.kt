package com.edt.ut3.ui.room_finder

import android.graphics.Rect
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.edt.ut3.R
import com.edt.ut3.refactored.models.domain.room_finder.Room
import com.edt.ut3.misc.extensions.toDp
import com.edt.ut3.misc.extensions.toFormattedTime
import kotlinx.android.synthetic.main.room_layout.view.*


class RoomAdapter(val dataset: List<Room>) : RecyclerView.Adapter<RoomAdapter.RoomViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RoomViewHolder {
        val rootView: View =
            LayoutInflater.from(parent.context).inflate(R.layout.room_layout, parent, false)

        return RoomViewHolder(rootView)
    }

    override fun onBindViewHolder(holder: RoomViewHolder, position: Int) {
        setSchedule(holder.scheduleView, dataset[position])
    }

    override fun getItemCount() = dataset.size

    fun setSchedule(view: View, room: Room) {
        view.title.text = room.room

        view.schedules.removeAllViews()
        val formatTime = view.context.getString(R.string.hour_format)
        val formatTotalTime = view.context.getString(R.string.from_to_format)

        room.freeSchedules.forEach { schedule ->
            val startTime = schedule.start.toFormattedTime(formatTime)
            val endTime = schedule.end.toFormattedTime(formatTime)

            view.schedules.addView(TextView(view.context).apply {
                text = formatTotalTime.format(startTime, endTime)
            })
        }
    }

    class RoomViewHolder(val scheduleView: View) : RecyclerView.ViewHolder(scheduleView)

    class RoomSeparator : RecyclerView.ItemDecoration() {

        override fun getItemOffsets(outRect: Rect, view: View,
                                    parent: RecyclerView, state: RecyclerView.State)
        {
            val spaceHeight = 16.toDp(parent.context).toInt()

            with(outRect) {
                if (parent.getChildAdapterPosition(view) == 0) {
                    top = spaceHeight
                }

                left = spaceHeight
                right = spaceHeight
                bottom = spaceHeight
            }
        }
    }

}

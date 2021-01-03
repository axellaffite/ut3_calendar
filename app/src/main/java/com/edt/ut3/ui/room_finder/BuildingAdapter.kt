package com.edt.ut3.ui.room_finder

import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.edt.ut3.backend.goulin_room_finder.Building

class BuildingAdapter(
    private val onClick: (Building) -> Unit
): ListAdapter<Building, BuildingAdapter.BuildingViewHolder>(BuildingDiff()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BuildingViewHolder {
        return BuildingViewHolder.from(parent)
    }

    override fun onBindViewHolder(holder: BuildingViewHolder, position: Int) {
        holder.bind(getItem(position), onClick)
    }

    class BuildingViewHolder private constructor(
        private val view: TextView
    ): RecyclerView.ViewHolder(view) {

        private var _item: Building? = null

        companion object {
            fun from(parent: ViewGroup) : BuildingViewHolder {
                return BuildingViewHolder(
                    TextView(parent.context)
                )
            }
        }

        fun bind(item: Building, onClick: (Building) -> Unit) {
            view.text = item.name
            view.setOnClickListener {
                _item?.let(onClick)
            }
        }

    }

    class BuildingDiff: DiffUtil.ItemCallback<Building>() {
        override fun areItemsTheSame(oldItem: Building, newItem: Building): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: Building, newItem: Building): Boolean {
            return oldItem == newItem
        }
    }

}
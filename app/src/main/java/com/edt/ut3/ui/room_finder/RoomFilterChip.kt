package com.edt.ut3.ui.room_finder

import android.content.Context
import android.util.AttributeSet
import androidx.core.content.ContextCompat
import com.edt.ut3.R
import com.edt.ut3.backend.goulin_room_finder.Room
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipDrawable

class RoomFilterChip(context: Context, attrs: AttributeSet? = null) : Chip(context, attrs) {
    init {
        setChipDrawable(
            ChipDrawable.createFromAttributes(
                context,
                null,
                0,
                R.style.Widget_MaterialComponents_Chip_Filter
            )
        )

        setChipBackgroundColorResource(R.color.foregroundColor)
        setTextColor(ContextCompat.getColor(context, R.color.textColor))
    }

    var filter: (List<Room>) -> List<Room> = { it }
}
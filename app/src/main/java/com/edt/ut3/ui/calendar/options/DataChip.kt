package com.edt.ut3.ui.calendar.options

import android.content.Context
import com.edt.ut3.R
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipDrawable

class DataChip<T>(context: Context, val data: T, parser: (T) -> String): Chip(context) {

    init {
        setChipDrawable(
            ChipDrawable.createFromAttributes(
                context,
                null,
                0,
                R.style.Widget_MaterialComponents_Chip_Filter
            )
        )

        var new = parser(data).subSequence(0, 13).toString()
        if (text.length < new.length) {
            new += "..."
        }

        text = new
    }

}
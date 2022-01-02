package com.edt.ut3.refactored.ui

import android.content.Context
import com.edt.ut3.R
import com.edt.ut3.refactored.models.domain.maps.Place
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipDrawable
import java.util.*

class PlaceChip(
    context: Context,
    val place: Place,
    onClickListener: () -> Unit = {}
) : Chip(context) {
    init {
        setChipDrawable(
            ChipDrawable.createFromAttributes(
                context,
                null,
                0,
                R.style.Widget_MaterialComponents_Chip_Action
            )
        )

        setChipBackgroundColorResource(R.color.foregroundColor)
        text = place.title.uppercase(Locale.FRENCH)

        setOnClickListener { onClickListener() }
    }
}
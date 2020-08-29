package com.edt.ut3.ui.map

import android.content.Context
import android.graphics.Bitmap
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout
import com.edt.ut3.R

class PlaceInfo(context: Context, attrs: AttributeSet?): ConstraintLayout(context, attrs) {

    constructor(context: Context): this(context, null)

    var title: String = ""
    var description: String = ""
    var picture: Bitmap? = null

    init {
        inflate(context, R.layout.place_info, this)
    }

    fun setImage() {

    }

    fun setDescription() {

    }

    fun setTitle() {

    }

    private fun redraw() {
        invalidate()
        requestLayout()
    }
}
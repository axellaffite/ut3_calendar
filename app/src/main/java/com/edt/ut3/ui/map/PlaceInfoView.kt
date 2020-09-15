package com.edt.ut3.ui.map

import android.content.Context
import android.util.AttributeSet
import androidx.core.widget.NestedScrollView
import com.edt.ut3.R
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.place_info.view.*

class PlaceInfoView(context: Context, attrs: AttributeSet?): NestedScrollView(context, attrs) {

    constructor(context: Context): this(context, null)

    var titleText: String = ""
        set(value) {
            title.text = value
            redraw()
            field = value
        }
    var descriptionText: String = ""
        set(value) {
            description.text = value
            redraw()
            field = value
        }

    var picture: String? = null
        set(value) {
            Picasso.get().load(value).into(image)
            field = value
        }

    init {
        inflate(context, R.layout.place_info, this)
    }

    private fun redraw() {
        invalidate()
        requestLayout()
    }
}
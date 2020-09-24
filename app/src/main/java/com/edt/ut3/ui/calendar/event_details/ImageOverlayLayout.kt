package com.edt.ut3.ui.calendar.event_details

import android.content.Context
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout
import com.edt.ut3.R
import kotlinx.android.synthetic.main.fragment_image_view_pager.view.*

class ImageOverlayLayout(context: Context, attrs: AttributeSet?): ConstraintLayout(context, attrs) {

    constructor(context: Context) : this(context, null)

    var onDeleteRequest: (() -> Unit)? = null

    init {
        val view = inflate(context, R.layout.fragment_image_view_pager, this)
        view.run {
            delete.setOnClickListener {
                onDeleteRequest?.invoke()
            }
        }
    }

}
package com.edt.ut3.ui.calendar.event_details

import android.content.Context
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout
import com.edt.ut3.R
import com.edt.ut3.databinding.FragmentImageViewPagerBinding

class ImageOverlayLayout(context: Context, attrs: AttributeSet?): ConstraintLayout(context, attrs) {

    constructor(context: Context) : this(context, null)

    var onDeleteRequest: (() -> Unit)? = null

    init {
        val view = FragmentImageViewPagerBinding.bind(this)
        view.run {
            view.delete.setOnClickListener {
                onDeleteRequest?.invoke()
            }
        }
    }

}
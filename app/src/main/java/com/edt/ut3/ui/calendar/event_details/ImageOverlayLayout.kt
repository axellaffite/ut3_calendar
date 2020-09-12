package com.edt.ut3.ui.calendar.event_details

import android.content.Context
import android.util.AttributeSet
import androidx.constraintlayout.motion.widget.MotionLayout
import com.edt.ut3.R
import kotlinx.android.synthetic.main.fragment_image_view_pager.view.*

class ImageOverlayLayout(context: Context, attrs: AttributeSet?): MotionLayout(context, attrs) {

    constructor(context: Context) : this(context, null)

    private var visible = false

    var onDeleteRequest: (() -> Unit)? = null

    init {
        val view = inflate(context, R.layout.fragment_image_view_pager, this)
        view.run {
            delete.setOnClickListener {
                onDeleteRequest?.invoke()
            }
        }

        setTransition(R.id.show_hide)
    }

    fun showHideOverlay() {
        if (!visible) {
            transitionToEnd()
        } else {
            transitionToStart()
        }

        visible = !visible
    }

}
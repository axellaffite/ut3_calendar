package com.edt.ut3.ui.custom_views

import android.content.Context
import android.util.AttributeSet
import androidx.constraintlayout.motion.widget.MotionLayout

class MotionLayoutSideScroll(context: Context, attrs: AttributeSet): MotionLayout(context, attrs) {

    val sideScrollListener = SideScrollViewListener(this)

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        sideScrollListener.updateDimensions(w.toFloat(), h.toFloat())
    }

}
package com.edt.ut3.ui.custom_views

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.core.widget.NestedScrollView

class BetterNestedScrollView(context: Context, attributeSet: AttributeSet? = null):
    NestedScrollView(context, attributeSet) {

    private val dragthreshold = 100
    private var downX = 0
    private var downY = 0

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        return when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.rawX.toInt()
                downY = event.rawY.toInt()
                false
            }

            MotionEvent.ACTION_MOVE -> {
                val distanceX = Math.abs(event.rawX.toInt() - downX)
                val distanceY = Math.abs(event.rawY.toInt() - downY)
                if (distanceY > distanceX && distanceY > dragthreshold) {
                    return true
                }

                false
            }

            else -> super.onInterceptTouchEvent(event)
        }
    }

}
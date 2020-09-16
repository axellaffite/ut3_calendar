package com.edt.ut3.ui.custom_views

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.core.widget.NestedScrollView

class BetterNestedScrollView(context: Context, attributeSet: AttributeSet? = null):
    NestedScrollView(context, attributeSet) {

    private val dragthreshold = 50
    private var downX = 0
    private var downY = 0

    var onScrollChangeListeners = mutableListOf<(x: Int, y: Int, oldX: Int, oldY: Int) -> Unit>()


    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        return when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.rawX.toInt()
                downY = event.rawY.toInt()
                super.onInterceptTouchEvent(event)
            }

            MotionEvent.ACTION_MOVE -> {

                val distanceX = Math.abs(event.rawX.toInt() - downX)
                val distanceY = Math.abs(event.rawY.toInt() - downY)

                distanceY > distanceX && distanceY > dragthreshold
            }

            else -> { super.onInterceptTouchEvent(event) }
        }
    }

    override fun onScrollChanged(x: Int, y: Int, oldX: Int, oldY: Int) {
        onScrollChangeListeners.forEach { it.invoke(x, y, oldX, oldY) }

        super.onScrollChanged(x, y, oldX, oldY)
    }

}
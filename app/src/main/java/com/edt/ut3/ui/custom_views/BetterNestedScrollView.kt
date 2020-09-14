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

                println("$distanceX $distanceY")
                if (distanceY > distanceX && distanceY > dragthreshold) {
                    println("TRUE SCROLL")
                    return true
                }

                false
            }

            else -> {
                val ev = super.onInterceptTouchEvent(event)
                println("ev scrollview: $ev")
                ev
            }
        }
    }

}
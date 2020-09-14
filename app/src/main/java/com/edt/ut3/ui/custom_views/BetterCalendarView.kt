package com.edt.ut3.ui.custom_views

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.CalendarView


class BetterCalendarView(context: Context, attributeSet: AttributeSet?):
    CalendarView(context, attributeSet)
{

    private val dragthreshold = 100
    private var downX = 0
    private var downY = 0
    private var handlingEvent = false




    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.rawX.toInt()
                downY = event.rawY.toInt()
                handlingEvent = false
            }

            MotionEvent.ACTION_MOVE -> {
                println("MOVING")
                val distanceX = Math.abs(event.rawX.toInt() - downX)
                val distanceY = Math.abs(event.rawY.toInt() - downY)
                if (distanceX > distanceY && distanceX > dragthreshold) {
                    println("TRUE CALENDAR")
                    handlingEvent = true
                }

                handlingEvent = false
            }

            else -> {
                val ret = super.onInterceptTouchEvent(event)
                println("ev: ${event.action} $ret")
                handlingEvent = ret
            }
        }

        return handlingEvent
    }

}
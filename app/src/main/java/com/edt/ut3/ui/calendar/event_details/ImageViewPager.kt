package com.edt.ut3.ui.calendar.event_details

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.viewpager.widget.ViewPager

class ImageViewPager(context: Context, attributeSet: AttributeSet): ViewPager(context, attributeSet) {

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        val handled = !ImageViewFragment.isZoomed() && super.onInterceptTouchEvent(ev)
//        println("$scrollable INTERCEPT $handled")
        return handled
    }

}
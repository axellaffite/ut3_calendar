package com.edt.ut3.ui.custom_views

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.ListView

class NoScrollListView(context: Context, attributeSet: AttributeSet?): ListView(context, attributeSet) {

    constructor(context: Context) : this(context, null)

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(ev: MotionEvent?) = when (ev?.action) {
        MotionEvent.ACTION_MOVE -> false
        else -> super.onTouchEvent(ev)
    }

}
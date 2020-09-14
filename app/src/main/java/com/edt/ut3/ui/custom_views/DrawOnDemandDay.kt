//package com.edt.ut3.ui.custom_views
//
//import android.content.Context
//import android.util.AttributeSet
//import com.elzozor.yoda.Day
//
//class DrawOnDemandDay(context: Context, attrs: AttributeSet? = null): Day(context, attrs) {
//
//    var drawBlocked : Boolean = false
//
//    override fun invalidate() {
//        if (!drawBlocked) {
//            super.invalidate()
//        }
//    }
//
//}
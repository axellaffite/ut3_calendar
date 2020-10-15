package com.edt.ut3.misc.extensions

import androidx.viewpager2.widget.ViewPager2

fun ViewPager2.notifyDataSetChanged() = this.post {
    adapter?.notifyDataSetChanged()
    post {
        requestTransform()
    }
}
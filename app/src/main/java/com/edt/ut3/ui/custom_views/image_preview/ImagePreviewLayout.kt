package com.edt.ut3.ui.custom_views.image_preview

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.edt.ut3.misc.toDp

class ImagePreviewLayout(context: Context, attributeSet: AttributeSet): RecyclerView(context, attributeSet) {

    init {
        // We want an horizontal scroll for this view
        layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        setHasFixedSize(true)
        addItemDecoration(MarginItemDecoration(3.toDp(context).toInt()))
    }

    fun notifyDataSetChanged() = adapter?.notifyDataSetChanged()

    class MarginItemDecoration(private val spaceHeight: Int) : RecyclerView.ItemDecoration() {
        override fun getItemOffsets(outRect: Rect, view: View,
                                    parent: RecyclerView, state: State) {
            with(outRect) {
//                if (parent.getChildAdapterPosition(view) == 0) {
//                    left =  spaceHeight
//                }

                top = spaceHeight
                right = spaceHeight
                bottom = spaceHeight
            }
        }
    }

}
package com.edt.ut3.ui.custom_views.image_preview

import android.graphics.Bitmap
import android.graphics.Rect
import android.media.ThumbnailUtils
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.ImageView
import androidx.cardview.widget.CardView
import androidx.core.graphics.scale
import androidx.core.view.marginLeft
import androidx.recyclerview.widget.RecyclerView
import com.edt.ut3.misc.toDp

class ImagePreviewAdapter(val dataset: List<Bitmap>) : RecyclerView.Adapter<ImagePreviewAdapter.ImageViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ImageViewHolder(
        CardView(parent.context).apply {
            addView(ImageView(parent.context).apply {
                layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
            })

            radius = 8.toDp(parent.context)
        }
    ).apply {

    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val size = 64.toDp(holder.imgView.context).toInt()
        val imgView = holder.imgView.getChildAt(0) as ImageView
        imgView.setImageBitmap(ThumbnailUtils.extractThumbnail(dataset[position], size, size))
    }

    override fun getItemCount() = dataset.size

    class ImageViewHolder(val imgView: CardView) : RecyclerView.ViewHolder(imgView)

}
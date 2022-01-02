package com.edt.ut3.ui.custom_views.image_preview

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.edt.ut3.R
import com.edt.ut3.backend.note.Picture
import kotlinx.android.synthetic.main.layout_image_preview_holder.view.*

typealias ImagePreviewItemClickListener = ((v: View, picture: Picture, pictures: List<Picture>) -> Unit)

class ImagePreviewAdapter(
    var dataset: List<Picture>,
    var onAddPictureClickListener: ((v: View) -> Unit)? = null,
    var onItemClickListener: ImagePreviewItemClickListener? = null
) : RecyclerView.Adapter<ImagePreviewAdapter.ImageViewHolder>() {

    companion object {
        fun from(layout: RecyclerView): ImagePreviewAdapter {
            return layout.adapter as ImagePreviewAdapter
        }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.layout_image_preview_holder, parent, false) as CardView

        return ImageViewHolder(v)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        holder.setIsRecyclable(false)

        if (position == dataset.size) {
            bindAddPicture(holder)
        } else {
            bindClassicPicture(holder, position)
        }
    }

    private fun bindClassicPicture(holder: ImageViewHolder, position: Int) {
        holder.apply {
            val picture = dataset[position]
            picture.loadThumbnailInto(view.thumbnail)


            view.setOnClickListener {
                onItemClickListener?.invoke(view, dataset[position], dataset)
            }
        }
    }

    private fun bindAddPicture(holder: ImageViewHolder) {
        holder.apply {
            view.thumbnail.setImageResource(R.drawable.ic_add)
            view.setOnClickListener { onAddPictureClickListener?.invoke(it) }
        }
    }

    override fun getItemCount() = dataset.size + 1

    class ImageViewHolder(val view: CardView) : RecyclerView.ViewHolder(view)

}
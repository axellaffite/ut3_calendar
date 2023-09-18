package com.edt.ut3.ui.custom_views.image_preview

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.edt.ut3.R
import com.edt.ut3.backend.note.Picture
import com.edt.ut3.databinding.LayoutImagePreviewHolderBinding

class ImagePreviewAdapter(var dataset: List<Picture>) : RecyclerView.Adapter<ImagePreviewAdapter.ImageViewHolder>() {

    companion object {
        fun from(layout: RecyclerView): ImagePreviewAdapter {
            return layout.adapter as ImagePreviewAdapter
        }
    }

    var onItemClickListener: ((v: View, picture: Picture, pictures: List<Picture>) -> Unit)? = null
    var onAddPictureClickListener: ((v: View) -> Unit)? = null
    private lateinit var binding: LayoutImagePreviewHolderBinding

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        binding = LayoutImagePreviewHolderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ImageViewHolder(binding.root)
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
            picture.loadThumbnailInto(binding.thumbnail)


            view.setOnClickListener {
                onItemClickListener?.invoke(view, dataset[position], dataset)
            }
        }
    }

    private fun bindAddPicture(holder: ImageViewHolder) {
        holder.apply {
            binding.thumbnail.setImageResource(R.drawable.ic_add)
            binding.root.setOnClickListener { onAddPictureClickListener?.invoke(it) }
        }
    }

    override fun getItemCount() = dataset.size + 1

    class ImageViewHolder(val view: CardView) : RecyclerView.ViewHolder(view)

}
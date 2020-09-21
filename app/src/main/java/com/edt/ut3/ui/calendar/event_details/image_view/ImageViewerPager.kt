package com.edt.ut3.ui.calendar.event_details.image_view

import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.lifecycle.LiveData
import androidx.recyclerview.widget.RecyclerView
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import com.edt.ut3.R
import com.edt.ut3.backend.note.Note
import kotlinx.android.synthetic.main.fragment_image_view.view.*

class ImageViewerPager(val note: LiveData<Note>) : RecyclerView.Adapter<ImageViewerPager.ImageViewHolder>() {

    inner class ImageViewHolder(val imgView: View): RecyclerView.ViewHolder(imgView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.fragment_image_view, parent, false)

        return ImageViewHolder(view)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        note.value?.pictures?.let {
            holder.imgView.image?.apply {
                orientation = SubsamplingScaleImageView.ORIENTATION_USE_EXIF
                setImage(ImageSource.uri(it[position].picture))
                setOnImageEventListener(object: SubsamplingScaleImageView.OnImageEventListener {
                    override fun onReady() {
                        holder.imgView.loading_animation?.visibility = VISIBLE
                    }

                    override fun onImageLoaded() {
                        holder.imgView.loading_animation.visibility = GONE
                    }

                    override fun onPreviewLoadError(e: Exception?) {
                        // Do nothing here
                    }

                    override fun onImageLoadError(e: Exception?) {
                        // Do nothing here
                    }

                    override fun onTileLoadError(e: Exception?) {
                        // Do nothing here
                    }

                    override fun onPreviewReleased() {
                        // Do nothing here
                    }

                })
            }
        }
    }

    override fun getItemCount() = note.value?.pictures?.size ?: 0

}
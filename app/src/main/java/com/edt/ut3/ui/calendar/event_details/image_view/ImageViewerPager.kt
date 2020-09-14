package com.edt.ut3.ui.calendar.event_details.image_view

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import com.edt.ut3.R
import com.edt.ut3.ui.calendar.CalendarViewModel
import kotlinx.android.synthetic.main.fragment_image_view.view.*

class ImageViewerPager(val viewModel: CalendarViewModel) : RecyclerView.Adapter<ImageViewerPager.ImageViewHolder>() {

    inner class ImageViewHolder(val imgView: View): RecyclerView.ViewHolder(imgView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.fragment_image_view, parent, false)

        return ImageViewHolder(view)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        viewModel.selectedEventNote?.pictures?.let {
            holder.imgView.image?.apply {
                orientation = SubsamplingScaleImageView.ORIENTATION_USE_EXIF
                setImage(ImageSource.uri(it[position].picture))
            }
        }
    }

    override fun getItemCount() = viewModel.selectedEventNote?.pictures?.size ?: 0

}
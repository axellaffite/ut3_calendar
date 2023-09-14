package com.edt.ut3.ui.map

import android.content.Context
import android.util.AttributeSet
import android.widget.ImageView
import androidx.core.widget.NestedScrollView
import com.edt.ut3.R
import com.edt.ut3.databinding.PlaceInfoBinding
import com.google.android.material.button.MaterialButton
import com.squareup.picasso.Picasso

class PlaceInfoView(context: Context, attrs: AttributeSet?): NestedScrollView(context, attrs) {
    private var binding: PlaceInfoBinding? = null
    public var goTo: MaterialButton? = null
    public var image: ImageView? = null

    constructor(context: Context): this(context, null) {
    }

    var titleText: String = ""
        set(value) {
            binding!!.title.text = value
            redraw()
            field = value
        }

    var descriptionText: String = ""
        set(value) {
            binding!!.description.text = value
            redraw()
            field = value
        }

    var picture: String? = null
        set(value) {
            if (value != null) {
                Picasso.get().load(value).into(binding!!.image)
            } else {
                Picasso.get().load(R.drawable.no_image_placeholder).into(binding!!.image)
            }
            field = value
        }

    init {
        binding = PlaceInfoBinding.bind(this)
        goTo = binding!!.goTo
        image = binding!!.image
    }

    private fun redraw() {
        invalidate()
        requestLayout()
    }
}
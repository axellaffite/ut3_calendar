package com.edt.ut3.ui.map.custom_makers

import androidx.core.content.ContextCompat
import com.edt.ut3.R
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

class LocationMarker(map: MapView): Marker(map) {
    init {
        icon = ContextCompat.getDrawable(map.context, R.drawable.position_icon)
    }
}
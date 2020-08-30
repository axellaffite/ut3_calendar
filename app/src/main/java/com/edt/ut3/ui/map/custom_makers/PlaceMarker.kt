package com.edt.ut3.ui.map.custom_makers

import android.view.MotionEvent
import androidx.core.content.ContextCompat
import com.edt.ut3.ui.map.SearchPlaceAdapter.Place
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

class PlaceMarker(map: MapView, val place: Place): Marker(map) {
    var onLongClickListener : (() -> Boolean)? = null

    init {
        icon = ContextCompat.getDrawable(map.context, place.getIcon())
        position = place.geolocalisation
        title = place.title
    }

    override fun onSingleTapConfirmed(event: MotionEvent?, mapView: MapView?): Boolean {

        return super.onSingleTapConfirmed(event, mapView)
    }

    override fun onLongPress(event: MotionEvent?, mapView: MapView?): Boolean {
        if (hitTest(event, mapView)) {
            return onLongClickListener?.invoke() ?: super.onLongPress(event, mapView)
        }
        return super.onLongPress(event, mapView)
    }
}
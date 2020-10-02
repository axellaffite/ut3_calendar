package com.edt.ut3.ui.map.custom_makers

import android.graphics.Color
import android.view.MotionEvent
import androidx.core.content.ContextCompat
import com.edt.ut3.R
import com.edt.ut3.backend.maps.Place
import com.elzozor.yoda.utils.NumberExtensions.toDp
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

class PlaceMarker(map: MapView, val place: Place): Marker(map) {
    var onLongClickListener : (() -> Boolean)? = null
    var onClickListener : (() -> Boolean)? = null

    init {
//        icon = ContextCompat.getDrawable(map.context, place.getIcon())
        position = place.geolocalisation
        title = place.title
        textLabelBackgroundColor = Color.TRANSPARENT//ContextCompat.getColor(map.context, R.color.mapLabelBackground)
        textLabelForegroundColor = ContextCompat.getColor(map.context, R.color.colorAccent)
        textLabelFontSize = 16.toDp(map.context).toInt()
        setTextIcon(place.title)
    }

    override fun onSingleTapConfirmed(event: MotionEvent?, mapView: MapView?): Boolean {
        if (hitTest(event, mapView)) {
            return onClickListener?.invoke() ?: super.onSingleTapConfirmed(event, mapView)
        }

        return super.onSingleTapConfirmed(event, mapView)
    }

    override fun onLongPress(event: MotionEvent?, mapView: MapView?): Boolean {
        if (hitTest(event, mapView)) {
            return onLongClickListener?.invoke() ?: super.onLongPress(event, mapView)
        }

        return super.onLongPress(event, mapView)
    }
}
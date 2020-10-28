package com.edt.ut3.backend.maps

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import org.osmdroid.util.GeoPoint

object MapsUtils {

    /**
     * Launch a GoogleMaps Intent to navigate
     * from the [current point][from] to the [destination point][to].
     *
     *
     * @param activity The current activity
     * @param from The current position, if null means the current position
     * @param to The destination point
     * @param toTitle The destination point title
     * @param onError If the Intent cannot be launched
     */
    fun routeFromTo(activity: Activity, from: GeoPoint?, to: GeoPoint, toTitle: String, onError: (() -> Unit)? = null) {
        try {
            var requestLink = ("https://www.google.com/maps/dir/?api=1" +
                    "&destination=${to.latitude.toFloat()},${to.longitude.toFloat()}" +
                    "&destination_place_id=$toTitle" +
                    "&travelmode=walking")

            from?.run {
                requestLink += "&origin=${latitude.toFloat()},${longitude.toFloat()}"
            }

            // Create a Uri from an intent string. Use the result to create an Intent.
            val gmmIntentUri = Uri.parse(requestLink)

            // Create an Intent from gmmIntentUri. Set the action to ACTION_VIEW
            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)

            // Make the Intent explicit by setting the Google Maps package
            mapIntent.setPackage("com.google.android.apps.maps")

            activity.startActivity(mapIntent)
        } catch (e: ActivityNotFoundException) {
            onError?.invoke()
        }
    }

}
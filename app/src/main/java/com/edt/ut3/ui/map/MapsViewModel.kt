package com.edt.ut3.ui.map

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import arrow.core.*
import arrow.typeclasses.Semigroup
import arrow.typeclasses.Semigroup.Companion.list
import com.edt.ut3.R
import com.edt.ut3.refactored.extensions.async
import com.edt.ut3.refactored.models.domain.maps.Place
import com.edt.ut3.refactored.models.repositories.preferences.PreferencesManager
import com.edt.ut3.refactored.models.services.maps.MapsService
import com.edt.ut3.refactored.viewmodels.PlaceViewModel
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers.IO
import org.osmdroid.util.GeoPoint
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds

class MapsViewModel(
    private val placeViewModel: PlaceViewModel,
    private val mapsService: MapsService,
    private val preferenceManager: PreferencesManager
) : ViewModel() {
    val places = placeViewModel.selectAllLD()


    /**
     * The error variable will store the last exception
     * encountered and the errorCount the number
     * of exceptions encountered.
     * There are 4 cases after the two downloads :
     * - errorCount = 0 : There are no error, we can
     *                    display a success message
     * - errorCount = 1 : The Paul Sabatier places aren't
     *                    available, the internet connection
     *                    seems to be good as the second download
     *                    is a success, we check if it's a parsing
     *                    error or a timeout error.
     * - errorCount = 2 : Same logic as =1 but for the Crous places.
     * - errorCount = 3 : The internet connection does not seem to work,
     *                    we display an error message saying to check it.
     */
    @Suppress("BlockingMethodInNonBlockingContext")
    suspend fun launchDataUpdate(): DownloadResult {
        if (successfulUpdateIsTooRecent()) return DownloadResult(null)

        return parseResult(handleCalls(prepareCalls()))
    }

    private suspend fun successfulUpdateIsTooRecent(): Boolean {
        val durationSinceLastUpdate = (
            System.currentTimeMillis() - preferenceManager.lastSuccessfulBuildingUpdate
        ).milliseconds

        return durationSinceLastUpdate < 1.hours && placeViewModel.hasPlaces()
    }

    private fun prepareCalls() = listOf(
        async(IO) {
            Either.catch { mapsService.getPaulSabatierPlaces() }
                .mapLeft { R.string.building_update_failed }
        },
        async(IO) {
            Either.catch { mapsService.getCrousPlaces() }
                .mapLeft { R.string.restaurant_update_failed }
        }
    )

    private suspend fun handleCalls(calls: List<Deferred<Either<Int, List<Place>>>>): Ior<NonEmptyList<Int>, List<Place>> {
        fun Ior<NonEmptyList<Int>, List<Place>>.combine(other: Ior<NonEmptyList<Int>, List<Place>>) =
            combine(Semigroup.nonEmptyList(), list(), other)

        return calls.fold(Ior.Right(emptyList())) { acc, deferred ->
            deferred.await().fold(
                ifLeft = { error -> acc.combine(nonEmptyListOf(error).leftIor()) },
                ifRight = { newPlaces -> acc.combine(newPlaces.rightIor()) }
            )
        }
    }

    private suspend fun parseResult(result: Ior<NonEmptyList<Int>, List<Place>>) = result.fold(
        fa = {
            DownloadResult(if (it.size == 2) R.string.unable_to_retrieve_data else it.head)
        },
        fb = {
            placeViewModel.insertAll(it)
            preferenceManager.lastSuccessfulBuildingUpdate = System.currentTimeMillis()
            DownloadResult()
        },
        fab = { error, places ->
            placeViewModel.insertAll(places)
            DownloadResult(error.head)
        },
    )

    class DownloadResult(@StringRes val errorRes: Int? = null)

    /**
     * Launch a GoogleMaps Intent to navigate
     * from the current position to the [destination point][to].
     *
     *
     * @param activity The current activity
     * @param to The destination point
     * @param toTitle The destination point title
     * @param onError If the Intent cannot be launched
     */
    fun routeFromTo(
        activity: Activity,
        to: GeoPoint,
        toTitle: String,
        onError: (() -> Unit)? = null
    ) {
        try {
            val requestLink = ("https://www.google.com/maps/dir/?api=1" +
                "&destination=${to.latitude.toFloat()},${to.longitude.toFloat()}" +
                "&destination_place_id=$toTitle" +
                "&travelmode=walking")

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


    fun matchingPlaces(placesToMatch: List<String>): List<Place> {
        val savedPlaces = places.value ?: return emptyList()
        return placesToMatch.mapNotNull { toMatch ->
            savedPlaces.find { toMatch.contains(it.title, ignoreCase = true) }
        }.distinct()
    }

}
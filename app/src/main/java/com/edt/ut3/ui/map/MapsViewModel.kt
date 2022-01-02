package com.edt.ut3.ui.map

import androidx.lifecycle.ViewModel
import com.edt.ut3.refactored.models.domain.maps.Place
import com.edt.ut3.refactored.models.services.maps.MapsService
import com.edt.ut3.refactored.viewmodels.PlaceViewModel
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class MapsViewModel: ViewModel(), KoinComponent {
    private val placeViewModel: PlaceViewModel by inject()
    private val mapsService: MapsService by inject()

    val places by lazy { placeViewModel.selectAllLD() }


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
        var error: Exception? = null
        var errorCount = 0

        val newPlaces = mutableListOf<Place>()

        // First download for Paul Sabatier places
        // (from our github)
        try {
            val paulSabatierPlaces = withContext(IO) { mapsService.getPaulSabatierPlaces() }
            newPlaces.addAll(paulSabatierPlaces)
        } catch (e: Exception) {
            e.printStackTrace()
            error = e
            errorCount += 1
        }


        // Second download for Crous places
        // (from the government website)
        try {
            val crousPlaces = withContext(IO) { mapsService.getCrousPlaces() }
            newPlaces.addAll(crousPlaces)
        } catch (e: Exception) {
            e.printStackTrace()
            error = e
            errorCount += 2
        }


        placeViewModel.insert(*newPlaces.toTypedArray())
        return DownloadResult(errorCount, error)
    }

    class DownloadResult(val errorCount: Int, val error: Exception?) {
        override fun toString(): String {
            return "DownloadResult: Error count=$errorCount, Error type= ${error?.javaClass?.simpleName}"
        }
    }

}
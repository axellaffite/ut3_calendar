package com.edt.ut3.ui.map

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.axellaffite.fastgallery.FastGallery
import com.axellaffite.fastgallery.ImageLoader
import com.edt.ut3.R
import com.edt.ut3.backend.maps.MapsUtils
import com.edt.ut3.backend.maps.Place
import com.edt.ut3.backend.preferences.PreferencesManager
import com.edt.ut3.misc.extensions.hideKeyboard
import com.edt.ut3.ui.map.custom_makers.PlaceMarker
import com.edt.ut3.ui.preferences.Theme
import com.edt.ut3.ui.theme.UT3Theme
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Overlay
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay
import java.io.File
import java.util.Locale

class MapsFragment : Fragment() {

    private val viewModel: MapsViewModel by viewModels()

    private var composeState by mutableStateOf(MapsScreenState())
    private val snackbarHostState = SnackbarHostState()

    private lateinit var mapView: MapView

    private var selectedPlace: Place? = null
    private var selectedPlaceMarker: PlaceMarker? = null
    private var downloadJob: Job? = null

    private val allPlaces = mutableListOf<Place>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        mapView = MapView(requireContext())
        return ComposeView(requireContext())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (view as ComposeView).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                UT3Theme {
                    MapsScreen(
                        state = composeState,
                        callbacks = buildCallbacks(),
                        mapView = mapView,
                        snackbarHostState = snackbarHostState
                    )
                }
            }
        }

        configureMap()
        setupListeners()
        moveToPaulSabatier()
        startDownloadJob()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    private fun buildCallbacks() = MapsScreenCallbacks(
        onSearchQueryChanged = ::handleSearchQueryChanged,
        onSearchFocused = ::handleSearchFocused,
        onCategoryToggled = ::handleCategoryToggled,
        onSearchResultClicked = ::handleSearchResultClicked,
        onGoToClicked = ::handleGoToClicked,
        onPlaceImageClicked = ::handlePlaceImageClicked,
        onMapTapped = ::handleMapTapped,
        onBackPressed = ::handleBackPressed
    )

    // --- Handler methods ---

    private fun handleSearchQueryChanged(query: String) {
        composeState = composeState.copy(searchQuery = query)
        filterPlaces()
    }

    private fun handleSearchFocused() {
        composeState = composeState.copy(isSearching = true)
        filterPlaces()
    }

    private fun handleCategoryToggled(category: String) {
        val updatedCategories = composeState.categories.map {
            if (it.name == category) it.copy(isSelected = !it.isSelected) else it
        }
        composeState = composeState.copy(categories = updatedCategories)
        filterPlaces()
    }

    private fun handleSearchResultClicked(place: Place) {
        selectedPlace = place
        composeState = composeState.copy(searchQuery = place.title)
        displayPlaceInfo()
    }

    private fun handleMapTapped() {
        composeState = composeState.copy(isSearching = false, selectedPlace = null)
        selectedPlaceMarker?.closeInfoWindow()
        selectedPlaceMarker = null
        hideKeyboard()
        refreshMapMarkers()
    }

    private fun handleGoToClicked(place: Place) {
        activity?.let {
            MapsUtils.routeFromTo(
                it,
                GeoPoint(place.geolocalisation),
                place.title
            ) {
                lifecycleScope.launch {
                    snackbarHostState.showSnackbar(
                        getString(R.string.unable_to_launch_googlemaps)
                    )
                }
            }
        }
    }

    private fun handlePlaceImageClicked(place: Place) {
        val image = Pair(place.photo, R.drawable.no_image_placeholder)
        FastGallery.Builder<Pair<String?, Int>>()
            .withImages(listOf(image))
            .withConverter { pair: Pair<String?, Int>, loader: ImageLoader<Pair<String?, Int>> ->
                viewLifecycleOwner.lifecycleScope.launch {
                    lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                        pair.first?.let {
                            loader.fromURL(it, true)
                        } ?: run {
                            loader.fromResource(pair.second)
                        }
                    }
                }
            }
            .build()
            .show(parentFragmentManager, "detailsImage")
    }

    private fun handleBackPressed() {
        if (composeState.isSearching) {
            handleMapTapped()
        } else {
            activity?.onBackPressed()
        }
    }

    // --- Search / filter logic ---

    private fun filterPlaces() {
        val query = composeState.searchQuery
        val selectedCategories = composeState.categories.filter { it.isSelected }.map { it.name }

        val filtered = allPlaces
            .filter { place ->
                place.title.contains(query, ignoreCase = true)
            }
            .let { list ->
                if (selectedCategories.isEmpty()) list
                else list.filter { it.type in selectedCategories }
            }
            .sortedBy { it.title.indexOf(query, ignoreCase = true) }

        composeState = composeState.copy(searchResults = filtered)
        refreshMapMarkers()
    }

    private fun refreshMapMarkers() {
        mapView.overlays.forEach { if (it is Marker) it.closeInfoWindow() }
        mapView.overlays.removeAll { it is PlaceMarker }
        addPlacesOnMap(composeState.searchResults)
        mapView.invalidate()
    }

    // --- Map configuration ---

    private fun configureMap() {
        val path: File = requireContext().filesDir
        val osmdroidBasePathNew = File(path, "osmdroid")
        osmdroidBasePathNew.mkdirs()
        val osmdroidTileCacheNew = File(osmdroidBasePathNew, "tiles")
        osmdroidTileCacheNew.mkdirs()

        val configuration = Configuration.getInstance()
        configuration.apply {
            userAgentValue = requireActivity().packageName
        }

        mapView.apply {
            tileProvider.clearTileCache()
            tileProvider.tileCache.clear()

            minZoomLevel = 15.0
            maxZoomLevel = 18.0

            val providers = context.resources.getStringArray(R.array.tile_provider)
            val providerName = getString(R.string.provider_name)
            Log.d(this@MapsFragment::class.simpleName, "Providers: ${providers.toList()}")
            val tileSource = XYTileSource(
                providerName, 1, 20, 256, ".png", providers
            )

            TileSourceFactory.addTileSource(tileSource)
            setTileSource(TileSourceFactory.getTileSource(providerName))

            isTilesScaledToDpi = true
            setMultiTouchControls(true)

            overlays.add(RotationGestureOverlay(this).apply {
                isEnabled = true
            })

            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)

            addMapListener(object : MapListener {
                var shouldHandle = true
                override fun onScroll(event: ScrollEvent?): Boolean {
                    if (composeState.isSearching) {
                        handleMapTapped()
                    }

                    if (!shouldHandle) {
                        controller.stopAnimation(false)
                        shouldHandle = true
                        return true
                    }

                    val newPos = GeoPoint(
                        mapCenter.latitude.coerceIn(43.55529003675331, 43.573841249471016),
                        mapCenter.longitude.coerceIn(1.4533669607980073, 1.47867475237166)
                    )

                    if (newPos != mapCenter) {
                        shouldHandle = false
                        controller.setCenter(newPos)
                        return false
                    }

                    return true
                }

                override fun onZoom(event: ZoomEvent?) = false
            })

            val overlay = object : Overlay() {
                override fun onSingleTapConfirmed(e: MotionEvent?, mapView: MapView?): Boolean {
                    handleMapTapped()
                    return super.onSingleTapConfirmed(e, mapView)
                }
            }

            overlays.add(overlay)

            when (PreferencesManager.getInstance(requireContext()).currentTheme()) {
                Theme.LIGHT -> overlayManager.tilesOverlay.setColorFilter(null)
                Theme.DARK -> {
                    val colorMatrix = ColorMatrix()

                    val c = 1.01f
                    colorMatrix.set(
                        floatArrayOf(
                            c, 0f, 0f, 0f,
                            0f, c, 0f, 0f,
                            0f, 0f, c, 0f,
                            0f, 0f, 0f, 1f,
                            1f, 1f, 1f, 0f
                        )
                    )

                    colorMatrix.preConcat(
                        ColorMatrix(
                            floatArrayOf(
                                -1.0f, 0f, 0f, 0f,
                                255f, 0f, -1.0f, 0f,
                                0f, 255f, 0f, 0f,
                                -1.0f, 0f, 255f, 0f,
                                0f, 0f, 1.0f, 0f
                            )
                        )
                    )

                    overlayManager.tilesOverlay.setColorFilter(ColorMatrixColorFilter(colorMatrix))
                }
            }
        }
    }

    // --- Listeners / observation ---

    private fun setupListeners() {
        setupBackButtonPressCallback()

        viewModel.getPlaces(requireContext()).observe(viewLifecycleOwner) { newPlaces ->
            setupCategoriesAndPlaces(newPlaces)
        }
    }

    private fun setupBackButtonPressCallback() {
        val activity = requireActivity()
        activity.onBackPressedDispatcher.addCallback(this) {
            if (composeState.isSearching) {
                handleMapTapped()
            } else {
                isEnabled = false
                activity.onBackPressed()
            }
        }
    }

    // --- Download job ---

    private fun startDownloadJob() {
        downloadJob?.cancel()

        downloadJob = lifecycleScope.launch {
            withContext(Main) {
                snackbarHostState.showSnackbar(
                    getString(R.string.data_update),
                    duration = SnackbarDuration.Short
                )
            }

            val downloadResult = viewModel.launchDataUpdate(requireContext())

            Log.d(this::class.simpleName, downloadResult.toString())

            when (downloadResult.errorCount) {
                0 -> {
                    snackbarHostState.showSnackbar(
                        getString(R.string.maps_update_success),
                        duration = SnackbarDuration.Long
                    )
                }

                1 -> {
                    val errRes = when (downloadResult.error) {
                        is JSONException -> R.string.building_data_invalid
                        else -> R.string.building_update_failed
                    }
                    val result = snackbarHostState.showSnackbar(
                        getString(errRes),
                        actionLabel = getString(R.string.action_retry),
                        duration = SnackbarDuration.Indefinite
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        startDownloadJob()
                    }
                }

                2 -> {
                    val errRes = when (downloadResult.error) {
                        is JSONException -> R.string.restaurant_data_invalid
                        else -> R.string.restaurant_update_failed
                    }
                    val result = snackbarHostState.showSnackbar(
                        getString(errRes),
                        actionLabel = getString(R.string.action_retry),
                        duration = SnackbarDuration.Indefinite
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        startDownloadJob()
                    }
                }

                else -> {
                    snackbarHostState.showSnackbar(
                        getString(R.string.unable_to_retrieve_data),
                        duration = SnackbarDuration.Indefinite
                    )
                }
            }
        }
    }

    // --- Places setup ---

    private fun setupCategoriesAndPlaces(incomingPlaces: List<Place>) {
        allPlaces.clear()
        allPlaces.addAll(incomingPlaces)

        val categories = allPlaces.map { it.type }.toHashSet().map { CategoryFilter(it) }
        composeState = composeState.copy(categories = categories)

        filterPlaces()
    }

    private fun addPlacesOnMap(places: List<Place>) {
        places.forEach { curr ->
            mapView.overlays.add(
                PlaceMarker(mapView, curr.copy()).apply {
                    onClickListener = {
                        selectedPlace = place
                        displayPlaceInfo()
                        true
                    }
                }
            )
        }
    }

    // --- Display place info ---

    private fun displayPlaceInfo() {
        selectedPlace?.let { selected ->
            composeState = composeState.copy(selectedPlace = selected, isSearching = false)
            hideKeyboard()
            smoothMoveTo(selected.geolocalisation)
        }
    }

    // --- Navigation helpers ---

    private fun smoothMoveTo(position: GeoPoint, zoom: Double = 17.0, ms: Long = 1000L) {
        mapView.controller.animateTo(position, Math.max(zoom, mapView.zoomLevelDouble), ms)
    }

    private fun moveToPaulSabatier() {
        val paulSabatier = GeoPoint(43.5618994, 1.4678633)
        smoothMoveTo(paulSabatier, 15.0, 0)
    }
}

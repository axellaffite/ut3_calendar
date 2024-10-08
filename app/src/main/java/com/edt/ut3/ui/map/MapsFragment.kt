package com.edt.ut3.ui.map

import android.content.res.ColorStateList
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.View.inflate
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.RecyclerView
import com.axellaffite.fastgallery.FastGallery
import com.axellaffite.fastgallery.ImageLoader
import com.edt.ut3.R
import com.edt.ut3.backend.maps.MapsUtils
import com.edt.ut3.backend.maps.Place
import com.edt.ut3.backend.preferences.PreferencesManager
import com.edt.ut3.databinding.FragmentMapsBinding
import com.edt.ut3.databinding.LayoutSearchBarBinding
import com.edt.ut3.databinding.SearchPlaceBinding
import com.edt.ut3.misc.extensions.hideKeyboard
import com.edt.ut3.ui.custom_views.searchbar.FilterChip
import com.edt.ut3.ui.custom_views.searchbar.SearchBar
import com.edt.ut3.ui.custom_views.searchbar.SearchBarAdapter
import com.edt.ut3.ui.custom_views.searchbar.SearchHandler
import com.edt.ut3.ui.map.custom_makers.PlaceMarker
import com.edt.ut3.ui.preferences.Theme
import com.google.android.material.bottomsheet.BottomSheetBehavior.*
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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

class MapsFragment : Fragment() {

    enum class State { MAP, SEARCHING }
    private var selectedPlace: Place? = null
    private var selectedPlaceMarker: PlaceMarker? = null
    private var state = MutableLiveData(State.MAP)

    private val viewModel: MapsViewModel by viewModels()

    private var downloadJob : Job? = null

    private lateinit var binding: FragmentMapsBinding

    private val places = mutableListOf<Place>()


    private var theSearchBar: (SearchBar<Place, MapsSearchBarAdapter>)? = null

    override fun onPause() {
        super.onPause()
        // Do not remove this line until we use osmdroid
        binding.map.onPause()
    }

    override fun onResume() {
        super.onResume()
        // Do not remove this line until we use omsdroid
        binding.map.onResume()
    }
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMapsBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        theSearchBar = binding.placeSearchBar as SearchBar<Place, MapsSearchBarAdapter>
        theSearchBar?.configure(
            dataSet = places,
            converter = { it.title },
            searchHandler = MapsSearchBarHandler(),
            adapter = MapsSearchBarAdapter().apply {
                onItemClicked = { _: View, _: Int, place: Place ->
                    theSearchBar?.clearFocus()
                    theSearchBar?.searchBar?.setText(place.title)
                    selectedPlace = place
                    displayPlaceInfo()
                }
            }
        )


        configureMap()
        setupListeners()
        moveToPaulSabatier()

        startDownloadJob()
        state.value = State.MAP
    }

    /**
     * Configures the MapView
     * for a better user experience.
     *
     */
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


        binding.map.apply {
            tileProvider.clearTileCache()
            tileProvider.tileCache.clear()

            minZoomLevel = 15.0
            maxZoomLevel = 18.0



            // Which tile source will gives
            // us the map resources, otherwise
            // it cannot display the map.
            val providers = context.resources.getStringArray(R.array.tile_provider)
            val providerName = getString(R.string.provider_name)
            Log.d(this@MapsFragment::class.simpleName, "Providers: ${providers.toList()}")
            val tileSource = XYTileSource(
                providerName, 1, 20, 256, ".png", providers
            )

            TileSourceFactory.addTileSource(tileSource)

            setTileSource(TileSourceFactory.getTileSource(providerName))


            // This setting allows us to correctly see
            // the text on the screen ( adapt to the screen's dpi )
            isTilesScaledToDpi = true

            // Allows the user to zoom with its fingers.
            setMultiTouchControls(true)

            overlays.add(RotationGestureOverlay(this).apply {
                isEnabled = true
            })

            // Disable the awful zoom buttons as the
            // user can now zoom with its fingers
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)


            addMapListener(object: MapListener {
                var shouldHandle = true
                override fun onScroll(event: ScrollEvent?): Boolean {
                    if (state.value == State.SEARCHING) {
                        state.value = State.MAP
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


            // As the MapView's setOnClickListener function did nothing
            // I decided to add an overlay to detect click and scroll events.
            val overlay = object: Overlay() {
                override fun onSingleTapConfirmed(e: MotionEvent?, mapView: MapView?): Boolean {
                    state.value = State.MAP
                    selectedPlaceMarker?.closeInfoWindow()

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

    /**
     * Setup all the listeners to handle user's actions.
     */
    private fun setupListeners() {
        theSearchBar?.searchBar?.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                refreshPlaces()
                theSearchBar?.search()
                state.value = State.SEARCHING
            }
        }

        theSearchBar?.results?.setOnClickListener {
            refreshPlaces()
            state.value = State.SEARCHING
        }

        state.observe(viewLifecycleOwner, {
            handleStateChange(it)
        })

        setupBackButtonPressCallback()

        viewModel.getPlaces(requireContext()).observe(viewLifecycleOwner, { newPlaces ->
            setupCategoriesAndPlaces(newPlaces)
        })
    }

    /**
     * This function is in charge to handle
     * when the user press the back button.
     * The behavior depends on what's the current
     * state.
     * Given that state it will simply changes the
     * current state or pop the fragment stack.
     */
    private fun setupBackButtonPressCallback() {
        val activity = requireActivity()
        activity.onBackPressedDispatcher.addCallback(this) {
            when (state.value) {
                State.SEARCHING -> state.value = State.MAP
                else -> {
                    isEnabled = false
                    activity.onBackPressed()
                }
            }
        }
    }

    private fun moveToPaulSabatier() {
        val paulSabatier = GeoPoint(43.5618994, 1.4678633)
        smoothMoveTo(paulSabatier, 15.0, 0)
    }

    /**
     * This function is in charge to download
     * the Paul Sabatier places and the Crous places.
     * In any cases it displays a Snackbar which
     * indicates the result.
     */
    private fun startDownloadJob() {
        // If a download job is pending, we do not
        // launch an another job.
//        if (downloadJob?.isActive == true) {
//            return
//        }


        downloadJob?.cancel()

        // Assign the downloadJob to the new operation
        downloadJob = lifecycleScope.launch {
            withContext(Main) {
                binding.mapsInfo.let {
                    Snackbar.make(it, R.string.data_update, Snackbar.LENGTH_SHORT).show()
                }
            }

            val downloadResult = viewModel.launchDataUpdate(requireContext())

            // This callback will hold the callback action.
            // We put it in a variable to avoid duplicate code
            // as the code must be use in a post {} function
            // of the main view (to avoid view nullability and things like that)
            val callback : () -> Unit
            when (downloadResult.errorCount) {
                // Display success message
                0 -> callback = {
                    binding.mapsInfo.let {
                        Snackbar.make(it, R.string.maps_update_success, Snackbar.LENGTH_LONG).show()
                    }
                }

                // Display an error message depending on
                // what type of error it is
                1 -> callback = {
                    val errRes = when (downloadResult.error) {
                        is JSONException -> R.string.building_data_invalid
                        else -> R.string.building_update_failed
                    }

                    binding.mapsInfo.let {
                        Snackbar.make(it, errRes, Snackbar.LENGTH_INDEFINITE)
                            .setAction(R.string.action_retry) {
                                startDownloadJob()
                            }
                            .show()
                    }
                }

                // Display an error message depending on
                // what type of error it is
                2 -> callback = {
                    val errRes = when (downloadResult.error) {
                        is JSONException -> R.string.restaurant_data_invalid
                        else -> R.string.restaurant_update_failed
                    }

                    binding.mapsInfo.let {
                        Snackbar.make(it, errRes, Snackbar.LENGTH_INDEFINITE)
                            .setAction(R.string.action_retry) {
                                startDownloadJob()
                            }
                            .show()
                    }
                }

                // Display an internet error message
                else -> callback = {
                    binding.mapsInfo.let {
                        Snackbar.make(it, R.string.unable_to_retrieve_data, Snackbar.LENGTH_INDEFINITE)
                            .show()
                    }
                }
            }

            // Calling the callback.
            Log.d(this::class.simpleName, downloadResult.toString())
            callback()
        }
    }

    private fun setupCategoriesAndPlaces(incomingPlaces: List<Place>) {
        places.clear()
        places.addAll(incomingPlaces)

        theSearchBar?.run {
            search()

            val categories = places.map { it.type }.toHashSet()
            val chips = categories.map { category ->
                FilterChip<Place>(requireContext()).apply {
                    val bgColor = ContextCompat.getColor(context, R.color.foregroundColor)
                    val iconTint = ContextCompat.getColor(context, R.color.iconTint)
                    val states = arrayOf(
                        intArrayOf(-android.R.attr.state_enabled), // disabled
                        intArrayOf(-android.R.attr.state_enabled), // disabled
                        intArrayOf(-android.R.attr.state_checked), // unchecked
                        intArrayOf(-android.R.attr.state_pressed)  // unpressed
                    )
                    chipBackgroundColor = ColorStateList(
                        states,
                        (0..3).map { bgColor }.toIntArray()
                    )
                    checkedIconTint = ColorStateList(
                        states,
                        (0..3).map { iconTint }.toIntArray()
                    )
                    text = category
                    isClickable = true

                    setOnClickListener {
                        refreshPlaces()
                    }

                    filter = FilterChip.GlobalFilter {
                        it.type == text
                    }
                }
            }

            setFilters(*chips.toTypedArray())
        }

        refreshPlaces()
    }

    private fun refreshPlaces() {
        theSearchBar!!.search(matchSearchBarText = false) { placesToShow ->
            binding.map.overlays.forEach { if (it is Marker) { it.closeInfoWindow() } }
            binding.map.overlays.removeAll { it is PlaceMarker }
            addPlacesOnMap(placesToShow)

            binding.map.invalidate()
            binding.map.requestLayout()
        }
    }

    private fun addPlacesOnMap(places: List<Place>) {
        places.forEach { curr ->
            binding.map.overlays.add(
                PlaceMarker(binding.map, curr.copy()).apply {
                    onClickListener = {
                        selectedPlace = place
                        displayPlaceInfo()
                        true
                    }
                }
            )
        }
    }

    private fun handleStateChange(state: State) {
        when (state) {
            State.MAP -> {
                foldEverything()
                refreshPlaces()
                selectedPlaceMarker = null
            }

            State.SEARCHING -> unfoldSearchTools()
        }
    }

    private fun foldEverything() {
        theSearchBar?.hideFilters()
        theSearchBar?.hideResults()
        from(binding.placeInfoContainer).state = STATE_HIDDEN

        theSearchBar?.clearFocus()
        binding.map.requestFocus()

        hideKeyboard()
    }

    private fun unfoldSearchTools() {
        theSearchBar?.showResults()
        theSearchBar?.showFilters()
        from(binding.placeInfoContainer).state = STATE_HIDDEN
    }

    private fun displayPlaceInfo() {
        theSearchBar?.clearFocus()
        selectedPlace?.let { selected ->
            theSearchBar?.hideResults()
            theSearchBar?.hideFilters()
            hideKeyboard()

            viewLifecycleOwner.lifecycleScope.launch {
                lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    delay(500)
                    binding.placeInfoContainer.let {
                        from(it).state = STATE_EXPANDED
                    }
                }
            }

            binding.placeInfo.titleText = selected.title
            binding.placeInfo.descriptionText = selected.short_desc ?: getString(R.string.no_description_available)
            binding.placeInfo.picture = selected.photo
            binding.placeInfo.goTo.setOnClickListener {
                activity?.let {
                    MapsUtils.routeFromTo(
                        it,
                        GeoPoint(selected.geolocalisation),
                        selected.title
                    ) {
                        binding.mapsInfo.let { info ->
                            Snackbar.make(
                                info,
                                R.string.unable_to_launch_googlemaps,
                                Snackbar.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            }

            val image = Pair(selected.photo, R.drawable.no_image_placeholder)
            binding.placeInfo.image?.setOnClickListener {
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

            smoothMoveTo(selected.geolocalisation)
        }
    }

    /**
     * Does a smooth move to the given
     * position.
     *
     * @param position The wanted position
     * @param zoom The zoom amount
     * @param ms The time in ms
     */
    private fun smoothMoveTo(position: GeoPoint, zoom: Double = 17.0, ms: Long = 1000L) {
        binding.map.controller.animateTo(position, Math.max(zoom, binding.map.zoomLevelDouble), ms)
    }


    private inner class MapsSearchBarHandler: SearchHandler() {
        override fun searchLauncher(searchFunction: suspend () -> Unit): Job {
            return viewLifecycleOwner.lifecycleScope.launch {
                lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                    searchFunction.invoke()
                }
            }
        }
    }

    private class MapsSearchBarAdapter : SearchBarAdapter<Place, MapsSearchBarAdapter.ViewHolder>() {
        private var dataset: List<Place>? = null
        var onItemClicked : ((item: View, position: Int, place: Place) -> Unit)? = null

        override fun setDataSet(dataSet: List<Place>) {
            this.dataset = dataSet
        }

        private lateinit var binding: SearchPlaceBinding
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            binding = SearchPlaceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(binding.root)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = dataset!![position]
            holder.setIsRecyclable(false)
            holder.v.run {
                binding.icon.setImageResource(item.getIcon())
                binding.name.text = item.title
                Log.d("place", item.title)

                setOnClickListener {
                    onItemClicked?.invoke(it, position, item)
                }
            }
        }

        override fun getItemCount() = dataset?.size ?: 0

        class ViewHolder(val v: ConstraintLayout) : RecyclerView.ViewHolder(v)
    }

}
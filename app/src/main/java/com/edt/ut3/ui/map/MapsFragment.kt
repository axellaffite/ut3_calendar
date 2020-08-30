package com.edt.ut3.ui.map

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.CompoundButton
import androidx.activity.addCallback
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.observe
import com.edt.ut3.BuildConfig
import com.edt.ut3.R
import com.edt.ut3.ui.map.SearchPlaceAdapter.Place
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.material.bottomsheet.BottomSheetBehavior.*
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipDrawable
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_maps.*
import kotlinx.android.synthetic.main.fragment_maps.view.*
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Overlay
import java.util.*
import kotlin.collections.HashSet

class MapsFragment : Fragment() {

    enum class State { MAP, SEARCHING, PLACE }
    private var selectedPlace: Place? = null
    private var state = MutableLiveData<State>(State.MAP)

    private val viewModel: MapsViewModel by viewModels { defaultViewModelProviderFactory }


    private var downloadJob : Job? = null

    private val selectedCategories = HashSet<String>()
    private var places = mutableMapOf<String, MutableList<Place>>()

    private var searchJob : Job? = null

    override fun onPause() {
        super.onPause()
        map.onPause()
    }

    override fun onResume() {
        super.onResume()
        map.onResume()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_maps, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        configureMap()
        setupListeners()

        val paulSabatier = GeoPoint(43.5618994,1.4678633)
        smoothMoveTo(paulSabatier, 15.0)
        map.controller.animateTo(paulSabatier, 15.0, 1L)

        startDownloadJob()
    }

    /**
     * Configures the MapView
     * for a better user experience.
     *
     */
    private fun configureMap() {
        Configuration.getInstance().userAgentValue = BuildConfig.APPLICATION_ID

        map.apply {
            // Which tile source will gives
            // us the map resources, otherwise
            // it cannot display the map.
            setTileSource(TileSourceFactory.MAPNIK)

            // This setting allows us to correctly see
            // the text on the screen ( adapt to the screen's dpi )
            isTilesScaledToDpi = true

            // Allows the user to zoom with its fingers.
            setMultiTouchControls(true)

            // Disable the awful zoom buttons as the
            // user can now zoom with its fingers
            //TODO correct this as it doesn't works
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)


            // As the MapView's setOnClickListener function did nothing
            // I decided to add an overlay to detect click and scroll events.
            val overlay = object: Overlay() {
                override fun onSingleTapConfirmed(e: MotionEvent?, mapView: MapView?): Boolean {
                    println("Click detected on map")
                    state.value = State.MAP

                    return super.onSingleTapConfirmed(e, mapView)
                }

                override fun onScroll(
                    pEvent1: MotionEvent?,
                    pEvent2: MotionEvent?,
                    pDistanceX: Float,
                    pDistanceY: Float,
                    pMapView: MapView?
                ): Boolean {
                    if (state.value == State.SEARCHING) {
                        state.value = State.MAP
                    }

                    return super.onScroll(pEvent1, pEvent2, pDistanceX, pDistanceY, pMapView)
                }
            }

            overlays.add(overlay)
        }
    }

    /**
     * Setup all the listeners to handle user's actions.
     */
    private fun setupListeners() {
        //text, start, before, count
        search_bar.doOnTextChanged { text, _, _, _ ->
            filterResults(text.toString())
        }

        search_bar.setOnClickListener {
            refreshPlaces()
            filterResults(search_bar.text.toString())
            state.value = State.SEARCHING
        }

        state.observe(viewLifecycleOwner) { handleStateChange(it) }

        handleBackButtonClick()
    }

    private fun handleBackButtonClick() {
        val activity = requireActivity()
        activity.onBackPressedDispatcher.addCallback {
            when (state.value) {
                State.SEARCHING, State.PLACE -> state.value = State.MAP
                State.MAP -> activity.onBackPressed()
            }
        }
    }

    private fun startDownloadJob() {
        downloadJob?.cancel()
        downloadJob = lifecycleScope.launchWhenResumed {
            var callback: () -> Unit
            try {
                places = viewModel.getCrousPlaces()
                callback = {
                    setupCategoriesAndPlaces(places)
                }

            } catch (e: Exception) {
                e.printStackTrace()
                val err = when (e) {
                    is JSONException -> R.string.unknow_error
                    is java.io.IOException -> R.string.unable_to_retrieve_data
                    else -> R.string.unknown_error
                }

                callback = {
                    Snackbar.make(requireView().maps_main, err, Snackbar.LENGTH_INDEFINITE)
                        .setAction(getString(R.string.action_retry)) {
                            startDownloadJob()
                        }
                        .show()
                }
            }

            view?.post { callback() }
        }
    }

    private fun setupCategoriesAndPlaces(places: MutableMap<String, MutableList<Place>>) {
        filters_group.post {
            filters_group.run {
                places.keys.forEach { category ->
                    addView(
                        Chip(requireContext()).apply {
                            setChipDrawable(
                                ChipDrawable.createFromAttributes(
                                    requireContext(),
                                    null,
                                    0,
                                    R.style.Widget_MaterialComponents_Chip_Filter
                                )
                            )

                            text = category
                            isClickable = true

                            setOnCheckedChangeListener { compoundButton: CompoundButton?, b: Boolean ->
                                val cate = text.toString()
                                if (b) {
                                    selectedCategories.add(cate)
                                    refreshPlaces()
                                    println("Added: $cate")
                                } else {
                                    selectedCategories.remove(cate)
                                    println("Removed: $cate")
                                }

                                filterResults(requireView().search_bar.text.toString())
                            }
                        }
                    )
                }
            }
        }
    }

    private fun refreshPlaces() {
        map.overlays.removeAll { it is Marker }
        selectedCategories.forEach {
            addPlacesOnMap(it)
        }
    }

    private fun addPlacesOnMap(category: String) {
        places[category]?.let { results ->
            results.forEach {
                map.overlays.add(
                    Marker(map).apply {
                        position = it.geolocalisation
                        title = it.title
                        //TODO set the icon drawable
                    }
                )
            }
        }
    }

    private fun handleStateChange(state: State) {
        when (state) {
            State.MAP -> foldEverything()

            State.SEARCHING -> unfoldSearchTools()

            State.PLACE -> displayPlaceInfo()
        }
    }

    private fun foldEverything() {
        filters_container.visibility = GONE
        search_result.visibility = GONE
        from(place_info_container).state = STATE_HIDDEN
        requireActivity().nav_view.visibility = VISIBLE

        search_bar.clearFocus()
        map.requestFocus()

        hideKeyboard()
    }

    private fun hideKeyboard() {
        val imm = requireActivity().getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(requireView().windowToken, 0)
    }

    private fun unfoldSearchTools() {
        search_result.visibility = VISIBLE
        filters_container.visibility = VISIBLE
        from(place_info_container).state = STATE_HIDDEN

        requireActivity().nav_view.visibility = GONE
    }

    private fun displayPlaceInfo() {
        selectedPlace?.let {
            search_result.visibility = GONE
            filters_container.visibility = GONE
            requireActivity().nav_view.visibility = GONE
            hideKeyboard()

            lifecycleScope.launchWhenStarted {
                delay(500)
                from(place_info_container).state = STATE_EXPANDED
            }

            place_info.titleText = it.title
            place_info.descriptionText = it.short_desc ?: getString(R.string.no_description_available)
            place_info.picture = it.photo

            //TODO edit this
//            googleMap.clear()
//            googleMap.addMarker(MarkerOptions().position(it.geolocalisation))
            smoothMoveTo(it.geolocalisation)
        }
    }

    private fun filterResults(text: String) {
        searchJob?.cancel()
        searchJob = lifecycleScope.launchWhenResumed {
            val lowerCaseText = text.toLowerCase(Locale.getDefault())
            val result = withContext(Default) {
                places.filterKeys { selectedCategories.isEmpty() || selectedCategories.contains(it) }
                    .flatMap { it.value }
                    .filter { it.title.toLowerCase(Locale.getDefault()).contains(lowerCaseText) }
                    .toTypedArray()
            }

            withContext(Main) {
                search_result.adapter = SearchPlaceAdapter(requireContext(), result)
                search_result.setOnItemClickListener { adapterView, view, pos, id ->
                    if (pos != -1) {
                        val place = search_result.getItemAtPosition(pos) as Place
                        selectedPlace = place
                        state.value = State.PLACE
                    }
                }
            }
        }
    }

    private fun smoothMoveTo(position: GeoPoint, zoom: Double = 17.0, speed: Long = 1000L) {
        map.controller.animateTo(position, zoom, speed)
    }
}
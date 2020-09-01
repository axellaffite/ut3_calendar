package com.edt.ut3.ui.map

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
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
import androidx.core.app.ActivityCompat
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.observe
import com.edt.ut3.R
import com.edt.ut3.backend.preferences.PreferencesManager
import com.edt.ut3.misc.Theme
import com.edt.ut3.ui.map.SearchPlaceAdapter.Place
import com.edt.ut3.ui.map.custom_makers.LocationMarker
import com.edt.ut3.ui.map.custom_makers.PlaceMarker
import com.google.android.material.bottomsheet.BottomSheetBehavior.*
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipDrawable
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_maps.*
import kotlinx.android.synthetic.main.fragment_maps.view.*
import kotlinx.android.synthetic.main.place_info.view.*
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.osmdroid.bonuspack.routing.OSRMRoadManager
import org.osmdroid.bonuspack.routing.RoadManager
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Overlay
import org.osmdroid.views.overlay.TilesOverlay
import java.util.*
import kotlin.collections.HashSet

class MapsFragment : Fragment() {

    enum class State { MAP, SEARCHING, PLACE }
    private var selectedPlace: Place? = null
    private var state = MutableLiveData<State>(State.MAP)

    private val viewModel: MapsViewModel by viewModels { defaultViewModelProviderFactory }


    private var searchJob : Job? = null
    private var downloadJob : Job? = null

    private val selectedCategories = HashSet<String>()
    private var places = mutableMapOf<String, MutableList<Place>>()

    override fun onPause() {
        super.onPause()
        // Do not remove this line until we use osmdroid
        map.onPause()
    }

    override fun onResume() {
        super.onResume()
        // Do not remove this line until we use omsdroid
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
        moveToPaulSabatier()

        startDownloadJob()
    }

    /**
     * Configures the MapView
     * for a better user experience.
     *
     */
    private fun configureMap() {
        Configuration.getInstance().userAgentValue = requireActivity().packageName

        map.apply {
            // Which tile source will gives
            // us the map resources, otherwise
            // it cannot display the map.
            val tileSource = XYTileSource(
                "HOT", 1, 20, 256, ".png", arrayOf(
                    "http://a.tile.openstreetmap.fr/hot/",
                    "http://b.tile.openstreetmap.fr/hot/",
                    "http://c.tile.openstreetmap.fr/hot/"
                ), "© OpenStreetMap contributors"
            )

            setTileSource(tileSource)

            // This setting allows us to correctly see
            // the text on the screen ( adapt to the screen's dpi )
            isTilesScaledToDpi = true

            // Allows the user to zoom with its fingers.
            setMultiTouchControls(true)

            // Disable the awful zoom buttons as the
            // user can now zoom with its fingers
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

            setupLocationListener()

            when (PreferencesManager(requireContext()).getTheme()) {
                Theme.LIGHT -> overlayManager.tilesOverlay.setColorFilter(null)
                Theme.DARK -> overlayManager.tilesOverlay.setColorFilter(TilesOverlay.INVERT_COLORS)
            }
        }
    }

    /**
     * This function will set the location listener
     * that will track the user's location and
     * display it on the map.
     *
     * The suppress lint is put there as
     * the requestLocationPermissionIfNecessary is
     * called and check it before executing it
     */
    @SuppressLint("MissingPermission")
    private fun setupLocationListener() {
        val listener = object: LocationListener {
            override fun onLocationChanged(p0: Location) {
                view?.let {
                    it.map.overlays.add(LocationMarker(it.map).apply {
                        title = "position"
                        position = GeoPoint(p0)
                    })
                }
            }

            /**
             * Unused but necessary to avoid crash
             * due to function deprecation
             */
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
            }

        }

        val manager = requireActivity().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        executeIfLocationPermissionIsGranted {
            manager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                0L,
                0f,
                listener
            )
        }
    }

    /**
     * Setup all the listeners to handle user's actions.
     */
    private fun setupListeners() {
        //text, start, before, count
        search_bar.doOnTextChanged { text, _, _, _ ->
            handleTextChanged(text.toString())
        }

        search_bar.setOnClickListener {
            refreshPlaces()
            handleTextChanged(search_bar.text.toString())
            state.value = State.SEARCHING
        }

        state.observe(viewLifecycleOwner) { handleStateChange(it) }

        setupBackButtonPressCallback()

        my_location.setOnClickListener {
            val locationMarker = map.overlays.find { it is LocationMarker } as? LocationMarker
            locationMarker?.let { me ->
                smoothMoveTo(me.position)
            }
        }
    }

    private fun executeIfLocationPermissionIsGranted(callback: () -> Unit) {
        // We check if the permissions are granted
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // If not we request it
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                0
            )

            return
        }

        // Otherwise we execute the callback
        callback()
    }

    /**
     * Simply redirect to the filterResults function
     * that will filter the results.
     *
     * @param text The search bar text
     */
    private fun handleTextChanged(text: String) = filterResults(text)

    /**
     * Filter the results and assign a job to it.
     * When the function is called a second time
     * while the job isn't finished yet, the previous
     * one is canceled and replaced by the new one.
     *
     * @param text The search bar text
     */
    private fun filterResults(text: String) {
        searchJob?.cancel()
        searchJob = lifecycleScope.launchWhenResumed {
            // We first set the text to lower case in order
            // to do a non-sensitive case search.
            val lowerCaseText = text.toLowerCase(Locale.getDefault())

            // We then filter the result and assign them to a variable
            val matchingPlaces = withContext(Default) {
                // Filtering the keys to keep only the ones that matches
                // the selected categories.
                // If no category is selected the Map is kept as it.
                val searchingMap = if (selectedCategories.isEmpty()) {
                    places
                } else {
                    places.filterKeys { selectedCategories.contains(it) }
                }

                // We then flat map every values for every remaining keys
                // to obtain them in a single Collection
                // After that, we keep only the ones that contains the
                // search bar text and return them
                searchingMap.
                    flatMap { it.value }
                    .filter { it.title.toLowerCase(Locale.getDefault()).contains(lowerCaseText) }
                    .toTypedArray()
            }

            // We them add them to the ListView that will contains the search
            // result.
            // As it modify the view I prefer do it on the Main thread to avoid problems.
            withContext(Main) {
                search_result.adapter = SearchPlaceAdapter(requireContext(), matchingPlaces)
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
                State.SEARCHING, State.PLACE -> state.value = State.MAP
                else -> activity.supportFragmentManager.popBackStack()
            }
        }
    }

    private fun moveToPaulSabatier() {
        val paulSabatier = GeoPoint(43.5618994, 1.4678633)
        smoothMoveTo(paulSabatier, 15.0)
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
        if (downloadJob?.isActive == true) {
            return
        }

        var newPlaces = mutableMapOf<String, MutableList<Place>>()

        // Assign the downloadJob to the new operation
        downloadJob = lifecycleScope.launchWhenResumed {
            // The error variable will store the last exception
            // encountered and the errorCount the number
            // of exceptions encountered.
            // There are 4 cases after the two downloads :
            // - errorCount = 0 : There are no error, we can
            //                    display a success message
            // - errorCount = 1 : The Paul Sabatier places aren't
            //                    available, the internet connection
            //                    seems to be good as the second download
            //                    is a success, we check if it's a parsing
            //                    error or a timeout error.
            // - errorCount = 2 : Same logic as =1 but for the Crous places.
            // - errorCount = 3 : The internet connection does not seem to work,
            //                    we display an error message saying to check it.
            var error: Exception? = null
            var errorCount = 0


            // First download for Paul Sabatier places
            // (from our github)
            try {
                newPlaces = withContext(IO) {
                    viewModel.getPaulSabatierPlaces()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                error = e
                errorCount += 1
            }

            // Second download for Crous places
            // (from the government website)
            try {
                val temp = withContext(IO) {
                    viewModel.getCrousPlaces()
                }

                temp.forEach { entry ->
                    newPlaces.getOrPut(entry.key) { mutableListOf() }.addAll(entry.value)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                error = e
                errorCount += 2
            }

            // This callback will hold the callback action.
            // We put it in a variable to avoid duplicate code
            // as the code must be use in a post {} function
            // of the main view (to avoid view nullability and things like that)
            val callback : () -> Unit
            when (errorCount) {
                // Display success message
                0 -> callback = {
                    Snackbar.make(maps_main, R.string.maps_update_success, Snackbar.LENGTH_LONG)
                        .show()
                    setupCategoriesAndPlaces(newPlaces)
                }

                // Display an error message depending on
                // what type of error it is
                1 -> callback = {
                    val errRes = when (error) {
                        is JSONException -> R.string.building_data_invalid
                        else -> R.string.building_update_failed
                    }

                    Snackbar.make(maps_main, errRes, Snackbar.LENGTH_INDEFINITE)
                        .setAction(R.string.action_retry) {
                            startDownloadJob()
                        }
                        .show()

                    setupCategoriesAndPlaces(newPlaces)
                }

                // Display an error message depending on
                // what type of error it is
                2 -> callback = {
                    val errRes = when (error) {
                        is JSONException -> R.string.restaurant_data_invalid
                        else -> R.string.restaurant_update_failed
                    }

                    Snackbar.make(maps_main, errRes, Snackbar.LENGTH_INDEFINITE)
                        .setAction(R.string.action_retry) {
                            startDownloadJob()
                        }
                        .show()

                    setupCategoriesAndPlaces(newPlaces)
                }

                // Display an internet error message
                else -> callback = {
                    Snackbar.make(
                        maps_main,
                        R.string.unable_to_retrieve_data,
                        Snackbar.LENGTH_INDEFINITE
                    ).show()
                }
            }

            // Calling the callback.
            println("DEBUG: Download result: $errorCount ${error?.javaClass?.simpleName}")
            maps_main?.post(callback)
        }
    }

    private fun setupCategoriesAndPlaces(incomingPlaces: MutableMap<String, MutableList<Place>>) {
        places = incomingPlaces
        filters_group.post {
            filters_group.run {
                incomingPlaces.keys.forEach { category ->
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
                                    println("Added: $cate")
                                } else {
                                    selectedCategories.remove(cate)
                                    println("Removed: $cate")
                                }


                                refreshPlaces()
                                filterResults(requireView().search_bar.text.toString())
                            }

                            refreshPlaces()
                        }
                    )
                }
            }
        }
    }

    private fun refreshPlaces() {
        map.overlays.removeAll { it is PlaceMarker }

        val categories = if (selectedCategories.isEmpty()) {
            places.keys
        } else {
            selectedCategories
        }


        categories.forEach {
            addPlacesOnMap(it)
        }
    }

    private fun addPlacesOnMap(category: String) {
        places[category]?.let { results ->
            results.forEach { curr ->
                map.overlays.add(
                    PlaceMarker(map, curr.copy()).apply {
                        onLongClickListener = {
                            selectedPlace = place
                            state.value = State.PLACE
                            true
                        }
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
        selectedPlace?.let { selected ->
            search_result.visibility = GONE
            filters_container.visibility = GONE
            requireActivity().nav_view.visibility = GONE
            hideKeyboard()

            lifecycleScope.launchWhenStarted {
                delay(500)
                from(place_info_container).state = STATE_EXPANDED
            }

            place_info.titleText = selected.title
            place_info.descriptionText = selected.short_desc ?: getString(R.string.no_description_available)
            place_info.picture = selected.photo
            place_info.go_to.setOnClickListener {
                val locationMarker = map.overlays.find { it is LocationMarker } as? LocationMarker
                locationMarker?.let { me ->
                    lifecycleScope.launchWhenResumed {
                        routeFromTo(me.position, GeoPoint(selected.geolocalisation), selected.title)
                    }
                } ?: {

                }()
            }

            map.overlays.removeAll { marker -> marker is PlaceMarker }
            map.overlays.add(
                PlaceMarker(map, selected).also { marker ->
                    marker.showInfoWindow()
                }
            )


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
        map.controller.animateTo(position, zoom, ms)
    }

    private fun routeFromTo(from: GeoPoint, to: GeoPoint, toTitle: String) {
        val requestLink = ("https://www.google.com/maps/dir/?api=1" +
                "&origin=${from.latitude.toFloat()},${from.longitude.toFloat()}" +
                "&destination=${to.latitude.toFloat()},${to.longitude.toFloat()}" +
                "&destination_place_id=$toTitle" +
                "&travelmode=walking")

        // Create a Uri from an intent string. Use the result to create an Intent.
        val gmmIntentUri = Uri.parse(requestLink)

        // Create an Intent from gmmIntentUri. Set the action to ACTION_VIEW
        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)

        // Make the Intent explicit by setting the Google Maps package
        mapIntent.setPackage("com.google.android.apps.maps")

        // Attempt to start an activity that can handle the Intent
        mapIntent.resolveActivity(requireContext().packageManager)?.let {
            startActivity(mapIntent)
        } ?: Snackbar.make(maps_main, R.string.unable_to_launch_googlemaps, Snackbar.LENGTH_LONG)
    }
}
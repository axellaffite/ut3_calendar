    package com.edt.ut3.ui.room_finder

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.*
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.view.children
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.edt.ut3.R
import com.edt.ut3.backend.goulin_room_finder.Building
import com.edt.ut3.backend.goulin_room_finder.Room
import com.edt.ut3.misc.extensions.addOnBackPressedListener
import com.edt.ut3.misc.extensions.hideKeyboard
import com.edt.ut3.misc.extensions.onBackPressed
import com.edt.ut3.misc.extensions.toDp
import com.edt.ut3.ui.room_finder.RoomFinderState.*
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.room_finder_fragment.*
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import java.util.*

    class RoomFinderFragment : Fragment() {

    private val viewModel: RoomFinderViewModel by sharedViewModel()


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.room_finder_fragment, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        result.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        result.addItemDecoration(RoomAdapter.RoomSeparator())

        setupFilters()
        setupListeners()
        viewModel.updateBuildingsData()
    }

    /**
     * Setup the listeners that are
     * in charge to update the view.
     *
     * As the logic is stored in the [view model][viewModel],
     * theses listeners don't modify the incoming data.
     */
    private fun setupListeners() {
        viewModel.run {
            state.observe(viewLifecycleOwner, ::handleStateChange)
            error.observe(viewLifecycleOwner, ::handleError)
            searchResult.observe(viewLifecycleOwner, ::handleSearchUpdate)
            buildings.observe(viewLifecycleOwner, ::handleBuildingUpdate)
        }

        hints.setOnItemClickListener { parent, view, position, id ->
            val item = hints.adapter.getItem(position)
            if (item is Building) {
                viewModel.selectBuilding(item)
                search_bar?.text = item.name
            }
        }

        search_bar.setOnClickListener {
            invertHintsVisibility()
        }

        addOnBackPressedListener {
            when (viewModel.state.value) {
                is Searching -> viewModel.state.value =
                    if (viewModel.ready) Result
                    else Presentation

                else -> {
                    isEnabled = false
                    onBackPressed()
                }
            }
        }

        filters_chipgroup.children.forEach { child ->
            if (child is RoomFilterChip) {
                child.setOnClickListener {
                    if (child.isChecked) {
                        viewModel.addFilter(child.filter)
                    } else {
                        viewModel.removeFilter(child.filter)
                    }
                }

                if (child.isChecked) {
                    viewModel.addFilter(child.filter)
                } else {
                    viewModel.removeFilter(child.filter)
                }
            }
        }
    }

    private fun invertHintsVisibility() {
        hints.run {
            if (visibility != VISIBLE) {
                visibility = VISIBLE
                search_bar_container?.cardElevation = 8.toDp(context)
            } else {
                clearSearchBarState()
            }
        }
    }

    private fun clearSearchBarState() {
        search_bar_container?.cardElevation = 0f
        hints?.visibility = GONE

        search_bar.clearFocus()
        hideKeyboard()
    }

    private fun setupFilters() {
        filter_from_now?.filter = { room ->
            val now = Date()
            room.map { it.withoutPastSchedules(now) }
                .filter { it.freeSchedules.isNotEmpty() }
        }
    }


    /**
     * Displays a [Snackbar] with the given action
     *
     * @param action The action to perform when the action button is clicked
     */
    private fun displayInternetError(actionLabel: Int = R.string.action_retry, action: (View?) -> Unit) {
        Snackbar.make(result, getString(R.string.data_update_failed), Snackbar.LENGTH_INDEFINITE)
            .setAction(actionLabel, action)
            .show()
    }


    /**
     * Handles all the errors that can occur in this Fragment.
     * There are all listed into the [RoomFinderState] class.
     *
     * @param state The given state.
     */
    private fun handleStateChange(state: RoomFinderState) = when (state) {
        is Presentation -> {
            loading_label?.setText(R.string.fragment_room_finder_presentation_text)

            thanks.visibility = VISIBLE
            loading_container.visibility = INVISIBLE
            result.visibility = INVISIBLE

            clearSearchBarState()
            search_bar.clearFocus()
            hideKeyboard()
        }

        is Result -> {
            thanks.visibility = GONE
            loading_container.visibility = INVISIBLE
            result.visibility = VISIBLE

            clearSearchBarState()
            search_bar.clearFocus()
            hideKeyboard()
        }

        is Searching -> {
            thanks.visibility = INVISIBLE
            loading_container.visibility = VISIBLE
            result.visibility = INVISIBLE

            clearSearchBarState()
        }

        is Downloading -> {
            loading_label?.setText(R.string.fragment_room_finder_downloading_buildings)

            thanks.visibility = INVISIBLE
            loading_container.visibility = VISIBLE
            result.visibility = INVISIBLE
        }
    }.also { println("STATE: $state") }

    /**
     * Handles all the errors that can occur in this Fragment.
     * There are all listed into the [RoomFinderFailure] class.
     *
     * @param error The given error.
     */
    private fun handleError(error: RoomFinderFailure?) {
        when (error) {
            is RoomFinderFailure.SearchFailure ->  {
                viewModel.state.value = Presentation
                displayInternetError {
                    viewModel.updateSearchResults()
                }
            }

            is RoomFinderFailure.UpdateBuildingFailure -> {
                viewModel.state.value = Presentation
                displayInternetError {
                    viewModel.updateBuildingsData(true)
                }
            }

            null -> {
                // Just to avoid logging false errors in the else statement
            }

            else -> {
                Log.e(RoomFinderFragment::class.simpleName,
                    "Unhandled error: ${error.javaClass}"
                )
            }
        }
    }

    /**
     * Display the incoming results by updating
     * the result adapter.
     *
     * @param rooms The available rooms
     */
    private fun handleSearchUpdate(rooms: List<Room>) {
        Log.d(this::class.simpleName, "New rooms: $rooms")
        result.adapter = RoomAdapter(rooms)
    }


    /**
     * Update the building adapter with the incoming data.
     *
     * @param buildings The new buildings
     */
    private fun handleBuildingUpdate(buildings: Set<Building>) {
        hints.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_list_item_1,
            buildings.toList()
        )
    }


}


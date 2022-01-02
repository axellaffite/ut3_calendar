    package com.edt.ut3.ui.room_finder

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.*
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.core.view.children
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.edt.ut3.R
import com.edt.ut3.refactored.models.domain.room_finder.Building
import com.edt.ut3.refactored.models.domain.room_finder.Room
import com.edt.ut3.misc.extensions.addOnBackPressedListener
import com.edt.ut3.misc.extensions.hideKeyboard
import com.edt.ut3.misc.extensions.onBackPressed
import com.edt.ut3.misc.extensions.toDp
import com.edt.ut3.ui.room_finder.RoomFinderState.*
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.room_finder_fragment.*
import java.util.*

    class RoomFinderFragment : Fragment() {

    private val viewModel: RoomFinderViewModel by activityViewModels()


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
            searchBarText.observe(viewLifecycleOwner, ::handleTextChanged)
            buildings.observe(viewLifecycleOwner, ::handleBuildingUpdate)
        }


        hints.setOnItemClickListener { _: AdapterView<*>, view: View, _: Int, _: Long ->
            if (view is TextView) {
                viewModel.selectBuilding(view.text.toString())
            }
        }

        search_bar.apply {
            doOnTextChanged { text, _, _, _ ->
                viewModel.updateBarText(text.toString())
            }

            setOnClickListener {
                invertHintsVisibility()
            }
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
                hideHints()
            }
        }
    }

    private fun hideHints() {
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
            thanks.visibility = VISIBLE
            hints.visibility = GONE
            loading_container.visibility = INVISIBLE
            result.visibility = INVISIBLE

            search_bar.clearFocus()
            hideKeyboard()
        }

        is Result -> {
            thanks.visibility = INVISIBLE
            loading_container.visibility = INVISIBLE
            result.visibility = VISIBLE

            hideHints()
            search_bar.clearFocus()
            hideKeyboard()
        }

        is Searching -> {
            thanks.visibility = INVISIBLE
            loading_container.visibility = VISIBLE
            result.visibility = INVISIBLE
            hideHints()
        }

        is Downloading -> {
            hints.visibility = GONE
            thanks.visibility = INVISIBLE
            loading_container.visibility = INVISIBLE
            result.visibility = INVISIBLE
        }
    }

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
                    viewModel.updateSearchResults(true)
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
     * Update the displayed text if it is different
     * from the given [text]
     *
     * @param text The text to display
     */
    private fun handleTextChanged(text: String?) {
        when (text) {
            search_bar?.text, null -> {}
            else -> {
                search_bar?.text = text
            }
        }
    }

    /**
     * Update the building adapter with the incoming data.
     *
     * @param buildings The new buildings
     */
    private fun handleBuildingUpdate(buildings: Set<Building>) {
        hints.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, buildings.map { it.name })
    }

}


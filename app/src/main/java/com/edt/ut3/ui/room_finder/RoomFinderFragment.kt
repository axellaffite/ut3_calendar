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
import com.edt.ut3.backend.goulin_room_finder.Building
import com.edt.ut3.backend.goulin_room_finder.Room
import com.edt.ut3.databinding.RoomFinderFragmentBinding
import com.edt.ut3.misc.extensions.addOnBackPressedListener
import com.edt.ut3.misc.extensions.hideKeyboard
import com.edt.ut3.misc.extensions.onBackPressed
import com.edt.ut3.misc.extensions.toDp
import com.edt.ut3.ui.room_finder.RoomFinderState.*
import com.google.android.material.snackbar.Snackbar
import java.util.*

    class RoomFinderFragment : Fragment() {

    private val viewModel: RoomFinderViewModel by activityViewModels()
    private lateinit var binding: RoomFinderFragmentBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = RoomFinderFragmentBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.result.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        binding.result.addItemDecoration(RoomAdapter.RoomSeparator())

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


        binding.hints.setOnItemClickListener { _: AdapterView<*>, view: View, _: Int, _: Long ->
            if (view is TextView) {
                viewModel.selectBuilding(view.text.toString())
            }
        }

        binding.searchBar.apply {
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

        binding.filtersChipgroup.children.forEach { child ->
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
        binding.hints.run {
            if (visibility != VISIBLE) {
                visibility = VISIBLE
                binding.searchBarContainer.cardElevation = 8.toDp(context)
            } else {
                hideHints()
            }
        }
    }

    private fun hideHints() {
        binding.searchBarContainer.cardElevation = 0f
        binding.hints.visibility = GONE

        binding.searchBar.clearFocus()
        hideKeyboard()
    }

    private fun setupFilters() {
        binding.filterFromNow.filter = { room ->
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
        Snackbar.make(binding.result, getString(R.string.data_update_failed), Snackbar.LENGTH_INDEFINITE)
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
            binding.thanks.visibility = VISIBLE
            binding.hints.visibility = GONE
            binding.loadingContainer.visibility = INVISIBLE
            binding.result.visibility = INVISIBLE

            binding.searchBar.clearFocus()
            hideKeyboard()
        }

        is Result -> {
            binding.thanks.visibility = INVISIBLE
            binding.loadingContainer.visibility = INVISIBLE
            binding.result.visibility = VISIBLE

            hideHints()
            binding.searchBar.clearFocus()
            hideKeyboard()
        }

        is Searching -> {
            binding.thanks.visibility = INVISIBLE
            binding.loadingContainer.visibility = VISIBLE
            binding.result.visibility = INVISIBLE
            hideHints()
        }

        is Downloading -> {
            binding.hints.visibility = GONE
            binding.thanks.visibility = INVISIBLE
                binding.loadingContainer.visibility = INVISIBLE
            binding.result.visibility = INVISIBLE
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
        binding.result.adapter = RoomAdapter(rooms)
    }

    /**
     * Update the displayed text if it is different
     * from the given [text]
     *
     * @param text The text to display
     */
    private fun handleTextChanged(text: String?) {
        when (text) {
            binding.searchBar.text, null -> {}
            else -> {
                binding.searchBar.text = text
            }
        }
    }

    /**
     * Update the building adapter with the incoming data.
     *
     * @param buildings The new buildings
     */
    private fun handleBuildingUpdate(buildings: Set<Building>) {
        binding.hints.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, buildings.map { it.name })
    }

}


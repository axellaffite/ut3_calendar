package com.edt.ut3.ui.room_finder

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.*
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.edt.ut3.R
import com.edt.ut3.misc.hideKeyboard
import com.edt.ut3.misc.toDp
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.room_finder_fragment.*
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.withContext
import java.io.IOException

class RoomFinderFragment : Fragment() {

    enum class Status { IDLE, SEARCHING, DOWNLOADING, RESULT }

    private var status = Status.IDLE
        set(value) {
            handleStatusChange(value)
            field = value
        }

    private var idle = true

    private val viewModel: RoomFinderViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View?
    {
        return inflater.inflate(R.layout.room_finder_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        status = Status.IDLE
        result.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        result.addItemDecoration(RoomAdapter.RoomSeparator())

        launchBuildingsDownload()
    }

    private fun launchBuildingsDownload() {
        lifecycleScope.launchWhenResumed {
            try {
                val buildings = viewModel.getBuildings()
                hints.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, buildings.map { it.name })

                setupListeners()
            } catch (e: IOException) {
                withContext(Main) {
                    Snackbar.make(result, getString(R.string.data_update_failed), Snackbar.LENGTH_INDEFINITE)
                        .setAction(R.string.action_retry) {
                            launchBuildingsDownload()
                        }
                        .show()
                }
            }
        }
    }

    private fun setupListeners() {
        hints.setOnItemClickListener { parent: AdapterView<*>, view: View, position: Int, id: Long ->
            with (view as TextView) {
                lifecycleScope.launchWhenResumed {
                    status = Status.DOWNLOADING
                    val rooms = viewModel.getFreeRooms(view.text.toString())
                    result.adapter = RoomAdapter(rooms)
                    status = Status.RESULT
                }
            }
        }

        search_bar.apply {
            setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    status = Status.SEARCHING
                }
            }

            setOnClickListener {
                status = Status.SEARCHING
            }
        }

        activity?.run {
            onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
                when (status) {
                    Status.SEARCHING -> status = if (idle) Status.IDLE else Status.RESULT
                    else -> {
                        isEnabled = false
                        onBackPressed()
                    }
                }
            }
        }
    }

    private fun handleStatusChange(status: Status) = when (status) {
        Status.IDLE -> {
            search_bar_container.cardElevation = 0f
            thanks.visibility = VISIBLE
            hints.visibility = GONE
            loading_container.visibility = INVISIBLE
            result.visibility = INVISIBLE

            search_bar.clearFocus()
            hideKeyboard()
        }

        Status.RESULT -> {
            idle = false
            search_bar_container.cardElevation = 0f


            thanks.visibility = INVISIBLE
            hints.visibility = GONE
            loading_container.visibility = INVISIBLE
            result.visibility = VISIBLE

            search_bar.clearFocus()
            hideKeyboard()
        }

        Status.SEARCHING -> {
            search_bar_container.cardElevation = 8.toDp(requireContext())

            thanks.visibility = INVISIBLE
            hints.visibility = VISIBLE
            loading_container.visibility = INVISIBLE
            result.visibility = VISIBLE
        }

        Status.DOWNLOADING -> {
            8.toDp(requireContext())

            hints.visibility = GONE
            thanks.visibility = GONE
            loading_container.visibility = VISIBLE
            result.visibility = INVISIBLE

            search_bar.clearFocus()
            hideKeyboard()
        }
    }

}
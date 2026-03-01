package com.edt.ut3.ui.notes

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.edt.ut3.R
import com.edt.ut3.backend.celcat.Event
import com.edt.ut3.backend.database.viewmodels.EventViewModel
import com.edt.ut3.backend.database.viewmodels.NotesViewModel
import com.edt.ut3.backend.note.Note
import com.edt.ut3.databinding.FragmentNotesBinding
import com.edt.ut3.ui.calendar.BottomSheetFragment
import com.edt.ut3.ui.calendar.event_details.FragmentEventDetails
import com.edt.ut3.ui.theme.UT3Theme
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_COLLAPSED
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NotesFragment : BottomSheetFragment() {

    private val notesViewModel: FragmentNotesViewModel by activityViewModels()

    private lateinit var binding: FragmentNotesBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentNotesBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupComposeView()
        setupBottomSheetManager()
        setupListeners()
    }

    private fun setupComposeView() {
        binding.notesComposeView.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                UT3Theme {
                    NotesContent(
                        notesLiveData = notesViewModel.getNotes(requireContext()),
                        onNoteClick = ::handleNoteClick
                    )
                }
            }
        }
    }

    private fun setupBottomSheetManager() {
        bottomSheetManager.add(binding.eventDetailsNotesContainer)
    }

    private fun setupListeners() {
        val childFragment = childFragmentManager.findFragmentById(R.id.event_details_notes)
        if (childFragment is FragmentEventDetails) {
            childFragment.onReady = {
                bottomSheetManager.setVisibleSheet(binding.eventDetailsNotesContainer)
            }

            childFragment.listenTo = notesViewModel.selectedEvent
        }

        binding.eventDetailsNotesContainer?.let {
            BottomSheetBehavior.from(it).addBottomSheetCallback(object: BottomSheetBehavior.BottomSheetCallback() {
                override fun onStateChanged(bottomSheet: View, newState: Int) {
                    if (newState == STATE_COLLAPSED) {
                        notesViewModel.selectedEvent.value = null
                    }
                }

                override fun onSlide(bottomSheet: View, slideOffset: Float) {
                    // Do nothing here
                }
            })
        }

        setupBackButtonClickListener()
    }

    private fun setupBackButtonClickListener() {
        activity?.onBackPressedDispatcher?.addCallback {
            if (bottomSheetManager.hasVisibleSheet()) {
                bottomSheetManager.setVisibleSheet(null)
            } else {
                isEnabled = false
                activity?.onBackPressed()
            }
        }
    }

    private fun handleNoteClick(note: Note) {
        val eventID = note.eventID

        lifecycleScope.launchWhenResumed {
            try {
                if (eventID is String) {
                    val context = context ?: return@launchWhenResumed
                    val event = EventViewModel(context).getEventsByIDs(eventID).firstOrNull()

                    if (event is Event) {
                        withContext(Main) {
                            notesViewModel.selectedEvent.value = event
                        }
                    } else {
                        throw IllegalStateException()
                    }
                } else {
                    throw IllegalStateException()
                }
            } catch (e: IllegalStateException) {
                withContext(Main) {
                    askToDeleteNote(note)
                }
            }
        }
    }

    private fun askToDeleteNote(note: Note) {
        context?.let { context ->
            AlertDialog.Builder(context)
                .setTitle(R.string.event_not_found)
                .setMessage(R.string.delete_note)
                .setPositiveButton(android.R.string.ok) { dialog, which ->
                    lifecycleScope.launchWhenResumed {
                        NotesViewModel(context).delete(note)
                    }
                }
                .setNegativeButton(android.R.string.cancel) { dialog, which ->
                    // Do nothing here
                }
                .show()
        }
    }
}

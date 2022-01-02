package com.edt.ut3.ui.notes

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.edt.ut3.R
import com.edt.ut3.refactored.models.domain.celcat.Event
import com.edt.ut3.refactored.viewmodels.EventViewModel
import com.edt.ut3.refactored.viewmodels.NotesViewModel
import com.edt.ut3.backend.note.Note
import com.edt.ut3.ui.calendar.BottomSheetFragment
import com.edt.ut3.ui.calendar.event_details.FragmentEventDetails
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_COLLAPSED
import kotlinx.android.synthetic.main.fragment_notes.*
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject
import java.lang.IllegalStateException

class NotesFragment : BottomSheetFragment() {

    private val eventViewModel: EventViewModel by inject()
    private val notesViewModel: NotesViewModel by inject()
    private val fragmentNotesViewModel: FragmentNotesViewModel by activityViewModels()

    private val notes = mutableListOf<Note>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_notes, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        notes_container.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        notes_container.addItemDecoration(NoteAdapter.NoteSeparator())

        setupBottomSheetManager()
        setupListeners()
    }

    private fun setupBottomSheetManager() {
        bottomSheetManager.add(event_details_notes_container)
    }

    private fun setupListeners() {
        val notesLD = fragmentNotesViewModel.getNotes(requireContext())
        notesLD.observe(viewLifecycleOwner) { newNotes ->
            notes.clear()
            notes.addAll(newNotes)

            if (notes.isEmpty()) {
                no_notes_layout.visibility = VISIBLE
            } else {
                no_notes_layout.visibility = GONE
            }

            updateRecyclerAdapter()
        }

        val childFragment = childFragmentManager.findFragmentById(R.id.event_details_notes)
        if (childFragment is FragmentEventDetails) {
            childFragment.onReady = {
                bottomSheetManager.setVisibleSheet(event_details_notes_container)
            }

            childFragment.listenTo = fragmentNotesViewModel.selectedEvent
        }

        event_details_notes_container?.let {
            BottomSheetBehavior.from(it).addBottomSheetCallback(object: BottomSheetBehavior.BottomSheetCallback() {
                override fun onStateChanged(bottomSheet: View, newState: Int) {
                    if (newState == STATE_COLLAPSED) {
                        fragmentNotesViewModel.selectedEvent.value = null
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

    private fun updateRecyclerAdapter() {
        if (notes_container.adapter == null) {
            notes_container.adapter = NoteAdapter(notes).apply {
                onItemClickListener = { note ->
                    val eventID = note.eventID

                    lifecycleScope.launchWhenResumed {
                        try {
                            if (eventID is String) {
                                val event =
                                    eventViewModel.getEventsByIDs(eventID).firstOrNull()

                                if (event is Event) {
                                    withContext(Main) {
                                        fragmentNotesViewModel.selectedEvent.value = event
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
            }
        }

        notes_container.adapter?.notifyDataSetChanged()
    }

    private fun askToDeleteNote(note: Note) {
        context?.let { context ->
            AlertDialog.Builder(context)
                .setTitle(R.string.event_not_found)
                .setMessage(R.string.delete_note)
                .setPositiveButton(android.R.string.ok) { dialog, which ->
                    lifecycleScope.launchWhenResumed {
                        notesViewModel.delete(note)
                    }
                }
                .setNegativeButton(android.R.string.cancel) { dialog, which ->
                    // Do nothing here
                }
                .show()
        }
    }

}
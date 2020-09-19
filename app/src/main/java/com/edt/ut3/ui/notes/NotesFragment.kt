package com.edt.ut3.ui.notes

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.edt.ut3.R
import com.edt.ut3.backend.database.viewmodels.EventViewModel
import com.edt.ut3.backend.note.Note
import com.edt.ut3.ui.calendar.BottomSheetFragment
import com.edt.ut3.ui.calendar.event_details.FragmentEventDetails
import kotlinx.android.synthetic.main.fragment_notes.*

class NotesFragment : BottomSheetFragment() {

    private val notesViewModel: FragmentNotesViewModel by activityViewModels()

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
        val notesLD = notesViewModel.getNotes(requireContext())
        notesLD.observe(viewLifecycleOwner) { newNotes ->
            notes.clear()
            notes.addAll(newNotes)
            updateRecyclerAdapter()
        }

        val childFragment = childFragmentManager.findFragmentById(R.id.event_details_notes)
        if (childFragment is FragmentEventDetails) {
            childFragment.onReady = {
                bottomSheetManager.setVisibleSheet(event_details_notes_container)
            }

            childFragment.listenTo = notesViewModel.selectedEvent
        }
    }

    private fun updateRecyclerAdapter() {
        if (notes_container.adapter == null) {
            notes_container.adapter = NoteAdapter(notes).apply {
                onItemClickListener = { note ->
                    val eventID = note.eventID
                    val ctx = context

                    if (eventID is String && ctx is Context) {
                        lifecycleScope.launchWhenResumed {
                            val event = EventViewModel(ctx).getEventsByIDs(eventID).firstOrNull()
                            notesViewModel.selectedEvent.value = event
                        }
                    }
                }
            }
        }

        notes_container.adapter?.notifyDataSetChanged()
    }

}
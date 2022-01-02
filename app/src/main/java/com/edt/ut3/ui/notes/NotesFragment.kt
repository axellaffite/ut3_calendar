package com.edt.ut3.ui.notes

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.activity.addCallback
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.edt.ut3.R
import com.edt.ut3.refactored.models.domain.celcat.Event
import com.edt.ut3.refactored.viewmodels.EventViewModel
import com.edt.ut3.refactored.viewmodels.NotesViewModel
import com.edt.ut3.backend.note.Note
import com.edt.ut3.refactored.viewmodels.event_details.IEventDetailsSharedViewModel
import com.edt.ut3.ui.calendar.BottomSheetFragment
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_COLLAPSED
import kotlinx.android.synthetic.main.fragment_calendar.*
import kotlinx.android.synthetic.main.fragment_notes.*
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.withContext
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.lang.IllegalStateException

const val layoutID = R.layout.fragment_notes
class NotesFragment : BottomSheetFragment(layoutID) {

    private val sharedViewModel: IEventDetailsSharedViewModel by viewModel()
    private val eventViewModel: EventViewModel by viewModel()
    private val notesViewModel: NotesViewModel by viewModel()
    private val fragmentNotesViewModel: FragmentNotesViewModel by activityViewModels()

    private val notes = mutableListOf<Note>()

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
        val notesLD = fragmentNotesViewModel.getNotes()
        notesLD.observe(viewLifecycleOwner) { newNotes ->
            notes.clear()
            notes.addAll(newNotes)

            no_notes_layout.visibility = if (notes.isEmpty()) { VISIBLE } else { GONE }

            updateRecyclerAdapter()
        }

        event_details_notes_container?.let {
            BottomSheetBehavior.from(it).addBottomSheetCallback(object: BottomSheetBehavior.BottomSheetCallback() {
                override fun onStateChanged(bottomSheet: View, newState: Int) {
                    if (newState == STATE_COLLAPSED) {
                        sharedViewModel.isSubFragmentShown = false
                        sharedViewModel.event.value = null
                    }
                }

                override fun onSlide(bottomSheet: View, slideOffset: Float) = Unit
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
                    sharedViewModel.event.value = eventID
                    sharedViewModel.isSubFragmentShown = true
                    bottomSheetManager.setVisibleSheet(event_details_notes_container)

                    lifecycleScope.launchWhenResumed {
                        if (!eventViewModel.eventExists(eventID)) {
                            askToDeleteNote(note)
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
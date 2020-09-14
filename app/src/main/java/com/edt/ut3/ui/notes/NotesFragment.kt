package com.edt.ut3.ui.notes

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.edt.ut3.R
import com.edt.ut3.backend.note.Note
import kotlinx.android.synthetic.main.fragment_notes.*

class NotesFragment : Fragment() {

    private val notesViewModel: FragmentNotesViewModel by activityViewModels()

    private val notes = mutableListOf<Note>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_notes, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        notes_container.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        notes_container.addItemDecoration(NoteAdapter.NoteSeparator())

        val notesLD = notesViewModel.getNotes(requireContext())
        notesLD.observe(viewLifecycleOwner) { newNotes ->
            notes.clear()
            notes.addAll(newNotes)
            updateRecyclerAdapter()
        }
    }

    private fun updateRecyclerAdapter() {
        if (notes_container.adapter == null) {
            notes_container.adapter = NoteAdapter(notes).apply {
                onItemClickListener = { note ->
                    findNavController().navigate(R.id.action_navigation_notes_to_fragmentNoteDetails)
                }
            }
        }

        notes_container.adapter?.notifyDataSetChanged()
    }

}
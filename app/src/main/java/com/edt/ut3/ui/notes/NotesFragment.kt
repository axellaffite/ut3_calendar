package com.edt.ut3.ui.notes

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Card
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.Text
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.edt.ut3.backend.database.viewmodels.EventViewModel
import com.edt.ut3.backend.note.Note
import com.edt.ut3.ui.calendar.BottomSheetFragment
import com.edt.ut3.ui.calendar.event_details.EventDetailsScreen
import kotlinx.coroutines.launch

class NotesFragment : BottomSheetFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                MaterialTheme {
                    NotesScreen()
                }
            }
        }
    }

//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//
//        binding.notesContainer.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
//        binding.notesContainer.addItemDecoration(NoteAdapter.NoteSeparator())
//
//        setupBottomSheetManager()
//        setupListeners()
//    }
//
//    private fun setupBottomSheetManager() {
//        bottomSheetManager.add(binding.eventDetailsNotesContainer)
//    }
//
//    private fun setupListeners() {
//        val notesLD = notesViewModel.getNotes(requireContext())
//        notesLD.observe(viewLifecycleOwner) { newNotes ->
//            notes.clear()
//            notes.addAll(newNotes)
//
//            if (notes.isEmpty()) {
//                binding.noNotesLayout.visibility = VISIBLE
//            } else {
//                binding.noNotesLayout.visibility = GONE
//            }
//
//            updateRecyclerAdapter()
//        }
//
//        val childFragment = childFragmentManager.findFragmentById(R.id.event_details_notes)
//        if (childFragment is FragmentEventDetails) {
//            childFragment.onReady = {
//                bottomSheetManager.setVisibleSheet(binding.eventDetailsNotesContainer)
//            }
//
//            childFragment.listenTo = notesViewModel.selectedEvent
//        }
//
//        binding.eventDetailsNotesContainer?.let {
//            BottomSheetBehavior.from(it).addBottomSheetCallback(object: BottomSheetBehavior.BottomSheetCallback() {
//                override fun onStateChanged(bottomSheet: View, newState: Int) {
//                    if (newState == STATE_COLLAPSED) {
//                        notesViewModel.selectedEvent.value = null
//                    }
//                }
//
//                override fun onSlide(bottomSheet: View, slideOffset: Float) {
//                    // Do nothing here
//                }
//
//            })
//        }
//
//        setupBackButtonClickListener()
//    }
//
//    private fun setupBackButtonClickListener() {
//        activity?.onBackPressedDispatcher?.addCallback {
//            if (bottomSheetManager.hasVisibleSheet()) {
//                bottomSheetManager.setVisibleSheet(null)
//            } else {
//                isEnabled = false
//                activity?.onBackPressed()
//            }
//        }
//    }
//
//    private fun updateRecyclerAdapter() {
//        if (binding.notesContainer.adapter == null) {
//            binding.notesContainer.adapter = NoteAdapter(notes).apply {
//                onItemClickListener = { note ->
//                    val eventID = note.eventID
//
//                    lifecycleScope.launchWhenResumed {
//                        try {
//                            if (eventID is String) {
//                                val context = context ?: return@launchWhenResumed
//                                val event =
//                                    EventViewModel(context).getEventsByIDs(eventID).firstOrNull()
//
//                                if (event is Event) {
//                                    withContext(Main) {
//                                        notesViewModel.selectedEvent.value = event
//                                    }
//                                } else {
//                                    throw IllegalStateException()
//                                }
//                            } else {
//                                throw IllegalStateException()
//                            }
//                        } catch (e: IllegalStateException) {
//                            withContext(Main) {
//                                askToDeleteNote(note)
//                            }
//                        }
//                    }
//                }
//            }
//        }
//
//        binding.notesContainer.adapter?.notifyDataSetChanged()
//    }
//
//    private fun askToDeleteNote(note: Note) {
//        context?.let { context ->
//            AlertDialog.Builder(context)
//                .setTitle(R.string.event_not_found)
//                .setMessage(R.string.delete_note)
//                .setPositiveButton(android.R.string.ok) { dialog, which ->
//                    lifecycleScope.launchWhenResumed {
//                        NotesViewModel(context).delete(note)
//                    }
//                }
//                .setNegativeButton(android.R.string.cancel) { dialog, which ->
//                    // Do nothing here
//                }
//                .show()
//        }
//    }

}

@OptIn(ExperimentalMaterialApi::class)
@Composable
@Preview
fun NotesScreen() {
    val viewModel: FragmentNotesViewModel = viewModel()
    val notes by viewModel.notesLD.collectAsStateWithLifecycle()
    val selectedEvent by viewModel.selectedEvent.observeAsState()
    val selectedNote by viewModel.selectedNote.observeAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val sheetState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden,
        skipHalfExpanded = true
    )

    LaunchedEffect(Unit) {
        snapshotFlow { sheetState.currentValue }
            .collect {
                when (it) {
                    ModalBottomSheetValue.Hidden -> { viewModel.clearState() }

                    else -> {
                        /* Do nothing */
                    }
                }
            }
    }

    // Show/hide bottom sheet based on selected event
    LaunchedEffect(selectedEvent) {
        if (selectedEvent != null) {
            sheetState.show()
        } else {
            sheetState.hide()
        }
    }

    MaterialTheme {
        ModalBottomSheetLayout(
            sheetState = sheetState,
            sheetContent = {
                // Event details content
                if (selectedEvent != null) {
                    EventDetailsScreen(
                        event = selectedEvent!!,
                        note = selectedNote
                    )
                } else {
                    // Empty content when no event is selected
                    Spacer(modifier = Modifier.height(1.dp))
                }
            }
        ) {
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(notes) { note ->
                    Note(
                        note = note,
                        onClick = {
                            scope.launch {
                                val event = note.eventID?.let { eventId ->
                                    EventViewModel(context).getEventsByIDs(eventId).firstOrNull()
                                }

                                viewModel.selectedNote.value = note
                                viewModel.selectedEvent.value = event
                            }
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun Note(note: Note, modifier: Modifier = Modifier, onClick: () -> Unit = {}) {
    Row(modifier.padding(8.dp)) {
        Card(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
            Column(modifier = Modifier.padding(16.dp)) {
                if (!note.title.isNullOrBlank()) {
                    Row {
                        Text(note.title ?: "No title defined")
                    }
                }

                Row {
                    Text(note.contents)
                }
            }
        }
    }
}
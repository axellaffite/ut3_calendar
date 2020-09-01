package com.edt.ut3.ui.calendar

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.edt.ut3.R
import com.edt.ut3.backend.celcat.Event
import com.edt.ut3.backend.database.AppDatabase
import com.edt.ut3.backend.note.Note
import com.edt.ut3.backend.preferences.PreferencesManager
import com.edt.ut3.misc.Theme
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_event_details.*
import kotlinx.coroutines.launch

class FragmentEventDetails(private val event: Event) : Fragment() {
    private val viewModel: CalendarViewModel by viewModels { defaultViewModelProviderFactory }
    private var note: Note? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requireActivity().nav_view.visibility = GONE
    }

    override fun onDestroy() {
        super.onDestroy()

        requireActivity().nav_view.visibility = VISIBLE
        viewModel.selectedEvent = null
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_event_details, container, false)
    }

    override fun onDestroyView() {
        super.onDestroyView()

        requireActivity().window.statusBarColor = ContextCompat.getColor(requireContext(), R.color.colorPrimaryDark)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        lifecycleScope.launch {
            AppDatabase.getInstance(requireContext()).noteDao().run {
                val result = selectByEventIDs(event.id)
                result.forEach {
                    println(it)
                }
                if (result.size == 1) {
                    println("Assigning note")
                    note = result[0]
                }

                setupContent()
            }
        }

        setupListeners()
    }

    private fun setupContent() {
        title.text = event.courseName ?: event.category
        when (PreferencesManager(requireContext()).getTheme()) {
            Theme.LIGHT -> {
                val textButtonColor = event.textColor?.toColorInt() ?: Color.BLACK
                title.setTextColor(textButtonColor)
                close_button.setColorFilter(textButtonColor)

                event.lightBackgroundColor(requireContext()).let {
                    title_container.setBackgroundColor(it)
                    requireActivity().window.statusBarColor = it
                }
            }

            Theme.DARK -> {
                val textButtonColor = Color.WHITE
                title.setTextColor(textButtonColor)
                close_button.setColorFilter(textButtonColor)

                event.darkBackgroundColor(requireContext()).let {
                    title_container.setBackgroundColor(it)
                    requireActivity().window.statusBarColor = it
                }
            }
        }

        description.text = event.description
        event_note.setText(note?.contents)
    }

    private fun setupListeners() {
        close_button.setOnClickListener {
            requireActivity().onBackPressed()
        }

        event_note.doOnTextChanged { text, start, before, count ->
            if (note == null) {
                note = Note(0L, event.id, null, text.toString(), event.start, event.backgroundColor, event.textColor, false)
            } else {
                note?.contents = text.toString()
            }

            lifecycleScope.launch {
                note?.let {
                    val ids = AppDatabase.getInstance(requireContext()).noteDao().insert(it)
                    if (ids.size == 1) {
                        note = Note(ids[0], it.eventID, it.title, it.contents, it.date, it.color, it.textColor, it.reminder)
                    }
                }
            }
        }
    }
}
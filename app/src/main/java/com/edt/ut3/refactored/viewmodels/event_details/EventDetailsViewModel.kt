package com.edt.ut3.refactored.viewmodels.event_details

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.edt.ut3.R
import com.edt.ut3.backend.note.Note
import com.edt.ut3.backend.note.Picture
import com.edt.ut3.refactored.models.domain.celcat.Event
import com.edt.ut3.refactored.models.repositories.preferences.PreferencesManager
import com.edt.ut3.refactored.viewmodels.NotesViewModel
import com.edt.ut3.ui.preferences.Theme
import com.elzozor.yoda.utils.DateExtensions.get
import kotlinx.android.synthetic.main.fragment_event_details.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class EventDetailsViewModel(
    private val preferencesManager: PreferencesManager,
    private val notesViewModel: NotesViewModel
): ViewModel() {
    private var saveNoteJob: Job? = null
    private val deletedPictures = mutableListOf<Int>()

    var pictureFile: Pair<String, File>? = null

    private val _saving = MutableLiveData(true)
    val saving : LiveData<Boolean> = _saving

    fun getEventCardBackgroundColor(context: Context, event: Event): Int {
        return when (preferencesManager.currentTheme()) {
            Theme.LIGHT -> event.lightBackgroundColor(context)
            Theme.DARK -> event.darkBackgroundColor(context)
        }
    }

    fun generateDateText(context: Context, event: Event): String {
        fun formatTime(date: Date) = "%02dh%02d".format(
            date.get(Calendar.HOUR_OF_DAY),
            date.get(Calendar.MINUTE)
        )

        val date = SimpleDateFormat("EEEE dd/MM/yyyy", Locale.getDefault())
            .format(event.start)
            .replaceFirstChar { it.titlecase(Locale.getDefault()) }

        val time =
            if (event.allday) {
                context.getString(R.string.all_day)
            } else {
                val start = formatTime(event.start)
                val end = formatTime(event.end ?: event.start)

                context.getString(R.string.from_to_format)
                    .format(start, end)
                    .replaceFirstChar { it.titlecase(Locale.getDefault()) }
            }

        return "$date\n$time"
    }

    fun buildDescription(event: Event) = buildString {
        event.categoryWithEmotions?.let(::appendLine)

        appendLine(
            if (event.locations.isNotEmpty()) event.locations.joinToString(", ")
            else event.sites.joinToString(", ")
        )

        event.description?.let(::append)
    }

    fun handleReminderChoice(previousNote: Note?, position: Int, customChoice: (Note) -> Unit) = previousNote?.let {
        val reminderType = Note.Reminder.ReminderType.values()[position]
        when (reminderType) {
            Note.Reminder.ReminderType.NONE -> it.reminder.disable()
            Note.Reminder.ReminderType.FIFTEEN_MINUTES -> it.reminder.setFifteenMinutesBefore()
            Note.Reminder.ReminderType.THIRTY_MINUTES -> it.reminder.setThirtyMinutesBefore()
            Note.Reminder.ReminderType.ONE_HOUR -> it.reminder.setOneHourBefore()
            Note.Reminder.ReminderType.CUSTOM -> customChoice(it)
        }

        if (reminderType != Note.Reminder.ReminderType.CUSTOM) {
            saveNote(it)
        }
    }

    fun saveNote(note: Note, delayMs: Long = 500) {
        saveNoteJob?.cancel()
        saveNoteJob = viewModelScope.launch {
            delay(delayMs)
            _saving.postValue(false)
            notesViewModel.save(note)
            _saving.postValue(true)
        }
    }

    fun addPictureToNote(context: Context, note: Note) = viewModelScope.launch {
        _saving.postValue(false)
        pictureFile?.let { (name, file) ->
            val generated = Picture.generateFromPictureUri(context, name, file.absolutePath)
            note.pictures.add(generated)
            notesViewModel.save(note)
            pictureFile = null
        }
        _saving.postValue(true)
    }

}
package com.edt.ut3.ui.notes

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.LiveData
import com.edt.ut3.R
import com.edt.ut3.backend.note.Note
import com.edt.ut3.ui.theme.UT3Theme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun NotesContent(
    notesLiveData: LiveData<List<Note>>,
    onNoteClick: (Note) -> Unit
) {
    val notes by notesLiveData.observeAsState(initial = emptyList())

    if (notes.isEmpty()) {
        EmptyNotesState()
    } else {
        NotesList(notes = notes, onNoteClick = onNoteClick)
    }
}

@Composable
fun EmptyNotesState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.no_notes),
            contentDescription = null,
            modifier = Modifier
                .weight(1f)
                .padding(32.dp)
        )
        Text(
            text = stringResource(id = R.string.no_notes),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .padding(horizontal = 32.dp, vertical = 32.dp)
        )
    }
}

@Composable
fun NotesList(
    notes: List<Note>,
    onNoteClick: (Note) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(items = notes, key = { it.id }) { note ->
            NoteCard(note = note, onClick = { onNoteClick(note) })
        }
    }
}

@Composable
fun NoteCard(
    note: Note,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            if (!note.title.isNullOrEmpty()) {
                Text(
                    text = note.title!!,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Text(
                text = note.contents,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            val reminderText = if (note.reminder.isActive()) {
                SimpleDateFormat("dd/MM/yyyy - HH:mm ", Locale.getDefault())
                    .format(note.reminder.getReminderDate()!!)
            } else {
                stringResource(id = R.string.no_reminder_set)
            }

            Text(
                text = stringResource(
                    id = R.string.note_reminder_pictures,
                    reminderText,
                    note.pictures.size
                ),
                fontSize = 12.sp,
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.outlineVariant,
                maxLines = 1,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun EmptyNotesStatePreview() {
    UT3Theme {
        EmptyNotesState()
    }
}

@Preview(showBackground = true)
@Composable
private fun NoteCardPreview() {
    UT3Theme {
        Box(modifier = Modifier.padding(16.dp)) {
            NoteCard(
                note = Note(
                    id = 1,
                    eventID = null,
                    title = "Sample Note Title",
                    contents = "This is a sample note content that can span multiple lines to show how the card handles text overflow.",
                    date = Date(),
                    color = null,
                    textColor = null
                ),
                onClick = {}
            )
        }
    }
}

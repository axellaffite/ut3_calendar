package com.edt.ut3.ui.calendar.event_details

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.edt.ut3.R
import com.edt.ut3.backend.celcat.Event
import com.edt.ut3.backend.database.viewmodels.NotesViewModel
import com.edt.ut3.backend.note.Note
import com.edt.ut3.backend.preferences.PreferencesManager
import com.edt.ut3.ui.preferences.Theme
import com.elzozor.yoda.utils.DateExtensions.get
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val TITLE_STYLE = TextStyle(
    fontSize = 24.sp,
    fontWeight = FontWeight.Bold
)

private val SUBTITLE_STYLE = TextStyle(
    fontSize = 20.sp,
    fontWeight = FontWeight.SemiBold
)

private val DATE_STYLE = TextStyle(
    fontSize = 14.sp,
    fontWeight = FontWeight.SemiBold
)

private val demoEvent = Event(
    id = "id",
    category = "Category",
    description = "Description\n\nSecond line\nThird line",
    courseName = "Course name",
    locations = listOf("U1", "U2"),
    sites = listOf("Paul Sabatier"),
    start = Date(),
    end = Date(),
    allday = false,
    backgroundColor = "#FF0000",
    textColor = "#FFFFFF",
    noteID = null
)

@Preview
@Composable
fun EventDetailsScreen(
    event: Event = demoEvent,
    note: Note? = null
) {
    // TODO add fallback
    val context = LocalContext.current
    val notesViewModel = NotesViewModel(context)
    val theme = PreferencesManager.getInstance(context).currentTheme()
    val title = event.courseOrCategory(context)
    val description = event.description ?: stringResource(R.string.no_description_available)
    val color = when (theme) {
        Theme.LIGHT -> event.lightBackgroundColor(context)
        else -> event.darkBackgroundColor(context)
    }
    val dateText = generateDateText(event)
    val locationsText = event
        .locations
        .joinToString()
        .ifEmpty { stringResource(R.string.no_location_found) }
    val noteSectionTitle = stringResource(R.string.note)
    var noteContent by rememberSaveable { mutableStateOf(note?.contents ?: "") }

    MaterialTheme {
        Column(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight()
        ) {
            // Event title
            Element(
                icon = {
                    Box(
                        Modifier
                            .width(16.dp)
                            .height(16.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(color))
                    )
                },
                content = {
                    Text(text = title, style = TITLE_STYLE)
                }
            )

            // Event date
            Element {
                Text(dateText, style = DATE_STYLE)
            }


            // Event description
            Element {
                Text(description)
            }

            // Locations
            Element(
                icon = {
                    Icon(theme = theme, res = R.drawable.ic_location)
                },
                content = {
                    Text(locationsText)
                }
            )

            // Note section
            Element {
                Text(noteSectionTitle, style = SUBTITLE_STYLE)
            }

            // Note pictures
            Element(
                icon = { Icon(theme = theme, res = R.drawable.ic_camera) },
                content = {

                }
            )

            // Note content
            Element(
                icon = { Icon(theme = theme, res = R.drawable.ic_note) },
                content = {
                    OutlinedTextField(
                        value = noteContent,
                        label = { Text(stringResource(R.string.note)) },
                        onValueChange = {
                            noteContent = it
                            notesViewModel.save(
                                note = Note(

                                )
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            )
        }
    }
}

@Composable
fun Element(
    icon: @Composable () -> Unit = {},
    content: @Composable () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
    ) {
        Column(
            modifier = Modifier
                .width(32.dp)
                .fillMaxHeight()
                .padding(start = 8.dp, top = 8.dp, bottom = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            icon()
        }

        Column(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(8.dp)
        ) {
            content()
        }
    }
}

@Composable
fun Icon(theme: Theme, @DrawableRes res: Int) {
    Image(
        painter = painterResource(res),
        contentDescription = null,
        colorFilter = ColorFilter.tint(
            when (theme) {
                Theme.LIGHT -> Color.Black
                Theme.DARK -> Color.White
            }
        )
    )
}


/**
 * Generate the date text depending on
 * the event status (all day or classic)
 * and its start and end dates.
 */
@Composable
private fun generateDateText(event: Event): String {
    val date =
        SimpleDateFormat("EEEE dd/MM/yyyy", Locale.getDefault()).format(event.start)
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

    val time =
        if (event.allday) {
            stringResource(R.string.all_day)
        } else {
            val start = "%02dh%02d".format(
                event.start.get(Calendar.HOUR_OF_DAY),
                event.start.get(Calendar.MINUTE)
            )
            val end = "%02dh%02d".format(
                event.end?.get(Calendar.HOUR_OF_DAY),
                event.end?.get(Calendar.MINUTE)
            )

            val fromToFormat = stringResource(R.string.from_to_format)

            fromToFormat.format(start, end)
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        }

    return "$date\n$time"
}

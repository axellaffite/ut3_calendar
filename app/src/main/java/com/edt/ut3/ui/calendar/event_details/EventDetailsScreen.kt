package com.edt.ut3.ui.calendar.event_details

import android.graphics.BitmapFactory
import android.text.util.Linkify
import android.widget.TextView
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.edt.ut3.R
import com.edt.ut3.backend.maps.Place
import com.edt.ut3.backend.note.Picture
import java.util.Locale

data class EventDetailsState(
    val title: String = "",
    val dateText: String = "",
    val descriptionText: String = "",
    val indicatorColor: Color = Color.Transparent,
    val matchingPlaces: List<Place> = emptyList(),
    val isLoading: Boolean = true,
    val selectedReminderIndex: Int = 0,
    val reminderOptions: List<String> = emptyList(),
    val pictures: List<Picture> = emptyList(),
    val noteText: String = ""
)

data class EventDetailsCallbacks(
    val onClose: () -> Unit = {},
    val onPlaceClick: (Place) -> Unit = {},
    val onReminderSelected: (Int) -> Unit = {},
    val onPictureClick: (Picture) -> Unit = {},
    val onAddPictureClick: () -> Unit = {},
    val onNoteTextChanged: (String) -> Unit = {}
)

@Composable
fun EventDetailsScreen(
    state: EventDetailsState,
    callbacks: EventDetailsCallbacks,
    snackbarHostState: SnackbarHostState
) {
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(paddingValues)
        ) {
            EventDetailsHeader(
                onClose = callbacks.onClose,
                indicatorColor = state.indicatorColor,
                title = state.title,
                dateText = state.dateText
            )

            EventDescriptionView(
                descriptionText = state.descriptionText
            )

            LocationSection(
                places = state.matchingPlaces,
                onPlaceClick = callbacks.onPlaceClick
            )

            Text(
                text = stringResource(R.string.note),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(start = 40.dp, top = 24.dp, end = 16.dp)
            )

            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                } else {
                    NoteSection(
                        state = state,
                        callbacks = callbacks
                    )
                }
            }
        }
    }
}

@Composable
private fun EventDetailsHeader(
    onClose: () -> Unit,
    indicatorColor: Color,
    title: String,
    dateText: String
) {
    IconButton(
        onClick = onClose,
        modifier = Modifier.padding(8.dp)
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_nav_before),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onBackground
        )
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp)
    ) {
        Card(
            modifier = Modifier.size(24.dp),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = indicatorColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {}

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
    }

    Text(
        text = dateText,
        style = MaterialTheme.typography.bodyLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(start = 56.dp, top = 8.dp, end = 16.dp)
    )
}

@Composable
private fun EventDescriptionView(descriptionText: String) {
    if (descriptionText.isNotBlank()) {
        AndroidView(
            factory = { context ->
                TextView(context).apply {
                    autoLinkMask = Linkify.WEB_URLS or Linkify.EMAIL_ADDRESSES
                    setTextIsSelectable(true)
                    setTextAppearance(com.google.android.material.R.style.TextAppearance_MaterialComponents_Body2)
                }
            },
            update = { textView ->
                textView.text = descriptionText
            },
            modifier = Modifier.padding(start = 56.dp, top = 8.dp, end = 16.dp)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LocationSection(
    places: List<Place>,
    onPlaceClick: (Place) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(start = 16.dp, top = 8.dp, end = 16.dp)
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_location),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.width(16.dp))

        if (places.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                places.forEach { place ->
                    SuggestionChip(
                        onClick = { onPlaceClick(place) },
                        label = {
                            Text(
                                text = place.title.uppercase(Locale.FRENCH)
                            )
                        },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                }
            }
        } else {
            Text(
                text = stringResource(R.string.no_location_found),
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

@Composable
private fun NoteSection(
    state: EventDetailsState,
    callbacks: EventDetailsCallbacks
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        ReminderRow(
            selectedIndex = state.selectedReminderIndex,
            options = state.reminderOptions,
            onReminderSelected = callbacks.onReminderSelected
        )

        PicturesRow(
            pictures = state.pictures,
            onPictureClick = callbacks.onPictureClick,
            onAddPictureClick = callbacks.onAddPictureClick
        )

        NoteTextRow(
            text = state.noteText,
            onTextChanged = callbacks.onNoteTextChanged
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderRow(
    selectedIndex: Int,
    options: List<String>,
    onReminderSelected: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(start = 16.dp, top = 8.dp, end = 16.dp)
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_alarm),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.width(16.dp))

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
            modifier = Modifier.weight(1f)
        ) {
            TextField(
                value = options.getOrElse(selectedIndex) { "" },
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.menuAnchor(),
                colors = ExposedDropdownMenuDefaults.textFieldColors()
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEachIndexed { index, option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            expanded = false
                            onReminderSelected(index)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun PicturesRow(
    pictures: List<Picture>,
    onPictureClick: (Picture) -> Unit,
    onAddPictureClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(start = 16.dp, top = 8.dp, end = 16.dp)
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_camera),
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.width(16.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(items = pictures, key = { it.thumbnail }) { picture ->
                PictureThumbnail(
                    picture = picture,
                    onClick = { onPictureClick(picture) }
                )
            }
            item(key = "add_picture") {
                AddPictureThumbnail(onClick = onAddPictureClick)
            }
        }
    }
}

@Composable
private fun PictureThumbnail(
    picture: Picture,
    onClick: () -> Unit
) {
    val bitmap = remember(picture.thumbnail) {
        BitmapFactory.decodeFile(picture.thumbnail)
    }

    Card(
        modifier = Modifier
            .size(64.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        bitmap?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun AddPictureThumbnail(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .size(64.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_add),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun NoteTextRow(
    text: String,
    onTextChanged: (String) -> Unit
) {
    Row(
        modifier = Modifier.padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 16.dp)
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_note),
            contentDescription = null,
            modifier = Modifier
                .size(24.dp)
                .padding(top = 16.dp),
            tint = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.width(16.dp))

        OutlinedTextField(
            value = text,
            onValueChange = onTextChanged,
            label = { Text(stringResource(R.string.note)) },
            minLines = 4,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

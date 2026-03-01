package com.edt.ut3.ui.map

import android.widget.ImageView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.edt.ut3.R
import com.edt.ut3.backend.maps.Place
import com.edt.ut3.ui.theme.Accent
import com.squareup.picasso.Picasso
import org.osmdroid.views.MapView

data class MapsScreenState(
    val searchQuery: String = "",
    val isSearching: Boolean = false,
    val categories: List<CategoryFilter> = emptyList(),
    val searchResults: List<Place> = emptyList(),
    val selectedPlace: Place? = null
)

data class CategoryFilter(
    val name: String,
    val isSelected: Boolean = false
)

data class MapsScreenCallbacks(
    val onSearchQueryChanged: (String) -> Unit = {},
    val onSearchFocused: () -> Unit = {},
    val onCategoryToggled: (String) -> Unit = {},
    val onSearchResultClicked: (Place) -> Unit = {},
    val onGoToClicked: (Place) -> Unit = {},
    val onPlaceImageClicked: (Place) -> Unit = {},
    val onMapTapped: () -> Unit = {},
    val onBackPressed: () -> Unit = {}
)

@Composable
fun MapsScreen(
    state: MapsScreenState,
    callbacks: MapsScreenCallbacks,
    mapView: MapView,
    snackbarHostState: SnackbarHostState
) {
    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize()
        )

        SearchOverlay(state, callbacks)

        AnimatedVisibility(
            visible = state.selectedPlace != null,
            enter = slideInVertically { it },
            exit = slideOutVertically { it },
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            state.selectedPlace?.let { place ->
                PlaceInfoCard(
                    place = place,
                    onGoToClicked = { callbacks.onGoToClicked(place) },
                    onImageClicked = { callbacks.onPlaceImageClicked(place) }
                )
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchOverlay(
    state: MapsScreenState,
    callbacks: MapsScreenCallbacks
) {
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(state.isSearching) {
        if (!state.isSearching) {
            focusManager.clearFocus()
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column {
            TextField(
                value = state.searchQuery,
                onValueChange = callbacks.onSearchQueryChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .onFocusChanged { focusState ->
                        if (focusState.isFocused) {
                            callbacks.onSearchFocused()
                        }
                    },
                singleLine = true,
                placeholder = { Text(stringResource(R.string.map_search_hint)) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )

            if (state.isSearching) {
                if (state.categories.isNotEmpty()) {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(state.categories) { category ->
                            FilterChip(
                                selected = category.isSelected,
                                onClick = { callbacks.onCategoryToggled(category.name) },
                                label = { Text(category.name) },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    selectedContainerColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp)
                ) {
                    items(state.searchResults) { place ->
                        SearchResultItem(
                            place = place,
                            onClick = { callbacks.onSearchResultClicked(place) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResultItem(
    place: Place,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            painter = painterResource(place.getIcon()),
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = place.title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PlaceInfoCard(
    place: Place,
    onGoToClicked: () -> Unit,
    onImageClicked: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AndroidView(
                    factory = { context ->
                        ImageView(context).apply {
                            scaleType = ImageView.ScaleType.CENTER_CROP
                            adjustViewBounds = true
                            clipToOutline = true
                        }
                    },
                    update = { imageView ->
                        if (place.photo != null) {
                            Picasso.get().load(place.photo).into(imageView)
                        } else {
                            Picasso.get().load(R.drawable.no_image_placeholder).into(imageView)
                        }
                    },
                    modifier = Modifier
                        .size(120.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onImageClicked)
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = place.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = place.short_desc ?: stringResource(R.string.no_description_available),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Button(
                onClick = onGoToClicked,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Accent,
                    contentColor = Color.White
                )
            ) {
                Text(stringResource(R.string.go_to))
            }
        }
    }
}

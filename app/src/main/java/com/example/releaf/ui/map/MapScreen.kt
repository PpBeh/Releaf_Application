package com.example.releaf.ui.map

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.releaf.model.CleanlinessStatus
import com.example.releaf.model.Poi
import com.example.releaf.model.PoiCategory
import com.example.releaf.ui.components.MapFilterBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    onDirectionClick: (String) -> Unit,
    onCommentClick: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<PoiCategory?>(null) }
    var selectedCleanliness by remember { mutableStateOf<CleanlinessStatus?>(null) }
    var selectedPoi by remember { mutableStateOf<Poi?>(null) }
    val sheetState = rememberModalBottomSheetState()

    Box(modifier = Modifier.fillMaxSize()) {

        // TODO: replace with the real map view once the Maps SDK/API is wired in.
        // When a pin is tapped there, set: selectedPoi = thatPoi
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFE5E7E0))
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Search") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true
                )
                Spacer(modifier = Modifier.width(12.dp))
                FilledIconButton(onClick = { /* TODO: open map settings */ }) {
                    Icon(Icons.Default.Settings, contentDescription = "Map settings")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            MapFilterBar(
                selectedCategory = selectedCategory,
                onCategorySelected = { selectedCategory = it /* TODO: re-filter pins by category */ },
                selectedCleanliness = selectedCleanliness,
                onCleanlinessSelected = { selectedCleanliness = it /* TODO: re-filter pins by cleanliness */ }
            )
        }

        // Notification bell, bottom-left
        BadgedBox(
            badge = { Badge { Text("9") } },
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
        ) {
            FloatingActionButton(
                onClick = { /* TODO: open notifications */ },
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Icon(Icons.Default.Notifications, contentDescription = "Notifications")
            }
        }

        // Add pin + locate-me, bottom-right
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FloatingActionButton(onClick = { /* TODO: add a new pin (toilet or trash can) */ }) {
                Icon(Icons.Default.Add, contentDescription = "Add pin")
            }
            SmallFloatingActionButton(
                onClick = { /* TODO: center map on current location */ },
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Icon(Icons.Default.MyLocation, contentDescription = "My location")
            }
        }
    }

    selectedPoi?.let { poi ->
        ModalBottomSheet(
            onDismissRequest = { selectedPoi = null },
            sheetState = sheetState
        ) {
            PoiDetailSheet(
                poi = poi,
                onCloseClick = { selectedPoi = null },
                onDirectionClick = { onDirectionClick(poi.id) },
                onCommentClick = { onCommentClick(poi.id) }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MapScreenPreview() {
    MapScreen(onDirectionClick = {}, onCommentClick = {})
}
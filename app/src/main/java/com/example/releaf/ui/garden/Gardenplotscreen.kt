package com.example.releaf.ui.garden

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.releaf.model.PlantSlot
import com.example.releaf.model.PlantSlotState

@Composable
fun GardenPlotScreen(
    onBackClick: () -> Unit
) {
    // TODO: replace with real points + plant slot data from save state
    val currentPoints = 60
    val pointsTarget = 100
    val slots = listOf(
        PlantSlot("1", PlantSlotState.EMPTY_POT),
        PlantSlot("2", PlantSlotState.EMPTY_POT),
        PlantSlot("3", PlantSlotState.EMPTY_POT),
        PlantSlot("4", PlantSlotState.GROWING),
        PlantSlot("5", PlantSlotState.FULLY_GROWN),
        PlantSlot("6", PlantSlotState.EMPTY_POT)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF8FD3F4))
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(20.dp))
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Text("$currentPoints/$pointsTarget points", fontWeight = FontWeight.Bold)
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(24.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(slots) { slot ->
                PlantPotTile(slot = slot, onClick = { /* TODO: handle tap on this plant slot */ })
            }
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("Watering can", "Cash", "Shovel", "Fertilizer").forEach { toolName ->
                GardenToolTile(
                    label = toolName,
                    modifier = Modifier.weight(1f),
                    onClick = { /* TODO: use this tool once tool actions are wired in */ }
                )
            }
        }
    }
}

@Composable
private fun PlantPotTile(slot: PlantSlot, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .background(Color(0xFFE8DFC0), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        // TODO: swap for the real pot / character illustration (painterResource) based on slot.state
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(
                    Color(0xFFB5652A),
                    RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
                )
        )
    }
}

@Composable
private fun GardenToolTile(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .background(Color(0xFF3E2415))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        // TODO: swap for the real tool icon (painterResource) — watering can / cash / shovel / fertilizer
    }
}

@Preview(showBackground = true)
@Composable
private fun GardenPlotScreenPreview() {
    GardenPlotScreen(onBackClick = {})
}
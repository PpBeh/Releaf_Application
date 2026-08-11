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
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.releaf.data.remote.dto.PlantSlotDto
import com.example.releaf.ui.viewmodel.GardenViewModel

@Composable
fun GardenPlotScreen(
    viewModel: GardenViewModel,
    userId: String,
    onBackClick: () -> Unit
) {
    val garden by viewModel.garden.collectAsState()
    val plantSlots by viewModel.plantSlots.collectAsState()

    LaunchedEffect(userId) {
        viewModel.loadGarden(userId)
    }

    val currentPoints = garden?.current_points ?: 0
    val pointsTarget = garden?.points_target ?: 100
    val slots = plantSlots.ifEmpty {
        listOf(
            PlantSlotDto(user_id = userId, slot_index = 1, state = "EMPTY_POT"),
            PlantSlotDto(user_id = userId, slot_index = 2, state = "EMPTY_POT"),
            PlantSlotDto(user_id = userId, slot_index = 3, state = "EMPTY_POT"),
            PlantSlotDto(user_id = userId, slot_index = 4, state = "GROWING"),
            PlantSlotDto(user_id = userId, slot_index = 5, state = "FULLY_GROWN"),
            PlantSlotDto(user_id = userId, slot_index = 6, state = "EMPTY_POT")
        )
    }

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
                PlantPotTile(
                    slot = slot,
                    onClick = {
                        if (slot.state == "FULLY_GROWN") {
                            viewModel.harvestSlot(slot.id, userId)
                        }
                    }
                )
            }
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            val tools = listOf(
                Triple("Water", Icons.Default.Opacity, { }),
                Triple("Cash", Icons.Default.AttachMoney, { }),
                Triple("Shovel", Icons.Default.Construction, { }),
                Triple("Fertilize", Icons.Default.Spa, { })
            )
            tools.forEach { (label, icon, action) ->
                GardenToolTile(
                    label = label,
                    icon = icon,
                    modifier = Modifier.weight(1f),
                    onClick = action
                )
            }
        }
    }
}

@Composable
private fun PlantPotTile(slot: PlantSlotDto, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .background(Color(0xFFE8DFC0), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        val color = when (slot.state) {
            "LOCKED" -> Color(0xFF9E9E9E)
            "EMPTY_POT" -> Color(0xFFB5652A)
            "GROWING" -> Color(0xFF66BB6A)
            "FULLY_GROWN" -> Color(0xFFFFC107)
            else -> Color(0xFFB5652A)
        }
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(
                    color,
                    RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
                )
        )
        if (slot.state == "FULLY_GROWN") {
            Text("Tap to\nHarvest", style = MaterialTheme.typography.labelSmall, color = Color.Black)
        }
    }
}

@Composable
private fun GardenToolTile(label: String, icon: ImageVector, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(
        modifier = modifier
            .aspectRatio(1f)
            .background(Color(0xFF3E2415))
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(icon, contentDescription = label, tint = Color.White)
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White)
    }
}

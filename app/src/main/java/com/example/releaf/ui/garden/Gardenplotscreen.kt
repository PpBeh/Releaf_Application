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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.releaf.data.remote.dto.PlantSlotDto
import com.example.releaf.ui.theme.string
import com.example.releaf.ui.viewmodel.GardenViewModel
import com.example.releaf.ui.viewmodel.ThemeViewModel

@Composable
fun GardenPlotScreen(
    viewModel: GardenViewModel,
    userId: String,
    themeViewModel: ThemeViewModel,
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
        List(6) { i -> PlantSlotDto(user_id = userId, slot_index = i + 1, state = "EMPTY_POT") }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(string("points", themeViewModel), style = MaterialTheme.typography.labelSmall)
                    Text("$currentPoints / $pointsTarget", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Box(
                modifier = Modifier
                    .background(Color(0xFFE91E63), RoundedCornerShape(20.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("\uD83D\uDC8E", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("${garden?.current_gems ?: 0}", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }

        Text(
            string("garden_plot", themeViewModel),
            modifier = Modifier.padding(start = 24.dp, top = 16.dp, bottom = 8.dp),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

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
                    themeViewModel = themeViewModel,
                    onClick = {
                        if (slot.state == "FULLY_GROWN") {
                            viewModel.harvestSlot(slot.id, userId)
                        }
                    }
                )
            }
        }

        // Bottom Tools Bar
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                val tools = listOf(
                    Triple("Water", Icons.Default.Opacity, Color(0xFF2196F3)),
                    Triple("Shovel", Icons.Default.Construction, Color(0xFF795548)),
                    Triple("Fertilize", Icons.Default.Spa, Color(0xFF4CAF50)),
                    Triple("Store", Icons.Default.AttachMoney, Color(0xFFFFC107))
                )
                tools.forEach { (label, icon, color) ->
                    GardenToolTile(
                        label = label,
                        icon = icon,
                        color = color,
                        onClick = { }
                    )
                }
            }
        }
    }
}

@Composable
private fun PlantPotTile(slot: PlantSlotDto, themeViewModel: ThemeViewModel, onClick: () -> Unit) {
    androidx.compose.material3.ElevatedCard(
        modifier = Modifier
            .aspectRatio(1f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (slot.state == "LOCKED") MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                             else MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            val emoji = when (slot.state) {
                "LOCKED" -> "\uD83D\uDD12"
                "EMPTY_POT" -> ""
                "GROWING" -> "\uD83C\uDF31"
                "FULLY_GROWN" -> "\uD83C\uDF3B"
                else -> ""
            }
            
            if (slot.state == "EMPTY_POT") {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(
                                Brush.radialGradient(
                                    listOf(Color(0xFF8D6E63), Color(0xFF5D4037))
                                ),
                                RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp, topStart = 6.dp, topEnd = 6.dp)
                            )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(string("empty_pot", themeViewModel), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else if (slot.state == "LOCKED") {
                Text(emoji, style = MaterialTheme.typography.displayMedium, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(emoji, style = MaterialTheme.typography.displayMedium)
                    if (slot.state == "FULLY_GROWN") {
                        Spacer(modifier = Modifier.height(8.dp))
                        androidx.compose.material3.Button(
                            onClick = onClick,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                            modifier = Modifier.height(28.dp),
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color(0xFFFFC107))
                        ) {
                            Text(string("harvest", themeViewModel).uppercase(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color.Black)
                        }
                    } else if (slot.state == "GROWING") {
                        Text(string("growing", themeViewModel), style = MaterialTheme.typography.labelSmall, color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun GardenToolTile(label: String, icon: ImageVector, color: Color, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(color.copy(alpha = 0.2f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = color)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium)
    }
}

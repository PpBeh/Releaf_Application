package com.example.releaf.ui.garden

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.releaf.R
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
    val context = LocalContext.current
    val currentExp by viewModel.currentExp.collectAsState()
    val plantSlots by viewModel.plantSlots.collectAsState()

    LaunchedEffect(userId) {
        viewModel.loadGarden(userId, context)
    }

    val treeStage = viewModel.getTreeStage(currentExp)
    val nextTargetExp = when (treeStage) {
        1 -> 2000
        2 -> 5000
        else -> 10000
    }
    val expProgress = (currentExp.toFloat() / nextTargetExp.toFloat()).coerceIn(0f, 1f)

    val slots = plantSlots.ifEmpty {
        List(6) { i -> PlantSlotDto(user_id = userId, slot_index = i + 1, state = "EMPTY_POT") }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
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
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🌟", style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "$currentExp / $nextTargetExp",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { expProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = Color(0xFF4CAF50),
                        trackColor = MaterialTheme.colorScheme.outlineVariant
                    )
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
                PlantPotTile(slot = slot)
            }
        }
    }
}

@Composable
private fun PlantPotTile(slot: PlantSlotDto) {
    ElevatedCard(
        modifier = Modifier.aspectRatio(1f),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            val imageRes = if (slot.state == "PLANTED") {
                when (slot.slot_index) {
                    1 -> R.drawable.ic_plant_1
                    2 -> R.drawable.ic_plant_2
                    3 -> R.drawable.ic_plant_3
                    4 -> R.drawable.ic_plant_4
                    5 -> R.drawable.ic_plant_5
                    6 -> R.drawable.ic_plant_6
                    else -> R.drawable.ic_plant_1
                }
            } else {
                R.drawable.ic_pot_empty
            }

            Image(
                painter = painterResource(id = imageRes),
                contentDescription = "Plant Slot ${slot.slot_index}",
                modifier = Modifier.size(72.dp)
            )
        }
    }
}
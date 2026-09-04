package com.example.releaf.ui.garden

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.releaf.R
import com.example.releaf.data.remote.dto.PlantSlotDto
import com.example.releaf.model.SeedData
import com.example.releaf.ui.theme.string
import com.example.releaf.ui.viewmodel.GardenViewModel
import com.example.releaf.ui.viewmodel.ThemeViewModel
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun GardenPlotScreen(
    viewModel: GardenViewModel,
    userId: String,
    themeViewModel: ThemeViewModel,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val lang by themeViewModel.language.collectAsState()
    fun t(key: String) = com.example.releaf.ui.theme.AppStrings.get(key, lang)
    val currentExp by viewModel.currentExp.collectAsState()
    val plantSlots by viewModel.plantSlots.collectAsState()
    val waterUsesLeft by viewModel.waterUsesLeft.collectAsState()
    val fertilizeUsesLeft by viewModel.fertilizeUsesLeft.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()

    var selectedSlotForDetails by remember { mutableStateOf<PlantSlotDto?>(null) }

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

    val hasPlantedPlant = slots.any { com.example.releaf.model.isPlantedState(it.state) }

    // Auto-dismiss transient status banners
    LaunchedEffect(statusMessage) {
        if (!statusMessage.isNullOrBlank()) {
            kotlinx.coroutines.delay(4000.milliseconds)
            viewModel.clearStatusMessage()
        }
    }

    if (selectedSlotForDetails != null) {
        val slot = selectedSlotForDetails!!
        val seedInfo = SeedData.getSeedForSlot(slot.slot_index)
        val isPlanted = com.example.releaf.model.isPlantedState(slot.state)

        Dialog(onDismissRequest = { selectedSlotForDetails = null }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Close Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isPlanted) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                if (isPlanted) String.format(
                                    java.util.Locale.US,
                                    t("active_plant"),
                                    slot.slot_index
                                )
                                else String.format(
                                    java.util.Locale.US,
                                    t("slot_empty"),
                                    slot.slot_index
                                ),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isPlanted) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }

                        IconButton(onClick = { selectedSlotForDetails = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Plant Illustration Box
                    val plantImageRes = if (isPlanted) {
                        when (slot.slot_index) {
                            1 -> R.drawable.ic_plant_1
                            2 -> R.drawable.ic_plant_2
                            3 -> R.drawable.ic_plant_3
                            4 -> R.drawable.ic_plant_4
                            5 -> R.drawable.ic_plant_5
                            else -> R.drawable.ic_plant_6
                        }
                    } else {
                        R.drawable.ic_pot_empty
                    }

                    Box(
                        modifier = Modifier
                            .size(110.dp)
                            .clip(CircleShape)
                            .background(
                                if (isPlanted) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.surfaceVariant
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = plantImageRes),
                            contentDescription = seedInfo.name,
                            modifier = Modifier.size(80.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Title & Nickname
                    Text(
                        if (seedInfo.nickname.isNotBlank()) "${seedInfo.nickname} the ${seedInfo.name}" else seedInfo.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    if (seedInfo.personality.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                seedInfo.personality,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    // Favorite Quote Card
                    if (seedInfo.quote.isNotBlank()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFFFF8E1),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                seedInfo.quote,
                                style = MaterialTheme.typography.bodySmall,
                                fontStyle = FontStyle.Italic,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF795548),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    // Mood & Vibe
                    if (seedInfo.mood.isNotBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                "Current Vibe: ",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                seedInfo.mood,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Description & Care Tip
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                RoundedCornerShape(12.dp)
                            )
                            .padding(12.dp)
                    ) {
                        Text(
                            "Description & Lore",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            seedInfo.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        if (seedInfo.careTip.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Care Tip",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2E7D32)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                seedInfo.careTip,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Action buttons in dialog
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.waterPlant(userId, context)
                            },
                            enabled = waterUsesLeft > 0 && hasPlantedPlant,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
                        ) {
                            Text(t("water_dialog"), fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                viewModel.fertilizePlant(userId, context)
                            },
                            enabled = fertilizeUsesLeft > 0 && hasPlantedPlant,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                        ) {
                            Text(t("fertilize_dialog"), fontSize = 12.sp)
                        }
                    }
                    if (isPlanted) {
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // TOP NAVIGATION BAR
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(painter = painterResource(id = R.drawable.ic_arrow_back), contentDescription = "Back")
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
                            "$currentExp / $nextTargetExp EXP",
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

        // TITLE & GARDEN CARE ACTIONS
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
            Text(
                string("garden_plot", themeViewModel),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Tap any plant to read its personality, lore, and care tips!",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (!statusMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        statusMessage!!,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Daily care uses remaining (actions live inside each plant's dialog)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (waterUsesLeft > 0) Color(0xFFE3F2FD) else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text("💧", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                t("water"),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (waterUsesLeft > 0) Color(0xFF1565C0) else Color.Gray
                            )
                            Text(
                                t("uses_left_today") + "$waterUsesLeft",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray
                            )
                        }
                    }
                }
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (fertilizeUsesLeft > 0) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text("🌱", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                t("fertilize"),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (fertilizeUsesLeft > 0) Color(0xFF2E7D32) else Color.Gray
                            )
                            Text(
                                t("uses_left_today") + "$fertilizeUsesLeft",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                t("plot_tap_hint"),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // GRID OF PLANT POT TILES WITH PERSONALITY INFO
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(slots) { slot ->
                PlantPotTileWithPersonality(
                    slot = slot,
                    onClick = { selectedSlotForDetails = slot }
                )
            }
        }

    }

}

@Composable
private fun PlantPotTileWithPersonality(
    slot: PlantSlotDto,
    onClick: () -> Unit
) {
    val seedInfo = SeedData.getSeedForSlot(slot.slot_index)
    val isPlanted = com.example.releaf.model.isPlantedState(slot.state)

    val imageRes = if (isPlanted) {
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

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPlanted) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Plant image inside circular tinted background
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(
                        if (isPlanted) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.surfaceVariant
                    )
            ) {
                Image(
                    painter = painterResource(id = imageRes),
                    contentDescription = "Plant Slot ${slot.slot_index}",
                    modifier = Modifier.size(46.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Plant Name & Nickname
            Text(
                if (isPlanted && seedInfo.nickname.isNotBlank()) "${seedInfo.nickname} (${seedInfo.name})" else "Slot ${slot.slot_index}: ${seedInfo.name}",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            // Personality or Description snippet
            Text(
                if (isPlanted && seedInfo.personality.isNotBlank()) seedInfo.personality else seedInfo.description,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.height(28.dp)
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Status Chip
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (isPlanted) Color(0xFFC8E6C9) else MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(
                    text = if (isPlanted) "Planted • Active" else "Empty Pot",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isPlanted) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }
    }
}

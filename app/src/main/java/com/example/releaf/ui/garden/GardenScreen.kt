package com.example.releaf.ui.garden

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.releaf.ui.theme.string
import com.example.releaf.ui.viewmodel.GardenViewModel
import com.example.releaf.ui.viewmodel.ThemeViewModel

@Composable
fun GardenScreen(
    viewModel: GardenViewModel,
    userId: String,
    themeViewModel: ThemeViewModel,
    onHouseClick: () -> Unit
) {
    val garden by viewModel.garden.collectAsState()

    LaunchedEffect(userId) {
        viewModel.loadGarden(userId)
    }

    val currentExp = garden?.current_exp ?: 0
    val expTarget = garden?.exp_target ?: 1000
    val growUsesLeft = garden?.grow_uses_left ?: 0
    val growUsesMax = garden?.grow_uses_max ?: 1
    val fertilizeUsesLeft = garden?.fertilize_uses_left ?: 0
    val fertilizeUsesMax = garden?.fertilize_uses_max ?: 1
    val expProgress = if (expTarget > 0) currentExp.toFloat() / expTarget else 0f

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Decorative background elements could go here
        
        Column(modifier = Modifier.fillMaxSize()) {
            // Header with stats
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(string("garden", themeViewModel) + " Status", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                            Text("Level ${ (currentExp / 1000) + 1 }", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        }
                        
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFE91E63), RoundedCornerShape(12.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("\uD83D\uDC8E", style = MaterialTheme.typography.bodySmall)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("${garden?.current_gems ?: 0}", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text("Exp: $currentExp / $expTarget", style = MaterialTheme.typography.labelSmall)
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { expProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = Color(0xFF4CAF50),
                        trackColor = MaterialTheme.colorScheme.outlineVariant
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Main Garden View
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.material3.ElevatedCard(
                    modifier = Modifier
                        .size(240.dp)
                        .clickable(onClick = onHouseClick),
                    shape = RoundedCornerShape(40.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                            MaterialTheme.colorScheme.primaryContainer
                                        )
                                    )
                                ),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                "\uD83C\uDFE1",
                                style = MaterialTheme.typography.displayLarge.copy(fontSize = 80.sp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                string("garden_plot", themeViewModel),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                "Enter your sanctuary",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1.2f))

            // Action Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                GardenActionButton(
                    label = "Grow",
                    icon = "\uD83C\uDF31",
                    usesLeft = growUsesLeft,
                    usesMax = growUsesMax,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.growPlant(userId) }
                )
                GardenActionButton(
                    label = "Fertilize",
                    icon = "\uD83E\uDDB4",
                    usesLeft = fertilizeUsesLeft,
                    usesMax = fertilizeUsesMax,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.fertilizePlant(userId) }
                )
            }
        }
    }
}

@Composable
private fun GardenActionButton(
    label: String,
    icon: String,
    usesLeft: Int,
    usesMax: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(64.dp),
        shape = RoundedCornerShape(16.dp),
        enabled = usesLeft > 0,
        contentPadding = PaddingValues(0.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(icon, style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(label, fontWeight = FontWeight.Bold)
                Text("$usesLeft/$usesMax Left", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

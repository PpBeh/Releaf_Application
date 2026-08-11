package com.example.releaf.ui.garden

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.releaf.ui.viewmodel.GardenViewModel

@Composable
fun GardenScreen(
    viewModel: GardenViewModel,
    userId: String,
    onHouseClick: () -> Unit
) {
    val garden by viewModel.garden.collectAsState()

    LaunchedEffect(userId) {
        viewModel.loadGarden(userId)
    }

    val progress = garden
    val currentExp = progress?.current_exp ?: 0
    val expTarget = progress?.exp_target ?: 1000
    val growUsesLeft = progress?.grow_uses_left ?: 0
    val growUsesMax = progress?.grow_uses_max ?: 1
    val fertilizeUsesLeft = progress?.fertilize_uses_left ?: 0
    val fertilizeUsesMax = progress?.fertilize_uses_max ?: 1

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF8FD3F4))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            Box(
                modifier = Modifier
                    .padding(16.dp)
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(20.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text("Exp: $currentExp/$expTarget")
            }

            Spacer(modifier = Modifier.weight(1f))

            Row(
                modifier = Modifier.fillMaxWidth().padding(end = 24.dp),
                horizontalArrangement = Arrangement.End
            ) {
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .background(Color(0xFFB0BEC5), RoundedCornerShape(12.dp))
                        .clickable(onClick = onHouseClick),
                    contentAlignment = Alignment.Center
                ) {
                    Text("House\n(tap to open garden plot)", textAlign = TextAlign.Center)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                GardenActionButton(
                    label = "Grow Plant",
                    usesLeft = growUsesLeft,
                    usesMax = growUsesMax,
                    onClick = { viewModel.growPlant(userId) }
                )
                GardenActionButton(
                    label = "Fertilize Plant",
                    usesLeft = fertilizeUsesLeft,
                    usesMax = fertilizeUsesMax,
                    onClick = { viewModel.fertilizePlant(userId) }
                )
            }
        }
    }
}

@Composable
private fun GardenActionButton(
    label: String,
    usesLeft: Int,
    usesMax: Int,
    onClick: () -> Unit
) {
    Button(onClick = onClick, shape = RoundedCornerShape(50), enabled = usesLeft > 0) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label)
            Text("$usesLeft/$usesMax", style = MaterialTheme.typography.labelSmall)
        }
    }
}

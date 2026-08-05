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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.releaf.model.GardenProgress

@Composable
fun GardenScreen(
    onHouseClick: () -> Unit
) {
    // TODO: replace with real progress from save data
    val progress = GardenProgress(
        currentExp = 0,
        expTarget = 1000,
        growUsesLeft = 1,
        growUsesMax = 1,
        fertilizeUsesLeft = 1,
        fertilizeUsesMax = 1
    )

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
                Text("Exp: ${progress.currentExp}/${progress.expTarget}")
            }

            Spacer(modifier = Modifier.weight(1f))

            Row(
                modifier = Modifier.fillMaxWidth().padding(end = 24.dp),
                horizontalArrangement = Arrangement.End
            ) {
                // TODO: swap this box for the real house illustration (painterResource) from the Figma export
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
                    usesLeft = progress.growUsesLeft,
                    usesMax = progress.growUsesMax,
                    onClick = { /* TODO: grow the plant */ }
                )
                GardenActionButton(
                    label = "Fertilize Plant",
                    usesLeft = progress.fertilizeUsesLeft,
                    usesMax = progress.fertilizeUsesMax,
                    onClick = { /* TODO: fertilize the plant */ }
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
    Button(onClick = onClick, shape = RoundedCornerShape(50)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label)
            Text("$usesLeft/$usesMax", style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun GardenScreenPreview() {
    GardenScreen(onHouseClick = {})
}
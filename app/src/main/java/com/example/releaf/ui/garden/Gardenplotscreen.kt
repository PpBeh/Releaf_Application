package com.example.releaf.ui.garden

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun GardenPlotScreen(
    onBackClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF8FD3F4))
    ) {
        IconButton(onClick = onBackClick, modifier = Modifier.padding(8.dp)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
        }

        // TODO: swap in the real trees/flowers illustration (painterResource) — this block is sample content for now
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp)
                .padding(horizontal = 16.dp)
                .background(
                    Color(0xFFC8B27A),
                    RoundedCornerShape(topStart = 60.dp, topEnd = 60.dp, bottomStart = 24.dp, bottomEnd = 24.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text("Trees + flowers plot (sample)")
        }

        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
                PlotActionButton(label = "Grow Plant", modifier = Modifier.weight(1f), onClick = { /* TODO: grow plant */ })
                PlotActionButton(label = "Harvest Plant", modifier = Modifier.weight(1f), onClick = { /* TODO: harvest plant */ })
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
                PlotActionButton(label = "Plant new plant", modifier = Modifier.weight(1f), onClick = { /* TODO: plant new plant */ })
                PlotActionButton(label = "Sell Plant", modifier = Modifier.weight(1f), onClick = { /* TODO: sell plant */ })
            }
        }
    }
}

@Composable
private fun PlotActionButton(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = modifier, shape = RoundedCornerShape(50)) {
        Text(label)
    }
}

@Preview(showBackground = true)
@Composable
private fun GardenPlotScreenPreview() {
    GardenPlotScreen(onBackClick = {})
}
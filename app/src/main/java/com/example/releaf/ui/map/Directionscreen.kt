package com.example.releaf.ui.map

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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

private data class DirectionStep(val distanceLabel: String, val instruction: String)

@Composable
fun DirectionScreen(
    poiId: String,
    onBackClick: () -> Unit
) {
    // TODO: replace with the real ETA / distance / steps once a routing API is wired in
    val steps = listOf(
        DirectionStep("600 m", "Go straight, then turn left"),
        DirectionStep("300 m", "Go straight, then turn right"),
        DirectionStep("60 m", "Go straight")
    )

    Box(modifier = Modifier.fillMaxSize()) {

        // TODO: replace with the real map + route once the Maps SDK/API is wired in
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFEDEDED))
        )

        Column(modifier = Modifier.fillMaxSize()) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFD6E9FF))
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                    Text("15 minutes", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                    Text("Arriving soon", style = MaterialTheme.typography.bodyMedium)
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFD6E9FF))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("1.0 km", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    steps.forEach { step ->
                        Row(modifier = Modifier.padding(vertical = 6.dp)) {
                            Text(
                                text = step.distanceLabel,
                                modifier = Modifier.padding(end = 16.dp),
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(step.instruction, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DirectionScreenPreview() {
    DirectionScreen(poiId = "sample", onBackClick = {})
}
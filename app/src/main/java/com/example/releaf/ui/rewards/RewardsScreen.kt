package com.example.releaf.ui.rewards

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

// Note: the plant-pot collection grid moved to GardenPlotScreen.kt — that
// screen was actually the real garden plot, not a Rewards sub-view. This
// screen is just the points/tier unlock list now.
private data class UnlockTier(val target: Int, val unlocked: Boolean)

@Composable
fun RewardsScreen() {
    // TODO: replace with real points / tier data
    val current = 600
    val tiers = listOf(UnlockTier(500, true), UnlockTier(2000, false), UnlockTier(10000, false))

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("$current/1000 points", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            tiers.forEach { tier ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .background(
                            if (tier.unlocked) Color(0xFF8BC34A) else MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(12.dp)
                        )
                        .padding(16.dp)
                ) {
                    Text("$current/${tier.target} Unlock")
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun RewardsScreenPreview() {
    RewardsScreen()
}
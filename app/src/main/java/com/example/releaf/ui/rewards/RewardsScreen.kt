package com.example.releaf.ui.rewards

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.releaf.model.PlantSlot
import com.example.releaf.model.PlantSlotState

private enum class RewardsTab { COLLECTION, UNLOCKS }
private data class UnlockTier(val target: Int, val unlocked: Boolean)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RewardsScreen() {
    var selectedTab by remember { mutableStateOf(RewardsTab.COLLECTION) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = selectedTab == RewardsTab.COLLECTION,
                onClick = { selectedTab = RewardsTab.COLLECTION },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
            ) { Text("Collection") }
            SegmentedButton(
                selected = selectedTab == RewardsTab.UNLOCKS,
                onClick = { selectedTab = RewardsTab.UNLOCKS },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
            ) { Text("Unlocks") }
        }

        Spacer(modifier = Modifier.height(16.dp))

        when (selectedTab) {
            RewardsTab.COLLECTION -> PlantCollectionSection()
            RewardsTab.UNLOCKS -> PointsUnlockSection()
        }
    }
}

@Composable
private fun PlantCollectionSection() {
    // TODO: replace with real plant collection data
    val slots = List(6) { PlantSlot(id = it.toString(), state = PlantSlotState.EMPTY_POT) }

    Column(modifier = Modifier.fillMaxSize()) {
        Text("60/100 points", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(slots) { slot ->
                // TODO: swap in the real pot / plant illustration (painterResource) based on slot.state
                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .background(Color(0xFFE8DFC0), RoundedCornerShape(12.dp))
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            // TODO: swap these 4 for the real watering can / cash / shovel / fertilizer icons
            repeat(4) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                        .background(Color(0xFF6D4C29), RoundedCornerShape(8.dp))
                )
            }
        }
    }
}

@Composable
private fun PointsUnlockSection() {
    // TODO: replace with real points / tier data
    val current = 600
    val tiers = listOf(UnlockTier(500, true), UnlockTier(2000, false), UnlockTier(10000, false))

    Column(modifier = Modifier.fillMaxSize()) {
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
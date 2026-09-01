package com.example.releaf.ui.rewards

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.releaf.ui.theme.string
import com.example.releaf.ui.viewmodel.RewardsViewModel
import com.example.releaf.ui.viewmodel.ThemeViewModel

@Composable
fun RewardsScreen(
    viewModel: RewardsViewModel,
    userId: String,
    themeViewModel: ThemeViewModel
) {
    val tiers by viewModel.tiers.collectAsState()
    val userRewards by viewModel.userRewards.collectAsState()
    val userPoints by viewModel.userPoints.collectAsState()
    val gardenSlots by viewModel.gardenSlots.collectAsState()

    val seedMilestones = listOf(50, 150, 300, 500, 800, 1200)

    LaunchedEffect(userId) {
        viewModel.loadRewards(userId)
    }

    val nextTargetExp = when {
        userPoints < 2000 -> 2000
        userPoints < 5000 -> 5000
        else -> 10000
    }
    val expProgress = (userPoints.toFloat() / nextTargetExp.toFloat()).coerceIn(0f, 1f)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🌟", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "$userPoints / $nextTargetExp",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
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

        Spacer(modifier = Modifier.height(24.dp))

        Text("Titles & Badges", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            if (tiers.isEmpty()) {
                val fallbackTiers = listOf(500 to true, 2000 to false, 10000 to false)
                fallbackTiers.forEach { (target, unlocked) ->
                    TierBox(points = userPoints, target = target, unlocked = unlocked, themeViewModel = themeViewModel)
                }
            } else {
                tiers.forEach { tier ->
                    val unlocked = userRewards.any { it.tier_id == tier.id }
                    TierBox(points = userPoints, target = tier.target_points, unlocked = unlocked, themeViewModel = themeViewModel)
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text("Garden Seeds", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            seedMilestones.forEachIndexed { index, targetPoints ->
                val slotIndex = index + 1
                val slot = gardenSlots.find { it.slot_index == slotIndex }
                val isPlanted = slot?.state == "PLANTED"

                SeedMilestoneBox(
                    points = userPoints,
                    target = targetPoints,
                    isPlanted = isPlanted,
                    themeViewModel = themeViewModel,
                    onClaim = { viewModel.claimPlantReward(userId, slotIndex) }
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun TierBox(points: Int, target: Int, unlocked: Boolean, themeViewModel: ThemeViewModel) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .background(
                if (unlocked) Color(0xFF8BC34A) else MaterialTheme.colorScheme.surfaceVariant,
                RoundedCornerShape(12.dp)
            )
            .padding(16.dp)
    ) {
        Text("$points/$target ${if (unlocked) string("unlocked", themeViewModel) else string("locked", themeViewModel)}")
    }
}

@Composable
private fun SeedMilestoneBox(
    points: Int,
    target: Int,
    isPlanted: Boolean,
    themeViewModel: ThemeViewModel,
    onClaim: () -> Unit
) {
    val unlocked = points >= target

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .background(
                if (isPlanted) Color(0xFF8BC34A) else MaterialTheme.colorScheme.surfaceVariant,
                RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("$points/$target ${if (isPlanted) "Planted" else if (unlocked) "Ready" else "Locked"}")

        if (unlocked && !isPlanted) {
            Button(
                onClick = onClaim,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
            ) {
                Text("Claim")
            }
        }
    }
}
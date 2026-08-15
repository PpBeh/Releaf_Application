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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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

    LaunchedEffect(userId) {
        viewModel.loadRewards(userId)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("$userPoints ${string("points", themeViewModel)}", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))
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

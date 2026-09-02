package com.example.releaf.ui.rewards

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.releaf.R
import com.example.releaf.model.SeedData
import com.example.releaf.model.SeedInfo
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
    val userGems by viewModel.userGems.collectAsState()
    val walletPoints by viewModel.walletPoints.collectAsState()
    val gardenSlots by viewModel.gardenSlots.collectAsState()
    val claimStatus by viewModel.claimStatus.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(userId) {
        viewModel.loadRewards(userId)
    }

    LaunchedEffect(claimStatus) {
        claimStatus?.let { status ->
            snackbarHostState.showSnackbar(status)
            viewModel.clearClaimStatus()
        }
    }

    val nextTargetExp = when {
        userPoints < 2000 -> 2000
        userPoints < 5000 -> 5000
        else -> 10000
    }
    val expProgress = (userPoints.toFloat() / nextTargetExp.toFloat()).coerceIn(0f, 1f)

    Box(modifier = Modifier.fillMaxSize()) {
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
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🌟", style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "$userPoints / $nextTargetExp EXP",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                        Text("•", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("💎", style = MaterialTheme.typography.bodyMedium)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "$userGems",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF00ACC1)
                            )
                            Text(" Gems", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
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

            Text("Garden Seeds", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SeedData.seedList.forEach { seed ->
                    val slot = gardenSlots.find { it.slot_index == seed.slotIndex }
                    val isPlanted = slot?.state != null && slot.state != "EMPTY_POT"

                    SeedMilestoneBox(
                        seedInfo = seed,
                        points = userPoints,
                        isPlanted = isPlanted,
                        onClaim = { viewModel.claimPlantReward(userId, seed.slotIndex) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text("Claimable Titles", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            val equippedTitle by viewModel.equippedTitle.collectAsState()
            val titles = listOf(
                "Gardener" to 0, "Sprout" to 500, "Green Thumb" to 2000, "Expert Gardener" to 5000, "Master Gardener" to 10000
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                titles.forEach { (titleName, req) ->
                    val isUnlocked = userPoints >= req
                    val isEquipped = titleName == equippedTitle
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = if (isUnlocked) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(if (isEquipped) "⭐" else if (isUnlocked) "✓" else "🔒", modifier = Modifier.padding(end = 8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(titleName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    when {
                                        isEquipped -> "Currently equipped"
                                        isUnlocked -> "Unlocked - tap to equip"
                                        else -> "Requires $req EXP"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (isUnlocked) {
                                Button(
                                    onClick = { viewModel.equipTitle(userId, titleName) },
                                    enabled = !isEquipped,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(if (isEquipped) "Equipped ✓" else "Equip")
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text("Avatar Frames", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("💎 $userGems Gems", fontWeight = FontWeight.Bold, color = Color(0xFF00ACC1))
                Spacer(modifier = Modifier.width(8.dp))
                Text("🪙 $walletPoints Points", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                "Frames are bought with 💎 Gems + 🪙 spendable Points (earn 🪙 by watering & quests, 💎 by watering and Pro daily rewards).",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            val framePrefs = androidx.compose.ui.platform.LocalContext.current.getSharedPreferences(
                "frames_$userId",
                android.content.Context.MODE_PRIVATE
            )
            val frames = listOf(
                Triple("None", 0 to 0, "No frame"), Triple("Leaf", 0 to 0, "🍃"), Triple("Blocks", 100 to 1000, "🧱"), Triple("Gold", 200 to 2000, "🏆"), Triple("Diamond", 500 to 5000, "💎")
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                frames.forEach { (frameName, price, icon) ->
                    val (gemPrice, pointPrice) = price
                    val alreadyOwned = framePrefs.getBoolean("owned_$frameName", false)
                    val canAfford = !alreadyOwned && userGems >= gemPrice && walletPoints >= pointPrice
                    Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(icon, fontSize = 24.sp, modifier = Modifier.padding(end = 12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(frameName, fontWeight = FontWeight.Bold)
                                Text(
                                    if (alreadyOwned) "Owned - pick it in Profile → frame picker"
                                    else "💎 $gemPrice Gems • 🪙 $pointPrice Points",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                            Button(
                                onClick = { viewModel.purchaseFrame(userId, frameName, gemPrice, pointPrice) },
                                enabled = canAfford,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    when {
                                        alreadyOwned -> "Owned ✓"
                                        canAfford -> "Buy"
                                        else -> "Locked"
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        )
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
            .padding(16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = "$points/$target ${if (unlocked) string("unlocked", themeViewModel) else string("locked", themeViewModel)}",
            fontWeight = FontWeight.SemiBold,
            color = if (unlocked) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SeedMilestoneBox(
    seedInfo: SeedInfo,
    points: Int,
    isPlanted: Boolean,
    onClaim: () -> Unit
) {
    val unlocked = points >= seedInfo.targetPoints

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPlanted) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            } else if (unlocked) {
                MaterialTheme.colorScheme.surfaceVariant
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (unlocked && !isPlanted) 3.dp else 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = if (unlocked || isPlanted) seedInfo.drawableRes else R.drawable.ic_pot_empty),
                contentDescription = seedInfo.name,
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(6.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = seedInfo.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = seedInfo.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isPlanted) "🌱 Planted in Garden Plot" else if (unlocked) "Unlocked (${points}/${seedInfo.targetPoints} EXP)" else "Requires ${seedInfo.targetPoints} EXP (${points}/${seedInfo.targetPoints})",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isPlanted) Color(0xFF2E7D32) else if (unlocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            if (isPlanted) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF4CAF50).copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "Planted",
                        color = Color(0xFF2E7D32),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            } else if (unlocked) {
                Button(
                    onClick = onClaim,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Claim", fontWeight = FontWeight.Bold)
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                ) {
                    Text(
                        text = "Locked 🔒",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

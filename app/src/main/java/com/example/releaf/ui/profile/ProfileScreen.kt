package com.example.releaf.ui.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.releaf.R
import com.example.releaf.model.SeedData
import com.example.releaf.ui.viewmodel.ProfileViewModel
import com.example.releaf.ui.viewmodel.ThemeViewModel

data class AchievementItemData(
    val id: String,
    val label: String,
    val description: String,
    val isUnlocked: Boolean,
    val iconRes: Int
)

@Composable
fun ProfileScreen(
    userId: String,
    currentUserId: String,
    viewModel: ProfileViewModel,
    themeViewModel: ThemeViewModel,
    onFavouritesClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onViewGardenClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val profile by viewModel.profile.collectAsState()
    val userAchievements by viewModel.achievements.collectAsState()
    val userGarden by viewModel.userGarden.collectAsState()
    val userPlantSlots by viewModel.userPlantSlots.collectAsState()

    val avatarPicker = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.uploadAvatar(userId, uri, context)
        }
    }

    LaunchedEffect(userId) {
        if (userId.isNotBlank()) {
            viewModel.loadProfile(userId)
        }
    }

    val displayName = profile?.name?.takeIf { it.isNotBlank() } ?: "User"
    val title = profile?.title?.takeIf { it.isNotBlank() } ?: "Gardener"
    val phone = profile?.phone?.takeIf { it.isNotBlank() } ?: "N/A"
    val email = profile?.email?.takeIf { it.isNotBlank() } ?: "N/A"
    val isOwnProfile = userId == currentUserId

    // Calculate garden tree stage and stats
    val gardenExp = userGarden?.current_exp ?: profile?.total_points ?: 0
    val treeStage = when {
        gardenExp >= 5000 -> 3
        gardenExp >= 2000 -> 2
        else -> 1
    }
    val targetExp = when (treeStage) {
        1 -> 2000
        2 -> 5000
        else -> 10000
    }
    val expProgress = (gardenExp.toFloat() / targetExp.toFloat()).coerceIn(0f, 1f)
    val stageTitle = when (treeStage) {
        1 -> "Stage 1: Seedling Tree"
        2 -> "Stage 2: Growing Tree"
        else -> "Stage 3: Full Bloom Tree"
    }
    val treeStageIcon = when (treeStage) {
        1 -> R.drawable.ic_tree_stage_1
        2 -> R.drawable.ic_tree_stage_2
        else -> R.drawable.ic_tree_stage_3
    }

    // Build achievements list in a row
    val defaultMasterAchievements = listOf(
        AchievementItemData("1", "Expert Reviewer", "Write 10 or more toilet reviews", false, R.drawable.ic_toilet),
        AchievementItemData("2", "Expert Gardener", "Grow 50 plants to full bloom", false, R.drawable.ic_plant_1),
        AchievementItemData("3", "Toilet Scout", "Verify 5 new toilet locations", false, R.drawable.ic_star),
        AchievementItemData("4", "Clean Crusader", "Rate cleanliness on 10 toilets", false, R.drawable.ic_favorite),
        AchievementItemData("5", "Early Adopter", "Joined Releaf community during launch", false, R.drawable.ic_person),
        AchievementItemData("6", "Photo Fanatic", "Upload 20 facility photos", false, R.drawable.ic_palette),
        AchievementItemData("7", "Social Butterfly", "Interact with 50 community reviews", false, R.drawable.ic_favorite),
        AchievementItemData("8", "Master Navigator", "Navigate to 20 different facilities", false, R.drawable.ic_refresh)
    )

    val achievementItems = defaultMasterAchievements.map { defaultItem ->
        val userEarned = userAchievements.any { userAch ->
            userAch.achievement_id == defaultItem.id ||
                    userAch.achievement?.label?.equals(defaultItem.label, ignoreCase = true) == true
        }
        defaultItem.copy(isUnlocked = userEarned)
    }
    val unlockedCount = achievementItems.count { it.isUnlocked }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
    ) {
        // TOP PROFILE HEADER BANNER
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primary)
                .padding(bottom = 20.dp)
        ) {
            if (isOwnProfile) {
                IconButton(
                    onClick = { onSettingsClick() },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_palette),
                        contentDescription = "Settings",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f))
                            .clickable(enabled = isOwnProfile) { avatarPicker.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        val avatarUrl = profile?.avatar_url.orEmpty()
                        if (avatarUrl.isNotBlank()) {
                            Image(
                                painter = rememberAsyncImagePainter(model = avatarUrl),
                                contentDescription = "Avatar",
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_person),
                                contentDescription = "Add photo",
                                tint = Color.White,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            displayName,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge
                        )
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White.copy(alpha = 0.2f),
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Text(
                                title,
                                color = Color.White,
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_star),
                                contentDescription = null,
                                tint = Color(0xFFFFC107),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "$gardenExp EXP / Points",
                                color = Color.White,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (isOwnProfile) {
                            if (phone != "N/A") {
                                Text(phone, color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.bodySmall)
                            }
                            if (email != "N/A") {
                                Text(email, color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // SECTION 1: GARDEN & PLANTS INFO
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (isOwnProfile) "My Garden & Plants" else "$displayName's Garden",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                if (onViewGardenClick != null) {
                    OutlinedButton(
                        onClick = { onViewGardenClick() },
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text("View Garden", fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Garden Overview Card
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(id = treeStageIcon),
                        contentDescription = "Tree Stage",
                        modifier = Modifier.size(54.dp)
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            stageTitle,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall
                        )
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
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "$gardenExp / $targetExp EXP",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "💎 ${userGarden?.current_gems ?: 0} Gems",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Plants Info Row
            Text(
                "Garden Plants Collection",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 20.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items((1..6).toList()) { slotIndex ->
                    val slot = userPlantSlots.find { it.slot_index == slotIndex }
                    val seedInfo = SeedData.getSeedForSlot(slotIndex)
                    val state = slot?.state ?: "EMPTY_POT"
                    val isPlanted = state == "PLANTED" || state == "GROWING" || state == "FULLY_GROWN"

                    val plantImageRes = if (isPlanted) {
                        when (slotIndex) {
                            1 -> R.drawable.ic_plant_1
                            2 -> R.drawable.ic_plant_2
                            3 -> R.drawable.ic_plant_3
                            4 -> R.drawable.ic_plant_4
                            5 -> R.drawable.ic_plant_5
                            else -> R.drawable.ic_plant_6
                        }
                    } else {
                        R.drawable.ic_pot_empty
                    }

                    ElevatedCard(
                        modifier = Modifier
                            .width(140.dp)
                            .clip(RoundedCornerShape(16.dp)),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isPlanted) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = painterResource(id = plantImageRes),
                                    contentDescription = seedInfo.name,
                                    modifier = Modifier.size(44.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                "Slot $slotIndex: ${seedInfo.name}",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(2.dp))

                            Text(
                                seedInfo.description,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.height(28.dp)
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isPlanted) Color(0xFFC8E6C9) else MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = if (isPlanted) "Planted" else "Empty Pot",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isPlanted) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // SECTION 2: ACHIEVEMENTS IN A ROW FORM
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Achievements ($unlockedCount/${achievementItems.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Row Form of Achievements
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(achievementItems) { achievement ->
                    ElevatedCard(
                        modifier = Modifier
                            .width(170.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .then(
                                if (achievement.isUnlocked) {
                                    Modifier.border(1.5.dp, Color(0xFF81C784), RoundedCornerShape(16.dp))
                                } else Modifier
                            ),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (achievement.isUnlocked) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                            }
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (achievement.isUnlocked) Color(0xFFFFD54F) else Color.Gray.copy(alpha = 0.2f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = achievement.iconRes),
                                    contentDescription = achievement.label,
                                    tint = if (achievement.isUnlocked) Color(0xFF5D4037) else Color.Gray,
                                    modifier = Modifier.size(26.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                achievement.label,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                achievement.description,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.height(28.dp)
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (achievement.isUnlocked) Color(0xFF2E7D32) else Color.Gray.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = if (achievement.isUnlocked) "Unlocked ✓" else "Locked 🔒",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (achievement.isUnlocked) Color.White else Color.DarkGray,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // SECTION 3: QUICK ACTIONS FOR OWN PROFILE
        if (isOwnProfile) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                Text(
                    "Quick Options",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                ElevatedCard(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onFavouritesClick() }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_favorite),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text("Favourite Locations", modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
                            Icon(
                                painter = painterResource(id = R.drawable.ic_chevron_right),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outlineVariant
                            )
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSettingsClick() }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_palette),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text("Settings & App Preferences", modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
                            Icon(
                                painter = painterResource(id = R.drawable.ic_chevron_right),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outlineVariant
                            )
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onLogoutClick() }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_logout),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text("Logout", modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Medium)
                            Icon(
                                painter = painterResource(id = R.drawable.ic_chevron_right),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outlineVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

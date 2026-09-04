package com.example.releaf.ui.profile

import android.content.Context
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.releaf.R
import com.example.releaf.model.SeedData
import com.example.releaf.model.SeedInfo
import com.example.releaf.ui.theme.AppStrings
import com.example.releaf.ui.viewmodel.ProfileViewModel
import com.example.releaf.ui.viewmodel.ThemeViewModel

data class AchievementItemData(
    val id: String,
    val label: String,
    val description: String,
    val isUnlocked: Boolean,
    val iconRes: Int,
    val progress: Int = 0,
    val target: Int = 1
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
    onBackClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val lang by themeViewModel.language.collectAsState()
    fun t(key: String) = AppStrings.get(key, lang)
    val profile by viewModel.profile.collectAsState()
    val userAchievements by viewModel.achievements.collectAsState()
    val userGarden by viewModel.userGarden.collectAsState()
    val userPlantSlots by viewModel.userPlantSlots.collectAsState()
    val userReviewCount by viewModel.userReviewCount.collectAsState()
    val userVerifiedCount by viewModel.userVerifiedCount.collectAsState()
    val userPhotoCount by viewModel.userPhotoCount.collectAsState()

    var selectedSeedForDialog by remember { mutableStateOf<SeedInfo?>(null) }


    val avatarPicker = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.uploadAvatar(userId, uri, context)
        }
    }
    val bannerPicker = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.uploadBanner(userId, uri, context)
        }
    }
    // Pro/VIP is an account property stored on the server (profiles.is_pro),
    // so a new account on the same device is not automatically VIP.
    val isVip = profile?.is_pro == true

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
    var showLogoutConfirm by androidx.compose.runtime.remember {
        mutableStateOf(
            false
        )
    }

    // Garden EXP (used for the title unlocks)
    val gardenExp = userGarden?.current_exp ?: profile?.total_points ?: 0

    // Build achievements list with progress
    val defaultMasterAchievements = listOf(
        AchievementItemData(
            "1",
            "Expert Reviewer",
            "Write 10 reviews",
            false,
            R.drawable.ic_star,
            0,
            10
        ),
        AchievementItemData(
            "2",
            "Expert Gardener",
            "Grow 50 plants",
            false,
            R.drawable.ic_plant_1,
            0,
            50
        ),
        AchievementItemData(
            "3",
            "Toilet Scout",
            "Verify 5 toilets",
            false,
            R.drawable.ic_toilet,
            0,
            5
        ),
        AchievementItemData(
            "4",
            "Clean Crusader",
            "Rate 10 toilets",
            false,
            R.drawable.ic_favorite,
            0,
            10
        ),
        AchievementItemData(
            "5",
            "Early Adopter",
            "Joined during launch",
            false,
            R.drawable.ic_person,
            0,
            1
        ),
        AchievementItemData(
            "6",
            "Photo Fanatic",
            "Upload 20 photos",
            false,
            R.drawable.ic_palette,
            0,
            20
        ),
        AchievementItemData(
            "7",
            "Social Butterfly",
            "Interact 50 times",
            false,
            R.drawable.ic_favorite,
            0,
            50
        ),
        AchievementItemData(
            "8",
            "Master Navigator",
            "Navigate 20 facilities",
            false,
            R.drawable.ic_refresh,
            0,
            20
        )
    )

    // Calculate progress from actual user data
    val reviewCount = minOf(userReviewCount, 10)
    val plantCount = userPlantSlots.count { com.example.releaf.model.isPlantedState(it.state) }
    val verifiedCount = minOf(userVerifiedCount, 5)

    val achievementItems = defaultMasterAchievements.map { defaultItem ->
        val earnedByRow = userAchievements.any { userAch ->
            userAch.achievement_id == defaultItem.id ||
                    userAch.achievement?.label?.equals(defaultItem.label, ignoreCase = true) == true
        }
        // Unlock measurable achievements directly from the user's real activity,
        // not only from (rarely-populated) user_achievements server rows.
        val activityCount = when (defaultItem.id) {
            "1", "4" -> reviewCount        // Expert Reviewer / Clean Crusader
            "2" -> plantCount              // Expert Gardener (current plants)
            "3" -> verifiedCount           // Toilet Scout
            "6" -> userPhotoCount          // Photo Fanatic
            else -> 0
        }
        val isUnlocked = earnedByRow || (activityCount > 0 && activityCount >= defaultItem.target)
        val progress = minOf(activityCount, defaultItem.target)
        defaultItem.copy(isUnlocked = isUnlocked, progress = progress)
    }
    val unlockedCount = achievementItems.count { it.isUnlocked }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
    ) {
        // TOP PROFILE HEADER BANNER - VIP can edit background
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
        ) {
            val bannerUrl = profile?.banner_url.orEmpty()
            if (bannerUrl.isNotBlank()) {
                Image(
                    painter = rememberAsyncImagePainter(model = bannerUrl),
                    contentDescription = "Banner",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.35f))
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!isOwnProfile && onBackClick != null) {
                    IconButton(onClick = { onBackClick() }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_arrow_back),
                            contentDescription = "Back",
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }
            if (isOwnProfile) {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isVip) {
                        var showFramePicker by remember { mutableStateOf(false) }
                        val currentFrame = profile?.avatar_frame.orEmpty()
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color.White.copy(alpha = 0.9f),
                            modifier = Modifier.clickable { bannerPicker.launch("image/*") }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("✏️", fontSize = 12.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "Edit Banner",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2E7D32)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color.White.copy(alpha = 0.9f),
                            modifier = Modifier.clickable { showFramePicker = true }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("🖼️", fontSize = 12.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    if (currentFrame.isBlank()) "Frame: None" else "Frame: $currentFrame",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2E7D32)
                                )
                            }
                        }
                        if (showFramePicker) {
                            val frames = listOf(
                                "None" to "No frame",
                                "Leaf" to "🍃 Leaf",
                                "Blocks" to "🧱 Blocks",
                                "Gold" to "🏆 Gold",
                                "Diamond" to "💎 Diamond"
                            )
                            val prefs = remember {
                                context.getSharedPreferences(
                                    "frames_${userId}",
                                    Context.MODE_PRIVATE
                                )
                            }
                            AlertDialog(
                                onDismissRequest = { showFramePicker = false },
                                title = { Text("Choose Frame") },
                                text = {
                                    Column {
                                        frames.forEach { (fid, label) ->
                                            // A frame is usable if it is free, purchased on this device,
                                            // or currently equipped on the server (survives reinstalls).
                                            val owned = fid == "None" || fid == "Leaf" ||
                                                    currentFrame == fid || prefs.getBoolean(
                                                "owned_$fid",
                                                false
                                            )
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable(enabled = owned) {
                                                        viewModel.updateAvatarFrame(
                                                            userId,
                                                            if (fid == "None") "" else fid
                                                        )
                                                        showFramePicker = false
                                                    }
                                                    .padding(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(label, modifier = Modifier.weight(1f))
                                                if (!owned) Text(
                                                    "🔒",
                                                    fontSize = 12.sp
                                                ) else if (currentFrame == fid || (fid == "None" && currentFrame.isBlank())) Text(
                                                    "✓",
                                                    color = Color(0xFF2E7D32)
                                                )
                                            }
                                        }
                                        Text(
                                            "Buy frames in Rewards → Avatar Frames",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.Gray,
                                            modifier = Modifier.padding(top = 8.dp)
                                        )
                                    }
                                },
                                confirmButton = {
                                    TextButton(onClick = {
                                        showFramePicker = false
                                    }) { Text("Close") }
                                }
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    } else if (isOwnProfile) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color.White.copy(alpha = 0.2f)
                        ) {
                            Text(
                                "VIP only",
                                fontSize = 10.sp,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    IconButton(
                        onClick = { onSettingsClick() }
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_settings),
                            contentDescription = "Settings",
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val selectedFrame = profile?.avatar_frame.orEmpty()
                    val frameColor = when (selectedFrame) {
                        "Leaf" -> Color(0xFF4CAF50)
                        "Blocks" -> Color(0xFF795548)
                        "Gold" -> Color(0xFFFFD700)
                        "Diamond" -> Color(0xFF00BCD4)
                        else -> Color.White.copy(alpha = 0.6f)
                    }
                    val frameWidth =
                        if (selectedFrame.isBlank() || selectedFrame == "None") 2.dp else 5.dp
                    Box(
                        modifier = Modifier
                            .size(78.dp)
                            .clip(CircleShape)
                            .background(Color.Transparent)
                            .border(frameWidth, frameColor, CircleShape)
                            .clickable(enabled = isOwnProfile) { avatarPicker.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(68.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f)),
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
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                displayName,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleLarge
                            )
                            if (isVip && isOwnProfile) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFFFD700)
                                ) {
                                    Text(
                                        "VIP",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black,
                                        modifier = Modifier.padding(
                                            horizontal = 6.dp,
                                            vertical = 2.dp
                                        )
                                    )
                                }
                            }
                        }
                        var showTitlePicker by remember { mutableStateOf(false) }
                        val titles = listOf(
                            "Gardener" to 0,
                            "Sprout" to 500,
                            "Green Thumb" to 2000,
                            "Expert Gardener" to 5000,
                            "Master Gardener" to 10000
                        )
                        val titleMetric = gardenExp
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White.copy(alpha = 0.2f),
                            modifier = Modifier
                                .padding(vertical = 4.dp)
                                .clickable(enabled = isOwnProfile) {
                                    if (isOwnProfile) showTitlePicker = true
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    title,
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelMedium
                                )
                                if (isOwnProfile) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("▼", fontSize = 10.sp, color = Color.White)
                                }
                            }
                        }
                        if (showTitlePicker) {
                            AlertDialog(
                                onDismissRequest = { showTitlePicker = false },
                                title = { Text("Choose Title") },
                                text = {
                                    Column {
                                        titles.forEach { (tName, req) ->
                                            val unlocked = titleMetric >= req
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable(enabled = unlocked) {
                                                        if (unlocked) {
                                                            viewModel.updateTitle(userId, tName)
                                                            showTitlePicker = false
                                                        }
                                                    }
                                                    .padding(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    tName,
                                                    modifier = Modifier.weight(1f),
                                                    color = if (unlocked) Color.Black else Color.Gray
                                                )
                                                if (!unlocked) Text(
                                                    "🔒 $req EXP",
                                                    fontSize = 11.sp,
                                                    color = Color.Gray
                                                ) else if (title == tName) Text(
                                                    "✓",
                                                    color = Color(0xFF2E7D32)
                                                )
                                            }
                                        }
                                    }
                                },
                                confirmButton = {
                                    TextButton(onClick = {
                                        showTitlePicker = false
                                    }) { Text("Close") }
                                }
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
                                "$gardenExp EXP",
                                color = Color.White,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("🪙", fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                "${userGarden?.current_points ?: 0} Points",
                                color = Color.White.copy(alpha = 0.9f),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        if (isOwnProfile) {
                            if (phone != "N/A") {
                                Text(
                                    phone,
                                    color = Color.White.copy(alpha = 0.8f),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            if (email != "N/A") {
                                Text(
                                    email,
                                    color = Color.White.copy(alpha = 0.8f),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // GARDEN PLANTS COLLECTION
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                t("garden_plants_collection"),
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
                    val isPlanted = com.example.releaf.model.isPlantedState(state)
                    val isUnlocked = isPlanted || gardenExp >= seedInfo.targetPoints

                    val plantImageRes = if (isUnlocked) {
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
                            .width(148.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { selectedSeedForDialog = seedInfo },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = when {
                                isPlanted -> MaterialTheme.colorScheme.surface
                                isUnlocked -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
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
                                    .size(60.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        when {
                                            isPlanted -> Color(0xFFE8F5E9)
                                            isUnlocked -> Color(0xFFFFF8E1)
                                            else -> MaterialTheme.colorScheme.surfaceVariant
                                        }
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
                                if (seedInfo.nickname.isNotBlank()) "${seedInfo.nickname} (${seedInfo.name})" else seedInfo.name,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(2.dp))

                            Text(
                                if (isUnlocked && seedInfo.personality.isNotBlank()) seedInfo.personality else seedInfo.description,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.height(28.dp)
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = when {
                                    isPlanted -> Color(0xFFC8E6C9)
                                    isUnlocked -> Color(0xFFFFE082)
                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                }
                            ) {
                                Text(
                                    text = when {
                                        isPlanted -> "Planted • Active"
                                        isUnlocked -> "Unlocked ✓"
                                        else -> "Locked • ${seedInfo.targetPoints} EXP"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = when {
                                        isPlanted -> Color(0xFF2E7D32)
                                        isUnlocked -> Color(0xFF5D4037)
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                    },
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
                    String.format(
                        java.util.Locale.US,
                        t("achievements_fmt"),
                        unlockedCount,
                        achievementItems.size
                    ),
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
                                    Modifier.border(
                                        1.5.dp,
                                        Color(0xFF81C784),
                                        RoundedCornerShape(16.dp)
                                    )
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
                                        if (achievement.isUnlocked) Color(0xFFFFD54F) else Color.Gray.copy(
                                            alpha = 0.2f
                                        )
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

                            if (!achievement.isUnlocked) {
                                Spacer(modifier = Modifier.height(6.dp))
                                LinearProgressIndicator(
                                    progress = { achievement.progress.toFloat() / achievement.target.toFloat() },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = Color(0xFF4CAF50),
                                    trackColor = Color.Gray.copy(alpha = 0.3f)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "${achievement.progress}/${achievement.target}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (achievement.isUnlocked) Color(0xFF2E7D32) else Color.Gray.copy(
                                    alpha = 0.2f
                                )
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
                    t("quick_options"),
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
                            Text(
                                t("favourite_locations"),
                                modifier = Modifier.weight(1f),
                                fontWeight = FontWeight.Medium
                            )
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
                            Text(
                                t("settings_prefs"),
                                modifier = Modifier.weight(1f),
                                fontWeight = FontWeight.Medium
                            )
                            Icon(
                                painter = painterResource(id = R.drawable.ic_chevron_right),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outlineVariant
                            )
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showLogoutConfirm = true }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_logout),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                t("logout"),
                                modifier = Modifier.weight(1f),
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Medium
                            )
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

    if (showLogoutConfirm) {
        com.example.releaf.ui.components.LogoutConfirmationDialog(
            onConfirm = {
                showLogoutConfirm = false
                onLogoutClick()
            },
            onDismiss = { showLogoutConfirm = false }
        )
    }

    if (selectedSeedForDialog != null) {
        val seedInfo = selectedSeedForDialog!!
        val slotIndex = seedInfo.slotIndex
        val slot = userPlantSlots.find { it.slot_index == slotIndex }
        val state = slot?.state ?: "EMPTY_POT"
        val isPlanted = com.example.releaf.model.isPlantedState(state)
        val isUnlocked = isPlanted || gardenExp >= seedInfo.targetPoints

        androidx.compose.ui.window.Dialog(onDismissRequest = { selectedSeedForDialog = null }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = when {
                                isPlanted -> Color(0xFFE8F5E9)
                                isUnlocked -> Color(0xFFFFF8E1)
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            }
                        ) {
                            Text(
                                when {
                                    isPlanted -> "Slot $slotIndex: Planted in Garden"
                                    isUnlocked -> "Slot $slotIndex: Unlocked Seed"
                                    else -> "Slot $slotIndex: Locked Seed"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    isPlanted -> Color(0xFF2E7D32)
                                    isUnlocked -> Color(0xFF5D4037)
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }

                        IconButton(onClick = { selectedSeedForDialog = null }) {
                            Icon(painter = painterResource(id = R.drawable.ic_close), contentDescription = "Close")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Box(
                        modifier = Modifier
                            .size(110.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    isPlanted -> Color(0xFFE8F5E9)
                                    isUnlocked -> Color(0xFFFFF8E1)
                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = if (isUnlocked) seedInfo.drawableRes else R.drawable.ic_pot_empty),
                            contentDescription = seedInfo.name,
                            modifier = Modifier.size(80.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        if (seedInfo.nickname.isNotBlank()) "${seedInfo.nickname} the ${seedInfo.name}" else seedInfo.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    if (seedInfo.personality.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                seedInfo.personality,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    if (seedInfo.quote.isNotBlank()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFFFF8E1),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                seedInfo.quote,
                                style = MaterialTheme.typography.bodySmall,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF795548),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    if (seedInfo.mood.isNotBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                "Current Vibe: ",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                seedInfo.mood,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                RoundedCornerShape(12.dp)
                            )
                            .padding(12.dp)
                    ) {
                        Text(
                            "Description & Lore",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            seedInfo.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        if (seedInfo.careTip.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Care Tip",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2E7D32)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                seedInfo.careTip,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (isOwnProfile && isUnlocked && !isPlanted) {
                        androidx.compose.material3.Button(
                            onClick = {
                                viewModel.plantSeed(slotIndex, userId)
                                selectedSeedForDialog = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4CAF50)
                            )
                        ) {
                            Text(
                                "🌱 Plant in Garden Plot",
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

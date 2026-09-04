package com.example.releaf.ui.map

import android.content.Context
import android.graphics.Bitmap
import android.location.LocationManager
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import coil.compose.rememberAsyncImagePainter
import com.example.releaf.R
import com.example.releaf.data.remote.TimeFormatter
import com.example.releaf.model.SeedData
import com.example.releaf.model.isPlantedState
import com.example.releaf.data.remote.dto.GardenDto
import com.example.releaf.data.remote.dto.PlantSlotDto
import com.example.releaf.data.remote.dto.ProfileDto
import com.example.releaf.data.remote.dto.ReviewDto
import com.example.releaf.data.repository.AuthRepository
import com.example.releaf.data.repository.GardenRepository
import com.example.releaf.data.repository.RewardRepository
import com.example.releaf.ui.theme.AppStrings
import com.example.releaf.ui.viewmodel.CommentViewModel
import com.example.releaf.ui.viewmodel.ThemeViewModel
import java.io.File
import kotlin.time.Duration.Companion.milliseconds

fun countWords(text: String): Int {
    if (text.isBlank()) return 0
    return text.trim().split("\\s+".toRegex()).size
}


fun createCameraImageUri(context: Context): Uri {

    val imagesDir = File(context.cacheDir, "camera_photos").apply {
        if (!exists()) mkdirs()
    }
    val file = File(imagesDir, "comment_camera_${System.currentTimeMillis()}.jpg")

    val authority = "com.example.releaf.fileprovider"
    return FileProvider.getUriForFile(context, authority, file)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentScreen(
    poiId: String,
    viewModel: CommentViewModel,
    currentUserId: String,
    onBackClick: () -> Unit,
    onAvatarClick: (String) -> Unit,
    themeViewModel: ThemeViewModel
) {
    val lang by themeViewModel.language.collectAsState()
    fun t(key: String) = AppStrings.get(key, lang)
    val reviews by viewModel.reviews.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val userVotes by viewModel.userVotes.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    val isVoting by viewModel.isVoting.collectAsState()

    var commentText by remember { mutableStateOf("") }
    var starRating by remember { mutableIntStateOf(5) }
    var selectedPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var showPhotoSourcePicker by remember { mutableStateOf(false) }

    val context = LocalContext.current

    LaunchedEffect(viewModel) {
        viewModel.registerAddReviewCallback { success ->
            if (success) {
                commentText = ""
                selectedPhotoUri = null
                starRating = 5
            }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            selectedPhotoUri = uri
        }
    }

    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }
    var cameraError by remember { mutableStateOf<String?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempCameraUri != null) {
            selectedPhotoUri = tempCameraUri
        }
    }

    var sheetUserId by remember { mutableStateOf<String?>(null) }
    var sheetProfile by remember {
        mutableStateOf<ProfileDto?>(
            null
        )
    }
    var sheetAchievements by remember { mutableIntStateOf(0) }
    var sheetPoints by remember { mutableIntStateOf(0) }
    var sheetGarden by remember { mutableStateOf<GardenDto?>(null) }
    var sheetPlantSlots by remember {
        mutableStateOf<List<PlantSlotDto>>(
            emptyList()
        )
    }

    LaunchedEffect(sheetUserId) {
        val uid = sheetUserId ?: return@LaunchedEffect
        sheetProfile = null
        sheetGarden = null
        sheetPlantSlots = emptyList()
        try {
            val authRepository = AuthRepository()
            sheetProfile = authRepository.getProfile(uid)
        } catch (_: Exception) {
        }
        try {
            val rewardRepository = RewardRepository()
            sheetAchievements = rewardRepository.getUserAchievements(uid).size
            sheetPoints = sheetProfile?.total_points ?: 0
        } catch (_: Exception) {
        }
        try {
            val gardenRepository = GardenRepository()
            sheetGarden = gardenRepository.getGarden(uid)
            sheetPlantSlots = gardenRepository.getPlantSlots(uid)
        } catch (_: Exception) {
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(poiId, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.loadReviews(poiId, currentUserId)
            while (true) {
                kotlinx.coroutines.delay(3000.milliseconds)
                viewModel.loadReviews(poiId, currentUserId)
            }
        }
    }

    val wordCount = countWords(commentText)
    val isWordLimitExceeded = wordCount > 500

    if (showPhotoSourcePicker) {
        Dialog(onDismissRequest = { showPhotoSourcePicker = false }) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        t("attach_photo_title"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showPhotoSourcePicker = false
                                galleryLauncher.launch("image/*")
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_image),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(t("choose_gallery"), fontWeight = FontWeight.Bold)
                            Text(
                                t("gallery_hint"),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showPhotoSourcePicker = false
                                try {
                                    val newUri = createCameraImageUri(context)
                                    tempCameraUri = newUri
                                    cameraError = null
                                    cameraLauncher.launch(newUri)
                                } catch (e: Exception) {
                                    Log.e("CameraDebug", "Failed to launch camera", e)
                                    cameraError = t("camera_unavailable")
                                }
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_camera),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(t("take_camera"), fontWeight = FontWeight.Bold)
                            Text(
                                t("camera_hint"),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    TextButton(onClick = { showPhotoSourcePicker = false }) {
                        Text(t("cancel"))
                    }
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(t("comments_title")) },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(painter = painterResource(id = R.drawable.ic_arrow_back), contentDescription = "Back")
                }
            }
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(5) { index ->
                IconButton(
                    onClick = { starRating = index + 1 },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_star),
                        contentDescription = null,
                        tint = if (index < starRating) Color(0xFFFFC107) else MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        if (selectedPhotoUri != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(60.dp)
                ) {
                    Image(
                        painter = rememberAsyncImagePainter(model = selectedPhotoUri),
                        contentDescription = "Attached Photo",
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )

                    IconButton(
                        onClick = { selectedPhotoUri = null },
                        modifier = Modifier
                            .size(20.dp)
                            .align(Alignment.TopEnd)
                            .background(Color.Red, CircleShape)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_close),
                            contentDescription = "Remove",
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    t("photo_attached"),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF2E7D32),
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = commentText,
                onValueChange = {
                    commentText = it
                    if (errorMessage != null) viewModel.clearError()
                },
                modifier = Modifier.weight(1f),
                placeholder = { Text(t("add_comment_hint")) },
                singleLine = false,
                maxLines = 3,
                isError = isWordLimitExceeded
            )

            Spacer(modifier = Modifier.width(4.dp))

            IconButton(
                onClick = { showPhotoSourcePicker = true }
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_add_a_photo),
                    contentDescription = "Attach Photo",
                    tint = if (selectedPhotoUri != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(
                onClick = {
                    if (commentText.isNotBlank() && !isWordLimitExceeded) {
                        val lm =
                            context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
                        val loc = try {
                            lm?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                                ?: lm?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                        } catch (_: SecurityException) {
                            null
                        }
                        val lat = loc?.latitude ?: 0.0
                        val lng = loc?.longitude ?: 0.0
                        viewModel.addReview(
                            poiId,
                            currentUserId,
                            starRating,
                            commentText,
                            lat,
                            lng,
                            selectedPhotoUri,
                            context
                        )
                    }
                },
                enabled = commentText.isNotBlank() && !isWordLimitExceeded && !isProcessing
            ) {
                Icon(painter = painterResource(id = R.drawable.ic_send), contentDescription = "Send")
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isWordLimitExceeded) {
                Text(
                    t("word_limit_exceeded"),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }

            Text(
                String.format(java.util.Locale.US, "%d / 500 words", wordCount),
                style = MaterialTheme.typography.labelSmall,
                color = if (isWordLimitExceeded) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (isWordLimitExceeded) FontWeight.Bold else FontWeight.Normal
            )
        }

        if (errorMessage != null) {
            Text(
                errorMessage ?: "",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }

        if (cameraError != null) {
            Text(
                cameraError ?: "",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }

        if (isProcessing) {
            androidx.compose.material3.LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (isLoading && reviews.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 8.dp,
                    bottom = 80.dp
                )
            ) {
                items(reviews, key = { it.id }) { review ->
                    var showEditDialog by remember(review.id) { mutableStateOf(false) }
                    ReviewRowItem(
                        review = review,
                        isOwnComment = review.user_id == currentUserId,
                        userVote = userVotes[review.id],
                        isVoting = isVoting,
                        themeViewModel = themeViewModel,
                        onAvatarClick = { sheetUserId = review.user_id },
                        onLike = { viewModel.toggleLike(review) },
                        onDislike = { viewModel.toggleDislike(review) },
                        onEdit = { showEditDialog = true },
                        onDelete = { viewModel.deleteReview(review.id) },
                        onReport = { viewModel.reportReview(review.id) }
                    )
                    if (showEditDialog) {
                        var editText by remember { mutableStateOf(review.text) }
                        val editWordCount = countWords(editText)
                        val isEditExceeded = editWordCount > 500

                        AlertDialog(
                            onDismissRequest = { if (!isProcessing) showEditDialog = false },
                            title = { Text(t("edit_comment")) },
                            text = {
                                Column {
                                    OutlinedTextField(
                                        value = editText,
                                        onValueChange = { editText = it },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = false,
                                        enabled = !isProcessing,
                                        isError = isEditExceeded
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        String.format(
                                            java.util.Locale.US,
                                            "%d / 500 words",
                                            editWordCount
                                        ),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isEditExceeded) MaterialTheme.colorScheme.error else Color.Gray
                                    )
                                }
                            },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        if (!isEditExceeded) {
                                            viewModel.updateReview(review.id, editText)
                                            showEditDialog = false
                                        }
                                    },
                                    enabled = !isProcessing && !isEditExceeded
                                ) {
                                    if (isProcessing) {
                                        CircularProgressIndicator(
                                            strokeWidth = 2.dp,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    } else {
                                        Text(t("save"))
                                    }
                                }
                            },
                            dismissButton = {
                                TextButton(
                                    onClick = { showEditDialog = false },
                                    enabled = !isProcessing
                                ) { Text(t("cancel")) }
                            }
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
                if (reviews.isEmpty() && !isLoading) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(t("no_comments"), style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }
        }
    }

    if (sheetUserId != null) {
        ModalBottomSheet(
            onDismissRequest = {
                sheetUserId = null
                sheetProfile = null
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    val sheetAvatar = sheetProfile?.avatar_url.orEmpty()
                    if (sheetAvatar.isNotBlank()) {
                        Image(
                            painter = rememberAsyncImagePainter(model = sheetAvatar),
                            contentDescription = "Avatar",
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text(
                            (sheetProfile?.name ?: "U").take(1).uppercase(),
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    sheetProfile?.name?.ifBlank { "User" } ?: "User",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    sheetProfile?.title?.ifBlank { "Gardener" } ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "$sheetPoints",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text("Points", style = MaterialTheme.typography.labelSmall)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "$sheetAchievements",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text("Achievements", style = MaterialTheme.typography.labelSmall)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                val targetExp = sheetGarden?.current_exp ?: sheetPoints
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        "Garden Plants Collection",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items((1..6).toList()) { slotIdx ->
                            val slot = sheetPlantSlots.find { it.slot_index == slotIdx }
                            val seedInfo = SeedData.getSeedForSlot(slotIdx)
                            val isPlanted = isPlantedState(slot?.state)
                            val isUnlocked = isPlanted || targetExp >= seedInfo.targetPoints

                            val imgRes = if (isUnlocked) {
                                when (slotIdx) {
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

                            Surface(
                                modifier = Modifier
                                    .width(100.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        val uid = sheetUserId ?: return@clickable
                                        sheetUserId = null
                                        sheetProfile = null
                                        onAvatarClick(uid)
                                    },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isUnlocked) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.surfaceVariant.copy(
                                    alpha = 0.5f
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Image(
                                        painter = painterResource(id = imgRes),
                                        contentDescription = seedInfo.name,
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        seedInfo.name,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                    Text(
                                        if (isPlanted) "Planted" else if (isUnlocked) "Unlocked" else "Locked",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 9.sp,
                                        color = if (isPlanted) Color(0xFF2E7D32) else if (isUnlocked) Color(
                                            0xFF5D4037
                                        ) else Color.Gray
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedButton(
                    onClick = {
                        val uid = sheetUserId ?: return@OutlinedButton
                        sheetUserId = null
                        sheetProfile = null
                        onAvatarClick(uid)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("View Full Profile")
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun ReviewRowItem(
    review: ReviewDto,
    isOwnComment: Boolean,
    userVote: String?,
    isVoting: Boolean = false,
    themeViewModel: ThemeViewModel,
    onAvatarClick: () -> Unit,
    onLike: () -> Unit,
    onDislike: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onReport: () -> Unit
) {
    val lang by themeViewModel.language.collectAsState()
    fun t(key: String) = AppStrings.get(key, lang)
    var menuExpanded by remember { mutableStateOf(false) }
    var showFullImage by remember { mutableStateOf(false) }

    Column {
        Row(verticalAlignment = Alignment.Top) {
            val frameColor = when (review.reviewer_frame) {
                "Leaf" -> Color(0xFF4CAF50)
                "Blocks" -> Color(0xFF795548)
                "Gold" -> Color(0xFFFFD700)
                "Diamond" -> Color(0xFF00BCD4)
                else -> null
            }
            if (review.reviewer_avatar_url.isNotBlank()) {
                Image(
                    painter = rememberAsyncImagePainter(model = review.reviewer_avatar_url),
                    contentDescription = "Avatar",
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .then(
                            if (frameColor != null) Modifier.border(
                                2.dp,
                                frameColor,
                                CircleShape
                            ) else Modifier
                        )
                        .clickable(onClick = onAvatarClick),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .then(
                            if (frameColor != null) Modifier.border(
                                2.dp,
                                frameColor,
                                CircleShape
                            ) else Modifier
                        )
                        .clickable(onClick = onAvatarClick)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_person),
                        contentDescription = "View profile",
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    review.reviewer_name.ifBlank { "User" },
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
                val stars = review.star_rating.coerceIn(0, 5)
                Row {
                    repeat(stars) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_star),
                            contentDescription = null,
                            tint = Color(0xFFFFC107),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    repeat(5 - stars) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_star),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outlineVariant,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                Row(verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        review.text,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Box {
                        IconButton(
                            onClick = { menuExpanded = true },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_more_vert),
                                contentDescription = "More options",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            if (isOwnComment) {
                                DropdownMenuItem(
                                    text = { Text(t("edit")) },
                                    onClick = {
                                        menuExpanded = false
                                        onEdit()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(t("delete"), color = Color(0xFFE53935)) },
                                    onClick = {
                                        menuExpanded = false
                                        onDelete()
                                    }
                                )
                            } else {
                                DropdownMenuItem(
                                    text = { Text(t("report"), color = Color(0xFFE53935)) },
                                    onClick = {
                                        menuExpanded = false
                                        onReport()
                                    }
                                )
                            }
                        }
                    }
                }
                if (!review.photo_url.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Image(
                        painter = rememberAsyncImagePainter(model = review.photo_url),
                        contentDescription = "Review photo",
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 220.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { showFullImage = true },
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 52.dp, top = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                TimeFormatter.formatCommentTime(review.created_at),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = onLike,
                enabled = !isVoting,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_thumb_up),
                    contentDescription = "Like",
                    modifier = Modifier.size(16.dp),
                    tint = if (userVote == "LIKE") Color(0xFF4285F4) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                review.like_count.toString(),
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(start = 2.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = onDislike,
                enabled = !isVoting,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_thumb_down),
                    contentDescription = "Dislike",
                    modifier = Modifier.size(16.dp),
                    tint = if (userVote == "DISLIKE") Color(0xFFE53935) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                review.dislike_count.toString(),
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(start = 2.dp)
            )
        }
    }

    if (showFullImage && !review.photo_url.isNullOrBlank()) {
        Dialog(onDismissRequest = { showFullImage = false }) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { showFullImage = false },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(
                        painter = rememberAsyncImagePainter(model = review.photo_url),
                        contentDescription = "Full photo",
                        modifier = Modifier
                            .fillMaxWidth(0.95f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White),
                        contentScale = ContentScale.Fit
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.clickable { showFullImage = false }
                    ) {
                        Text(
                            "✕ Close",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                        )
                    }
                }
            }
        }
    }
}
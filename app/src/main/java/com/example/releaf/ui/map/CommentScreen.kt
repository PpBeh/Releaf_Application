package com.example.releaf.ui.map

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.rememberAsyncImagePainter
import com.example.releaf.data.remote.dto.ReviewDto
import com.example.releaf.ui.viewmodel.CommentViewModel
import java.io.File

fun countWords(text: String): Int {
    if (text.isBlank()) return 0
    return text.trim().split("\\s+".toRegex()).size
}

fun saveBitmapToUri(context: android.content.Context, bitmap: Bitmap): Uri {
    val file = File(context.cacheDir, "comment_camera_${System.currentTimeMillis()}.jpg")
    file.outputStream().use { out ->
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
    }
    return Uri.fromFile(file)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentScreen(
    poiId: String,
    viewModel: CommentViewModel,
    currentUserId: String,
    onBackClick: () -> Unit,
    onAvatarClick: (String) -> Unit
) {
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

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            selectedPhotoUri = uri
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            selectedPhotoUri = saveBitmapToUri(context, bitmap)
        }
    }

    var sheetUserId by remember { mutableStateOf<String?>(null) }
    var sheetProfile by remember { mutableStateOf<com.example.releaf.data.remote.dto.ProfileDto?>(null) }
    var sheetAchievements by remember { mutableIntStateOf(0) }
    var sheetPoints by remember { mutableIntStateOf(0) }

    LaunchedEffect(sheetUserId) {
        val uid = sheetUserId ?: return@LaunchedEffect
        try {
            val authRepository = com.example.releaf.data.repository.AuthRepository()
            sheetProfile = authRepository.getProfile(uid)
        } catch (_: Exception) { }
        try {
            val rewardRepository = com.example.releaf.data.repository.RewardRepository()
            sheetAchievements = rewardRepository.getUserAchievements(uid).size
            sheetPoints = sheetProfile?.total_points ?: 0
        } catch (_: Exception) { }
    }

    LaunchedEffect(poiId) {
        viewModel.loadReviews(poiId, currentUserId)
        while (true) {
            kotlinx.coroutines.delay(3000)
            viewModel.loadReviews(poiId, currentUserId)
        }
    }

    val wordCount = countWords(commentText)
    val isWordLimitExceeded = wordCount > 500

    // Photo Source Picker Dialog (Gallery vs Camera)
    if (showPhotoSourcePicker) {
        Dialog(onDismissRequest = { showPhotoSourcePicker = false }) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Attach Photo",
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
                        Icon(Icons.Default.Image, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("Choose from Gallery", fontWeight = FontWeight.Bold)
                            Text("Select an existing photo from device", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showPhotoSourcePicker = false
                                cameraLauncher.launch(null)
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("Take Photo with Camera", fontWeight = FontWeight.Bold)
                            Text("Capture a new photo right now", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    TextButton(onClick = { showPhotoSourcePicker = false }) {
                        Text("Cancel")
                    }
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Comments") },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            }
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(5) { index ->
                IconButton(
                    onClick = { starRating = index + 1 },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        tint = if (index < starRating) Color(0xFFFFC107) else MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Selected Photo Preview Banner
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
                        Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color.White, modifier = Modifier.size(12.dp))
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    "Photo attached to comment ✓",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF2E7D32),
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = commentText,
                onValueChange = { commentText = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Add a comment...") },
                singleLine = false,
                maxLines = 3,
                isError = isWordLimitExceeded
            )

            Spacer(modifier = Modifier.width(4.dp))

            // Photo Attachment Choice Button
            IconButton(
                onClick = { showPhotoSourcePicker = true }
            ) {
                Icon(
                    Icons.Default.AddAPhoto,
                    contentDescription = "Attach Photo",
                    tint = if (selectedPhotoUri != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(
                onClick = {
                    if (commentText.isNotBlank() && !isWordLimitExceeded) {
                        val lm = context.getSystemService(android.content.Context.LOCATION_SERVICE) as? android.location.LocationManager
                        val loc = try {
                            lm?.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER)
                                ?: lm?.getLastKnownLocation(android.location.LocationManager.NETWORK_PROVIDER)
                        } catch (_: SecurityException) { null }
                        val lat = loc?.latitude ?: 0.0
                        val lng = loc?.longitude ?: 0.0
                        viewModel.addReview(poiId, currentUserId, starRating, commentText, lat, lng, selectedPhotoUri, context)
                        commentText = ""
                        selectedPhotoUri = null
                        starRating = 5
                    }
                },
                enabled = commentText.isNotBlank() && !isWordLimitExceeded && !isProcessing
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
            }
        }

        // Word Limit Counter Banner
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isWordLimitExceeded) {
                Text(
                    "Word limit exceeded! (Max 500 words)",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }

            Text(
                "$wordCount / 500 words",
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
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 80.dp)
            ) {
                items(reviews) { review ->
                    var showEditDialog by remember { mutableStateOf(false) }
                    ReviewRowItem(
                        review = review,
                        isOwnComment = review.user_id == currentUserId,
                        userVote = userVotes[review.id],
                        isVoting = isVoting,
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
                            title = { Text("Edit Comment") },
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
                                        "$editWordCount / 500 words",
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
                                        CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                                    } else {
                                        Text("Save")
                                    }
                                }
                            },
                            dismissButton = {
                                TextButton(
                                    onClick = { showEditDialog = false },
                                    enabled = !isProcessing
                                ) { Text("Cancel") }
                            }
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
                if (reviews.isEmpty() && !isLoading) {
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("No comments yet", style = MaterialTheme.typography.bodyLarge)
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
                    sheetProfile?.name?.ifBlank { "User" } ?: "Loading...",
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
    onAvatarClick: () -> Unit,
    onLike: () -> Unit,
    onDislike: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onReport: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    var showFullImage by remember { mutableStateOf(false) }

    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
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
                        .then(if (frameColor != null) Modifier.border(2.dp, frameColor, CircleShape) else Modifier)
                        .clickable(onClick = onAvatarClick),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .then(if (frameColor != null) Modifier.border(2.dp, frameColor, CircleShape) else Modifier)
                        .clickable(onClick = onAvatarClick)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.AccountCircle,
                        contentDescription = "View profile",
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    review.reviewer_name.ifBlank { "User" },
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
                val stars = review.star_rating.coerceIn(0, 5)
                Row {
                    repeat(stars) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFC107), modifier = Modifier.size(14.dp))
                    }
                    repeat(5 - stars) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.size(14.dp))
                    }
                }
                Text(review.text, style = MaterialTheme.typography.bodyMedium)
                if (!review.photo_url.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
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
            Box {
                IconButton(onClick = { menuExpanded = true }, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "More options",
                        modifier = Modifier.size(20.dp)
                    )
                }
                androidx.compose.material3.DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    if (isOwnComment) {
                        androidx.compose.material3.DropdownMenuItem(
                            text = { Text("Edit") },
                            onClick = {
                                menuExpanded = false
                                onEdit()
                            }
                        )
                        androidx.compose.material3.DropdownMenuItem(
                            text = { Text("Delete", color = Color(0xFFE53935)) },
                            onClick = {
                                menuExpanded = false
                                onDelete()
                            }
                        )
                    } else {
                        androidx.compose.material3.DropdownMenuItem(
                            text = { Text("Report", color = Color(0xFFE53935)) },
                            onClick = {
                                menuExpanded = false
                                onReport()
                            }
                        )
                    }
                }
            }
        }
        // Likes directly under comment text with thumb icons
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 52.dp, top = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                com.example.releaf.data.remote.TimeFormatter.formatCommentTime(review.created_at),
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
                    Icons.Default.ThumbUp,
                    contentDescription = "Like",
                    modifier = Modifier.size(16.dp),
                    tint = if (userVote == "LIKE") Color(0xFF4285F4) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(review.like_count.toString(), style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(start = 2.dp))
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = onDislike,
                enabled = !isVoting,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    Icons.Default.ThumbDown,
                    contentDescription = "Dislike",
                    modifier = Modifier.size(16.dp),
                    tint = if (userVote == "DISLIKE") Color(0xFFE53935) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(review.dislike_count.toString(), style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(start = 2.dp))
        }
    }

    if (showFullImage && !review.photo_url.isNullOrBlank()) {
        Dialog(onDismissRequest = { showFullImage = false }) {
            Box(modifier = Modifier.fillMaxSize().clickable { showFullImage = false }, contentAlignment = Alignment.Center) {
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
                        Text("✕ Close", color = Color.Black, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp))
                    }
                }
            }
        }
    }
}

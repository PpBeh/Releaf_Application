package com.example.releaf.ui.map

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.releaf.data.remote.dto.ReviewDto
import com.example.releaf.ui.viewmodel.CommentViewModel

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
    val context = androidx.compose.ui.platform.LocalContext.current

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

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = commentText,
                onValueChange = { commentText = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Add a comment...") },
                singleLine = true
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = {
                    if (commentText.isNotBlank()) {
                        val lm = context.getSystemService(android.content.Context.LOCATION_SERVICE) as? android.location.LocationManager
                        val loc = try {
                            lm?.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER)
                                ?: lm?.getLastKnownLocation(android.location.LocationManager.NETWORK_PROVIDER)
                        } catch (_: SecurityException) { null }
                        val lat = loc?.latitude ?: 0.0
                        val lng = loc?.longitude ?: 0.0
                        viewModel.addReview(poiId, currentUserId, starRating, commentText, lat, lng)
                        commentText = ""
                        starRating = 5
                    }
                }
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
            }
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
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
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
                        androidx.compose.material3.AlertDialog(
                            onDismissRequest = { if (!isProcessing) showEditDialog = false },
                            title = { Text("Edit Comment") },
                            text = {
                                OutlinedTextField(
                                    value = editText,
                                    onValueChange = { editText = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = false,
                                    enabled = !isProcessing
                                )
                            },
                            confirmButton = {
                                androidx.compose.material3.TextButton(
                                    onClick = {
                                        viewModel.updateReview(review.id, editText)
                                        showEditDialog = false
                                    },
                                    enabled = !isProcessing
                                ) {
                                    if (isProcessing) {
                                        CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                                    } else {
                                        Text("Save")
                                    }
                                }
                            },
                            dismissButton = {
                                androidx.compose.material3.TextButton(
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
        androidx.compose.material3.ModalBottomSheet(
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
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        (sheetProfile?.name ?: "U").take(1).uppercase(),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
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
                androidx.compose.material3.OutlinedButton(
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

    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onAvatarClick) {
                Icon(
                    Icons.Default.AccountCircle,
                    contentDescription = "View profile",
                    modifier = Modifier.size(40.dp)
                )
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
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 52.dp, top = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                com.example.releaf.data.remote.TimeFormatter.formatCommentTime(review.created_at),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.weight(1f))
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
            Text(review.like_count.toString(), style = MaterialTheme.typography.labelSmall)
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
            Text(review.dislike_count.toString(), style = MaterialTheme.typography.labelSmall)
        }
    }
}

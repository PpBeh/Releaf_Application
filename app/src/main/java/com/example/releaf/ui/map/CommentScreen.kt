package com.example.releaf.ui.map

import androidx.compose.foundation.layout.Arrangement
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
    var commentText by remember { mutableStateOf("") }
    var starRating by remember { mutableIntStateOf(5) }

    LaunchedEffect(poiId) {
        viewModel.loadReviews(poiId)
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
                        viewModel.addReview(poiId, currentUserId, starRating, commentText)
                        commentText = ""
                        starRating = 5
                    }
                }
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
            }
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
                    ReviewRowItem(
                        review = review,
                        onAvatarClick = { onAvatarClick(review.user_id) },
                        onLike = { viewModel.likeReview(review.id, review.like_count) },
                        onDislike = { viewModel.dislikeReview(review.id, review.dislike_count) }
                    )
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
}

@Composable
private fun ReviewRowItem(
    review: ReviewDto,
    onAvatarClick: () -> Unit,
    onLike: () -> Unit,
    onDislike: () -> Unit
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onAvatarClick) {
                Icon(
                    Icons.Default.AccountCircle,
                    contentDescription = "View profile",
                    modifier = Modifier.size(40.dp)
                )
            }
            Column {
                Text(
                    review.reviewer_name.ifBlank { "User" },
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
                Row {
                    repeat(review.star_rating) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFC107), modifier = Modifier.size(14.dp))
                    }
                    repeat(5 - review.star_rating) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.size(14.dp))
                    }
                }
                Text(review.text, style = MaterialTheme.typography.bodyMedium)
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 52.dp, top = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(review.created_at.take(10), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = onLike, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.ThumbUp, contentDescription = "Like", modifier = Modifier.size(16.dp))
            }
            Text(review.like_count.toString(), style = MaterialTheme.typography.labelSmall)
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = onDislike, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.ThumbDown, contentDescription = "Dislike", modifier = Modifier.size(16.dp))
            }
            Text(review.dislike_count.toString(), style = MaterialTheme.typography.labelSmall)
        }
    }
}

package com.example.releaf.ui.map

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
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.releaf.model.Review

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentScreen(
    poiId: String,
    onBackClick: () -> Unit,
    onAvatarClick: (String) -> Unit
) {
    var commentText by remember { mutableStateOf("") }

    // TODO: replace with real reviews for this poiId once the backend is wired in
    val reviews = emptyList<Review>()

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
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = commentText,
                onValueChange = { commentText = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Comment") },
                singleLine = true
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = { /* TODO: submit commentText for poiId */ }) {
                Icon(Icons.Default.Send, contentDescription = "Send")
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            items(reviews) { review ->
                ReviewRow(review = review, onAvatarClick = { onAvatarClick(review.reviewerUserId) })
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun ReviewRow(review: Review, onAvatarClick: () -> Unit) {
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
                Row {
                    repeat(review.starRating) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFC107), modifier = Modifier.size(16.dp))
                    }
                    repeat(5 - review.starRating) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.size(16.dp))
                    }
                }
                Text(review.text, style = MaterialTheme.typography.bodyMedium)
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 52.dp, top = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(review.timeAgoLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.weight(1f))
            Icon(Icons.Default.ThumbUp, contentDescription = "Like", modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(review.likeCount.toString(), style = MaterialTheme.typography.labelSmall)
            Spacer(modifier = Modifier.width(12.dp))
            Icon(Icons.Default.ThumbDown, contentDescription = "Dislike", modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(review.dislikeCount.toString(), style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CommentScreenPreview() {
    CommentScreen(poiId = "sample", onBackClick = {}, onAvatarClick = {})
}
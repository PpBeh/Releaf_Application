package com.example.releaf.ui.map

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.releaf.model.Poi

@Composable
fun PoiDetailSheet(
    poi: Poi,
    onCloseClick: () -> Unit,
    onDirectionClick: () -> Unit,
    onCommentClick: () -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            IconButton(onClick = { /* TODO: save to favorites */ }) {
                Icon(Icons.Default.FavoriteBorder, contentDescription = "Favorite")
            }
            IconButton(onClick = { /* TODO: share this pin */ }) {
                Icon(Icons.Default.Share, contentDescription = "Share")
            }
            IconButton(onClick = onCloseClick) {
                Icon(Icons.Default.Close, contentDescription = "Close")
            }
        }

        Text(text = poi.name, style = MaterialTheme.typography.headlineMedium)

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFC107))
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = poi.rating.toString(), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "The toilet might be in cleaning based on other user feedback in this time.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = onDirectionClick,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2962FF))
            ) {
                Icon(Icons.Default.Directions, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Direction")
            }
            Button(
                onClick = onCommentClick,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
            ) {
                Text("Comment")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Photo gallery — TODO: replace placeholder tiles with poi.photoUrls once available
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PhotoTile(modifier = Modifier.weight(1f))
            AddPhotoTile(modifier = Modifier.weight(1f), onClick = { /* TODO: add a new photo */ })
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PhotoTile(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
private fun PhotoTile(modifier: Modifier = Modifier) {
    // TODO: replace with painterResource / AsyncImage of the real photo
    Column(
        modifier = modifier
            .aspectRatio(1f)
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
    ) {}
}

@Composable
private fun AddPhotoTile(modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(
        modifier = modifier
            .aspectRatio(1f)
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.Add, contentDescription = "Add photo")
    }
}
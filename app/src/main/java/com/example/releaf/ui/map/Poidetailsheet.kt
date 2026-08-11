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
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Report
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
import com.example.releaf.data.remote.dto.PoiDto
import com.example.releaf.data.remote.dto.PoiPhotoDto
import com.example.releaf.ui.viewmodel.PoiActionResult

@Composable
fun PoiDetailSheet(
    poi: PoiDto,
    photos: List<PoiPhotoDto>,
    onCloseClick: () -> Unit,
    onDirectionClick: () -> Unit,
    onCommentClick: () -> Unit,
    onVerifyClick: () -> Unit,
    onReportNotExist: () -> Unit,
    actionResult: PoiActionResult?
) {
    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            IconButton(onClick = { /* save to favorites */ }) {
                Icon(Icons.Default.FavoriteBorder, contentDescription = "Favorite")
            }
            IconButton(onClick = { /* share */ }) {
                Icon(Icons.Default.Share, contentDescription = "Share")
            }
            IconButton(onClick = onCloseClick) {
                Icon(Icons.Default.Close, contentDescription = "Close")
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = poi.name, style = MaterialTheme.typography.headlineMedium)
            if (!poi.is_verified) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "(Unverified)",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFFFF9800),
                    fontWeight = FontWeight.Bold
                )
            } else {
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.Default.Verified, contentDescription = "Verified", tint = Color(0xFF4CAF50))
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFC107))
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = String.format("%.1f", poi.rating), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "${poi.cleanliness} | ${if (poi.is_paid) "Paid" else "Free"} | ${poi.category}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            "Verified: ${poi.verification_count}/3 | Reports: ${poi.report_count}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

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

        Spacer(modifier = Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = onVerifyClick,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
            ) {
                Icon(Icons.Default.Verified, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Verify")
            }
            Button(
                onClick = onReportNotExist,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF44336))
            ) {
                Icon(Icons.Default.Report, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Not Exist")
            }
        }

        actionResult?.let {
            if (it is PoiActionResult.Message) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = when (it.message) {
                        "already_verified" -> "You already verified this POI."
                        "now_verified" -> "POI is now verified!"
                        "verification_counted" -> "Verification recorded."
                        "already_reported" -> "You already reported this POI."
                        "now_unverified" -> "Too many reports. POI is now unverified."
                        "removed" -> "POI has been removed due to reports."
                        "report_counted" -> "Report recorded."
                        else -> it.message
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (photos.isNotEmpty()) {
                photos.take(3).forEach { photo ->
                    PhotoTile(modifier = Modifier.weight(1f))
                }
            } else {
                PhotoTile(modifier = Modifier.weight(1f))
                PhotoTile(modifier = Modifier.weight(1f))
            }
            AddPhotoTile(modifier = Modifier.weight(1f), onClick = { })
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (photos.size > 3) {
                PhotoTile(modifier = Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
private fun PhotoTile(modifier: Modifier = Modifier) {
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

package com.example.releaf.ui.map

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.releaf.data.remote.dto.PoiDto
import com.example.releaf.data.remote.dto.PoiPhotoDto
import com.example.releaf.ui.viewmodel.PoiActionResult

@Composable
fun PoiDetailSheet(
    poi: PoiDto,
    photos: List<PoiPhotoDto>,
    reviewCount: Int,
    isFavorite: Boolean,
    isProcessing: Boolean = false,
    analyzedStatus: String? = null,
    analyzedStatusTime: String? = null,
    onCloseClick: () -> Unit,
    onDirectionClick: () -> Unit,
    onCommentClick: () -> Unit,
    onVerifyClick: () -> Unit,
    onReportNotExist: () -> Unit,
    onFavoriteClick: () -> Unit,
    onShareClick: () -> Unit,
    onAddPhotoClick: () -> Unit,
    actionResult: PoiActionResult?,
    themeViewModel: com.example.releaf.ui.viewmodel.ThemeViewModel
) {
    val context = LocalContext.current
    val lang by themeViewModel.language.collectAsState()
    fun t(key: String) = com.example.releaf.ui.theme.AppStrings.get(key, lang)
    val validPhotos = photos.filter { !it.photo_url.isNullOrBlank() }

    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            IconButton(onClick = onFavoriteClick) {
                Icon(
                    if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (isFavorite) Color(0xFFE53935) else MaterialTheme.colorScheme.onSurface
                )
            }
            IconButton(onClick = onShareClick) {
                Icon(Icons.Default.Share, contentDescription = "Share")
            }
            IconButton(onClick = onCloseClick) {
                Icon(Icons.Default.Close, contentDescription = "Close")
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = poi.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f, fill = false)
            )
            Spacer(modifier = Modifier.width(8.dp))
            if (!poi.is_verified) {
                Text(
                    t("unverified"),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFFF9800),
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            } else {
                Icon(Icons.Default.Verified, contentDescription = null, tint = Color(0xFF4CAF50))
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFC107))
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = String.format(java.util.Locale.US, "%.1f", poi.rating),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "${poi.cleanliness} | ${if (poi.is_paid) "Paid" else "Free"} | $reviewCount comments",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (poi.description.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                poi.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        val status = analyzedStatus ?: poi.recent_status
        val statusTime = analyzedStatusTime ?: poi.recent_status_time
        if (!status.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFFFF3E0), RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("\u26A0", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    buildString {
                        append("Users reported this ${if (poi.category == "TRASH_CAN") "trash can" else "toilet"} $status")
                        val time =
                            com.example.releaf.data.remote.TimeFormatter.formatHour(statusTime.orEmpty())
                        if (time != null) append(" at $time")
                        append(".")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFE65100),
                    fontWeight = FontWeight.Medium
                )
            }
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
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Directions, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text(t("direction"))
            }
            Button(
                onClick = onCommentClick,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(t("comment"))
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = onVerifyClick,
                enabled = !isProcessing,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(t("updating"))
                } else {
                    Icon(Icons.Default.Verified, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(t("verify"))
                }
            }
            Button(
                onClick = onReportNotExist,
                enabled = !isProcessing,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF44336))
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(18.dp)
                    )
                } else {
                    Icon(Icons.Default.Report, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(t("not_exist"))
                }
            }
        }

        actionResult?.let {
            if (it is PoiActionResult.Message) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = when {
                        it.message.startsWith("too_far_") -> {
                            val meters = it.message.removePrefix("too_far_")
                            t("too_far_fmt").replace("%s", meters)
                        }

                        it.message == "already_verified" -> t("already_verified")
                        it.message == "now_verified" -> t("now_verified")
                        it.message == "verification_counted" -> t("verification_counted")
                        it.message == "verify_failed" -> t("verify_failed")
                        it.message == "already_reported" -> t("already_reported")
                        it.message == "now_unverified" -> t("now_unverified")
                        it.message == "removed" -> t("removed_poi")
                        it.message == "report_counted" -> t("report_counted")
                        it.message == "report_failed" -> t("report_failed")
                        else -> it.message
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        var selectedFullUrl by remember { mutableStateOf<String?>(null) }

        // Photo grid: 3 tiles per row; the "add photo" tile fills the last free cell.
        val photoRows = validPhotos.chunked(3).toMutableList()
        if (photoRows.isEmpty()) photoRows.add(emptyList())
        val addRowIndex = if (photoRows.last().size < 3) photoRows.size - 1 else {
            photoRows.add(emptyList())
            photoRows.size - 1
        }
        photoRows.forEachIndexed { rowIndex, rowPhotos ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowPhotos.forEach { photo ->
                    PhotoTile(
                        url = photo.photo_url,
                        modifier = Modifier.weight(1f),
                        onClick = { selectedFullUrl = photo.photo_url }
                    )
                }
                val addCellCount = if (rowIndex == addRowIndex) 1 else 0
                if (addCellCount == 1) {
                    AddPhotoTile(modifier = Modifier.weight(1f), onClick = onAddPhotoClick)
                }
                repeat(3 - rowPhotos.size - addCellCount) {
                    PhotoTile(url = null, modifier = Modifier.weight(1f))
                }
            }
            if (rowIndex != photoRows.lastIndex) {
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
        Spacer(modifier = Modifier.height(20.dp))

        if (selectedFullUrl != null) {
            androidx.compose.ui.window.Dialog(onDismissRequest = { selectedFullUrl = null }) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable { selectedFullUrl = null },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        androidx.compose.foundation.Image(
                            painter = coil.compose.rememberAsyncImagePainter(model = selectedFullUrl),
                            contentDescription = "Full photo",
                            modifier = Modifier
                                .fillMaxWidth(0.95f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White),
                            contentScale = androidx.compose.ui.layout.ContentScale.Fit
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        androidx.compose.material3.Surface(
                            shape = RoundedCornerShape(50),
                            color = Color.White.copy(alpha = 0.9f),
                            modifier = Modifier.clickable { selectedFullUrl = null }
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
}

@Composable
private fun PhotoTile(url: String?, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    if (url.isNullOrBlank()) {
        Column(
            modifier = modifier
                .aspectRatio(1f)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
        ) {}
    } else {
        androidx.compose.foundation.Image(
            painter = coil.compose.rememberAsyncImagePainter(model = url),
            contentDescription = "Photo",
            modifier = modifier
                .aspectRatio(1f)
                .clip(RoundedCornerShape(12.dp))
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
            contentScale = androidx.compose.ui.layout.ContentScale.Crop
        )
    }
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

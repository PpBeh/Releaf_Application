package com.example.releaf.ui.profile

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.releaf.ui.theme.AppTheme
import com.example.releaf.ui.viewmodel.ProfileViewModel
import com.example.releaf.ui.viewmodel.ThemeViewModel

@Composable
fun ProfileScreen(
    userId: String,
    currentUserId: String,
    viewModel: ProfileViewModel,
    themeViewModel: ThemeViewModel,
    onFavouritesClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    val profile by viewModel.profile.collectAsState()
    val achievements by viewModel.achievements.collectAsState()
    val totalAchievements by viewModel.totalAchievements.collectAsState()
    val currentTheme by themeViewModel.theme.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    var showThemeDialog by remember { mutableStateOf(false) }

    if (showThemeDialog) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { showThemeDialog = false }) {
            androidx.compose.material3.Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Select Theme", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                    AppTheme.entries.forEach { theme ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    themeViewModel.setTheme(theme)
                                    showThemeDialog = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            androidx.compose.material3.RadioButton(
                                selected = (currentTheme == theme),
                                onClick = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(theme.name.lowercase().replaceFirstChar { it.uppercase() })
                        }
                    }
                }
            }
        }
    }

    val avatarPicker = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.uploadAvatar(userId, uri, context)
        }
    }

    LaunchedEffect(userId) {
        viewModel.loadProfile(userId)
    }

    val displayName = profile?.name?.ifBlank { "User" } ?: "User"
    val title = profile?.title?.ifBlank { "Gardener" } ?: "Gardener"
    val phone = profile?.phone?.ifBlank { "N/A" } ?: "N/A"
    val email = profile?.email?.ifBlank { "N/A" } ?: "N/A"
    val isOwnProfile = userId == currentUserId

    val settingsRows = listOf(
        SettingsRowData(Icons.Default.Favorite, "Favourite Toilets"),
        SettingsRowData(Icons.Default.Refresh, "Updates"),
        SettingsRowData(Icons.Default.Notifications, "Notification Settings"),
        SettingsRowData(Icons.Default.Palette, "Theme"),
        SettingsRowData(Icons.Default.Block, "Permission"),
        SettingsRowData(Icons.Default.Delete, "Clear Cache"),
        SettingsRowData(Icons.Default.Person, "Account"),
        SettingsRowData(Icons.Default.Info, "About Us"),
        SettingsRowData(Icons.AutoMirrored.Filled.Logout, LOG_OUT_LABEL)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .background(Color(0xFF1E88E5))
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface)
                            .clickable(enabled = isOwnProfile) { avatarPicker.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        val avatarUrl = profile?.avatar_url.orEmpty()
                        if (avatarUrl.isNotBlank()) {
                            androidx.compose.foundation.Image(
                                painter = coil.compose.rememberAsyncImagePainter(model = avatarUrl),
                                contentDescription = "Avatar",
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                        } else {
                            androidx.compose.material3.Icon(
                                Icons.Default.Person,
                                contentDescription = "Add photo",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            displayName,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            title,
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        if (isOwnProfile) {
                            Text(phone, color = Color.White, style = MaterialTheme.typography.bodySmall)
                            Text(email, color = Color.White, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }

        OutlinedButton(
            onClick = { /* TODO: navigate to garden */ },
            modifier = Modifier.padding(start = 20.dp, top = 16.dp)
        ) {
            Text("View Garden")
        }

        Text(
            "Achievements (${achievements.size}/$totalAchievements)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 8.dp)
        )

        val achievementList = if (achievements.isEmpty()) {
            List(6) { null }
        } else {
            achievements.map { it.achievement?.label ?: "?" }
        }

        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            achievementList.chunked(2).forEach { rowItems ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    rowItems.forEach { label ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .background(Color(0xFF6D4C29), RoundedCornerShape(12.dp))
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                label ?: "?",
                                color = Color.White,
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    if (rowItems.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        if (isOwnProfile) {
            Text(
                "Settings",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 20.dp, top = 12.dp, bottom = 4.dp)
            )

            settingsRows.forEach { row ->
                SettingsRow(
                    row = row,
                    onClick = {
                        when (row.label) {
                            LOG_OUT_LABEL -> onLogoutClick()
                            "Favourite Toilets" -> onFavouritesClick()
                            "Theme" -> showThemeDialog = true
                        }
                    }
                )
            }
        }
    }
}

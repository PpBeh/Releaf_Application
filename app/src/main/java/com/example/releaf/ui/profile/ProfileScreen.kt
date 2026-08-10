package com.example.releaf.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

private data class Achievement(val id: String, val label: String)

@Composable
fun ProfileScreen(
    userId: String,
    onLogoutClick: () -> Unit
) {
    // TODO: load the real user (own profile vs. another user's) based on userId
    val isOwnProfile = userId == "me"
    val achievements = listOf(
        Achievement("1", "Expert Reviewer"),
        Achievement("2", "Expert Gardener"),
        Achievement("3", "Expert Navigator")
    )

    val settingsRows = listOf(
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
                .height(340.dp)
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
                        // TODO: painterResource(R.drawable.avatar_placeholder) or AsyncImage(user.avatarUrl)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            "User$userId",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            "Veteran Gardener",
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        if (isOwnProfile) {
                            Text(
                                "000000128",
                                color = Color.White,
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                "email@whatever.com",
                                color = Color.White,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Achievements (${achievements.size}/55)",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    achievements.forEach { achievement ->
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .background(Color(0xFF6D4C29), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            // TODO: swap in the real trophy illustration (painterResource) per achievement
                            Text(
                                achievement.label,
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        OutlinedButton(
            onClick = { /* TODO: navigate to this user's garden */ },
            modifier = Modifier.padding(start = 20.dp, top = 16.dp)
        ) {
            Text("View Garden")
        }

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
                    if (row.label == LOG_OUT_LABEL) {
                        onLogoutClick()
                    }
                }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ProfileScreenPreview() {
    ProfileScreen(userId = "me", onLogoutClick = {})
}
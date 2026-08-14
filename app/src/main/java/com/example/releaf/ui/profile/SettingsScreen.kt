package com.example.releaf.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import com.example.releaf.ui.theme.AppTheme
import com.example.releaf.ui.viewmodel.ThemeViewModel
import androidx.compose.ui.unit.dp

internal const val LOG_OUT_LABEL = "Log out"

internal data class SettingsRowData(val icon: ImageVector, val label: String)

@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    onLogoutClick: () -> Unit,
    themeViewModel: ThemeViewModel
) {
    val currentTheme by themeViewModel.theme.collectAsState()
    var showThemeDialog by remember { mutableStateOf(false) }

    if (showThemeDialog) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { showThemeDialog = false }) {
            androidx.compose.material3.Surface(
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
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

    val rows = listOf(
        SettingsRowData(Icons.Default.Refresh, "Updates"),
        SettingsRowData(Icons.Default.Notifications, "Notification Settings"),
        SettingsRowData(Icons.Default.Palette, "Theme"),
        SettingsRowData(Icons.Default.Block, "Permission"),
        SettingsRowData(Icons.Default.Delete, "Clear Cache"),
        SettingsRowData(Icons.Default.Person, "Account"),
        SettingsRowData(Icons.Default.Info, "About Us"),
        SettingsRowData(Icons.AutoMirrored.Filled.Logout, LOG_OUT_LABEL)
    )

    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .background(MaterialTheme.colorScheme.primary)
        ) {
            IconButton(onClick = onBackClick, modifier = Modifier.padding(8.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                    // TODO: painterResource(R.drawable.avatar_placeholder) or AsyncImage(user.avatarUrl)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("User120033029", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Text("000000128", color = Color.White, style = MaterialTheme.typography.bodySmall)
                    Text("email@whatever.com", color = Color.White, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        Text(
            "Settings",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(20.dp)
        )

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(rows) { row ->
                SettingsRow(
                    row = row,
                    onClick = {
                        when (row.label) {
                            LOG_OUT_LABEL -> onLogoutClick()
                            "Theme" -> showThemeDialog = true
                            else -> { /* TODO: handle other rows */ }
                        }
                    }
                )
            }
        }
    }
}

@Composable
internal fun SettingsRow(row: SettingsRowData, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(row.icon, contentDescription = null)
        Spacer(modifier = Modifier.width(16.dp))
        Text(row.label, modifier = Modifier.weight(1f))
        Icon(Icons.Default.ChevronRight, contentDescription = null)
    }
}


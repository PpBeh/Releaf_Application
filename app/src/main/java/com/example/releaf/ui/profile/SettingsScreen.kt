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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.releaf.R
import com.example.releaf.ui.theme.AppTheme
import com.example.releaf.ui.theme.string
import com.example.releaf.ui.viewmodel.AppLanguage
import com.example.releaf.ui.viewmodel.ProfileViewModel
import com.example.releaf.ui.viewmodel.ThemeViewModel

internal const val LOG_OUT_LABEL = "Log out"

data class SettingsRowData(val icon: Int, val title: String, val key: String = "")

@Composable
fun SettingsScreen(
    userId: String,
    onBackClick: () -> Unit,
    onLogoutClick: () -> Unit,
    themeViewModel: ThemeViewModel,
    viewModel: ProfileViewModel
) {
    val context = LocalContext.current
    val currentTheme by themeViewModel.theme.collectAsState()
    val currentLang by themeViewModel.language.collectAsState()
    var showThemeDialog by remember { mutableStateOf(false) }
    var showLangDialog by remember { mutableStateOf(false) }
    val profile by viewModel.profile.collectAsState()

    val avatarPicker = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.uploadAvatar(userId, uri, context)
        }
    }

    LaunchedEffect(userId) {
        if (userId.isNotBlank()) {
            viewModel.loadProfile(userId)
        }
    }

    if (showLangDialog) {
        Dialog(onDismissRequest = { showLangDialog = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Select Language",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    AppLanguage.entries.forEach { lang ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    themeViewModel.setLanguage(lang)
                                    showLangDialog = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (currentLang == lang),
                                onClick = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(lang.name.lowercase().replaceFirstChar { it.uppercase() })
                        }
                    }
                }
            }
        }
    }

    if (showThemeDialog) {
        Dialog(onDismissRequest = { showThemeDialog = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Select Theme",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
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
                            RadioButton(
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
        SettingsRowData(R.drawable.ic_refresh, "Updates"),
        SettingsRowData(R.drawable.ic_notifications, "Notification Settings"),
        SettingsRowData(R.drawable.ic_palette, string("theme", themeViewModel), "theme"),
        SettingsRowData(R.drawable.ic_translate, string("language", themeViewModel), "language"),
        SettingsRowData(R.drawable.ic_block, "Permission"),
        SettingsRowData(R.drawable.ic_delete, "Clear Cache"),
        SettingsRowData(R.drawable.ic_person, "Account"),
        SettingsRowData(R.drawable.ic_info, "About Us"),
        SettingsRowData(R.drawable.ic_logout, string("logout", themeViewModel), "logout")
    )

    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primary)
                .padding(bottom = 20.dp)
        ) {
            IconButton(onClick = onBackClick, modifier = Modifier.padding(8.dp)) {

                Icon(
                    painter = painterResource(id = R.drawable.ic_arrow_back),
                    contentDescription = "Back",
                    tint = Color.White
                )
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
                        .background(MaterialTheme.colorScheme.surface),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_person),
                        contentDescription = "Avatar",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        profile?.name?.takeIf { it.isNotBlank() } ?: "User",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )

                    Text(
                        profile?.phone?.takeIf { it.isNotBlank() } ?: "N/A",
                        color = Color.White,
                        style = MaterialTheme.typography.bodySmall
                    )

                    Text(
                        profile?.email?.takeIf { it.isNotBlank() } ?: "N/A",
                        color = Color.White,
                        style = MaterialTheme.typography.bodySmall
                    )
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
                        when (row.key) {
                            "logout" -> onLogoutClick()
                            "theme" -> showThemeDialog = true
                            "language" -> showLangDialog = true
                            else -> { /* TODO: handle other rows */
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun SettingsRow(row: SettingsRowData, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = row.icon),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(row.title, modifier = Modifier.weight(1f))
        Icon(
            painter = painterResource(id = R.drawable.ic_chevron_right),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outlineVariant
        )
    }
}

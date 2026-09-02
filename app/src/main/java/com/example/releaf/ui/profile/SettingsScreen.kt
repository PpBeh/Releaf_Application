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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import coil.imageLoader
import com.example.releaf.R
import com.example.releaf.ui.theme.AppTheme
import com.example.releaf.ui.theme.string
import com.example.releaf.ui.viewmodel.AppLanguage
import com.example.releaf.ui.viewmodel.ProfileViewModel
import com.example.releaf.ui.viewmodel.ThemeViewModel
import kotlinx.coroutines.launch

internal const val LOG_OUT_LABEL = "Log out"

data class SettingsRowData(val icon: Int, val title: String, val key: String = "")

@OptIn(coil.annotation.ExperimentalCoilApi::class)
@Composable
fun SettingsScreen(
    userId: String,
    onBackClick: () -> Unit,
    onLogoutClick: () -> Unit,
    themeViewModel: ThemeViewModel,
    viewModel: ProfileViewModel,
    onFavouritesClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val currentTheme by themeViewModel.theme.collectAsState()
    val currentLang by themeViewModel.language.collectAsState()
    var showThemeDialog by remember { mutableStateOf(false) }
    var showLangDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showAccountDialog by remember { mutableStateOf(false) }
    val profile by viewModel.profile.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val prefs = remember { context.getSharedPreferences("settings_prefs", android.content.Context.MODE_PRIVATE) }
    var notificationsEnabled by remember { mutableStateOf(prefs.getBoolean("notifications_enabled", true)) }
    var showLogoutConfirm by remember { mutableStateOf(false) }
    val lang by themeViewModel.language.collectAsState()
    fun t(key: String) = com.example.releaf.ui.theme.AppStrings.get(key, lang)

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

    if (showAccountDialog) {
        AlertDialog(
            onDismissRequest = { showAccountDialog = false },
            title = { Text("Account") },
            text = {
                Column {
                    Text("Name: ${profile?.name?.ifBlank { "User" } ?: "User"}")
                    Text("Email: ${profile?.email?.ifBlank { "N/A" } ?: "N/A"}")
                    Text("Phone: ${profile?.phone?.ifBlank { "N/A" } ?: "N/A"}")
                    Text("Title: ${profile?.title?.ifBlank { "Gardener" } ?: "Gardener"}")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Manage your account in Profile.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = { TextButton(onClick = { showAccountDialog = false }) { Text("Close") } }
        )
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text("About Us") },
            text = {
                Column {
                    Text("Releaf — Find clean toilets & trash cans near you.", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Version: ${try { context.packageManager.getPackageInfo(context.packageName, 0).versionName } catch (_: Exception) { "1.0" }}")
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Built with care for community cleanliness. Share feedback via reviews!", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = { TextButton(onClick = { showAboutDialog = false }) { Text("Close") } }
        )
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
        SettingsRowData(R.drawable.ic_favorite, t("favourite_toilets"), "favourites"),
        SettingsRowData(R.drawable.ic_person, t("settings_account"), "account"),
        SettingsRowData(R.drawable.ic_info, t("settings_about"), "about"),
        SettingsRowData(R.drawable.ic_notifications, t("settings_notify"), "notifications"),
        SettingsRowData(R.drawable.ic_delete, t("settings_clear_cache"), "clear_cache"),
        SettingsRowData(R.drawable.ic_refresh, t("settings_updates"), "updates"),
        SettingsRowData(R.drawable.ic_palette, t("theme"), "theme"),
        SettingsRowData(R.drawable.ic_translate, t("language"), "language"),
        SettingsRowData(R.drawable.ic_logout, t("logout"), "logout")
    )

    if (showLogoutConfirm) {
        com.example.releaf.ui.components.LogoutConfirmationDialog(
            onConfirm = {
                showLogoutConfirm = false
                onLogoutClick()
            },
            onDismiss = { showLogoutConfirm = false }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { scaffoldPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(scaffoldPadding)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primary)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp, end = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBackClick, modifier = Modifier.padding(4.dp)) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_arrow_back),
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surface)
                                .clickable { avatarPicker.launch("image/*") },
                            contentAlignment = Alignment.Center
                        ) {
                            val avatarUrl = profile?.avatar_url.orEmpty()
                            if (avatarUrl.isNotBlank()) {
                                androidx.compose.foundation.Image(
                                    painter = coil.compose.rememberAsyncImagePainter(model = avatarUrl),
                                    contentDescription = "Avatar",
                                    modifier = Modifier.size(64.dp).clip(CircleShape),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_person),
                                    contentDescription = "Avatar",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
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
            }

            Text(
                t("settings"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(20.dp)
            )

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(rows) { row ->
                    SettingsRow(
                        row = row,
                        notificationsEnabled = notificationsEnabled,
                        onNotificationsToggle = { enabled ->
                            notificationsEnabled = enabled
                            prefs.edit().putBoolean("notifications_enabled", enabled).apply()
                            scope.launch { snackbarHostState.showSnackbar(if (enabled) "Notifications enabled" else "Notifications disabled") }
                        },
                        onClick = {
                            when (row.key) {
                                "logout" -> showLogoutConfirm = true
                                "theme" -> showThemeDialog = true
                                "language" -> showLangDialog = true
                                "favourites" -> {
                                    if (onFavouritesClick != null) onFavouritesClick()
                                    else scope.launch { snackbarHostState.showSnackbar("Coming soon") }
                                }
                                "account" -> showAccountDialog = true
                                "about" -> showAboutDialog = true
                                "notifications" -> {
                                    val newVal = !notificationsEnabled
                                    notificationsEnabled = newVal
                                    prefs.edit().putBoolean("notifications_enabled", newVal).apply()
                                    scope.launch { snackbarHostState.showSnackbar(if (newVal) "Notifications enabled" else "Notifications disabled") }
                                }
                                "clear_cache" -> {
                                    try {
                                        val imageLoader = context.imageLoader
                                        imageLoader.diskCache?.clear()
                                        imageLoader.memoryCache?.clear()
                                        // Clear osmdroid tile cache references without deleting map tiles aggressively
                                        // (removed duplicate delete in MainActivity)
                                        android.widget.Toast.makeText(context, "Cache cleared", android.widget.Toast.LENGTH_SHORT).show()
                                        scope.launch { snackbarHostState.showSnackbar("Cache cleared ✓") }
                                    } catch (e: Exception) {
                                        scope.launch { snackbarHostState.showSnackbar("Failed to clear cache") }
                                    }
                                }
                                "updates" -> {
                                    val version = try { context.packageManager.getPackageInfo(context.packageName, 0).versionName } catch (_: Exception) { "1.0" }
                                    scope.launch { snackbarHostState.showSnackbar("Releaf v$version • You are up to date ✓") }
                                }
                                else -> scope.launch { snackbarHostState.showSnackbar("Coming soon") }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsRow(
    row: SettingsRowData,
    onClick: () -> Unit,
    notificationsEnabled: Boolean = true,
    onNotificationsToggle: (Boolean) -> Unit = {}
) {
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
        if (row.key == "notifications") {
            Switch(checked = notificationsEnabled, onCheckedChange = { onNotificationsToggle(it) })
        } else {
            Icon(
                painter = painterResource(id = R.drawable.ic_chevron_right),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outlineVariant
            )
        }
    }
}

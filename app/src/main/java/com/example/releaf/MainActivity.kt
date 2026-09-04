package com.example.releaf

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.releaf.data.remote.DeepLinkHolder
import com.example.releaf.data.repository.SessionState
import com.example.releaf.navigation.ReleafNavGraph
import com.example.releaf.navigation.Screen
import com.example.releaf.ui.components.BottomNavBar
import com.example.releaf.ui.theme.AppStrings
import com.example.releaf.ui.theme.ReleafTheme
import com.example.releaf.ui.viewmodel.AuthViewModel
import com.example.releaf.ui.viewmodel.ThemeViewModel
import com.example.releaf.utils.NotificationHelper
import org.osmdroid.config.Configuration

class MainActivity : ComponentActivity() {
    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().apply {
            userAgentValue = "Releaf/1.0"
            osmdroidBasePath = filesDir
            osmdroidTileCache = filesDir.resolve("tiles").also {
                it.mkdirs()
                // Do not delete cache on every launch — keep tiles for offline use
            }
        }
        handleDeepLink(intent)
        handleNotificationTap(intent)
        setContent {
            val themeViewModel: ThemeViewModel = viewModel()
            val appTheme by themeViewModel.theme.collectAsState()
            ReleafTheme(appTheme = appTheme) {
                ReleafApp(themeViewModel = themeViewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleDeepLink(intent)
        handleNotificationTap(intent)
        // Deep link arriving while the app is already running
        authViewModel.handleDeepLink()
    }

    private fun handleNotificationTap(intent: Intent?) {
        if (intent?.getBooleanExtra(
                NotificationHelper.EXTRA_OPEN_NOTIFICATIONS,
                false
            ) == true
        ) {
            DeepLinkHolder.requestOpenNotifications()
        }
    }

    private fun handleDeepLink(intent: Intent?) {
        val data = intent?.data ?: return
        if (data.scheme == "releaf" && data.host == "login-callback") {
            // Support both fragment (#access_token=...) and query (?access_token=...) forms
            val params = mutableMapOf<String, String>()
            listOfNotNull(data.encodedFragment, data.encodedQuery).forEach { raw ->
                raw.split("&").forEach { part ->
                    val kv = part.split("=", limit = 2)
                    if (kv.size == 2 && params[kv[0]] == null) {
                        params[kv[0]] = Uri.decode(kv[1])
                    }
                }
            }
            // Raw query params as fallback
            if (params["access_token"] == null) {
                data.getQueryParameter("access_token")
                    ?.let { params["access_token"] = Uri.decode(it) }
                data.getQueryParameter("refresh_token")
                    ?.let { params["refresh_token"] = Uri.decode(it) }
                data.getQueryParameter("type")?.let { params["type"] = Uri.decode(it) }
            }
            if (params["access_token"] != null && params["refresh_token"] != null) {
                DeepLinkHolder.setTokens(
                    params.getValue("access_token"),
                    params.getValue("refresh_token"),
                    params["type"]
                )
            }
        } else if (data.scheme == "releaf" && data.host == "poi") {
            val poiId = data.pathSegments.firstOrNull() ?: data.lastPathSegment ?: ""
            if (poiId.isNotBlank()) {
                DeepLinkHolder.pendingPoiId = poiId
            } else {
                // Fallback: try query param id=
                data.getQueryParameter("id")?.let { id ->
                    if (id.isNotBlank()) DeepLinkHolder.pendingPoiId = id
                }
            }
        }
    }
}

@Composable
fun ReleafApp(themeViewModel: ThemeViewModel) {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = viewModel()
    val context = LocalContext.current

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val isChecking by authViewModel.isCheckingSession.collectAsState()
    val session by authViewModel.session.collectAsState()
    val pendingPoiId by DeepLinkHolder.pendingPoiIdFlow.collectAsState()
    val openNotifications by DeepLinkHolder.openNotificationsFlow.collectAsState()
    val lang by themeViewModel.language.collectAsState()
    fun t(key: String) = AppStrings.get(key, lang)

    LaunchedEffect(pendingPoiId, isChecking, session) {
        if (!isChecking && pendingPoiId != null && session is SessionState.LoggedIn) {
            if (currentRoute != Screen.Map.route) {
                navController.navigate(Screen.Map.route) {
                    launchSingleTop = true
                }
            }
        }
    }

    LaunchedEffect(openNotifications, isChecking, session) {
        if (!isChecking && openNotifications && session is SessionState.LoggedIn) {
            if (currentRoute != Screen.Map.route) {
                navController.navigate(Screen.Map.route) {
                    launchSingleTop = true
                }
            }
        }
    }

    // Logging out (or tapping a stale notification while logged out) must never
    // leave a shade entry or a pending sheet-open behind for the next account.
    LaunchedEffect(session) {
        if (session is SessionState.LoggedOut) {
            NotificationHelper.cancelSummary(context)
            DeepLinkHolder.consumeOpenNotifications()
        }
    }

    // Track internet connectivity and ask the user to turn Wi-Fi/mobile data on.
    var isOnline by remember { mutableStateOf(true) }
    var offlinePromptShown by remember {
        mutableStateOf(
            false
        )
    }
    DisposableEffect(context) {
        val cm =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                isOnline = true
            }

            override fun onLost(network: Network) {
                isOnline = cm?.activeNetwork == null
            }
        }
        try {
            cm?.registerDefaultNetworkCallback(callback)
        } catch (_: Exception) {
        }
        onDispose {
            try {
                cm?.unregisterNetworkCallback(callback)
            } catch (_: Exception) {
            }
        }
    }

    if (!isOnline && !offlinePromptShown) {
        AlertDialog(
            onDismissRequest = { offlinePromptShown = true },
            title = { Text(t("no_internet_title")) },
            text = { Text(t("no_internet_text")) },
            confirmButton = {
                TextButton(onClick = {
                    offlinePromptShown = true
                    try {
                        context.startActivity(Intent(Settings.ACTION_WIRELESS_SETTINGS))
                    } catch (_: Exception) {
                    }
                }) {
                    Text(t("open_settings"))
                }
            },
            dismissButton = {
                TextButton(onClick = { offlinePromptShown = true }) {
                    Text(t("ok"))
                }
            }
        )
    }
    if (isOnline) {
        offlinePromptShown = false
    }

    val isAuthScreen = currentRoute == Screen.Login.route ||
            currentRoute == Screen.Register.route ||
            currentRoute == Screen.ForgotPassword.route ||
            currentRoute == Screen.SetNewPassword.route ||
            currentRoute?.startsWith(Screen.Verify.route.substringBefore("/{")) == true

    val showBottomBar = !isChecking && !isAuthScreen

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                BottomNavBar(navController = navController, themeViewModel = themeViewModel)
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            ReleafNavGraph(
                navController = navController,
                authViewModel = authViewModel,
                themeViewModel = themeViewModel
            )
        }
    }
}

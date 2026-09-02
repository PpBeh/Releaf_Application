package com.example.releaf

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.releaf.data.remote.DeepLinkHolder
import com.example.releaf.navigation.ReleafNavGraph
import com.example.releaf.navigation.Screen
import com.example.releaf.ui.components.BottomNavBar
import com.example.releaf.ui.theme.AppTheme
import com.example.releaf.ui.theme.ReleafTheme
import com.example.releaf.ui.viewmodel.AuthViewModel
import com.example.releaf.ui.viewmodel.ThemeViewModel
import org.osmdroid.config.Configuration
import androidx.activity.viewModels

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
        // Deep link arriving while the app is already running
        authViewModel.handleDeepLink()
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
                        params[kv[0]] = android.net.Uri.decode(kv[1])
                    }
                }
            }
            // Raw query params as fallback
            if (params["access_token"] == null) {
                data.getQueryParameter("access_token")?.let { params["access_token"] = android.net.Uri.decode(it) }
                data.getQueryParameter("refresh_token")?.let { params["refresh_token"] = android.net.Uri.decode(it) }
                data.getQueryParameter("type")?.let { params["type"] = android.net.Uri.decode(it) }
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

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val isChecking by authViewModel.isCheckingSession.collectAsState()
    val session by authViewModel.session.collectAsState()
    val pendingPoiId by DeepLinkHolder.pendingPoiIdFlow.collectAsState()

    androidx.compose.runtime.LaunchedEffect(pendingPoiId, isChecking, session) {
        if (!isChecking && pendingPoiId != null && session is com.example.releaf.data.repository.SessionState.LoggedIn) {
            if (currentRoute != Screen.Map.route) {
                navController.navigate(Screen.Map.route) {
                    launchSingleTop = true
                }
            }
        }
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

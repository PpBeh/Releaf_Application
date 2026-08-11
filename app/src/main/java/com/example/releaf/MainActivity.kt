package com.example.releaf

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.releaf.navigation.ReleafNavGraph
import com.example.releaf.navigation.Screen
import com.example.releaf.ui.components.BottomNavBar
import com.example.releaf.ui.theme.ReleafTheme
import com.example.releaf.ui.viewmodel.AuthViewModel
import org.osmdroid.config.Configuration

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().apply {
            userAgentValue = "ReleafToiletFinder/1.0"
            osmdroidBasePath = filesDir
            osmdroidTileCache = filesDir.resolve("tiles").also {
                it.mkdirs()
                it.listFiles()?.forEach { f -> f.deleteRecursively() }
            }
        }
        setContent {
            ReleafTheme {
                ReleafApp()
            }
        }
    }
}

@Composable
fun ReleafApp() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = viewModel()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = currentRoute != Screen.Login.route && currentRoute != Screen.Register.route

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                BottomNavBar(navController = navController)
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            ReleafNavGraph(navController = navController, authViewModel = authViewModel)
        }
    }
}

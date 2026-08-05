package com.example.releaf

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.example.releaf.navigation.ReleafNavGraph
import com.example.releaf.ui.components.BottomNavBar
import com.example.releaf.ui.theme.ReleafTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
    Scaffold(
        bottomBar = { BottomNavBar(navController = navController) }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            ReleafNavGraph(navController = navController)
        }
    }
}
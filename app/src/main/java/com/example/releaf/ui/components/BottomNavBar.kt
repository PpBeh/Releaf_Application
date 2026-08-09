package com.example.releaf.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Yard
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.releaf.navigation.Screen
import com.example.releaf.navigation.toggleGardenSection

@Composable
fun BottomNavBar(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val onGardenSection = currentRoute == Screen.Garden.route || currentRoute == Screen.GardenPlot.route

    Box(modifier = Modifier.fillMaxWidth()) {
        NavigationBar {
            // Tapping this while already in the Garden section flips between
            // the home garden and the garden plot instead of just re-opening home.
            NavigationBarItem(
                selected = onGardenSection,
                onClick = { navController.toggleGardenSection() },
                icon = { Icon(Icons.Default.Yard, contentDescription = "Garden") },
                label = { Text("Garden") }
            )
            NavigationBarItem(
                selected = currentRoute == Screen.Activity.route,
                onClick = { navController.navigate(Screen.Activity.route) { launchSingleTop = true } },
                icon = { Icon(Icons.Default.MonitorHeart, contentDescription = "Activity") },
                label = { Text("Activity") }
            )
            // Reserved middle slot — the real Map button floats above this space
            NavigationBarItem(
                selected = false,
                onClick = {},
                enabled = false,
                icon = {},
                label = {}
            )
            NavigationBarItem(
                selected = currentRoute == Screen.Rewards.route,
                onClick = { navController.navigate(Screen.Rewards.route) { launchSingleTop = true } },
                icon = { Icon(Icons.Default.EmojiEvents, contentDescription = "Rewards") },
                label = { Text("Rewards") }
            )
            NavigationBarItem(
                selected = currentRoute == Screen.Profile.route,
                onClick = { navController.navigate(Screen.Profile.createRoute()) { launchSingleTop = true } },
                icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                label = { Text("Profile") }
            )
        }

        // Map is the app's main function, so it's raised in a blue circle that
        // pokes above the bar to draw the eye, instead of sitting flush with
        // the other four tabs.
        FloatingActionButton(
            onClick = { navController.navigate(Screen.Map.route) { launchSingleTop = true } },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-20).dp),
            shape = CircleShape,
            containerColor = Color(0xFF2196F3),
            contentColor = Color.White
        ) {
            Icon(Icons.Default.Map, contentDescription = "Map")
        }
    }
}
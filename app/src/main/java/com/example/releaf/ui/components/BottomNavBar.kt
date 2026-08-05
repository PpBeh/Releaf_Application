package com.example.releaf.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Yard
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.releaf.navigation.Screen

private data class BottomNavEntry(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val onClick: (NavHostController) -> Unit
)

@Composable
fun BottomNavBar(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val entries = listOf(
        BottomNavEntry(Screen.Garden.route, "Garden", Icons.Default.Yard) {
            it.navigate(Screen.Garden.route) { launchSingleTop = true }
        },
        BottomNavEntry(Screen.Activity.route, "Activity", Icons.Default.MonitorHeart) {
            it.navigate(Screen.Activity.route) { launchSingleTop = true }
        },
        BottomNavEntry(Screen.Map.route, "", Icons.Default.Map) {
            it.navigate(Screen.Map.route) { launchSingleTop = true }
        },
        BottomNavEntry(Screen.Rewards.route, "Rewards", Icons.Default.EmojiEvents) {
            it.navigate(Screen.Rewards.route) { launchSingleTop = true }
        },
        BottomNavEntry(Screen.Profile.route, "Profile", Icons.Default.Person) {
            it.navigate(Screen.Profile.createRoute()) { launchSingleTop = true }
        }
    )

    NavigationBar {
        entries.forEach { entry ->
            NavigationBarItem(
                selected = currentRoute == entry.route,
                onClick = { entry.onClick(navController) },
                icon = { Icon(entry.icon, contentDescription = entry.label.ifEmpty { "Map" }) },
                label = { Text(entry.label) }
            )
        }
    }
}
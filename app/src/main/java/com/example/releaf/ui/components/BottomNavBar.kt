package com.example.releaf.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MonitorHeart
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.releaf.R
import com.example.releaf.navigation.Screen
import com.example.releaf.navigation.toggleGardenSection
import com.example.releaf.ui.theme.string
import com.example.releaf.ui.viewmodel.ThemeViewModel

@Composable
fun BottomNavBar(navController: NavHostController, themeViewModel: ThemeViewModel) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val onGardenSection = currentRoute?.startsWith("garden") == true

    Box(modifier = Modifier.fillMaxWidth()) {
        NavigationBar {
            // Tapping this while already in the Garden section flips between
            // the home garden and the garden plot instead of just re-opening home.
            NavigationBarItem(
                selected = onGardenSection,
                onClick = { navController.toggleGardenSection() },
                icon = {
                    Icon(
                        Icons.Default.Yard,
                        contentDescription = string("garden", themeViewModel)
                    )
                },
                label = { Text(string("garden", themeViewModel)) }
            )
            NavigationBarItem(
                selected = currentRoute == Screen.Activity.route,
                onClick = {
                    if (currentRoute == Screen.Activity.route) return@NavigationBarItem
                    navController.navigate(Screen.Activity.route) {
                        launchSingleTop = true
                    }
                },
                icon = {
                    Icon(
                        Icons.Default.MonitorHeart,
                        contentDescription = string("activity", themeViewModel)
                    )
                },
                label = { Text(string("activity", themeViewModel)) }
            )
            // Reserved middle slot — the real Map button floats above this space
            NavigationBarItem(
                selected = false,
                onClick = {},
                enabled = false,
                icon = {},
                label = {}
            )
            // Rewards
            NavigationBarItem(
                selected = currentRoute == Screen.Rewards.route,
                onClick = {
                    if (currentRoute == Screen.Rewards.route) return@NavigationBarItem
                    navController.navigate(Screen.Rewards.route) {
                        launchSingleTop = true
                    }
                },
                icon = {
                    Icon(
                        Icons.Default.EmojiEvents,
                        contentDescription = string("rewards", themeViewModel)
                    )
                },
                label = { Text(string("rewards", themeViewModel)) }
            )
            // Profile
            NavigationBarItem(
                selected = currentRoute == Screen.Profile.route,
                onClick = {
                    if (currentRoute == Screen.Profile.route) return@NavigationBarItem
                    navController.navigate(Screen.Profile.createRoute()) {
                        launchSingleTop = true
                    }
                },
                icon = {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_person),
                        contentDescription = string("profile", themeViewModel)
                    )
                },
                label = { Text(string("profile", themeViewModel)) }
            )
        }

        // Map is the app's main function, so it's raised in a blue circle that
        // pokes above the bar to draw the eye, instead of sitting flush with
        // the other four tabs.
        FloatingActionButton(
            onClick = {
                if (currentRoute == Screen.Map.route) return@FloatingActionButton
                navController.navigate(Screen.Map.route) {
                    launchSingleTop = true
                }
            },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-10).dp),
            shape = CircleShape,
            containerColor = Color(0xFF2196F3),
            contentColor = Color.White
        ) {
            Icon(Icons.Default.Map, contentDescription = string("map", themeViewModel))
        }
    }
}

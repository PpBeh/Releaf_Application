package com.example.releaf.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.releaf.ui.activity.ActivityScreen
import com.example.releaf.ui.garden.GardenPlotScreen
import com.example.releaf.ui.garden.GardenScreen
import com.example.releaf.ui.map.CommentScreen
import com.example.releaf.ui.map.DirectionScreen
import com.example.releaf.ui.map.MapScreen
import com.example.releaf.ui.profile.ProfileScreen
import com.example.releaf.ui.profile.SettingsScreen
import com.example.releaf.ui.rewards.RewardsScreen

@Composable
fun ReleafNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Map.route
    ) {
        composable(Screen.Garden.route) {
            GardenScreen(
                onHouseClick = { navController.navigate(Screen.GardenPlot.route) }
            )
        }
        composable(Screen.GardenPlot.route) {
            GardenPlotScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(Screen.Activity.route) {
            ActivityScreen()
        }
        composable(Screen.Map.route) {
            MapScreen(
                onDirectionClick = { poiId -> navController.navigate(Screen.Direction.createRoute(poiId)) },
                onCommentClick = { poiId -> navController.navigate(Screen.Comment.createRoute(poiId)) }
            )
        }
        composable(
            route = Screen.Direction.route,
            arguments = listOf(navArgument("poiId") { type = NavType.StringType })
        ) { backStackEntry ->
            val poiId = backStackEntry.arguments?.getString("poiId").orEmpty()
            DirectionScreen(
                poiId = poiId,
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(
            route = Screen.Comment.route,
            arguments = listOf(navArgument("poiId") { type = NavType.StringType })
        ) { backStackEntry ->
            val poiId = backStackEntry.arguments?.getString("poiId").orEmpty()
            CommentScreen(
                poiId = poiId,
                onBackClick = { navController.popBackStack() },
                onAvatarClick = { userId -> navController.navigate(Screen.Profile.createRoute(userId)) }
            )
        }
        composable(Screen.Rewards.route) {
            RewardsScreen()
        }
        composable(
            route = Screen.Profile.route,
            arguments = listOf(navArgument(Screen.Profile.ARG_USER_ID) { type = NavType.StringType })
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString(Screen.Profile.ARG_USER_ID) ?: Screen.Profile.ME
            ProfileScreen(
                userId = userId,
                onSettingsClick = { navController.navigate(Screen.Settings.route) }
            )
        }
        composable(Screen.Settings.route) {
            SettingsScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}
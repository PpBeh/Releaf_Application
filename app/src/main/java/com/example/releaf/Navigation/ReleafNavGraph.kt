package com.example.releaf.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.releaf.ui.activity.ActivityScreen
import com.example.releaf.ui.auth.LoginScreen
import com.example.releaf.ui.auth.RegisterScreen
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
        startDestination = Screen.Login.route
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginClick = { _, _ ->
                    // TODO: validate credentials / call your auth API before entering the app
                    navController.enterAppAfterAuth()
                },
                onRegisterClick = { navController.navigate(Screen.Register.route) }
            )
        }
        composable(Screen.Register.route) {
            RegisterScreen(
                onRegisterClick = { _, _, _ ->
                    // TODO: create the account / call your auth API before entering the app
                    navController.enterAppAfterAuth()
                },
                onLoginClick = { navController.popBackStack() }
            )
        }
        composable(Screen.Garden.route) {
            GardenScreen(
                onHouseClick = { navController.navigateToGardenSection(toPlot = true) }
            )
        }
        composable(Screen.GardenPlot.route) {
            GardenPlotScreen(
                onBackClick = { navController.navigateToGardenSection(toPlot = false) }
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
                onBackClick = { navController.popBackStack() },
                onLogoutClick = { navController.logout() }
            )
        }
    }
}

/**
 * Garden and Garden Plot are treated as peer "pages" you switch between —
 * via the house tap, the back arrow, or re-tapping the Garden nav icon —
 * not a parent/child drill-in. Each switch replaces the other on the back
 * stack instead of stacking on top of it, so toggling back and forth
 * doesn't pile up back-stack entries.
 */
fun NavHostController.navigateToGardenSection(toPlot: Boolean) {
    val current = currentBackStackEntry?.destination?.route
    val target = if (toPlot) Screen.GardenPlot.route else Screen.Garden.route
    val inGardenSection = current == Screen.Garden.route || current == Screen.GardenPlot.route

    navigate(target) {
        if (inGardenSection && current != null) {
            popUpTo(current) { inclusive = true }
        }
        launchSingleTop = true
    }
}

fun NavHostController.toggleGardenSection() {
    val current = currentBackStackEntry?.destination?.route
    navigateToGardenSection(toPlot = current == Screen.Garden.route)
}

/** Clears Login/Register off the back stack once sign-in or sign-up succeeds. */
fun NavHostController.enterAppAfterAuth() {
    navigate(Screen.Map.route) {
        popUpTo(Screen.Login.route) { inclusive = true }
        launchSingleTop = true
    }
}

/** Wipes the whole back stack and returns to Login. */
fun NavHostController.logout() {
    navigate(Screen.Login.route) {
        popUpTo(graph.id) { inclusive = true }
        launchSingleTop = true
    }
}
package com.example.releaf.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.releaf.data.repository.SessionState
import com.example.releaf.ui.activity.ActivityScreen
import com.example.releaf.ui.auth.ForgotPasswordScreen
import com.example.releaf.ui.auth.LoginScreen
import com.example.releaf.ui.auth.RegisterScreen
import com.example.releaf.ui.auth.SetNewPasswordScreen
import com.example.releaf.ui.auth.VerificationScreen
import com.example.releaf.ui.garden.GardenPlotScreen
import com.example.releaf.ui.garden.GardenScreen
import com.example.releaf.ui.map.CommentScreen
import com.example.releaf.ui.map.DirectionScreen
import com.example.releaf.ui.map.MapScreen
import com.example.releaf.ui.profile.FavouritesScreen
import com.example.releaf.ui.profile.ProfileScreen
import com.example.releaf.ui.profile.SettingsScreen
import com.example.releaf.ui.rewards.RewardsScreen
import com.example.releaf.ui.viewmodel.ActivityViewModel
import com.example.releaf.ui.viewmodel.AuthViewModel
import com.example.releaf.ui.viewmodel.CommentViewModel
import com.example.releaf.ui.viewmodel.FavouritesViewModel
import com.example.releaf.ui.viewmodel.GardenViewModel
import com.example.releaf.ui.viewmodel.MapViewModel
import com.example.releaf.ui.viewmodel.ProfileViewModel
import com.example.releaf.ui.viewmodel.RewardsViewModel

@Composable
fun ReleafNavGraph(
    navController: NavHostController,
    authViewModel: AuthViewModel
) {
    val session by authViewModel.session.collectAsState()
    val authUiState by authViewModel.uiState.collectAsState()
    val isChecking by authViewModel.isCheckingSession.collectAsState()
    val needsPasswordReset by authViewModel.needsPasswordReset.collectAsState()

    if (isChecking) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    LaunchedEffect(needsPasswordReset) {
        if (needsPasswordReset) {
            navController.navigate(Screen.SetNewPassword.route) {
                popUpTo(Screen.Login.route) { inclusive = true }
            }
        }
    }

    val startDestination = when (session) {
        is SessionState.LoggedIn -> Screen.Map.route
        is SessionState.LoggedOut -> Screen.Login.route
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Login.route) {
            LaunchedEffect(authUiState.isSuccess) {
                if (authUiState.isSuccess) {
                    navController.enterAppAfterAuth()
                }
            }
            LoginScreen(
                isLoading = authUiState.isLoading,
                error = authUiState.error,
                onLoginClick = { email, password ->
                    authViewModel.login(email, password)
                },
                onRegisterClick = { navController.navigate(Screen.Register.route) },
                onForgotPasswordClick = { navController.navigate(Screen.ForgotPassword.route) },
                onClearError = { authViewModel.clearError() }
            )
        }
        composable(Screen.ForgotPassword.route) {
            val resetState by authViewModel.resetState.collectAsState()
            ForgotPasswordScreen(
                isLoading = resetState.isLoading,
                isSuccess = resetState.isSuccess,
                error = resetState.error,
                onSendClick = { email -> authViewModel.sendResetEmail(email) },
                onBackClick = {
                    authViewModel.clearResetState()
                    navController.popBackStack()
                }
            )
        }
        composable(Screen.SetNewPassword.route) {
            SetNewPasswordScreen(
                isLoading = authUiState.isLoading,
                error = authUiState.error,
                onSubmit = { password ->
                    authViewModel.updatePassword(password) {
                        navController.logout()
                    }
                }
            )
        }
        composable(Screen.Register.route) {
            LaunchedEffect(authUiState.isSuccess) {
                if (authUiState.isSuccess) {
                    navController.navigate(Screen.Verify.createRoute(authUiState.registeredEmail))
                }
            }
            RegisterScreen(
                isLoading = authUiState.isLoading,
                error = authUiState.error,
                onRegisterClick = { name, email, password ->
                    authViewModel.register(name, email, password)
                },
                onLoginClick = { navController.popBackStack() },
                onClearError = { authViewModel.clearError() }
            )
        }
        composable(
            route = Screen.Verify.route,
            arguments = listOf(navArgument("email") { type = NavType.StringType })
        ) { backStackEntry ->
            val email = backStackEntry.arguments?.getString("email").orEmpty()
            LaunchedEffect(authUiState.isSuccess) {
                if (authUiState.isSuccess) {
                    navController.enterAppAfterAuth()
                }
            }
            VerificationScreen(
                email = email,
                isLoading = authUiState.isLoading,
                error = authUiState.error,
                onVerifyClick = { code ->
                    authViewModel.verifyEmail(email, code)
                },
                onResendClick = {
                    authViewModel.resendVerification(email)
                },
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(Screen.Garden.route) {
            val gardenViewModel: GardenViewModel = viewModel()
            val userId = (session as? SessionState.LoggedIn)?.userId ?: return@composable
            GardenScreen(
                viewModel = gardenViewModel,
                userId = userId,
                onHouseClick = { navController.navigateToGardenSection(toPlot = true) }
            )
        }
        composable(Screen.GardenPlot.route) {
            val gardenViewModel: GardenViewModel = viewModel()
            val userId = (session as? SessionState.LoggedIn)?.userId ?: return@composable
            GardenPlotScreen(
                viewModel = gardenViewModel,
                userId = userId,
                onBackClick = { navController.navigateToGardenSection(toPlot = false) }
            )
        }
        composable(Screen.Activity.route) {
            val activityViewModel: ActivityViewModel = viewModel()
            val userId = (session as? SessionState.LoggedIn)?.userId ?: return@composable
            ActivityScreen(
                viewModel = activityViewModel,
                userId = userId
            )
        }
        composable(Screen.Map.route) {
            val mapViewModel: MapViewModel = viewModel()
            MapScreen(
                viewModel = mapViewModel,
                onDirectionClick = { poiId -> navController.navigate(Screen.Direction.createRoute(poiId)) },
                onCommentClick = { poiId -> navController.navigate(Screen.Comment.createRoute(poiId)) },
                currentUserId = (session as? SessionState.LoggedIn)?.userId ?: ""
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
            val commentViewModel: CommentViewModel = viewModel()
            val userId = (session as? SessionState.LoggedIn)?.userId ?: return@composable
            CommentScreen(
                poiId = poiId,
                viewModel = commentViewModel,
                currentUserId = userId,
                onBackClick = { navController.popBackStack() },
                onAvatarClick = { userId -> navController.navigate(Screen.Profile.createRoute(userId)) }
            )
        }
        composable(Screen.Rewards.route) {
            val rewardsViewModel: RewardsViewModel = viewModel()
            val userId = (session as? SessionState.LoggedIn)?.userId ?: return@composable
            RewardsScreen(
                viewModel = rewardsViewModel,
                userId = userId
            )
        }
        composable(
            route = Screen.Profile.route,
            arguments = listOf(navArgument(Screen.Profile.ARG_USER_ID) { type = NavType.StringType })
        ) { backStackEntry ->
            val paramUserId = backStackEntry.arguments?.getString(Screen.Profile.ARG_USER_ID) ?: Screen.Profile.ME
            val currentUserId = (session as? SessionState.LoggedIn)?.userId ?: ""
            val actualUserId = if (paramUserId == Screen.Profile.ME) currentUserId else paramUserId
            val profileViewModel: ProfileViewModel = viewModel()
            ProfileScreen(
                userId = actualUserId,
                currentUserId = currentUserId,
                viewModel = profileViewModel,
                onFavouritesClick = { navController.navigate(Screen.Favourites.route) },
                onLogoutClick = {
                    authViewModel.logout()
                    navController.logout()
                }
            )
        }
        composable(Screen.Favourites.route) {
            val favouritesViewModel: FavouritesViewModel = viewModel()
            val userId = (session as? SessionState.LoggedIn)?.userId ?: ""
            FavouritesScreen(
                viewModel = favouritesViewModel,
                userId = userId,
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(Screen.Settings.route) {
            SettingsScreen(
                onBackClick = { navController.popBackStack() },
                onLogoutClick = {
                    authViewModel.logout()
                    navController.logout()
                }
            )
        }
    }
}

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

fun NavHostController.enterAppAfterAuth() {
    navigate(Screen.Map.route) {
        popUpTo(Screen.Login.route) { inclusive = true }
        launchSingleTop = true
    }
}

fun NavHostController.logout() {
    navigate(Screen.Login.route) {
        popUpTo(graph.id) { inclusive = true }
        launchSingleTop = true
    }
}

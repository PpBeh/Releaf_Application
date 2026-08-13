package com.example.releaf.navigation

sealed class Screen(val route: String) {
    data object Login : Screen("login")
    data object Register : Screen("register")
    data object ForgotPassword : Screen("forgot_password")
    data object SetNewPassword : Screen("set_new_password")
    data object Verify : Screen("verify/{email}") {
        fun createRoute(email: String) = "verify/$email"
    }

    data object Garden : Screen("garden")
    data object GardenPlot : Screen("garden_plot")
    data object Activity : Screen("activity")
    data object Map : Screen("map")

    data object Direction : Screen("direction/{poiId}") {
        fun createRoute(poiId: String) = "direction/$poiId"
    }

    data object Comment : Screen("comment/{poiId}") {
        fun createRoute(poiId: String) = "comment/$poiId"
    }

    data object Rewards : Screen("rewards")

    data object Profile : Screen("profile/{userId}") {
        const val ARG_USER_ID = "userId"
        const val ME = "me"
        fun createRoute(userId: String = ME) = "profile/$userId"
    }

    data object Settings : Screen("settings")
    data object Favourites : Screen("favourites")
}
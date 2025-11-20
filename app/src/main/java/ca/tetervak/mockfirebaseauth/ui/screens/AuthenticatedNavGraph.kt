package ca.tetervak.mockfirebaseauth.ui.screens

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import ca.tetervak.mockfirebaseauth.repository.MockDataRepository
import ca.tetervak.mockfirebaseauth.repository.MockFirebaseUser
import ca.tetervak.mockfirebaseauth.ui.screens.home.HomeScreen
import ca.tetervak.mockfirebaseauth.ui.screens.home.HomeViewModel
import ca.tetervak.mockfirebaseauth.ui.screens.profile.ProfileScreen
import ca.tetervak.mockfirebaseauth.ui.screens.settings.SettingsScreen
import ca.tetervak.mockfirebaseauth.ui.screens.upgrade.UpgradeAccountScreen

@Composable
fun AuthenticatedNavGraph(
    // FIX: Changed from NavController to NavHostController to satisfy the NavHost composable.
    navController: NavHostController,
    user: MockFirebaseUser,
    authViewModel: MockAuthViewModel,
    snackbarHostState: SnackbarHostState
) {

    // FIX: Use LaunchedEffect to reset the navigation stack whenever this graph is composed
    // (i.e., immediately after a login, including anonymous).
    // This prevents stale destinations (like Profile) from being reused after logout/login cycle.
    LaunchedEffect(Unit) {
        // Pop everything off the stack and navigate to home_route,
        // ensuring home_route is the only destination on the stack.
        // We use popUpTo("home_route") with inclusive=true to effectively clear and set the new root.
        navController.navigate("home_route") {
            popUpTo("home_route") { inclusive = true }
        }
    }


    NavHost(
        navController = navController,
        startDestination = "home_route"
    ) {
        composable("home_route") {
            val homeViewModel = remember {
                HomeViewModel(
                    authViewModel = authViewModel,
                    dataRepository = MockDataRepository()
                )
            }
            HomeScreen(user = user, viewModel = homeViewModel, navController = navController)
        }
        composable("profile_route") {
            ProfileScreen(user = user, viewModel = authViewModel, navController = navController)
        }
        composable("settings_route") {
            SettingsScreen()
        }
        // Updated Upgrade route
        composable("upgrade_route") {
            UpgradeAccountScreen(
                authViewModel = authViewModel,
                navController = navController,
                snackbarHostState = snackbarHostState
            )
        }
    }
}
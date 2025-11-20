package ca.tetervak.mockfirebaseauth.ui.screens

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import ca.tetervak.mockfirebaseauth.ui.screens.login.LoginScreen
import ca.tetervak.mockfirebaseauth.ui.screens.register.RegisterScreen

@Composable
fun UnauthenticatedNavGraph(
    authViewModel: MockAuthViewModel,
    snackbarHostState: SnackbarHostState
) {
    // Nested NavController for Login/Register flow
    val flowNavController = rememberNavController()

    NavHost(
        navController = flowNavController,
        startDestination = "login_route"
    ) {
        composable("login_route") {
            LoginScreen(
                navController = flowNavController,
                authViewModel = authViewModel,
                snackbarHostState = snackbarHostState
            )
        }
        composable("register_route") {
            RegisterScreen(
                navController = flowNavController,
                authViewModel = authViewModel
            )
        }
    }
}
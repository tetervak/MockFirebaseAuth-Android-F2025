package ca.tetervak.mockfirebaseauth.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.rememberNavController

@Preview(showBackground = true)
@Composable
fun AppRootScreen() {
    val mockAuthViewModel = remember { MockAuthViewModel() }
    val authState by mockAuthViewModel.authState.collectAsState()

    // rememberNavController returns NavHostController, which is the necessary type
    val navController = rememberNavController()

    // NEW: SnackBar Host State
    val snackbarHostState = remember { SnackbarHostState() }

    // NEW: Observe the transient error channel from the ViewModel
    LaunchedEffect(Unit) {
        mockAuthViewModel.errorFlow.collect { message ->
            snackbarHostState.showSnackbar(
                message = message,
                actionLabel = "Dismiss",
                withDismissAction = true
            )
        }
    }

    // This logic mimics the central NavHost that observes global AuthState
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }, // Display the SnackBar
        bottomBar = {
            if (authState is MockAuthViewModel.AuthState.Authenticated) {
                NavigationBar {
                    NavigationBarItem(
                        icon = { Icon(Icons.Filled.Home, contentDescription = "Home") },
                        label = { Text("Home") },
                        selected = navController.currentDestination?.route == "home_route",
                        onClick = {
                            navController.navigate("home_route") {
                                popUpTo("home_route") {
                                    inclusive = true
                                }
                            }
                        }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Filled.Person, contentDescription = "Profile") },
                        label = { Text("Profile") },
                        selected = navController.currentDestination?.route == "profile_route",
                        onClick = { navController.navigate("profile_route") { popUpTo("home_route") } }
                    )
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.Companion.padding(padding)) {
            when (authState) {
                is MockAuthViewModel.AuthState.Loading -> {
                    Box(
                        Modifier.Companion.fillMaxSize(),
                        contentAlignment = Alignment.Companion.Center
                    ) {
                        CircularProgressIndicator()
                        Text("Initializing...")
                    }
                }

                is MockAuthViewModel.AuthState.Unauthenticated -> {
                    UnauthenticatedNavGraph(
                        authViewModel = mockAuthViewModel,
                        snackbarHostState = snackbarHostState
                    )
                }

                is MockAuthViewModel.AuthState.Authenticated -> {
                    val user = (authState as MockAuthViewModel.AuthState.Authenticated).user

                    AuthenticatedNavGraph(
                        navController = navController,
                        user = user,
                        authViewModel = mockAuthViewModel,
                        snackbarHostState = snackbarHostState
                    )
                }
            }
        }
    }
}
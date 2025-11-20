package ca.tetervak.mockfirebaseauth.ui.screens.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import ca.tetervak.mockfirebaseauth.ui.screens.MockAuthViewModel

@Composable
fun LoginScreen(
    navController: NavController, // Generic NavController is fine here
    authViewModel: MockAuthViewModel,
    snackbarHostState: SnackbarHostState
) {
    var email by remember { mutableStateOf("") }

    // The user must now manually sign in or click the Anonymous button below.

    Column(Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center) {

        Text("Sign in or use anonymous mode", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(32.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email (Use 'user@test.com' or 'fail@test.com')") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))

        // Button for permanent sign-in
        Button(onClick = {
            authViewModel.performSignIn(email)
        }, enabled = email.isNotBlank(), modifier = Modifier.fillMaxWidth().height(56.dp)) {
            Text("Sign In (Permanent User)")
        }

        Spacer(Modifier.height(16.dp))

        // New Button: Option for Anonymous Sign-In
        TextButton(onClick = {
            println("Anonymous Sign-In initiated. App state will switch to Authenticated and land on home_route.")
            authViewModel.performAnonymousSignIn()
        }) {
            Text("Continue Anonymously")
        }

        Spacer(Modifier.height(16.dp))

        TextButton(onClick = { navController.navigate("register_route") }) {
            Text("Don't have an account? Register")
        }
    }
}
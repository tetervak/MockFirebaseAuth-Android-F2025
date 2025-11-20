package ca.tetervak.mockfirebaseauth.ui.screens.upgrade

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import ca.tetervak.mockfirebaseauth.ui.screens.MockAuthViewModel
import kotlinx.coroutines.flow.map

// Account Upgrade Screen logic is now safer
@Composable
fun UpgradeAccountScreen(
    authViewModel: MockAuthViewModel,
    navController: NavController, // Generic NavController is fine here
    snackbarHostState: SnackbarHostState // Passed in to show immediate success/failure
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    // FIX: Correctly map the flow first, then collect as State using 'by'.
    // We assume 'true' (isAnonymous) for initial/loading states since this screen is only
    // reachable if the user is currently authenticated and anonymous.
    val isAnonymous by authViewModel.authState
        .map { state ->
            (state as? MockAuthViewModel.AuthState.Authenticated)?.user?.isAnonymous ?: true
        }
        .collectAsState(initial = true)

    // Auto-navigate on successful link
    LaunchedEffect(isAnonymous) {
        if (!isAnonymous && !isLoading) {
            // Success! Pop back to the home screen
            snackbarHostState.showSnackbar("Account successfully upgraded and linked!")
            navController.popBackStack()
        }
    }

    Column(
        modifier = Modifier.Companion
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.Companion.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Make Your Account Permanent", style = MaterialTheme.typography.headlineMedium)
        Text("You can access your data on any device after upgrading.")
        Spacer(Modifier.Companion.height(32.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email (Use 'fail' to test error)") },
            modifier = Modifier.Companion.fillMaxWidth(),
            enabled = !isLoading
        )
        Spacer(Modifier.Companion.height(16.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier.Companion.fillMaxWidth(),
            enabled = !isLoading
        )
        Spacer(Modifier.Companion.height(32.dp))

        Button(
            onClick = {
                isLoading = true
                authViewModel.linkAccount(email, password)
                // In a real app, loading state would be managed by observing a result flow,
                // but for this mock, we reset it, relying on the error channel or isAnonymous check.
                isLoading = false
            },
            enabled = email.isNotBlank() && password.length >= 6 && !isLoading,
            modifier = Modifier.Companion.fillMaxWidth().height(56.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    Modifier.Companion.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Complete Upgrade")
            }
        }
        Spacer(Modifier.Companion.height(16.dp))
        TextButton(onClick = { navController.popBackStack() }, enabled = !isLoading) {
            Text("Cancel")
        }
    }
}
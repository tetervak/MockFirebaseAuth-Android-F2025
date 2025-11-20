package ca.tetervak.mockfirebaseauth.ui.screens.register

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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
fun RegisterScreen(
    navController: NavController, // Generic NavController is fine here
    authViewModel: MockAuthViewModel
) {
    var email by remember { mutableStateOf("") }

    Column(
        Modifier.Companion.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.Companion.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Text("Register Account (Mock)", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Successful registration signs you in and redirects to home.",
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(Modifier.Companion.height(32.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.Companion.fillMaxWidth()
        )
        Spacer(Modifier.Companion.height(16.dp))

        Button(
            onClick = {
                // In a real app, this would be a register call, followed by a sign-in call.
                // Here, we mock the sign-in directly to trigger the main navigation.
                authViewModel.performSignIn(email.ifBlank { "register@mock.com" })
            },
            enabled = email.isNotBlank(),
            modifier = Modifier.Companion.fillMaxWidth().height(56.dp)
        ) {
            Text("Register and Sign In")
        }

        Spacer(Modifier.Companion.height(16.dp))

        TextButton(onClick = { navController.popBackStack() }) {
            Text("Already have an account? Login")
        }
    }
}
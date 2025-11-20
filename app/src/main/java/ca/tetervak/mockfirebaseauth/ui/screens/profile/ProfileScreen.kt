package ca.tetervak.mockfirebaseauth.ui.screens.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import ca.tetervak.mockfirebaseauth.ui.screens.MockAuthViewModel
import ca.tetervak.mockfirebaseauth.repository.MockFirebaseUser

@Composable
fun ProfileScreen(
    user: MockFirebaseUser,
    viewModel: MockAuthViewModel,
    navController: NavController // Generic NavController is fine here
) {
    Column(
        modifier = Modifier.Companion
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.Companion.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Profile Settings", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.Companion.height(16.dp))
        Text("Email: ${user.email}")
        Text("Internal ID: ${user.uid}")

        Spacer(Modifier.Companion.height(32.dp))

        // Crucial: The Logout button
        Button(
            onClick = {
                viewModel.performSignOut()
            },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            modifier = Modifier.Companion.fillMaxWidth().height(56.dp)
        ) {
            Text("Logout")
        }

        Spacer(Modifier.Companion.height(16.dp))

        Button(onClick = { navController.navigate("settings_route") }) {
            Text("Go to Settings")
        }
    }
}
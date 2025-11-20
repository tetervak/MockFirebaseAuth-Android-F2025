package ca.tetervak.mockfirebaseauth.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import ca.tetervak.mockfirebaseauth.repository.MockFirebaseUser
import ca.tetervak.mockfirebaseauth.ui.screens.home.HomeViewModel

@Composable
fun HomeScreen(
    user: MockFirebaseUser,
    viewModel: HomeViewModel,
    navController: NavController // Generic NavController is fine here
) {
    // Collects the list of items from the ViewModel's StateFlow
    val userItems by viewModel.userItems.collectAsState()

    Column(
        modifier = Modifier.Companion
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.Companion.CenterHorizontally
    ) {
        Text(
            "Welcome, ${user.email.substringBefore('@')}!",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(Modifier.Companion.height(16.dp))

        // Anonymous User Prompt
        if (user.isAnonymous) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                modifier = Modifier.Companion.fillMaxWidth().padding(bottom = 16.dp)
            ) {
                Row(
                    Modifier.Companion.padding(16.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.Companion.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("You are signed in anonymously.")
                    Button(onClick = { navController.navigate("upgrade_route") }) {
                        Text("Upgrade Account")
                    }
                }
            }
        }

        Text("Your Private Data (UID: ${user.uid})", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.Companion.height(8.dp))

        // Check if items are empty and show loading/no items state
        if (userItems.isEmpty()) {
            // Since the mock repo has an initial delay, we show a loading indicator
            // to cover that initial period, or if the list is genuinely empty.
            CircularProgressIndicator(Modifier.Companion.size(32.dp))
            Text("Loading data or no items found...")
        } else {
            LazyColumn(modifier = Modifier.Companion.fillMaxWidth().weight(1f)) {
                items(userItems, key = { it.id }) { item ->
                    Card(
                        Modifier.Companion
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.Companion
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.Companion.CenterVertically
                        ) {
                            Text(item.name, modifier = Modifier.Companion.weight(1f))

                            // Action 1: Update Button
                            IconButton(onClick = {
                                viewModel.updateItem(item)
                            }) {
                                Icon(Icons.Filled.Edit, contentDescription = "Edit")
                            }

                            // Action 2: Delete Button
                            IconButton(onClick = {
                                viewModel.deleteItem(item.id)
                            }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete")
                            }
                        }
                    }
                }
            }
        }
    }
}
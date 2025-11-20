package ca.tetervak.mockfirebaseauth.ui.screens.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun SettingsScreen() {
    Box(Modifier.Companion.fillMaxSize(), contentAlignment = Alignment.Companion.Center) {
        Text("App Settings (Only accessible when logged in)")
    }
}
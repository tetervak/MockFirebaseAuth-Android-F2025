package ca.tetervak.mockfirebaseauth.repository

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID

// Sealed class for action results, including failure message
sealed class ActionStatus {
    object Success : ActionStatus()
    data class Failure(val message: String) : ActionStatus()
}

data class MockFirebaseUser(
    val uid: String,
    val email: String,
    val isAnonymous: Boolean
)

class MockAuthRepository {
    private val _userState = MutableStateFlow<MockFirebaseUser?>(null)
    val userState: StateFlow<MockFirebaseUser?> = _userState.asStateFlow()

    init {
        _userState.update { null }
    }

    // UPDATED: Simulate sign-in failure based on email content
    suspend fun mockSignIn(email: String): ActionStatus {
        if (email.contains("fail", ignoreCase = true)) {
            return ActionStatus.Failure("Invalid credentials. Try 'user@test.com'.")
        }
        if (email.contains("network", ignoreCase = true)) {
            return ActionStatus.Failure("Network error. Please check connection and retry.")
        }

        delay(500)
        _userState.update { MockFirebaseUser(
            uid = "user-${email.hashCode()}",
            email = email,
            isAnonymous = false
        ) }
        return ActionStatus.Success
    }

    fun signInAnonymously() {
        _userState.update { MockFirebaseUser(
            uid = "anon-${UUID.randomUUID()}",
            email = "anonymous@temp.com",
            isAnonymous = true
        ) }
    }

    // UPDATED: Simulate linking failure based on email content
    suspend fun linkEmailPassword(email: String): ActionStatus {
        if (email.contains("fail", ignoreCase = true)) {
            return ActionStatus.Failure("Email is already linked to another account.")
        }

        delay(500)
        _userState.update { currentUser ->
            if (currentUser != null && currentUser.isAnonymous) {
                // Keep the same UID but update email and set isAnonymous to false
                currentUser.copy(email = email, isAnonymous = false)
            } else {
                currentUser
            }
        }
        return ActionStatus.Success
    }

    fun signOut() {
        _userState.update { null }
    }
}
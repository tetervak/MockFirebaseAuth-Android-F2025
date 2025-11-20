package ca.tetervak.mockfirebaseauth.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ca.tetervak.mockfirebaseauth.repository.ActionStatus
import ca.tetervak.mockfirebaseauth.repository.MockAuthRepository
import ca.tetervak.mockfirebaseauth.repository.MockFirebaseUser
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MockAuthViewModel(
    private val repository: MockAuthRepository = MockAuthRepository()
) : ViewModel() {

    sealed class AuthState {
        object Loading : AuthState()
        data class Authenticated(val user: MockFirebaseUser) : AuthState()
        object Unauthenticated : AuthState()
    }

    // NEW: Channel to send transient error messages to the UI (e.g., Snackbar)
    private val _errorChannel = Channel<String>(Channel.Factory.BUFFERED)
    val errorFlow = _errorChannel.receiveAsFlow()


    val authState: StateFlow<AuthState> = repository.userState
        .map { user ->
            if (user == null) AuthState.Unauthenticated else AuthState.Authenticated(user)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Companion.Eagerly,
            initialValue = AuthState.Loading
        )

    // UPDATED: Handle failure result
    fun performSignIn(email: String) = viewModelScope.launch {
        when (val result = repository.mockSignIn(email)) {
            is ActionStatus.Success -> {} // AuthState flow handles the success
            is ActionStatus.Failure -> _errorChannel.send(result.message)
        }
    }

    fun performSignOut() = viewModelScope.launch { repository.signOut() }

    fun performAnonymousSignIn() = viewModelScope.launch { repository.signInAnonymously() }

    // UPDATED: Handle failure result
    fun linkAccount(email: String, password: String) = viewModelScope.launch {
        when (val result = repository.linkEmailPassword(email)) {
            is ActionStatus.Success -> {} // AuthState flow handles the success
            is ActionStatus.Failure -> _errorChannel.send(result.message)
        }
    }
}
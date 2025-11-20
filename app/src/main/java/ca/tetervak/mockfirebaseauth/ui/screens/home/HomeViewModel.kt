package ca.tetervak.mockfirebaseauth.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ca.tetervak.mockfirebaseauth.repository.MockDataRepository
import ca.tetervak.mockfirebaseauth.repository.UserItem
import ca.tetervak.mockfirebaseauth.ui.screens.MockAuthViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(
    authViewModel: MockAuthViewModel,
    private val dataRepository: MockDataRepository
) : ViewModel() {

    private val userIdFlow: Flow<String?> = authViewModel.authState.map { state ->
        when (state) {
            is MockAuthViewModel.AuthState.Authenticated -> state.user.uid
            else -> null
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val userItems: StateFlow<List<UserItem>> = userIdFlow
        .flatMapLatest { uid ->
            if (uid != null) {
                dataRepository.getUserItemsFlow(uid)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Companion.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun updateItem(item: UserItem) {
        viewModelScope.launch {
            val userId = userIdFlow.firstOrNull()
            if (userId != null) {
                dataRepository.updateUserItem(userId, item.id, item.name + " (EDITED)")
            } else {
                println("ERROR: Attempted update without authentication.")
            }
        }
    }

    fun deleteItem(itemId: Int) {
        viewModelScope.launch {
            val userId = userIdFlow.firstOrNull()
            if (userId != null) {
                dataRepository.deleteUserItem(userId, itemId)
            } else {
                println("ERROR: Attempted delete without authentication.")
            }
        }
    }
}
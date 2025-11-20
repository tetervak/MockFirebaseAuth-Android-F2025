package ca.tetervak.mockfirebaseauth.repository

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update

data class UserItem(val id: Int, val name: String)

class MockDataRepository {
    private val _mockItems = MutableStateFlow(mapOf<String, List<UserItem>>())

    fun getUserItemsFlow(userId: String): Flow<List<UserItem>> = _mockItems
        .map { it.getOrDefault(userId, emptyList()) }
        .onStart {
            delay(500)
            if (_mockItems.value.getOrDefault(userId, emptyList()).isEmpty()) {
                val initialItems = listOf(
                    UserItem(1, "Task A"),
                    UserItem(2, "Task B")
                )
                _mockItems.update { currentMap ->
                    currentMap + (userId to initialItems)
                }
            }
        }

    suspend fun updateUserItem(userId: String, itemId: Int, newName: String) {
        println("Repository: Attempting update for user $userId, item $itemId")
        delay(200)
        _mockItems.update { currentMap ->
            val items = currentMap.getOrDefault(userId, emptyList())
            val updatedItems = items.map { item ->
                if (item.id == itemId) item.copy(name = newName) else item
            }
            currentMap + (userId to updatedItems)
        }
        println("Repository: Item $itemId updated successfully.")
    }

    suspend fun deleteUserItem(userId: String, itemId: Int) {
        println("Repository: Attempting deletion for user $userId, item $itemId")
        delay(200)
        _mockItems.update { currentMap ->
            val items = currentMap.getOrDefault(userId, emptyList())
            val updatedItems = items.filter { it.id != itemId }
            currentMap + (userId to updatedItems)
        }
        println("Repository: Item $itemId deleted successfully.")
    }
}
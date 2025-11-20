package ca.tetervak.mockfirebaseauth.ui.theme.screens
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController // Keeping NavController for generic usage if needed
import androidx.navigation.NavHostController // NEW: Required for NavHost composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

// --- MOCK/SIMPLIFIED AUTH INFRASTRUCTURE ---

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

        kotlinx.coroutines.delay(500)
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

        kotlinx.coroutines.delay(500)
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

class MockAuthViewModel(
    private val repository: MockAuthRepository = MockAuthRepository()
) : ViewModel() {

    sealed class AuthState {
        object Loading : AuthState()
        data class Authenticated(val user: MockFirebaseUser) : AuthState()
        object Unauthenticated : AuthState()
    }

    // NEW: Channel to send transient error messages to the UI (e.g., Snackbar)
    private val _errorChannel = Channel<String>(Channel.BUFFERED)
    val errorFlow = _errorChannel.receiveAsFlow()


    val authState: StateFlow<AuthState> = repository.userState
        .map { user ->
            if (user == null) AuthState.Unauthenticated else AuthState.Authenticated(user)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
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

// --- MOCK DATA INFRASTRUCTURE (Unchanged) ---

data class UserItem(val id: Int, val name: String)

class MockDataRepository {
    private val _mockItems = MutableStateFlow(mapOf<String, List<UserItem>>())

    fun getUserItemsFlow(userId: String): Flow<List<UserItem>> = _mockItems
        .map { it.getOrDefault(userId, emptyList()) }
        .onStart {
            kotlinx.coroutines.delay(500)
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
        kotlinx.coroutines.delay(200)
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
        kotlinx.coroutines.delay(200)
        _mockItems.update { currentMap ->
            val items = currentMap.getOrDefault(userId, emptyList())
            val updatedItems = items.filter { it.id != itemId }
            currentMap + (userId to updatedItems)
        }
        println("Repository: Item $itemId deleted successfully.")
    }
}

// --- HOME VIEWMODEL (Unchanged) ---

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
            started = SharingStarted.WhileSubscribed(5000),
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


// --- AUTHENTICATED COMPOSABLES (HomeScreen updated for clarity) ---

@Composable
fun HomeScreen(
    user: MockFirebaseUser,
    viewModel: HomeViewModel,
    navController: NavController // Generic NavController is fine here
) {
    // Collects the list of items from the ViewModel's StateFlow
    val items by viewModel.userItems.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Welcome, ${user.email.substringBefore('@')}!", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))

        // Anonymous User Prompt
        if (user.isAnonymous) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            ) {
                Row(
                    Modifier.padding(16.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
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
        Spacer(Modifier.height(8.dp))

        // Check if items are empty and show loading/no items state
        if (items.isEmpty()) {
            // Since the mock repo has an initial delay, we show a loading indicator
            // to cover that initial period, or if the list is genuinely empty.
            CircularProgressIndicator(Modifier.size(32.dp))
            Text("Loading data or no items found...")
        } else {
            LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
                items(items, key = { it.id }) { item ->
                    Card(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(item.name, modifier = Modifier.weight(1f))

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

@Composable
fun ProfileScreen(
    user: MockFirebaseUser,
    viewModel: MockAuthViewModel,
    navController: NavController // Generic NavController is fine here
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Profile Settings", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))
        Text("Email: ${user.email}")
        Text("Internal ID: ${user.uid}")

        Spacer(Modifier.height(32.dp))

        // Crucial: The Logout button
        Button(
            onClick = {
                viewModel.performSignOut()
            },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("Logout")
        }

        Spacer(Modifier.height(16.dp))

        Button(onClick = { navController.navigate("settings_route") }) {
            Text("Go to Settings")
        }
    }
}

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
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Make Your Account Permanent", style = MaterialTheme.typography.headlineMedium)
        Text("You can access your data on any device after upgrading.")
        Spacer(Modifier.height(32.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email (Use 'fail' to test error)") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        )
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        )
        Spacer(Modifier.height(32.dp))

        Button(
            onClick = {
                isLoading = true
                authViewModel.linkAccount(email, password)
                // In a real app, loading state would be managed by observing a result flow,
                // but for this mock, we reset it, relying on the error channel or isAnonymous check.
                isLoading = false
            },
            enabled = email.isNotBlank() && password.length >= 6 && !isLoading,
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Text("Complete Upgrade")
            }
        }
        Spacer(Modifier.height(16.dp))
        TextButton(onClick = { navController.popBackStack() }, enabled = !isLoading) {
            Text("Cancel")
        }
    }
}

@Composable
fun SettingsScreen() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("App Settings (Only accessible when logged in)")
    }
}


// --- LOGIN/REGISTER COMPOSABLES (New additions for UnauthenticatedNavGraph) ---

@Composable
fun LoginScreen(
    navController: NavController, // Generic NavController is fine here
    authViewModel: MockAuthViewModel,
    snackbarHostState: SnackbarHostState
) {
    var email by remember { mutableStateOf("") }

    // Automatic anonymous sign-in for quick testing and access to anonymous features
    LaunchedEffect(Unit) {
        authViewModel.performAnonymousSignIn()
    }

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

        Button(onClick = {
            authViewModel.performSignIn(email)
        }, enabled = email.isNotBlank(), modifier = Modifier.fillMaxWidth().height(56.dp)) {
            Text("Sign In (Tests Auth Redirect)")
        }

        Spacer(Modifier.height(16.dp))

        TextButton(onClick = { navController.navigate("register_route") }) {
            Text("Don't have an account? Register")
        }
    }
}

@Composable
fun RegisterScreen(
    navController: NavController, // Generic NavController is fine here
    authViewModel: MockAuthViewModel
) {
    var email by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center) {

        Text("Register Account (Mock)", style = MaterialTheme.typography.headlineMedium)
        Text("Successful registration signs you in and redirects to home.", style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(32.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))

        Button(onClick = {
            // In a real app, this would be a register call, followed by a sign-in call.
            // Here, we mock the sign-in directly to trigger the main navigation.
            authViewModel.performSignIn(email.ifBlank { "register@mock.com" })
        }, enabled = email.isNotBlank(), modifier = Modifier.fillMaxWidth().height(56.dp)) {
            Text("Register and Sign In")
        }

        Spacer(Modifier.height(16.dp))

        TextButton(onClick = { navController.popBackStack() }) {
            Text("Already have an account? Login")
        }
    }
}


// --- THE AUTHENTICATED NAVIGATION GRAPH ---

@Composable
fun AuthenticatedNavGraph(
    // FIX: Changed from NavController to NavHostController to satisfy the NavHost composable.
    navController: NavHostController,
    user: MockFirebaseUser,
    authViewModel: MockAuthViewModel,
    snackbarHostState: SnackbarHostState
) {
    NavHost(
        navController = navController,
        startDestination = "home_route"
    ) {
        composable("home_route") {
            val homeViewModel = remember {
                HomeViewModel(
                    authViewModel = authViewModel,
                    dataRepository = MockDataRepository()
                )
            }
            HomeScreen(user = user, viewModel = homeViewModel, navController = navController)
        }
        composable("profile_route") {
            ProfileScreen(user = user, viewModel = authViewModel, navController = navController)
        }
        composable("settings_route") {
            SettingsScreen()
        }
        // Updated Upgrade route
        composable("upgrade_route") {
            UpgradeAccountScreen(
                authViewModel = authViewModel,
                navController = navController,
                snackbarHostState = snackbarHostState
            )
        }
    }
}

// --- UNAUTHENTICATED NAVIGATION GRAPH (Fixed to use NavHost) ---

@Composable
fun UnauthenticatedNavGraph(
    authViewModel: MockAuthViewModel,
    snackbarHostState: SnackbarHostState
) {
    // Nested NavController for Login/Register flow
    val flowNavController = rememberNavController()

    NavHost(
        navController = flowNavController,
        startDestination = "login_route"
    ) {
        composable("login_route") {
            LoginScreen(
                navController = flowNavController,
                authViewModel = authViewModel,
                snackbarHostState = snackbarHostState
            )
        }
        composable("register_route") {
            RegisterScreen(
                navController = flowNavController,
                authViewModel = authViewModel
            )
        }
    }
}


// --- MAIN APPLICATION PREVIEW (Updated for Snackbar) ---

@Preview(showBackground = true)
@Composable
fun AppNavigationPreview() {
    val mockAuthViewModel = remember { MockAuthViewModel() }
    val authState by mockAuthViewModel.authState.collectAsState()

    // rememberNavController returns NavHostController, which is the necessary type
    val navController = rememberNavController()

    // NEW: SnackBar Host State
    val snackbarHostState = remember { SnackbarHostState() }

    // NEW: Observe the transient error channel from the ViewModel
    LaunchedEffect(Unit) {
        mockAuthViewModel.errorFlow.collect { message ->
            snackbarHostState.showSnackbar(
                message = message,
                actionLabel = "Dismiss",
                withDismissAction = true
            )
        }
    }

    // This logic mimics the central NavHost that observes global AuthState
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }, // Display the SnackBar
        bottomBar = {
            if (authState is MockAuthViewModel.AuthState.Authenticated) {
                NavigationBar {
                    NavigationBarItem(
                        icon = { Icon(Icons.Filled.Home, contentDescription = "Home") },
                        label = { Text("Home") },
                        selected = navController.currentDestination?.route == "home_route",
                        onClick = { navController.navigate("home_route") { popUpTo("home_route") { inclusive = true } } }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Filled.Person, contentDescription = "Profile") },
                        label = { Text("Profile") },
                        selected = navController.currentDestination?.route == "profile_route",
                        onClick = { navController.navigate("profile_route") { popUpTo("home_route") } }
                    )
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (authState) {
                is MockAuthViewModel.AuthState.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                        Text("Initializing...")
                    }
                }
                is MockAuthViewModel.AuthState.Unauthenticated -> {
                    UnauthenticatedNavGraph(
                        authViewModel = mockAuthViewModel,
                        snackbarHostState = snackbarHostState
                    )
                }
                is MockAuthViewModel.AuthState.Authenticated -> {
                    val user = (authState as MockAuthViewModel.AuthState.Authenticated).user

                    AuthenticatedNavGraph(
                        navController = navController,
                        user = user,
                        authViewModel = mockAuthViewModel,
                        snackbarHostState = snackbarHostState
                    )
                }
            }
        }
    }
}
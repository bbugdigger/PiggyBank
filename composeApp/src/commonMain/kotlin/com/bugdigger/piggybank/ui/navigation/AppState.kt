package com.bugdigger.piggybank.ui.navigation

import androidx.compose.runtime.*
import com.bugdigger.piggybank.data.api.PiggyBankApi
import com.bugdigger.piggybank.domain.model.AccountTreeNode
import com.bugdigger.piggybank.domain.model.TransactionResponse
import com.bugdigger.piggybank.domain.model.UserResponse

/**
 * Navigation destinations in the app
 */
sealed class Screen {
    data object Login : Screen()
    data object Register : Screen()
    data object Dashboard : Screen()
    data object Accounts : Screen()
    data class AccountDetail(val accountId: String) : Screen()
    data object Transactions : Screen()
    data class TransactionDetail(val transactionId: String) : Screen()
    data object NewTransaction : Screen()
    data class EditTransaction(val transactionId: String) : Screen()
    data object Settings : Screen()
}

/**
 * Application state holder
 */
class AppState(
    val api: PiggyBankApi = PiggyBankApi()
) {
    // Navigation
    var currentScreen by mutableStateOf<Screen>(Screen.Login)
        private set
    
    private val navigationStack = mutableListOf<Screen>()
    
    // Auth state
    var isLoggedIn by mutableStateOf(false)
        private set
    var currentUser by mutableStateOf<UserResponse?>(null)
        private set
    var authToken by mutableStateOf<String?>(null)
        private set
    
    // Theme
    var isDarkTheme by mutableStateOf(false)
    
    // Loading states
    var isLoading by mutableStateOf(false)
    
    // Error state
    var errorMessage by mutableStateOf<String?>(null)
    
    // Data cache
    var accountsTree by mutableStateOf<List<AccountTreeNode>>(emptyList())
    var recentTransactions by mutableStateOf<List<TransactionResponse>>(emptyList())
    
    fun navigateTo(screen: Screen) {
        if (currentScreen != screen) {
            navigationStack.add(currentScreen)
            currentScreen = screen
        }
    }
    
    fun navigateBack(): Boolean {
        return if (navigationStack.isNotEmpty()) {
            currentScreen = navigationStack.removeLast()
            true
        } else {
            false
        }
    }
    
    fun clearNavigationStack() {
        navigationStack.clear()
    }
    
    fun setAuthenticated(token: String, user: UserResponse) {
        authToken = token
        currentUser = user
        isLoggedIn = true
        api.setAuthToken(token)
        clearNavigationStack()
        currentScreen = Screen.Dashboard
    }
    
    fun logout() {
        authToken = null
        currentUser = null
        isLoggedIn = false
        api.setAuthToken(null)
        accountsTree = emptyList()
        recentTransactions = emptyList()
        clearNavigationStack()
        currentScreen = Screen.Login
    }
    
    fun showError(message: String) {
        errorMessage = message
    }
    
    fun clearError() {
        errorMessage = null
    }
}

/**
 * Local composition for app state
 */
val LocalAppState = compositionLocalOf<AppState> { 
    error("No AppState provided") 
}

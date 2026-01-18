package com.bugdigger.piggybank

import androidx.compose.runtime.*
import com.bugdigger.piggybank.ui.screens.LoginScreen
import com.bugdigger.piggybank.ui.screens.MainScreen
import com.bugdigger.piggybank.ui.theme.PiggyBankTheme
import com.bugdigger.piggybank.ui.viewmodel.AuthViewModel
import org.koin.compose.viewmodel.koinViewModel

/**
 * Navigation destinations
 */
sealed class Screen {
    data object Login : Screen()
    data object Main : Screen()
}

/**
 * Main application composable
 */
@Composable
fun App() {
    PiggyBankTheme {
        val authViewModel: AuthViewModel = koinViewModel()
        val authState by authViewModel.state.collectAsState()
        
        // Navigate based on authentication state
        val currentScreen = if (authState.isAuthenticated) {
            Screen.Main
        } else {
            Screen.Login
        }
        
        when (currentScreen) {
            Screen.Login -> LoginScreen(
                onLoginSuccess = { /* Navigation handled by state */ }
            )
            Screen.Main -> MainScreen(
                onLogout = { authViewModel.logout() }
            )
        }
    }
}

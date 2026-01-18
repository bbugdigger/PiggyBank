package com.bugdigger.piggybank

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.bugdigger.piggybank.ui.navigation.AppState
import com.bugdigger.piggybank.ui.navigation.LocalAppState
import com.bugdigger.piggybank.ui.navigation.Screen
import com.bugdigger.piggybank.ui.screens.accounts.AccountDetailScreen
import com.bugdigger.piggybank.ui.screens.accounts.AccountsScreen
import com.bugdigger.piggybank.ui.screens.auth.LoginScreen
import com.bugdigger.piggybank.ui.screens.auth.RegisterScreen
import com.bugdigger.piggybank.ui.screens.dashboard.DashboardScreen
import com.bugdigger.piggybank.ui.screens.settings.SettingsScreen
import com.bugdigger.piggybank.ui.screens.transactions.TransactionDetailScreen
import com.bugdigger.piggybank.ui.screens.transactions.TransactionEntryScreen
import com.bugdigger.piggybank.ui.screens.transactions.TransactionsScreen
import com.bugdigger.piggybank.ui.theme.PiggyBankTheme

@Composable
fun App() {
    val appState = remember { AppState() }
    
    CompositionLocalProvider(LocalAppState provides appState) {
        PiggyBankTheme(darkTheme = appState.isDarkTheme) {
            Surface(modifier = Modifier.fillMaxSize()) {
                AppContent()
            }
        }
    }
}

@Composable
private fun AppContent() {
    val appState = LocalAppState.current
    
    when (val screen = appState.currentScreen) {
        is Screen.Login -> LoginScreen()
        is Screen.Register -> RegisterScreen()
        is Screen.Dashboard -> DashboardScreen()
        is Screen.Accounts -> AccountsScreen()
        is Screen.AccountDetail -> AccountDetailScreen(accountId = screen.accountId)
        is Screen.Transactions -> TransactionsScreen()
        is Screen.TransactionDetail -> TransactionDetailScreen(transactionId = screen.transactionId)
        is Screen.NewTransaction -> TransactionEntryScreen(transactionId = null)
        is Screen.EditTransaction -> TransactionEntryScreen(transactionId = screen.transactionId)
        is Screen.Settings -> SettingsScreen()
    }
}

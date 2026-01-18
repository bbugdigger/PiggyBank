package com.bugdigger.piggybank.ui.screens.accounts

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bugdigger.piggybank.domain.model.AccountRegisterEntry
import com.bugdigger.piggybank.domain.model.AccountRegisterResponse
import com.bugdigger.piggybank.domain.model.AccountWithBalanceResponse
import com.bugdigger.piggybank.ui.components.AccountTypeChip
import com.bugdigger.piggybank.ui.components.AmountText
import com.bugdigger.piggybank.ui.components.EmptyStateMessage
import com.bugdigger.piggybank.ui.components.LoadingOverlay
import com.bugdigger.piggybank.ui.components.PiggyBankTopBar
import com.bugdigger.piggybank.ui.navigation.LocalAppState
import com.bugdigger.piggybank.ui.navigation.Screen
import com.bugdigger.piggybank.ui.theme.AccountingColors
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountDetailScreen(accountId: String) {
    val appState = LocalAppState.current
    val scope = rememberCoroutineScope()

    var isLoading by remember { mutableStateOf(true) }
    var accountInfo by remember { mutableStateOf<AccountWithBalanceResponse?>(null) }
    var register by remember { mutableStateOf<AccountRegisterResponse?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(accountId) {
        isLoading = true
        appState.api.getAccountBalance(accountId)
            .onSuccess { accountInfo = it }
            .onError { error -> errorMessage = "Failed to load account: $error" }

        appState.api.getAccountRegister(accountId)
            .onSuccess { register = it }
            .onError { error ->
                if (errorMessage == null) errorMessage = "Failed to load transactions: $error"
            }
        isLoading = false
    }

    Scaffold(
        topBar = {
            PiggyBankTopBar(
                title = accountInfo?.account?.fullName ?: "Account",
                canNavigateBack = true,
                onNavigateBack = { appState.navigateBack() },
                actions = {
                    IconButton(onClick = {
                        scope.launch {
                            isLoading = true
                            appState.api.getAccountBalance(accountId).onSuccess { accountInfo = it }
                            appState.api.getAccountRegister(accountId).onSuccess { register = it }
                            isLoading = false
                        }
                    }) {
                        Icon(Icons.Default.Refresh, "Refresh")
                    }
                }
            )
        },
        floatingActionButton = {
            if (accountInfo?.account?.placeholder == false) {
                FloatingActionButton(
                    onClick = { appState.navigateTo(Screen.NewTransaction) },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Add, "New Transaction")
                }
            }
        }
    ) { padding ->
        LoadingOverlay(isLoading = isLoading, modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                accountInfo?.let { info ->
                    item { AccountInfoCard(info = info) }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Transaction Register", style = MaterialTheme.typography.titleMedium)
                        register?.let {
                            Text(
                                text = "${it.entries.size} transactions",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }

                register?.let { reg ->
                    if (reg.entries.isEmpty()) {
                        item {
                            EmptyStateMessage(
                                message = "No transactions in this account",
                                icon = Icons.Default.Receipt
                            )
                        }
                    } else {
                        item { RegisterHeaderRow() }

                        items(reg.entries) { entry ->
                            RegisterEntryRow(
                                entry = entry,
                                onClick = { appState.navigateTo(Screen.TransactionDetail(entry.transactionId)) }
                            )
                        }

                        item {
                            HorizontalDivider()
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Closing Balance",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                AmountText(
                                    amount = reg.closingBalance,
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }

        if (errorMessage != null) {
            Snackbar(
                modifier = Modifier.padding(16.dp),
                action = {
                    TextButton(onClick = { errorMessage = null }) { Text("Dismiss") }
                }
            ) {
                Text(errorMessage!!)
            }
        }
    }
}

@Composable
private fun AccountInfoCard(info: AccountWithBalanceResponse) {
    val typeColor = when (info.account.type.uppercase()) {
        "ASSET" -> AccountingColors.Asset
        "LIABILITY" -> AccountingColors.Liability
        "EQUITY" -> AccountingColors.Equity
        "INCOME" -> AccountingColors.Income
        "EXPENSE" -> AccountingColors.Expense
        else -> MaterialTheme.colorScheme.primary
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = info.account.name, style = MaterialTheme.typography.headlineSmall)
                    Text(
                        text = info.account.fullName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
                AccountTypeChip(type = info.account.type)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Balance",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    AmountText(amount = info.balance, style = MaterialTheme.typography.headlineMedium)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Currency",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(text = info.account.currency, style = MaterialTheme.typography.titleLarge)
                }
            }

            info.account.description?.let { desc ->
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@Composable
private fun RegisterHeaderRow() {
    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.small) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "Date", style = MaterialTheme.typography.labelMedium, modifier = Modifier.width(80.dp))
            Text(text = "Description", style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
            Text(text = "Amount", style = MaterialTheme.typography.labelMedium, modifier = Modifier.width(100.dp))
            Text(text = "Balance", style = MaterialTheme.typography.labelMedium, modifier = Modifier.width(100.dp))
        }
    }
}

@Composable
private fun RegisterEntryRow(entry: AccountRegisterEntry, onClick: () -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick), color = MaterialTheme.colorScheme.surface) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = entry.date, style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(80.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = entry.description, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                if (entry.otherAccounts.isNotEmpty()) {
                    Text(
                        text = entry.otherAccounts.joinToString(", "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                        maxLines = 1
                    )
                }
            }
            AmountText(amount = entry.amount, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.width(100.dp))
            AmountText(amount = entry.balance, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.width(100.dp))
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

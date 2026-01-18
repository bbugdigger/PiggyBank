package com.bugdigger.piggybank.ui.screens.dashboard

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import com.bugdigger.piggybank.domain.model.AccountTreeNode
import com.bugdigger.piggybank.domain.model.TransactionResponse
import com.bugdigger.piggybank.ui.components.AmountText
import com.bugdigger.piggybank.ui.components.EmptyStateMessage
import com.bugdigger.piggybank.ui.components.LoadingOverlay
import com.bugdigger.piggybank.ui.navigation.LocalAppState
import com.bugdigger.piggybank.ui.navigation.Screen
import com.bugdigger.piggybank.ui.theme.AccountingColors
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen() {
    val appState = LocalAppState.current
    val scope = rememberCoroutineScope()

    var isLoading by remember { mutableStateOf(false) }
    var accountsTree by remember { mutableStateOf<List<AccountTreeNode>>(emptyList()) }
    var recentTransactions by remember { mutableStateOf<List<TransactionResponse>>(emptyList()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        isLoading = true
        appState.api.getAccountsTree()
            .onSuccess { tree ->
                accountsTree = tree
                appState.accountsTree = tree
            }
            .onError { error -> errorMessage = "Failed to load accounts: $error" }

        appState.api.getTransactions(page = 1, pageSize = 10)
            .onSuccess { response ->
                recentTransactions = response.transactions
                appState.recentTransactions = response.transactions
            }
            .onError { error ->
                if (errorMessage == null) errorMessage = "Failed to load transactions: $error"
            }
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("PiggyBank")
                        appState.currentUser?.let {
                            Text(
                                text = "Hello, ${it.username}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = {
                        scope.launch {
                            isLoading = true
                            appState.api.getAccountsTree().onSuccess { accountsTree = it }
                            appState.api.getTransactions(page = 1, pageSize = 10)
                                .onSuccess { recentTransactions = it.transactions }
                            isLoading = false
                        }
                    }) {
                        Icon(Icons.Default.Refresh, "Refresh")
                    }
                    IconButton(onClick = { appState.navigateTo(Screen.Settings) }) {
                        Icon(Icons.Default.Settings, "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { appState.navigateTo(Screen.NewTransaction) },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, "New Transaction")
            }
        }
    ) { padding ->
        LoadingOverlay(isLoading = isLoading) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item { Spacer(modifier = Modifier.height(8.dp)) }

                item {
                    QuickActionsCard(
                        onAccountsClick = { appState.navigateTo(Screen.Accounts) },
                        onTransactionsClick = { appState.navigateTo(Screen.Transactions) },
                        onNewTransactionClick = { appState.navigateTo(Screen.NewTransaction) }
                    )
                }

                item { AccountSummaryCard(accountsTree = accountsTree) }

                item {
                    Text(
                        text = "Recent Transactions",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                if (recentTransactions.isEmpty() && !isLoading) {
                    item {
                        EmptyStateMessage(
                            message = "No transactions yet",
                            icon = Icons.Default.Receipt
                        )
                    }
                } else {
                    items(recentTransactions.take(5)) { transaction ->
                        TransactionCard(
                            transaction = transaction,
                            onClick = { appState.navigateTo(Screen.TransactionDetail(transaction.id)) }
                        )
                    }
                }

                if (recentTransactions.size > 5) {
                    item {
                        TextButton(
                            onClick = { appState.navigateTo(Screen.Transactions) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("View All Transactions")
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
                    TextButton(onClick = { errorMessage = null }) {
                        Text("Dismiss")
                    }
                }
            ) {
                Text(errorMessage!!)
            }
        }
    }
}

@Composable
private fun QuickActionsCard(
    onAccountsClick: () -> Unit,
    onTransactionsClick: () -> Unit,
    onNewTransactionClick: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            QuickActionButton(Icons.Default.AccountBalance, "Accounts", onAccountsClick)
            QuickActionButton(Icons.Default.Receipt, "Transactions", onTransactionsClick)
            QuickActionButton(Icons.Default.AddCircle, "New Entry", onNewTransactionClick)
        }
    }
}

@Composable
private fun QuickActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier.clickable(onClick = onClick).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(32.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun AccountSummaryCard(accountsTree: List<AccountTreeNode>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Account Summary",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (accountsTree.isEmpty()) {
                Text(
                    text = "Loading accounts...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline
                )
            } else {
                accountsTree.forEach { rootNode ->
                    AccountSummaryRow(
                        name = rootNode.account.name,
                        type = rootNode.account.type,
                        balance = rootNode.balance
                    )
                }
            }
        }
    }
}

@Composable
private fun AccountSummaryRow(name: String, type: String, balance: String) {
    val typeColor = when (type.uppercase()) {
        "ASSET" -> AccountingColors.Asset
        "LIABILITY" -> AccountingColors.Liability
        "EQUITY" -> AccountingColors.Equity
        "INCOME" -> AccountingColors.Income
        "EXPENSE" -> AccountingColors.Expense
        else -> MaterialTheme.colorScheme.primary
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                color = typeColor,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.size(8.dp)
            ) {}
            Spacer(modifier = Modifier.width(12.dp))
            Text(text = name, style = MaterialTheme.typography.bodyLarge)
        }
        AmountText(
            amount = balance,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
        )
    }
}

@Composable
private fun TransactionCard(transaction: TransactionResponse, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.description,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = transaction.date,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            val amount = transaction.splits
                .firstOrNull { (it.amount.toDoubleOrNull() ?: 0.0) > 0 }
                ?.amount ?: "0.00"

            AmountText(
                amount = amount,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
            )
        }
    }
}

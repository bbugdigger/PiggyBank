package com.bugdigger.piggybank.ui.screens.transactions

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Refresh
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
import com.bugdigger.piggybank.domain.model.TransactionResponse
import com.bugdigger.piggybank.ui.components.AmountText
import com.bugdigger.piggybank.ui.components.EmptyStateMessage
import com.bugdigger.piggybank.ui.components.LoadingOverlay
import com.bugdigger.piggybank.ui.components.PiggyBankTopBar
import com.bugdigger.piggybank.ui.navigation.LocalAppState
import com.bugdigger.piggybank.ui.navigation.Screen
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen() {
    val appState = LocalAppState.current
    val scope = rememberCoroutineScope()

    var isLoading by remember { mutableStateOf(false) }
    var transactions by remember { mutableStateOf<List<TransactionResponse>>(emptyList()) }
    var totalCount by remember { mutableStateOf(0) }
    var currentPage by remember { mutableStateOf(1) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(currentPage) {
        isLoading = true
        appState.api.getTransactions(page = currentPage, pageSize = 50)
            .onSuccess { response ->
                transactions = response.transactions
                totalCount = response.total
            }
            .onError { error -> errorMessage = "Failed to load transactions: $error" }
        isLoading = false
    }

    Scaffold(
        topBar = {
            PiggyBankTopBar(
                title = "Transactions",
                canNavigateBack = true,
                onNavigateBack = { appState.navigateBack() },
                actions = {
                    IconButton(onClick = {
                        scope.launch {
                            isLoading = true
                            appState.api.getTransactions(page = currentPage, pageSize = 50)
                                .onSuccess { response ->
                                    transactions = response.transactions
                                    totalCount = response.total
                                }
                            isLoading = false
                        }
                    }) {
                        Icon(Icons.Default.Refresh, "Refresh")
                    }
                }
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
        LoadingOverlay(isLoading = isLoading, modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                Surface(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "$totalCount transactions", style = MaterialTheme.typography.bodyMedium)
                    }
                }

                if (transactions.isEmpty() && !isLoading) {
                    EmptyStateMessage(
                        message = "No transactions yet. Create your first entry!",
                        icon = Icons.Default.Receipt,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(transactions) { transaction ->
                            TransactionListItem(
                                transaction = transaction,
                                onClick = { appState.navigateTo(Screen.TransactionDetail(transaction.id)) }
                            )
                        }
                        item { Spacer(modifier = Modifier.height(80.dp)) }
                    }
                }
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
private fun TransactionListItem(transaction: TransactionResponse, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
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

                val primarySplit = transaction.splits.firstOrNull { (it.amount.toDoubleOrNull() ?: 0.0) > 0 }
                if (primarySplit != null) {
                    Column(horizontalAlignment = Alignment.End) {
                        AmountText(
                            amount = primarySplit.amount,
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
                        )
                        Text(
                            text = primarySplit.currency,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                transaction.splits.forEach { split ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = split.accountName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.weight(1f)
                        )
                        AmountText(amount = split.amount, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

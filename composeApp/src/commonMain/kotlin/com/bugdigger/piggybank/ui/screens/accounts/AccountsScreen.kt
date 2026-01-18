package com.bugdigger.piggybank.ui.screens.accounts

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.bugdigger.piggybank.domain.model.AccountTreeNode
import com.bugdigger.piggybank.ui.components.AmountText
import com.bugdigger.piggybank.ui.components.EmptyStateMessage
import com.bugdigger.piggybank.ui.components.LoadingOverlay
import com.bugdigger.piggybank.ui.components.PiggyBankTopBar
import com.bugdigger.piggybank.ui.components.formatAmount
import com.bugdigger.piggybank.ui.navigation.LocalAppState
import com.bugdigger.piggybank.ui.navigation.Screen
import com.bugdigger.piggybank.ui.theme.AccountingColors
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsScreen() {
    val appState = LocalAppState.current
    val scope = rememberCoroutineScope()

    var isLoading by remember { mutableStateOf(false) }
    var accountsTree by remember { mutableStateOf(appState.accountsTree) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        if (accountsTree.isEmpty()) {
            isLoading = true
            appState.api.getAccountsTree()
                .onSuccess { tree ->
                    accountsTree = tree
                    appState.accountsTree = tree
                }
                .onError { error -> errorMessage = "Failed to load accounts: $error" }
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            PiggyBankTopBar(
                title = "Accounts",
                canNavigateBack = true,
                onNavigateBack = { appState.navigateBack() },
                actions = {
                    IconButton(onClick = {
                        scope.launch {
                            isLoading = true
                            appState.api.getAccountsTree().onSuccess {
                                accountsTree = it
                                appState.accountsTree = it
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
                onClick = { /* TODO: Create account dialog */ },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, "New Account")
            }
        }
    ) { padding ->
        LoadingOverlay(isLoading = isLoading, modifier = Modifier.fillMaxSize()) {
            if (accountsTree.isEmpty() && !isLoading) {
                EmptyStateMessage(
                    message = "No accounts found",
                    icon = Icons.Default.AccountBalance,
                    modifier = Modifier.fillMaxSize().padding(padding)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(accountsTree) { rootNode ->
                        AccountTreeItem(
                            node = rootNode,
                            depth = 0,
                            onAccountClick = { accountId ->
                                appState.navigateTo(Screen.AccountDetail(accountId))
                            }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
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
private fun AccountTreeItem(
    node: AccountTreeNode,
    depth: Int,
    onAccountClick: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(depth == 0) }
    val hasChildren = node.children.isNotEmpty()

    val typeColor = when (node.account.type.uppercase()) {
        "ASSET" -> AccountingColors.Asset
        "LIABILITY" -> AccountingColors.Liability
        "EQUITY" -> AccountingColors.Equity
        "INCOME" -> AccountingColors.Income
        "EXPENSE" -> AccountingColors.Expense
        else -> MaterialTheme.colorScheme.primary
    }

    Column {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = (depth * 24).dp)
                .clickable {
                    if (hasChildren) expanded = !expanded
                    else onAccountClick(node.account.id)
                },
            colors = CardDefaults.cardColors(
                containerColor = if (node.account.placeholder) {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                } else {
                    MaterialTheme.colorScheme.surface
                }
            )
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (hasChildren) {
                    IconButton(
                        onClick = { expanded = !expanded },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = if (expanded) Icons.Default.ExpandMore else Icons.Default.ChevronRight,
                            contentDescription = if (expanded) "Collapse" else "Expand",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(24.dp))
                }

                Surface(
                    color = typeColor,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.size(4.dp, 24.dp)
                ) {}

                Spacer(modifier = Modifier.width(8.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = node.account.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (node.account.placeholder) FontWeight.Medium else FontWeight.Normal
                    )
                    if (depth == 0) {
                        Text(
                            text = node.account.type,
                            style = MaterialTheme.typography.bodySmall,
                            color = typeColor
                        )
                    }
                }

                if (!node.account.placeholder) {
                    AmountText(amount = node.balance, style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = { onAccountClick(node.account.id) },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = "View details",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                } else {
                    Text(
                        text = formatAmount(node.balance),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }

        AnimatedVisibility(visible = expanded && hasChildren) {
            Column {
                node.children.forEach { child ->
                    AccountTreeItem(node = child, depth = depth + 1, onAccountClick = onAccountClick)
                }
            }
        }
    }
}

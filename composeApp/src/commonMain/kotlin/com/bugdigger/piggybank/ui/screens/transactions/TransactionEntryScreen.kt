package com.bugdigger.piggybank.ui.screens.transactions

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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.unit.dp
import com.bugdigger.piggybank.domain.model.AccountResponse
import com.bugdigger.piggybank.domain.model.CreateTransactionRequest
import com.bugdigger.piggybank.domain.model.SplitRequest
import com.bugdigger.piggybank.domain.model.UpdateTransactionRequest
import com.bugdigger.piggybank.ui.components.LoadingOverlay
import com.bugdigger.piggybank.ui.components.PiggyBankTextField
import com.bugdigger.piggybank.ui.components.PiggyBankTopBar
import com.bugdigger.piggybank.ui.navigation.LocalAppState
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.math.abs

data class SplitEntry(
    val id: Int,
    var accountId: String = "",
    var accountName: String = "",
    var amount: String = "",
    var currency: String = "USD",
    var memo: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionEntryScreen(transactionId: String? = null) {
    val appState = LocalAppState.current
    val scope = rememberCoroutineScope()
    val isEditing = transactionId != null

    var isLoading by remember { mutableStateOf(isEditing) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var date by remember {
        mutableStateOf(
            Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date.toString()
        )
    }
    var description by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var splits by remember { mutableStateOf(listOf(SplitEntry(1), SplitEntry(2))) }
    var accounts by remember { mutableStateOf<List<AccountResponse>>(emptyList()) }

    LaunchedEffect(Unit) {
        appState.api.getAccounts()
            .onSuccess { list -> accounts = list.filter { !it.placeholder } }
            .onError { error -> errorMessage = "Failed to load accounts: $error" }
    }

    LaunchedEffect(transactionId) {
        if (transactionId != null) {
            appState.api.getTransaction(transactionId)
                .onSuccess { txn ->
                    date = txn.date
                    description = txn.description
                    notes = txn.notes ?: ""
                    splits = txn.splits.mapIndexed { index, split ->
                        SplitEntry(
                            id = index + 1,
                            accountId = split.accountId,
                            accountName = split.accountName,
                            amount = split.amount,
                            currency = split.currency,
                            memo = split.memo ?: ""
                        )
                    }
                }
                .onError { error -> errorMessage = "Failed to load transaction: $error" }
            isLoading = false
        }
    }

    fun validateAndSave() {
        if (description.isBlank()) {
            errorMessage = "Please enter a description"
            return
        }
        if (splits.size < 2) {
            errorMessage = "Transaction must have at least 2 splits"
            return
        }
        val invalidSplits = splits.filter { it.accountId.isBlank() || it.amount.isBlank() }
        if (invalidSplits.isNotEmpty()) {
            errorMessage = "Please fill in all split fields"
            return
        }
        val total = splits.sumOf { it.amount.toDoubleOrNull() ?: 0.0 }
        if (abs(total) > 0.001) {
            errorMessage = "Splits must balance to zero (current: ${"%.2f".format(total)})"
            return
        }

        scope.launch {
            isLoading = true
            errorMessage = null

            val splitRequests = splits.map { split ->
                SplitRequest(
                    accountId = split.accountId,
                    amount = split.amount,
                    currency = split.currency,
                    memo = split.memo.ifBlank { null }
                )
            }

            val result = if (isEditing) {
                appState.api.updateTransaction(
                    transactionId!!,
                    UpdateTransactionRequest(
                        date = date,
                        description = description,
                        notes = notes.ifBlank { null },
                        splits = splitRequests
                    )
                )
            } else {
                appState.api.createTransaction(
                    CreateTransactionRequest(
                        date = date,
                        description = description,
                        notes = notes.ifBlank { null },
                        splits = splitRequests
                    )
                )
            }

            result
                .onSuccess {
                    appState.api.getAccountsTree().onSuccess { appState.accountsTree = it }
                    appState.api.getTransactions(page = 1, pageSize = 10)
                        .onSuccess { appState.recentTransactions = it.transactions }
                    appState.navigateBack()
                }
                .onError { error -> errorMessage = "Failed to save: $error" }

            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            PiggyBankTopBar(
                title = if (isEditing) "Edit Transaction" else "New Transaction",
                canNavigateBack = true,
                onNavigateBack = { appState.navigateBack() }
            )
        }
    ) { padding ->
        LoadingOverlay(isLoading = isLoading, modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(text = "Transaction Details", style = MaterialTheme.typography.titleMedium)

                            PiggyBankTextField(
                                value = date,
                                onValueChange = { date = it },
                                label = "Date (YYYY-MM-DD)",
                                leadingIcon = Icons.Default.CalendarToday
                            )

                            PiggyBankTextField(
                                value = description,
                                onValueChange = { description = it },
                                label = "Description",
                                leadingIcon = Icons.Default.Description
                            )

                            PiggyBankTextField(
                                value = notes,
                                onValueChange = { notes = it },
                                label = "Notes (optional)",
                                leadingIcon = Icons.Default.Notes
                            )
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Splits", style = MaterialTheme.typography.titleMedium)
                        val total = splits.sumOf { it.amount.toDoubleOrNull() ?: 0.0 }
                        val isBalanced = abs(total) < 0.001
                        Surface(
                            color = if (isBalanced) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.errorContainer,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                text = if (isBalanced) "Balanced" else "Unbalanced: ${"%.2f".format(total)}",
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                itemsIndexed(splits) { index, split ->
                    SplitEntryCard(
                        split = split,
                        accounts = accounts,
                        onSplitChange = { updated ->
                            splits = splits.toMutableList().apply { set(index, updated) }
                        },
                        onDelete = if (splits.size > 2) {
                            { splits = splits.filter { it.id != split.id } }
                        } else null,
                        splitNumber = index + 1
                    )
                }

                item {
                    OutlinedButton(
                        onClick = {
                            val newId = (splits.maxOfOrNull { it.id } ?: 0) + 1
                            splits = splits + SplitEntry(newId)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, "Add Split")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add Split")
                    }
                }

                val total = splits.sumOf { it.amount.toDoubleOrNull() ?: 0.0 }
                if (abs(total) > 0.001 && splits.any { it.amount.isBlank() }) {
                    val emptyIndex = splits.indexOfFirst { it.amount.isBlank() }
                    if (emptyIndex >= 0) {
                        item {
                            TextButton(
                                onClick = {
                                    val balancingAmount = -total
                                    splits = splits.toMutableList().apply {
                                        set(emptyIndex, get(emptyIndex).copy(amount = "%.2f".format(balancingAmount)))
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Auto-balance: Set split ${emptyIndex + 1} to ${"%.2f".format(-total)}")
                            }
                        }
                    }
                }

                if (errorMessage != null) {
                    item {
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = errorMessage!!, color = MaterialTheme.colorScheme.onErrorContainer)
                            }
                        }
                    }
                }

                item {
                    Button(
                        onClick = { validateAndSave() },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Save, "Save")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isEditing) "Update Transaction" else "Create Transaction")
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SplitEntryCard(
    split: SplitEntry,
    accounts: List<AccountResponse>,
    onSplitChange: (SplitEntry) -> Unit,
    onDelete: (() -> Unit)?,
    splitNumber: Int
) {
    var accountExpanded by remember { mutableStateOf(false) }
    var currencyExpanded by remember { mutableStateOf(false) }
    val currencies = listOf("USD", "EUR", "RSD")

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Split $splitNumber", style = MaterialTheme.typography.titleSmall)
                if (onDelete != null) {
                    IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete split",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            ExposedDropdownMenuBox(
                expanded = accountExpanded,
                onExpandedChange = { accountExpanded = it }
            ) {
                OutlinedTextField(
                    value = split.accountName.ifBlank { "Select Account" },
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Account") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = accountExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    isError = split.accountId.isBlank()
                )
                ExposedDropdownMenu(
                    expanded = accountExpanded,
                    onDismissRequest = { accountExpanded = false }
                ) {
                    accounts.forEach { account ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(account.fullName)
                                    Text(
                                        text = "${account.type} - ${account.currency}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            },
                            onClick = {
                                onSplitChange(
                                    split.copy(
                                        accountId = account.id,
                                        accountName = account.fullName,
                                        currency = account.currency
                                    )
                                )
                                accountExpanded = false
                            }
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = split.amount,
                    onValueChange = { onSplitChange(split.copy(amount = it)) },
                    label = { Text("Amount") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    isError = split.amount.isNotBlank() && split.amount.toDoubleOrNull() == null,
                    supportingText = { Text("+ debit, - credit") }
                )

                ExposedDropdownMenuBox(
                    expanded = currencyExpanded,
                    onExpandedChange = { currencyExpanded = it },
                    modifier = Modifier.width(100.dp)
                ) {
                    OutlinedTextField(
                        value = split.currency,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Cur.") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = currencyExpanded) },
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = currencyExpanded,
                        onDismissRequest = { currencyExpanded = false }
                    ) {
                        currencies.forEach { currency ->
                            DropdownMenuItem(
                                text = { Text(currency) },
                                onClick = {
                                    onSplitChange(split.copy(currency = currency))
                                    currencyExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            OutlinedTextField(
                value = split.memo,
                onValueChange = { onSplitChange(split.copy(memo = it)) },
                label = { Text("Memo (optional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }
    }
}

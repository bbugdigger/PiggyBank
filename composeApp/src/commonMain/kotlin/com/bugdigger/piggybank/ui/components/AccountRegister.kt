package com.bugdigger.piggybank.ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bugdigger.piggybank.domain.model.AccountRegisterEntry
import com.bugdigger.piggybank.domain.model.AccountResponse
import com.bugdigger.piggybank.ui.theme.PiggyBankColors
import com.bugdigger.piggybank.ui.theme.MonospaceAmount
import com.bugdigger.piggybank.ui.viewmodel.RegisterViewModel
import com.bugdigger.piggybank.ui.viewmodel.AccountsViewModel
import com.bugdigger.piggybank.ui.util.CurrencyUtils
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.viewmodel.koinViewModel

/**
 * State for a new transaction being edited
 */
data class NewTransactionState(
    val date: String = "",
    val num: String = "",
    val description: String = "",
    val transferAccountId: String = "",
    val transferAccountName: String = "",
    val deposit: String = "",
    val withdrawal: String = ""
) {
    val isValid: Boolean
        get() = date.isNotBlank() && 
                description.isNotBlank() && 
                transferAccountId.isNotBlank() &&
                (deposit.isNotBlank() || withdrawal.isNotBlank())
}

/**
 * Which cell is being edited
 */
enum class EditingCell {
    DATE, NUM, DESCRIPTION, TRANSFER, DEPOSIT, WITHDRAWAL
}

@Composable
fun AccountRegister(
    accountId: String,
    accountName: String,
    showSplits: Boolean = false,
    viewModel: RegisterViewModel = koinViewModel(),
    accountsViewModel: AccountsViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    val accountsState by accountsViewModel.state.collectAsState()
    
    // New transaction editing state
    var newTxState by remember { mutableStateOf(NewTransactionState()) }
    var editingCell by remember { mutableStateOf<EditingCell?>(null) }
    var isEditingNewRow by remember { mutableStateOf(false) }
    
    // Get flat list of accounts for the dropdown
    val allAccounts = remember(accountsState.accounts) {
        flattenAccountsForDropdown(accountsState.accounts)
            .filter { it.first != accountId } // Exclude current account
    }
    
    // Load register when account changes
    LaunchedEffect(accountId) {
        viewModel.loadRegister(accountId)
    }
    
    // Reset new transaction state when account changes
    LaunchedEffect(accountId) {
        newTxState = NewTransactionState(
            date = getTodayDate()
        )
        editingCell = null
        isEditingNewRow = false
    }
    
    fun saveNewTransaction() {
        if (!newTxState.isValid) return
        
        // Determine amount (positive for deposit, negative for withdrawal)
        val amount = if (newTxState.deposit.isNotBlank()) {
            newTxState.deposit
        } else {
            "-${newTxState.withdrawal}"
        }
        
        // Get the transfer account's currency (default to USD if not found)
        val transferCurrency = allAccounts
            .find { it.first == newTxState.transferAccountId }
            ?.let { "USD" } ?: "USD" // TODO: Get actual currency from account
        
        viewModel.createSimpleTransaction(
            date = newTxState.date,
            description = newTxState.description,
            amount = amount,
            transferAccountId = newTxState.transferAccountId,
            transferAccountCurrency = transferCurrency,
            num = newTxState.num.takeIf { it.isNotBlank() }
        )
        
        // Reset for next transaction
        newTxState = NewTransactionState(
            date = getTodayDate()
        )
        editingCell = null
        isEditingNewRow = false
    }
    
    fun moveToNextCell() {
        editingCell = when (editingCell) {
            EditingCell.DATE -> EditingCell.NUM
            EditingCell.NUM -> EditingCell.DESCRIPTION
            EditingCell.DESCRIPTION -> EditingCell.TRANSFER
            EditingCell.TRANSFER -> EditingCell.DEPOSIT
            EditingCell.DEPOSIT -> EditingCell.WITHDRAWAL
            EditingCell.WITHDRAWAL -> {
                // At the end, save if valid
                if (newTxState.isValid) {
                    saveNewTransaction()
                }
                null
            }
            null -> null
        }
    }
    
    fun moveToPreviousCell() {
        editingCell = when (editingCell) {
            EditingCell.DATE -> null
            EditingCell.NUM -> EditingCell.DATE
            EditingCell.DESCRIPTION -> EditingCell.NUM
            EditingCell.TRANSFER -> EditingCell.DESCRIPTION
            EditingCell.DEPOSIT -> EditingCell.TRANSFER
            EditingCell.WITHDRAWAL -> EditingCell.DEPOSIT
            null -> null
        }
    }
    
    fun cancelEdit() {
        editingCell = null
        isEditingNewRow = false
        newTxState = NewTransactionState(date = getTodayDate())
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Loading state
        if (state.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Column
        }
        
        // Error state
        state.error?.let { error ->
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Error: $error",
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { viewModel.loadRegister(accountId) }) {
                        Text("Retry")
                    }
                }
            }
            return@Column
        }
        
        // Header row - different columns for split view
        if (showSplits) {
            SplitViewHeader()
        } else {
            RegisterHeader(normalBalance = state.normalBalance)
        }
        
        // Transaction rows
        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            state.entries.forEachIndexed { index, entry ->
                // Main transaction row
                item(key = "entry-$index") {
                    if (showSplits) {
                        SplitViewTransactionRow(
                            entry = entry,
                            isSelected = index == state.selectedEntryIndex && !isEditingNewRow,
                            isEvenRow = index % 2 == 0,
                            onClick = { 
                                viewModel.selectEntry(index)
                                isEditingNewRow = false
                                editingCell = null
                            }
                        )
                    } else {
                        RegisterRow(
                            entry = entry,
                            normalBalance = state.normalBalance,
                            isSelected = index == state.selectedEntryIndex && !isEditingNewRow,
                            isEvenRow = index % 2 == 0,
                            onClick = { 
                                viewModel.selectEntry(index)
                                isEditingNewRow = false
                                editingCell = null
                            },
                            onDoubleClick = { /* TODO: Edit existing transaction */ }
                        )
                    }
                }
                
                // If showSplits is on and this is a split transaction, show the splits
                if (showSplits && entry.isSplit) {
                    val splits = viewModel.getTransactionSplits(entry.transactionId)
                    val isLoading = viewModel.isTransactionLoading(entry.transactionId)
                    
                    // Load splits on demand
                    if (splits == null && !isLoading) {
                        viewModel.toggleExpandTransaction(entry.transactionId)
                    }
                    
                    if (isLoading) {
                        item(key = "loading-${entry.transactionId}") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(28.dp)
                                    .background(PiggyBankColors.ZebraWhite),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                            }
                        }
                    } else {
                        splits?.forEach { split ->
                            item(key = "split-${split.id}") {
                                SplitDetailRow(
                                    split = split,
                                    currentAccountId = accountId,
                                    isEvenRow = index % 2 == 0
                                )
                            }
                        }
                    }
                }
            }
            
            // Editable row for new transaction
            item(key = "new-transaction") {
                EditableNewTransactionRow(
                    state = newTxState,
                    editingCell = editingCell,
                    isSelected = isEditingNewRow,
                    isEvenRow = state.entries.size % 2 == 0,
                    accounts = allAccounts,
                    normalBalance = state.normalBalance,
                    onCellClick = { cell ->
                        isEditingNewRow = true
                        editingCell = cell
                        viewModel.selectEntry(null)
                    },
                    onDateChange = { newTxState = newTxState.copy(date = it) },
                    onNumChange = { newTxState = newTxState.copy(num = it) },
                    onDescriptionChange = { newTxState = newTxState.copy(description = it) },
                    onTransferChange = { id, name -> 
                        newTxState = newTxState.copy(
                            transferAccountId = id,
                            transferAccountName = name
                        )
                    },
                    onDepositChange = { 
                        newTxState = newTxState.copy(deposit = it, withdrawal = "")
                    },
                    onWithdrawalChange = { 
                        newTxState = newTxState.copy(withdrawal = it, deposit = "")
                    },
                    onTab = { moveToNextCell() },
                    onShiftTab = { moveToPreviousCell() },
                    onEnter = { saveNewTransaction() },
                    onEscape = { cancelEdit() }
                )
            }
        }
    }
}

@Composable
private fun RegisterHeader(normalBalance: String) {
    Surface(
        color = PiggyBankColors.TableHeader,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HeaderCell("Date", Modifier.width(100.dp))
            HeaderCell("Num", Modifier.width(60.dp))
            HeaderCell("Description", Modifier.weight(1f))
            HeaderCell("Transfer", Modifier.width(180.dp))
            HeaderCell("R", Modifier.width(30.dp), TextAlign.Center)
            
            val (col1, col2) = if (normalBalance == "DEBIT") {
                "Deposit" to "Withdrawal"
            } else {
                "Increase" to "Decrease"
            }
            
            HeaderCell(col1, Modifier.width(100.dp), TextAlign.End)
            HeaderCell(col2, Modifier.width(100.dp), TextAlign.End)
            HeaderCell("Balance", Modifier.width(110.dp), TextAlign.End)
        }
    }
}

@Composable
private fun HeaderCell(
    text: String,
    modifier: Modifier,
    textAlign: TextAlign = TextAlign.Start
) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = PiggyBankColors.TableHeaderText,
        modifier = modifier.padding(horizontal = 4.dp),
        textAlign = textAlign,
        maxLines = 1
    )
}

@Composable
private fun RegisterRow(
    entry: AccountRegisterEntry,
    normalBalance: String,
    isSelected: Boolean,
    isEvenRow: Boolean,
    onClick: () -> Unit,
    onDoubleClick: () -> Unit
) {
    val backgroundColor = when {
        isSelected -> PiggyBankColors.SelectedRow
        isEvenRow -> PiggyBankColors.ZebraLight
        else -> PiggyBankColors.ZebraWhite
    }
    
    val textDecoration = if (entry.voided) TextDecoration.LineThrough else TextDecoration.None
    val textColor = if (entry.voided) PiggyBankColors.VoidedTransaction else Color.Unspecified
    
    Surface(
        color = backgroundColor,
        modifier = Modifier
            .fillMaxWidth()
            .height(32.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onClick() },
                    onDoubleTap = { onDoubleClick() }
                )
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp)
                .then(
                    if (isSelected) {
                        Modifier.border(1.dp, PiggyBankColors.SelectedRowBorder)
                    } else {
                        Modifier
                    }
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Date
            DataCell(entry.date, Modifier.width(100.dp), textDecoration = textDecoration, color = textColor)
            // Num
            DataCell(entry.num ?: "", Modifier.width(60.dp), textDecoration = textDecoration, color = textColor)
            // Description
            DataCell(entry.description, Modifier.weight(1f), textDecoration = textDecoration, color = textColor)
            // Transfer
            val transferText = if (entry.isSplit) "-- Split Transaction --" else entry.otherAccounts.firstOrNull() ?: ""
            DataCell(
                transferText, 
                Modifier.width(180.dp), 
                textDecoration = textDecoration,
                color = if (entry.isSplit) MaterialTheme.colorScheme.onSurfaceVariant else textColor
            )
            // Reconcile status
            val reconcileSymbol = when (entry.reconcileStatus) {
                "NEW" -> "n"
                "CLEARED" -> "c"
                "RECONCILED" -> "y"
                else -> "n"
            }
            val reconcileColor = when (entry.reconcileStatus) {
                "NEW" -> PiggyBankColors.ReconcileNew
                "CLEARED" -> PiggyBankColors.ReconcileCleared
                "RECONCILED" -> PiggyBankColors.ReconcileReconciled
                else -> PiggyBankColors.ReconcileNew
            }
            DataCell(reconcileSymbol, Modifier.width(30.dp), TextAlign.Center, reconcileColor)
            
            // Parse amount using cross-platform code
            val amount = entry.amount.toDoubleOrNull() ?: 0.0
            val depositAmount = if (amount > 0) CurrencyUtils.formatNumber(amount) else ""
            val withdrawalAmount = if (amount < 0) CurrencyUtils.formatNumber(kotlin.math.abs(amount)) else ""
            
            AmountCell(
                depositAmount, 
                Modifier.width(100.dp),
                if (depositAmount.isNotEmpty()) PiggyBankColors.DepositColor else Color.Unspecified,
                textDecoration
            )
            AmountCell(
                withdrawalAmount, 
                Modifier.width(100.dp),
                if (withdrawalAmount.isNotEmpty()) PiggyBankColors.WithdrawalColor else Color.Unspecified,
                textDecoration
            )
            
            // Balance
            val balance = entry.balance.toDoubleOrNull() ?: 0.0
            AmountCell(CurrencyUtils.formatNumber(balance), Modifier.width(110.dp), textDecoration = textDecoration)
        }
    }
    
    HorizontalDivider(thickness = 0.5.dp, color = PiggyBankColors.CellBorder)
}

@Composable
private fun EditableNewTransactionRow(
    state: NewTransactionState,
    editingCell: EditingCell?,
    isSelected: Boolean,
    isEvenRow: Boolean,
    accounts: List<Pair<String, String>>,
    normalBalance: String,
    onCellClick: (EditingCell) -> Unit,
    onDateChange: (String) -> Unit,
    onNumChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onTransferChange: (String, String) -> Unit,
    onDepositChange: (String) -> Unit,
    onWithdrawalChange: (String) -> Unit,
    onTab: () -> Unit,
    onShiftTab: () -> Unit,
    onEnter: () -> Unit,
    onEscape: () -> Unit
) {
    val backgroundColor = when {
        isSelected -> PiggyBankColors.SelectedRow
        isEvenRow -> PiggyBankColors.ZebraLight
        else -> PiggyBankColors.ZebraWhite
    }
    
    Surface(
        color = backgroundColor,
        modifier = Modifier
            .fillMaxWidth()
            .height(32.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp)
                .then(
                    if (isSelected) {
                        Modifier.border(1.dp, PiggyBankColors.SelectedRowBorder)
                    } else {
                        Modifier
                    }
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Date
            EditableCell(
                value = state.date,
                onValueChange = onDateChange,
                modifier = Modifier.width(100.dp),
                isEditing = editingCell == EditingCell.DATE,
                onEditStart = { onCellClick(EditingCell.DATE) },
                onTab = onTab,
                onShiftTab = onShiftTab,
                onEnter = onEnter,
                onEscape = onEscape,
                placeholder = getTodayDate()
            )
            
            // Num
            EditableCell(
                value = state.num,
                onValueChange = onNumChange,
                modifier = Modifier.width(60.dp),
                isEditing = editingCell == EditingCell.NUM,
                onEditStart = { onCellClick(EditingCell.NUM) },
                onTab = onTab,
                onShiftTab = onShiftTab,
                onEnter = onEnter,
                onEscape = onEscape,
                placeholder = "Num"
            )
            
            // Description
            EditableCell(
                value = state.description,
                onValueChange = onDescriptionChange,
                modifier = Modifier.weight(1f),
                isEditing = editingCell == EditingCell.DESCRIPTION,
                onEditStart = { onCellClick(EditingCell.DESCRIPTION) },
                onTab = onTab,
                onShiftTab = onShiftTab,
                onEnter = onEnter,
                onEscape = onEscape,
                placeholder = "Description"
            )
            
            // Transfer account
            AccountSelectorCell(
                value = state.transferAccountName,
                onValueChange = onTransferChange,
                accounts = accounts,
                modifier = Modifier.width(180.dp),
                isEditing = editingCell == EditingCell.TRANSFER,
                onEditStart = { onCellClick(EditingCell.TRANSFER) },
                onTab = onTab,
                onShiftTab = onShiftTab,
                onEnter = onEnter,
                onEscape = onEscape,
                placeholder = "Account"
            )
            
            // R (reconcile) - not editable for new transactions
            DataCell("n", Modifier.width(30.dp), TextAlign.Center, PiggyBankColors.ReconcileNew)
            
            // Deposit
            AmountEditableCell(
                value = state.deposit,
                onValueChange = onDepositChange,
                modifier = Modifier.width(100.dp),
                isEditing = editingCell == EditingCell.DEPOSIT,
                onEditStart = { onCellClick(EditingCell.DEPOSIT) },
                onTab = onTab,
                onShiftTab = onShiftTab,
                onEnter = onEnter,
                onEscape = onEscape,
                textColor = if (state.deposit.isNotBlank()) PiggyBankColors.DepositColor else Color.Unspecified
            )
            
            // Withdrawal
            AmountEditableCell(
                value = state.withdrawal,
                onValueChange = onWithdrawalChange,
                modifier = Modifier.width(100.dp),
                isEditing = editingCell == EditingCell.WITHDRAWAL,
                onEditStart = { onCellClick(EditingCell.WITHDRAWAL) },
                onTab = onTab,
                onShiftTab = onShiftTab,
                onEnter = onEnter,
                onEscape = onEscape,
                textColor = if (state.withdrawal.isNotBlank()) PiggyBankColors.WithdrawalColor else Color.Unspecified
            )
            
            // Balance - empty for new transaction
            DataCell("", Modifier.width(110.dp))
        }
    }
    
    HorizontalDivider(thickness = 0.5.dp, color = PiggyBankColors.CellBorder)
}

@Composable
private fun DataCell(
    text: String,
    modifier: Modifier,
    textAlign: TextAlign = TextAlign.Start,
    color: Color = Color.Unspecified,
    textDecoration: TextDecoration = TextDecoration.None
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium.copy(textDecoration = textDecoration),
        color = color,
        modifier = modifier.padding(horizontal = 4.dp),
        textAlign = textAlign,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun AmountCell(
    text: String,
    modifier: Modifier,
    color: Color = Color.Unspecified,
    textDecoration: TextDecoration = TextDecoration.None
) {
    Text(
        text = text,
        style = MonospaceAmount.copy(textDecoration = textDecoration),
        color = color,
        modifier = modifier.padding(horizontal = 4.dp),
        textAlign = TextAlign.End,
        maxLines = 1
    )
}

private fun getTodayDate(): String {
    val now = Clock.System.now()
    val localDate = now.toLocalDateTime(TimeZone.currentSystemDefault()).date
    return "${localDate.year}-${localDate.monthNumber.toString().padStart(2, '0')}-${localDate.dayOfMonth.toString().padStart(2, '0')}"
}

/**
 * Flatten account tree to a list of (id, fullName) pairs for dropdown
 */
private fun flattenAccountsForDropdown(
    nodes: List<com.bugdigger.piggybank.domain.model.AccountTreeNode>
): List<Pair<String, String>> {
    val result = mutableListOf<Pair<String, String>>()
    
    fun traverse(node: com.bugdigger.piggybank.domain.model.AccountTreeNode) {
        // Only add non-placeholder accounts (accounts that can have transactions)
        if (!node.account.placeholder) {
            result.add(node.account.id to node.account.fullName)
        }
        node.children.forEach { traverse(it) }
    }
    
    nodes.forEach { traverse(it) }
    return result.sortedBy { it.second }
}

// ============ Split View Components ============

/**
 * Header for split view mode - matches GnuCash 2.png
 * Columns: Date | Action | Memo | Account | R | Deposit | Withdrawal
 */
@Composable
private fun SplitViewHeader() {
    Surface(
        color = PiggyBankColors.TableHeader,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HeaderCell("Date", Modifier.width(100.dp))
            HeaderCell("Action", Modifier.width(60.dp))
            HeaderCell("Memo", Modifier.weight(1f))
            HeaderCell("Account", Modifier.width(220.dp))
            HeaderCell("R", Modifier.width(30.dp), TextAlign.Center)
            HeaderCell("Deposit", Modifier.width(100.dp), TextAlign.End)
            HeaderCell("Withdrawal", Modifier.width(100.dp), TextAlign.End)
        }
    }
}

/**
 * Transaction header row in split view - shows date and description
 */
@Composable
private fun SplitViewTransactionRow(
    entry: AccountRegisterEntry,
    isSelected: Boolean,
    isEvenRow: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = when {
        isSelected -> PiggyBankColors.SelectedRow
        isEvenRow -> PiggyBankColors.ZebraLight
        else -> PiggyBankColors.ZebraWhite
    }
    
    val textDecoration = if (entry.voided) TextDecoration.LineThrough else TextDecoration.None
    val textColor = if (entry.voided) PiggyBankColors.VoidedTransaction else Color.Unspecified
    
    Surface(
        color = backgroundColor,
        modifier = Modifier
            .fillMaxWidth()
            .height(28.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp)
                .then(
                    if (isSelected) {
                        Modifier.border(1.dp, PiggyBankColors.SelectedRowBorder)
                    } else {
                        Modifier
                    }
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Date
            DataCell(
                entry.date, 
                Modifier.width(100.dp), 
                textDecoration = textDecoration, 
                color = textColor
            )
            // Action (empty for main row)
            DataCell("", Modifier.width(60.dp))
            // Description (in memo column position for main row)
            Text(
                text = entry.description,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                    textDecoration = textDecoration
                ),
                color = if (textColor != Color.Unspecified) textColor else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            // Account (empty for main row)
            DataCell("", Modifier.width(220.dp))
            // R (empty for main row)
            DataCell("", Modifier.width(30.dp))
            // Deposit - total deposit amount
            val amount = entry.amount.toDoubleOrNull() ?: 0.0
            val depositAmount = if (amount > 0) CurrencyUtils.formatNumber(amount) else ""
            AmountCell(
                depositAmount,
                Modifier.width(100.dp),
                if (depositAmount.isNotEmpty()) PiggyBankColors.DepositColor else Color.Unspecified,
                textDecoration
            )
            // Withdrawal - total withdrawal amount
            val withdrawalAmount = if (amount < 0) CurrencyUtils.formatNumber(kotlin.math.abs(amount)) else ""
            AmountCell(
                withdrawalAmount,
                Modifier.width(100.dp),
                if (withdrawalAmount.isNotEmpty()) PiggyBankColors.WithdrawalColor else Color.Unspecified,
                textDecoration
            )
        }
    }
    
    HorizontalDivider(thickness = 0.5.dp, color = PiggyBankColors.CellBorder)
}

/**
 * Individual split detail row
 */
@Composable
private fun SplitDetailRow(
    split: com.bugdigger.piggybank.domain.model.SplitResponse,
    currentAccountId: String,
    isEvenRow: Boolean
) {
    val backgroundColor = if (isEvenRow) {
        PiggyBankColors.ZebraLight.copy(alpha = 0.7f)
    } else {
        PiggyBankColors.ZebraWhite
    }
    
    // Highlight the current account's split
    val isCurrentAccount = split.accountId == currentAccountId
    val fontWeight = if (isCurrentAccount) {
        androidx.compose.ui.text.font.FontWeight.Medium
    } else {
        androidx.compose.ui.text.font.FontWeight.Normal
    }
    
    Surface(
        color = backgroundColor,
        modifier = Modifier
            .fillMaxWidth()
            .height(26.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Date (empty - shown on parent row)
            Spacer(Modifier.width(100.dp))
            // Action (split indicator)
            Text(
                text = "Action",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(60.dp).padding(horizontal = 4.dp)
            )
            // Memo
            Text(
                text = split.memo ?: "",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = fontWeight),
                modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            // Account name
            Text(
                text = split.accountName,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = fontWeight),
                modifier = Modifier.width(220.dp).padding(horizontal = 4.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            // Reconcile status
            val reconcileSymbol = when (split.reconcileStatus) {
                "NEW" -> "n"
                "CLEARED" -> "c"
                "RECONCILED" -> "y"
                else -> "n"
            }
            val reconcileColor = when (split.reconcileStatus) {
                "NEW" -> PiggyBankColors.ReconcileNew
                "CLEARED" -> PiggyBankColors.ReconcileCleared
                "RECONCILED" -> PiggyBankColors.ReconcileReconciled
                else -> PiggyBankColors.ReconcileNew
            }
            Text(
                text = reconcileSymbol,
                style = MaterialTheme.typography.bodySmall,
                color = reconcileColor,
                modifier = Modifier.width(30.dp).padding(horizontal = 4.dp),
                textAlign = TextAlign.Center
            )
            
            // Parse amount - positive = debit, negative = credit
            val amount = split.amount.toDoubleOrNull() ?: 0.0
            val depositAmount = if (amount > 0) CurrencyUtils.formatNumber(amount) else ""
            val withdrawalAmount = if (amount < 0) CurrencyUtils.formatNumber(kotlin.math.abs(amount)) else ""
            
            // Deposit
            Text(
                text = depositAmount,
                style = MonospaceAmount.copy(
                    fontSize = MaterialTheme.typography.bodySmall.fontSize,
                    fontWeight = fontWeight
                ),
                color = if (depositAmount.isNotEmpty()) PiggyBankColors.DepositColor else Color.Unspecified,
                modifier = Modifier.width(100.dp).padding(horizontal = 4.dp),
                textAlign = TextAlign.End
            )
            
            // Withdrawal
            Text(
                text = withdrawalAmount,
                style = MonospaceAmount.copy(
                    fontSize = MaterialTheme.typography.bodySmall.fontSize,
                    fontWeight = fontWeight
                ),
                color = if (withdrawalAmount.isNotEmpty()) PiggyBankColors.WithdrawalColor else Color.Unspecified,
                modifier = Modifier.width(100.dp).padding(horizontal = 4.dp),
                textAlign = TextAlign.End
            )
        }
    }
    
    HorizontalDivider(thickness = 0.5.dp, color = PiggyBankColors.CellBorder.copy(alpha = 0.5f))
}

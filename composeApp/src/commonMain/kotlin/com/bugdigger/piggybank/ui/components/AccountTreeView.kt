package com.bugdigger.piggybank.ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bugdigger.piggybank.domain.model.AccountTreeNode
import com.bugdigger.piggybank.ui.theme.PiggyBankColors
import com.bugdigger.piggybank.ui.theme.MonospaceAmount
import com.bugdigger.piggybank.ui.theme.accountTypeColor
import com.bugdigger.piggybank.ui.util.CurrencyUtils

/**
 * Flattened account for display in the tree view
 */
private data class FlattenedAccount(
    val node: AccountTreeNode,
    val depth: Int,
    val hasChildren: Boolean
)

@Composable
fun AccountTreeView(
    accounts: List<AccountTreeNode>,
    expandedIds: Set<String>,
    onToggleExpand: (String) -> Unit,
    onAccountDoubleClick: (AccountTreeNode) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedAccountId by remember { mutableStateOf<String?>(null) }
    
    // Flatten the tree for display
    val flattenedAccounts = remember(accounts, expandedIds) {
        flattenTree(accounts, expandedIds)
    }
    
    Column(modifier = modifier.fillMaxSize()) {
        // Header row
        AccountTreeHeader()
        
        // Account rows
        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            itemsIndexed(flattenedAccounts) { index, flatAccount ->
                AccountTreeRow(
                    flatAccount = flatAccount,
                    isSelected = flatAccount.node.account.id == selectedAccountId,
                    isExpanded = flatAccount.node.account.id in expandedIds,
                    isEvenRow = index % 2 == 0,
                    onClick = { selectedAccountId = flatAccount.node.account.id },
                    onDoubleClick = { onAccountDoubleClick(flatAccount.node) },
                    onToggleExpand = { onToggleExpand(flatAccount.node.account.id) }
                )
            }
        }
    }
}

@Composable
private fun AccountTreeHeader() {
    Surface(
        color = PiggyBankColors.TableHeader,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Account Name column
            Text(
                text = "Account Name",
                style = MaterialTheme.typography.titleMedium,
                color = PiggyBankColors.TableHeaderText,
                modifier = Modifier.weight(2f)
            )
            
            // Description column
            Text(
                text = "Description",
                style = MaterialTheme.typography.titleMedium,
                color = PiggyBankColors.TableHeaderText,
                modifier = Modifier.weight(1.5f)
            )
            
            // Currency column
            Text(
                text = "Currency",
                style = MaterialTheme.typography.titleMedium,
                color = PiggyBankColors.TableHeaderText,
                modifier = Modifier.width(80.dp),
                textAlign = TextAlign.Center
            )
            
            // Balance column
            Text(
                text = "Balance",
                style = MaterialTheme.typography.titleMedium,
                color = PiggyBankColors.TableHeaderText,
                modifier = Modifier.width(120.dp),
                textAlign = TextAlign.End
            )
        }
    }
}

@Composable
private fun AccountTreeRow(
    flatAccount: FlattenedAccount,
    isSelected: Boolean,
    isExpanded: Boolean,
    isEvenRow: Boolean,
    onClick: () -> Unit,
    onDoubleClick: () -> Unit,
    onToggleExpand: () -> Unit
) {
    val account = flatAccount.node.account
    val balance = flatAccount.node.balance
    val depth = flatAccount.depth
    val hasChildren = flatAccount.hasChildren
    
    val backgroundColor = when {
        isSelected -> PiggyBankColors.SelectedRow
        isEvenRow -> PiggyBankColors.ZebraLight
        else -> PiggyBankColors.ZebraWhite
    }
    
    Surface(
        color = backgroundColor,
        modifier = Modifier
            .fillMaxWidth()
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
                .padding(horizontal = 8.dp, vertical = 6.dp)
                .then(
                    if (isSelected) {
                        Modifier.border(1.dp, PiggyBankColors.SelectedRowBorder)
                    } else {
                        Modifier
                    }
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Account Name with indentation and expand/collapse
            Row(
                modifier = Modifier.weight(2f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Indentation
                Spacer(modifier = Modifier.width((depth * 24).dp))
                
                // Expand/Collapse icon or spacer
                if (hasChildren) {
                    IconButton(
                        onClick = onToggleExpand,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = if (isExpanded) {
                                Icons.Default.ExpandMore
                            } else {
                                Icons.Default.ChevronRight
                            },
                            contentDescription = if (isExpanded) "Collapse" else "Expand",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(24.dp))
                }
                
                // Type indicator (small colored dot)
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            color = accountTypeColor(account.type),
                            shape = MaterialTheme.shapes.small
                        )
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                // Account name
                Text(
                    text = account.name,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = if (account.placeholder) FontWeight.SemiBold else FontWeight.Normal
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            // Description
            Text(
                text = account.description ?: "",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1.5f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            // Currency
            Text(
                text = if (!account.placeholder) account.currency else "",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.width(80.dp),
                textAlign = TextAlign.Center
            )
            
            // Balance - show with currency formatting
            val balanceValue = balance.toDoubleOrNull() ?: 0.0
            val formattedBalance = CurrencyUtils.formatWithCurrency(balanceValue, account.currency)
            val balanceColor = when {
                balanceValue < 0 -> PiggyBankColors.WithdrawalColor
                balanceValue > 0 -> PiggyBankColors.DepositColor
                else -> MaterialTheme.colorScheme.onSurface
            }
            
            Text(
                text = formattedBalance,
                style = MonospaceAmount,
                color = balanceColor,
                modifier = Modifier.width(120.dp),
                textAlign = TextAlign.End
            )
        }
    }
    
    // Divider
    HorizontalDivider(
        thickness = 0.5.dp,
        color = PiggyBankColors.CellBorder
    )
}

/**
 * Flatten the tree structure for display, respecting expanded state
 */
private fun flattenTree(
    nodes: List<AccountTreeNode>,
    expandedIds: Set<String>,
    depth: Int = 0
): List<FlattenedAccount> {
    val result = mutableListOf<FlattenedAccount>()
    
    for (node in nodes) {
        result.add(
            FlattenedAccount(
                node = node,
                depth = depth,
                hasChildren = node.children.isNotEmpty()
            )
        )
        
        // If expanded, add children
        if (node.account.id in expandedIds && node.children.isNotEmpty()) {
            result.addAll(flattenTree(node.children, expandedIds, depth + 1))
        }
    }
    
    return result
}

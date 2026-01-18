package com.bugdigger.piggybank.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bugdigger.piggybank.ui.components.AccountTreeView
import com.bugdigger.piggybank.ui.components.AccountRegister
import com.bugdigger.piggybank.ui.theme.PiggyBankColors
import com.bugdigger.piggybank.ui.viewmodel.AccountsViewModel
import org.koin.compose.viewmodel.koinViewModel

/**
 * Represents a tab in the main screen
 */
sealed class Tab(val id: String, val title: String, val closeable: Boolean = true) {
    data object Accounts : Tab("accounts", "Accounts", closeable = false)
    data class AccountRegisterTab(
        val accountId: String,
        val accountName: String
    ) : Tab(accountId, accountName)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onLogout: () -> Unit,
    accountsViewModel: AccountsViewModel = koinViewModel()
) {
    val accountsState by accountsViewModel.state.collectAsState()
    
    // Tab management
    var tabs by remember { mutableStateOf(listOf<Tab>(Tab.Accounts)) }
    var activeTabId by remember { mutableStateOf(Tab.Accounts.id) }
    
    // Show splits toggle (for register view)
    var showSplits by remember { mutableStateOf(false) }
    
    // Load accounts on first composition
    LaunchedEffect(Unit) {
        accountsViewModel.loadAccounts()
    }
    
    fun openAccountRegister(accountId: String, accountName: String) {
        // Check if tab already exists
        val existingTab = tabs.find { it.id == accountId }
        if (existingTab != null) {
            activeTabId = accountId
        } else {
            val newTab = Tab.AccountRegisterTab(accountId, accountName)
            tabs = tabs + newTab
            activeTabId = accountId
        }
    }
    
    fun closeTab(tabId: String) {
        val tab = tabs.find { it.id == tabId } ?: return
        if (!tab.closeable) return
        
        val tabIndex = tabs.indexOfFirst { it.id == tabId }
        tabs = tabs.filter { it.id != tabId }
        
        // If closing active tab, switch to previous tab or Accounts
        if (activeTabId == tabId) {
            activeTabId = if (tabIndex > 0) {
                tabs.getOrNull(tabIndex - 1)?.id ?: Tab.Accounts.id
            } else {
                Tab.Accounts.id
            }
        }
    }
    
    Scaffold(
        topBar = {
            Column {
                // Menu bar
                TopAppBar(
                    title = { Text("PiggyBank") },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = PiggyBankColors.Primary,
                        titleContentColor = PiggyBankColors.OnPrimary
                    ),
                    actions = {
                        // Refresh button
                        IconButton(onClick = { accountsViewModel.loadAccounts() }) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "Refresh",
                                tint = PiggyBankColors.OnPrimary
                            )
                        }
                        
                        // Logout button
                        IconButton(onClick = onLogout) {
                            Icon(
                                Icons.AutoMirrored.Filled.Logout,
                                contentDescription = "Logout",
                                tint = PiggyBankColors.OnPrimary
                            )
                        }
                    }
                )
                
                // Toolbar
                Surface(
                    color = PiggyBankColors.SurfaceVariant,
                    tonalElevation = 1.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // New Account button
                        OutlinedButton(
                            onClick = { /* TODO: New account dialog */ },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("New Account", style = MaterialTheme.typography.labelMedium)
                        }
                        
                        VerticalDivider(
                            modifier = Modifier.height(24.dp),
                            thickness = 1.dp,
                            color = PiggyBankColors.CellBorder
                        )
                        
                        // Expand/Collapse buttons
                        IconButton(
                            onClick = { accountsViewModel.expandAll() },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.UnfoldMore,
                                contentDescription = "Expand All",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        
                        IconButton(
                            onClick = { accountsViewModel.collapseAll() },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.UnfoldLess,
                                contentDescription = "Collapse All",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        
                        // Show splits toggle (only relevant for register tabs)
                        val activeTab = tabs.find { it.id == activeTabId }
                        if (activeTab is Tab.AccountRegisterTab) {
                            VerticalDivider(
                                modifier = Modifier.height(24.dp),
                                thickness = 1.dp,
                                color = PiggyBankColors.CellBorder
                            )
                            
                            OutlinedButton(
                                onClick = { showSplits = !showSplits },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                colors = if (showSplits) {
                                    ButtonDefaults.outlinedButtonColors(
                                        containerColor = PiggyBankColors.Primary.copy(alpha = 0.1f)
                                    )
                                } else {
                                    ButtonDefaults.outlinedButtonColors()
                                }
                            ) {
                                Icon(
                                    Icons.Default.TableRows,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text("Show Splits", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
                
                // Tab bar
                TabBar(
                    tabs = tabs,
                    activeTabId = activeTabId,
                    onTabClick = { activeTabId = it },
                    onTabClose = { closeTab(it) }
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Show loading indicator
            if (accountsState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            
            // Show error if any
            accountsState.error?.let { error ->
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Error: $error",
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { accountsViewModel.loadAccounts() }) {
                        Text("Retry")
                    }
                }
            }
            
            // Content based on active tab
            if (!accountsState.isLoading && accountsState.error == null) {
                when (val activeTab = tabs.find { it.id == activeTabId }) {
                    Tab.Accounts -> AccountTreeView(
                        accounts = accountsState.accounts,
                        expandedIds = accountsState.expandedAccountIds,
                        onToggleExpand = { accountsViewModel.toggleExpanded(it) },
                        onAccountDoubleClick = { account ->
                            openAccountRegister(account.account.id, account.account.name)
                        }
                    )
                    is Tab.AccountRegisterTab -> AccountRegister(
                        accountId = activeTab.accountId,
                        accountName = activeTab.accountName,
                        showSplits = showSplits
                    )
                    null -> {
                        // This shouldn't happen, but show Accounts view as fallback
                        AccountTreeView(
                            accounts = accountsState.accounts,
                            expandedIds = accountsState.expandedAccountIds,
                            onToggleExpand = { accountsViewModel.toggleExpanded(it) },
                            onAccountDoubleClick = { account ->
                                openAccountRegister(account.account.id, account.account.name)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TabBar(
    tabs: List<Tab>,
    activeTabId: String,
    onTabClick: (String) -> Unit,
    onTabClose: (String) -> Unit
) {
    Surface(
        color = PiggyBankColors.SurfaceVariant,
        tonalElevation = 2.dp
    ) {
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            itemsIndexed(tabs) { index, tab ->
                TabItem(
                    tab = tab,
                    isActive = tab.id == activeTabId,
                    onClick = { onTabClick(tab.id) },
                    onClose = { onTabClose(tab.id) }
                )
            }
        }
    }
}

@Composable
private fun TabItem(
    tab: Tab,
    isActive: Boolean,
    onClick: () -> Unit,
    onClose: () -> Unit
) {
    Surface(
        modifier = Modifier
            .clickable(onClick = onClick),
        color = if (isActive) {
            MaterialTheme.colorScheme.surface
        } else {
            PiggyBankColors.SurfaceVariant
        },
        shape = MaterialTheme.shapes.small.copy(
            bottomStart = androidx.compose.foundation.shape.CornerSize(0.dp),
            bottomEnd = androidx.compose.foundation.shape.CornerSize(0.dp)
        )
    ) {
        Row(
            modifier = Modifier
                .padding(start = 12.dp, end = if (tab.closeable) 4.dp else 12.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Tab icon
            Icon(
                imageVector = when (tab) {
                    Tab.Accounts -> Icons.Default.AccountTree
                    is Tab.AccountRegisterTab -> Icons.Default.TableChart
                },
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = if (isActive) {
                    PiggyBankColors.Primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            
            Spacer(Modifier.width(6.dp))
            
            // Tab title
            Text(
                text = tab.title,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (isActive) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            
            // Close button
            if (tab.closeable) {
                Spacer(Modifier.width(4.dp))
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close tab",
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

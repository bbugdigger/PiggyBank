package com.bugdigger.piggybank.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bugdigger.piggybank.data.api.PiggyBankApi
import com.bugdigger.piggybank.domain.model.AccountTreeNode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AccountsState(
    val isLoading: Boolean = false,
    val accounts: List<AccountTreeNode> = emptyList(),
    val expandedAccountIds: Set<String> = emptySet(),
    val error: String? = null
)

class AccountsViewModel(
    private val api: PiggyBankApi
) : ViewModel() {
    
    private val _state = MutableStateFlow(AccountsState())
    val state: StateFlow<AccountsState> = _state.asStateFlow()
    
    fun loadAccounts() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            
            val result = api.getAccountsTree()
            
            result.onSuccess { accounts ->
                // Auto-expand root accounts on first load
                val rootIds = accounts.map { it.account.id }.toSet()
                _state.value = AccountsState(
                    accounts = accounts,
                    expandedAccountIds = if (_state.value.expandedAccountIds.isEmpty()) rootIds else _state.value.expandedAccountIds
                )
            }.onError { error ->
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = error
                )
            }
        }
    }
    
    fun toggleExpanded(accountId: String) {
        val current = _state.value.expandedAccountIds
        _state.value = _state.value.copy(
            expandedAccountIds = if (accountId in current) {
                current - accountId
            } else {
                current + accountId
            }
        )
    }
    
    fun expandAll() {
        val allIds = collectAllAccountIds(_state.value.accounts)
        _state.value = _state.value.copy(expandedAccountIds = allIds)
    }
    
    fun collapseAll() {
        _state.value = _state.value.copy(expandedAccountIds = emptySet())
    }
    
    private fun collectAllAccountIds(nodes: List<AccountTreeNode>): Set<String> {
        val ids = mutableSetOf<String>()
        fun collect(node: AccountTreeNode) {
            ids.add(node.account.id)
            node.children.forEach { collect(it) }
        }
        nodes.forEach { collect(it) }
        return ids
    }
    
    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}

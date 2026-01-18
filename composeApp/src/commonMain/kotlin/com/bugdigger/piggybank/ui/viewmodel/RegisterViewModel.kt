package com.bugdigger.piggybank.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bugdigger.piggybank.data.api.PiggyBankApi
import com.bugdigger.piggybank.domain.model.AccountRegisterEntry
import com.bugdigger.piggybank.domain.model.AccountRegisterResponse
import com.bugdigger.piggybank.domain.model.CreateTransactionRequest
import com.bugdigger.piggybank.domain.model.SplitRequest
import com.bugdigger.piggybank.domain.model.SplitResponse
import com.bugdigger.piggybank.domain.model.TransactionResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class RegisterState(
    val isLoading: Boolean = false,
    val accountId: String? = null,
    val accountName: String = "",
    val accountType: String = "",
    val normalBalance: String = "DEBIT",
    val entries: List<AccountRegisterEntry> = emptyList(),
    val openingBalance: String = "0",
    val closingBalance: String = "0",
    val selectedEntryIndex: Int? = null,
    val expandedTransactionIds: Set<String> = emptySet(),
    // Cache of full transaction details for expanded rows
    val transactionDetails: Map<String, TransactionResponse> = emptyMap(),
    val loadingTransactionIds: Set<String> = emptySet(),
    val error: String? = null
)

class RegisterViewModel(
    private val api: PiggyBankApi
) : ViewModel() {
    
    private val _state = MutableStateFlow(RegisterState())
    val state: StateFlow<RegisterState> = _state.asStateFlow()
    
    fun loadRegister(accountId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null, accountId = accountId)
            
            val result = api.getAccountRegister(accountId)
            
            result.onSuccess { response ->
                _state.value = _state.value.copy(
                    isLoading = false,
                    accountId = response.accountId,
                    accountName = response.accountName,
                    accountType = response.accountType,
                    normalBalance = response.normalBalance,
                    entries = response.entries,
                    openingBalance = response.openingBalance,
                    closingBalance = response.closingBalance
                )
            }.onError { error ->
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = error
                )
            }
        }
    }
    
    fun selectEntry(index: Int?) {
        _state.value = _state.value.copy(selectedEntryIndex = index)
    }
    
    fun toggleExpandTransaction(transactionId: String) {
        val current = _state.value.expandedTransactionIds
        val isExpanding = transactionId !in current
        
        _state.value = _state.value.copy(
            expandedTransactionIds = if (isExpanding) {
                current + transactionId
            } else {
                current - transactionId
            }
        )
        
        // If expanding and we don't have the transaction details yet, fetch them
        if (isExpanding && transactionId !in _state.value.transactionDetails) {
            loadTransactionDetails(transactionId)
        }
    }
    
    private fun loadTransactionDetails(transactionId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(
                loadingTransactionIds = _state.value.loadingTransactionIds + transactionId
            )
            
            val result = api.getTransaction(transactionId)
            
            result.onSuccess { transaction ->
                _state.value = _state.value.copy(
                    transactionDetails = _state.value.transactionDetails + (transactionId to transaction),
                    loadingTransactionIds = _state.value.loadingTransactionIds - transactionId
                )
            }.onError { error ->
                _state.value = _state.value.copy(
                    loadingTransactionIds = _state.value.loadingTransactionIds - transactionId,
                    error = "Failed to load transaction: $error"
                )
            }
        }
    }
    
    fun getTransactionSplits(transactionId: String): List<SplitResponse>? {
        return _state.value.transactionDetails[transactionId]?.splits
    }
    
    fun isTransactionLoading(transactionId: String): Boolean {
        return transactionId in _state.value.loadingTransactionIds
    }
    
    fun createSimpleTransaction(
        date: String,
        description: String,
        amount: String,
        transferAccountId: String,
        transferAccountCurrency: String,
        num: String? = null
    ) {
        val accountId = _state.value.accountId ?: return
        
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            
            // For a simple transfer, we need two splits that sum to zero
            // The amount entered is from the perspective of the current account
            val amountDecimal = amount.toBigDecimalOrNull() ?: return@launch
            
            val request = CreateTransactionRequest(
                date = date,
                num = num,
                description = description,
                splits = listOf(
                    SplitRequest(
                        accountId = accountId,
                        amount = amount,  // Positive = debit for this account
                        currency = transferAccountCurrency
                    ),
                    SplitRequest(
                        accountId = transferAccountId,
                        amount = (-amountDecimal).toString(),  // Opposite sign
                        currency = transferAccountCurrency
                    )
                )
            )
            
            val result = api.createTransaction(request)
            
            result.onSuccess {
                // Reload the register to show the new transaction
                loadRegister(accountId)
            }.onError { error ->
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = error
                )
            }
        }
    }
    
    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
    
    fun clear() {
        _state.value = RegisterState()
    }
}

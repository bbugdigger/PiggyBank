package com.bugdigger.piggybank.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class SplitRequest(
    val accountId: String,
    val amount: String, // BigDecimal as string for precision
    val currency: String, // USD, EUR, RSD
    val memo: String? = null,
    val reconcileStatus: String? = null // NEW, CLEARED, RECONCILED
)

@Serializable
data class CreateTransactionRequest(
    val date: String, // ISO-8601 date: YYYY-MM-DD
    val num: String? = null, // Check number, invoice number, reference
    val description: String,
    val notes: String? = null,
    val splits: List<SplitRequest>
)

@Serializable
data class UpdateTransactionRequest(
    val date: String? = null,
    val num: String? = null,
    val description: String? = null,
    val notes: String? = null,
    val splits: List<SplitRequest>? = null
)

@Serializable
data class SplitResponse(
    val id: String,
    val accountId: String,
    val accountName: String,
    val amount: String,
    val currency: String,
    val memo: String?,
    val reconcileStatus: String // NEW, CLEARED, RECONCILED
)

@Serializable
data class TransactionResponse(
    val id: String,
    val date: String,
    val num: String?,
    val description: String,
    val notes: String?,
    val voided: Boolean,
    val voidReason: String?,
    val splits: List<SplitResponse>,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class TransactionListResponse(
    val transactions: List<TransactionResponse>,
    val total: Int,
    val page: Int,
    val pageSize: Int
)

/**
 * For account register view - shows transaction from perspective of a single account
 */
@Serializable
data class AccountRegisterEntry(
    val transactionId: String,
    val splitId: String, // ID of the split for this account (for reconciliation updates)
    val date: String,
    val num: String?, // Check/reference number
    val description: String,
    val memo: String?, // Split-level memo
    val amount: String, // Amount for this account (positive = debit, negative = credit)
    val balance: String, // Running balance after this transaction
    val reconcileStatus: String, // NEW, CLEARED, RECONCILED
    val voided: Boolean,
    val otherAccounts: List<String>, // Names of other accounts in this transaction
    val isSplit: Boolean // True if transaction has more than 2 splits
)

@Serializable
data class AccountRegisterResponse(
    val accountId: String,
    val accountName: String,
    val accountType: String,
    val normalBalance: String, // DEBIT or CREDIT - for UI column labels
    val entries: List<AccountRegisterEntry>,
    val openingBalance: String,
    val closingBalance: String
)

@Serializable
data class VoidTransactionRequest(
    val reason: String? = null
)

@Serializable
data class UpdateReconcileStatusRequest(
    val status: String // NEW, CLEARED, RECONCILED
)

package com.bugdigger.piggybank.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class SplitRequest(
    val accountId: String,
    val amount: String, // BigDecimal as string for precision
    val currency: String, // USD, EUR, RSD
    val memo: String? = null
)

@Serializable
data class CreateTransactionRequest(
    val date: String, // ISO-8601 date: YYYY-MM-DD
    val description: String,
    val notes: String? = null,
    val splits: List<SplitRequest>
)

@Serializable
data class UpdateTransactionRequest(
    val date: String? = null,
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
    val memo: String?
)

@Serializable
data class TransactionResponse(
    val id: String,
    val date: String,
    val description: String,
    val notes: String?,
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
    val date: String,
    val description: String,
    val amount: String, // Amount for this account (positive = debit, negative = credit)
    val balance: String, // Running balance after this transaction
    val otherAccounts: List<String> // Names of other accounts in this transaction
)

@Serializable
data class AccountRegisterResponse(
    val accountId: String,
    val accountName: String,
    val entries: List<AccountRegisterEntry>,
    val openingBalance: String,
    val closingBalance: String
)

package com.bugdigger.piggybank.api.dto

import kotlinx.serialization.Serializable

@Serializable
data class RecurringSplitRequest(
    val accountId: String,
    val amount: String,
    val currency: String,
    val memo: String? = null
)

@Serializable
data class CreateRecurringTransactionRequest(
    val name: String,
    val description: String,
    val frequency: String, // DAILY, WEEKLY, MONTHLY, YEARLY
    val interval: Int = 1,
    val startDate: String, // ISO-8601 date: YYYY-MM-DD
    val endDate: String? = null,
    val splits: List<RecurringSplitRequest>
)

@Serializable
data class UpdateRecurringTransactionRequest(
    val name: String? = null,
    val description: String? = null,
    val frequency: String? = null,
    val interval: Int? = null,
    val endDate: String? = null,
    val isActive: Boolean? = null,
    val splits: List<RecurringSplitRequest>? = null
)

@Serializable
data class RecurringSplitResponse(
    val id: String,
    val accountId: String,
    val accountName: String,
    val amount: String,
    val currency: String,
    val memo: String?
)

@Serializable
data class RecurringTransactionResponse(
    val id: String,
    val name: String,
    val description: String,
    val frequency: String,
    val interval: Int,
    val startDate: String,
    val endDate: String?,
    val nextDueDate: String,
    val lastProcessedDate: String?,
    val isActive: Boolean,
    val splits: List<RecurringSplitResponse>,
    val createdAt: String
)

@Serializable
data class GenerateTransactionsRequest(
    val upToDate: String // Generate all due transactions up to this date
)

@Serializable
data class GenerateTransactionsResponse(
    val generatedCount: Int,
    val transactionIds: List<String>
)

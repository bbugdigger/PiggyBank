package com.bugdigger.piggybank.api.dto

import kotlinx.serialization.Serializable

@Serializable
data class CreateAccountRequest(
    val parentId: String? = null,
    val name: String,
    val type: String, // ASSET, LIABILITY, EQUITY, INCOME, EXPENSE
    val currency: String, // USD, EUR, RSD
    val description: String? = null,
    val placeholder: Boolean = false
)

@Serializable
data class UpdateAccountRequest(
    val name: String? = null,
    val description: String? = null,
    val parentId: String? = null
)

@Serializable
data class AccountResponse(
    val id: String,
    val parentId: String?,
    val name: String,
    val fullName: String,
    val type: String,
    val currency: String,
    val description: String?,
    val placeholder: Boolean,
    val createdAt: String
)

@Serializable
data class AccountWithBalanceResponse(
    val account: AccountResponse,
    val balance: String, // BigDecimal as string for precision
    val childrenCount: Int
)

@Serializable
data class AccountTreeNode(
    val account: AccountResponse,
    val balance: String,
    val children: List<AccountTreeNode>
)

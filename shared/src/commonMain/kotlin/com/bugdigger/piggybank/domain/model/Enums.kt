package com.bugdigger.piggybank.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class AccountType {
    ASSET,
    LIABILITY,
    EQUITY,
    INCOME,
    EXPENSE
}

@Serializable
enum class Currency(val symbol: String, val displayName: String) {
    USD("$", "US Dollar"),
    EUR("\u20AC", "Euro"),
    RSD("RSD", "Serbian Dinar")
}

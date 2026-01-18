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

/**
 * Normal balance type for accounts:
 * - DEBIT: Assets and Expenses increase with debits
 * - CREDIT: Liabilities, Equity, and Income increase with credits
 */
@Serializable
enum class NormalBalance {
    DEBIT,
    CREDIT;
    
    companion object {
        fun fromAccountType(type: AccountType): NormalBalance = when (type) {
            AccountType.ASSET, AccountType.EXPENSE -> DEBIT
            AccountType.LIABILITY, AccountType.EQUITY, AccountType.INCOME -> CREDIT
        }
        
        fun fromString(value: String): NormalBalance = valueOf(value.uppercase())
    }
}

/**
 * Reconciliation status for splits:
 * - NEW: Not yet reviewed
 * - CLEARED: Confirmed in statement but not reconciled
 * - RECONCILED: Fully reconciled with bank statement
 */
@Serializable
enum class ReconcileStatus(val symbol: String) {
    NEW("n"),      // not reconciled
    CLEARED("c"),  // cleared but not reconciled
    RECONCILED("y"); // fully reconciled
    
    companion object {
        fun fromString(value: String): ReconcileStatus = valueOf(value.uppercase())
    }
}

@Serializable
enum class Currency(val symbol: String, val displayName: String) {
    USD("$", "US Dollar"),
    EUR("\u20AC", "Euro"),
    RSD("RSD", "Serbian Dinar")
}

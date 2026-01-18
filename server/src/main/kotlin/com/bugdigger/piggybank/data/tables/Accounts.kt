package com.bugdigger.piggybank.data.tables

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

/**
 * Account types following double-entry accounting principles:
 * - ASSET: Things you own (positive normal balance)
 * - LIABILITY: Things you owe (negative normal balance)
 * - EQUITY: Net worth (negative normal balance)
 * - INCOME: Money received (negative normal balance)
 * - EXPENSE: Money spent (positive normal balance)
 */
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
enum class NormalBalance {
    DEBIT,
    CREDIT;
    
    companion object {
        fun fromAccountType(type: AccountType): NormalBalance = when (type) {
            AccountType.ASSET, AccountType.EXPENSE -> DEBIT
            AccountType.LIABILITY, AccountType.EQUITY, AccountType.INCOME -> CREDIT
        }
    }
}

/**
 * Supported currencies
 */
enum class Currency {
    USD,
    EUR,
    RSD
}

/**
 * Accounts table - hierarchical account structure for double-entry bookkeeping
 * 
 * Accounts form a tree structure where:
 * - Root accounts have null parentId
 * - Child accounts reference their parent via parentId
 * - Placeholder accounts (placeholder=true) are containers that can't hold transactions
 */
object Accounts : UUIDTable("accounts") {
    val userId = reference("user_id", Users)
    val parentId = reference("parent_id", Accounts).nullable()
    val name = varchar("name", 100)
    val fullName = varchar("full_name", 500) // e.g., "Assets:Bank:Checking"
    val type = enumerationByName<AccountType>("type", 20)
    val normalBalance = enumerationByName<NormalBalance>("normal_balance", 10).default(NormalBalance.DEBIT)
    val currency = enumerationByName<Currency>("currency", 10)
    val description = text("description").nullable()
    val placeholder = bool("placeholder").default(false)
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
    
    init {
        // Unique constraint: account name must be unique within same parent for same user
        uniqueIndex("idx_accounts_user_parent_name", userId, parentId, name)
    }
}

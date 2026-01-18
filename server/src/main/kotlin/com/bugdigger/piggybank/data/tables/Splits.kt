package com.bugdigger.piggybank.data.tables

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

/**
 * Splits table - individual debit/credit entries within a transaction
 * 
 * In double-entry accounting:
 * - Each transaction has 2+ splits
 * - The sum of all split amounts in a transaction MUST equal zero
 * - Positive amounts are debits, negative amounts are credits
 * 
 * For different account types:
 * - Assets/Expenses: Debit (positive) increases, Credit (negative) decreases
 * - Liabilities/Equity/Income: Credit (negative) increases, Debit (positive) decreases
 * 
 * Example: Buying groceries for $50 with cash
 *   Split 1: Expenses:Food +50.00 (debit - increases expense)
 *   Split 2: Assets:Cash  -50.00 (credit - decreases asset)
 *   Total: 0.00 ✓
 */
object Splits : UUIDTable("splits") {
    val transactionId = reference("transaction_id", Transactions)
    val accountId = reference("account_id", Accounts)
    val amount = decimal("amount", 19, 4) // Supports up to 999 trillion with 4 decimal places
    val currency = enumerationByName<Currency>("currency", 10)
    val memo = varchar("memo", 255).nullable()
    val createdAt = timestamp("created_at")
    
    init {
        index("idx_splits_transaction", false, transactionId)
        index("idx_splits_account", false, accountId)
    }
}

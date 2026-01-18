package com.bugdigger.piggybank.data.tables

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.kotlin.datetime.date
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

/**
 * Frequency for recurring transactions
 */
enum class Frequency {
    DAILY,
    WEEKLY,
    MONTHLY,
    YEARLY
}

/**
 * RecurringTransactions table - templates for automatically generated transactions
 * 
 * Examples:
 * - Monthly rent: frequency=MONTHLY, interval=1, on the 1st of each month
 * - Bi-weekly paycheck: frequency=WEEKLY, interval=2, every other Friday
 * - Quarterly insurance: frequency=MONTHLY, interval=3
 * - Annual subscription: frequency=YEARLY, interval=1
 */
object RecurringTransactions : UUIDTable("recurring_transactions") {
    val userId = reference("user_id", Users)
    val name = varchar("name", 100) // e.g., "Monthly Rent"
    val description = varchar("description", 500)
    val frequency = enumerationByName<Frequency>("frequency", 20)
    val interval = integer("interval").default(1) // Every N periods
    val startDate = date("start_date")
    val endDate = date("end_date").nullable() // null = indefinite
    val nextDueDate = date("next_due_date")
    val lastProcessedDate = date("last_processed_date").nullable()
    val isActive = bool("is_active").default(true)
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
    
    init {
        index("idx_recurring_transactions_user", false, userId)
        index("idx_recurring_transactions_next_due", false, nextDueDate, isActive)
    }
}

/**
 * RecurringSplits table - template splits for recurring transactions
 * 
 * When a recurring transaction is generated, these template splits are
 * copied to create actual Splits in the new Transaction.
 */
object RecurringSplits : UUIDTable("recurring_splits") {
    val recurringTransactionId = reference("recurring_transaction_id", RecurringTransactions)
    val accountId = reference("account_id", Accounts)
    val amount = decimal("amount", 19, 4)
    val currency = enumerationByName<Currency>("currency", 10)
    val memo = varchar("memo", 255).nullable()
    
    init {
        index("idx_recurring_splits_recurring_transaction", false, recurringTransactionId)
    }
}

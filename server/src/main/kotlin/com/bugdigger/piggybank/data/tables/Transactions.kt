package com.bugdigger.piggybank.data.tables

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.kotlin.datetime.date
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

/**
 * Transactions table - represents a complete financial transaction
 * 
 * In double-entry accounting, a transaction consists of multiple splits (postings)
 * that must sum to zero. This table stores the transaction metadata, while the
 * actual debit/credit entries are stored in the Splits table.
 */
object Transactions : UUIDTable("transactions") {
    val userId = reference("user_id", Users)
    val date = date("date")
    val description = varchar("description", 500)
    val notes = text("notes").nullable()
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
    
    init {
        index("idx_transactions_user_date", false, userId, date)
    }
}

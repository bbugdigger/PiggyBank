package com.bugdigger.piggybank.service

import com.bugdigger.piggybank.api.dto.*
import com.bugdigger.piggybank.data.tables.Accounts
import com.bugdigger.piggybank.data.tables.Currency
import com.bugdigger.piggybank.data.tables.Splits
import com.bugdigger.piggybank.data.tables.Transactions
import com.bugdigger.piggybank.plugins.BadRequestException
import com.bugdigger.piggybank.plugins.NotFoundException
import com.bugdigger.piggybank.plugins.ValidationException
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.math.BigDecimal
import java.util.UUID

class TransactionService {
    
    /**
     * Create a new transaction with splits
     * 
     * The fundamental rule of double-entry accounting: all splits must sum to zero
     */
    fun createTransaction(userId: String, request: CreateTransactionRequest): TransactionResponse {
        validateTransaction(request)
        
        return transaction {
            val userUuid = UUID.fromString(userId)
            val transactionId = UUID.randomUUID()
            val now = Clock.System.now()
            val date = LocalDate.parse(request.date)
            
            // Verify all accounts exist and belong to user
            val accountIds = request.splits.map { UUID.fromString(it.accountId) }
            val accounts = Accounts.selectAll()
                .where { (Accounts.id inList accountIds) and (Accounts.userId eq userUuid) }
                .toList()
            
            if (accounts.size != accountIds.size) {
                throw NotFoundException("One or more accounts not found")
            }
            
            // Verify no placeholder accounts
            val placeholderAccounts = accounts.filter { it[Accounts.placeholder] }
            if (placeholderAccounts.isNotEmpty()) {
                val names = placeholderAccounts.map { it[Accounts.name] }
                throw BadRequestException("Cannot post to placeholder accounts: $names")
            }
            
            // Create transaction
            Transactions.insert {
                it[Transactions.id] = transactionId
                it[Transactions.userId] = userUuid
                it[Transactions.date] = date
                it[description] = request.description
                it[notes] = request.notes
                it[createdAt] = now
                it[updatedAt] = now
            }
            
            // Create splits
            request.splits.forEach { splitRequest ->
                val accountUuid = UUID.fromString(splitRequest.accountId)
                val amount = BigDecimal(splitRequest.amount)
                val currency = Currency.valueOf(splitRequest.currency.uppercase())
                
                Splits.insert {
                    it[Splits.id] = UUID.randomUUID()
                    it[Splits.transactionId] = transactionId
                    it[Splits.accountId] = accountUuid
                    it[Splits.amount] = amount
                    it[Splits.currency] = currency
                    it[memo] = splitRequest.memo
                    it[createdAt] = now
                }
            }
            
            // Fetch and return the created transaction
            getTransactionById(transactionId)
        }
    }
    
    /**
     * Get a transaction by ID
     */
    fun getTransaction(userId: String, transactionId: String): TransactionResponse {
        return transaction {
            val userUuid = UUID.fromString(userId)
            val txnUuid = UUID.fromString(transactionId)
            
            val txn = Transactions.selectAll()
                .where { (Transactions.id eq txnUuid) and (Transactions.userId eq userUuid) }
                .singleOrNull()
                ?: throw NotFoundException("Transaction not found")
            
            getTransactionById(txnUuid)
        }
    }
    
    /**
     * Get all transactions for a user with pagination and filtering
     */
    fun getTransactions(
        userId: String,
        startDate: String? = null,
        endDate: String? = null,
        page: Int = 1,
        pageSize: Int = 50
    ): TransactionListResponse {
        return transaction {
            val userUuid = UUID.fromString(userId)
            
            var query = Transactions.selectAll()
                .where { Transactions.userId eq userUuid }
            
            if (startDate != null) {
                val start = LocalDate.parse(startDate)
                query = query.andWhere { Transactions.date greaterEq start }
            }
            
            if (endDate != null) {
                val end = LocalDate.parse(endDate)
                query = query.andWhere { Transactions.date lessEq end }
            }
            
            val total = query.count().toInt()
            
            val transactions = query
                .orderBy(Transactions.date to SortOrder.DESC, Transactions.createdAt to SortOrder.DESC)
                .limit(pageSize)
                .offset(((page - 1) * pageSize).toLong())
                .map { row -> getTransactionById(row[Transactions.id].value) }
            
            TransactionListResponse(
                transactions = transactions,
                total = total,
                page = page,
                pageSize = pageSize
            )
        }
    }
    
    /**
     * Update a transaction
     */
    fun updateTransaction(userId: String, transactionId: String, request: UpdateTransactionRequest): TransactionResponse {
        if (request.splits != null) {
            validateSplits(request.splits)
        }
        
        return transaction {
            val userUuid = UUID.fromString(userId)
            val txnUuid = UUID.fromString(transactionId)
            val now = Clock.System.now()
            
            // Verify transaction exists and belongs to user
            val txn = Transactions.selectAll()
                .where { (Transactions.id eq txnUuid) and (Transactions.userId eq userUuid) }
                .singleOrNull()
                ?: throw NotFoundException("Transaction not found")
            
            // Update transaction fields
            Transactions.update({ Transactions.id eq txnUuid }) {
                if (request.date != null) it[date] = LocalDate.parse(request.date)
                if (request.description != null) it[description] = request.description
                if (request.notes != null) it[notes] = request.notes
                it[updatedAt] = now
            }
            
            // Update splits if provided
            if (request.splits != null) {
                // Verify all accounts exist and belong to user
                val accountIds = request.splits.map { UUID.fromString(it.accountId) }
                val accounts = Accounts.selectAll()
                    .where { (Accounts.id inList accountIds) and (Accounts.userId eq userUuid) }
                    .toList()
                
                if (accounts.size != accountIds.size) {
                    throw NotFoundException("One or more accounts not found")
                }
                
                // Verify no placeholder accounts
                val placeholderAccounts = accounts.filter { it[Accounts.placeholder] }
                if (placeholderAccounts.isNotEmpty()) {
                    val names = placeholderAccounts.map { it[Accounts.name] }
                    throw BadRequestException("Cannot post to placeholder accounts: $names")
                }
                
                // Delete existing splits
                Splits.deleteWhere { Splits.transactionId eq txnUuid }
                
                // Create new splits
                request.splits.forEach { splitRequest ->
                    val accountUuid = UUID.fromString(splitRequest.accountId)
                    val amount = BigDecimal(splitRequest.amount)
                    val currency = Currency.valueOf(splitRequest.currency.uppercase())
                    
                    Splits.insert {
                        it[Splits.id] = UUID.randomUUID()
                        it[Splits.transactionId] = txnUuid
                        it[Splits.accountId] = accountUuid
                        it[Splits.amount] = amount
                        it[Splits.currency] = currency
                        it[memo] = splitRequest.memo
                        it[createdAt] = now
                    }
                }
            }
            
            getTransactionById(txnUuid)
        }
    }
    
    /**
     * Delete a transaction
     */
    fun deleteTransaction(userId: String, transactionId: String) {
        transaction {
            val userUuid = UUID.fromString(userId)
            val txnUuid = UUID.fromString(transactionId)
            
            // Verify transaction exists and belongs to user
            val txn = Transactions.selectAll()
                .where { (Transactions.id eq txnUuid) and (Transactions.userId eq userUuid) }
                .singleOrNull()
                ?: throw NotFoundException("Transaction not found")
            
            // Delete splits first (foreign key constraint)
            Splits.deleteWhere { Splits.transactionId eq txnUuid }
            
            // Delete transaction
            Transactions.deleteWhere { Transactions.id eq txnUuid }
        }
    }
    
    /**
     * Get transactions for a specific account (account register view)
     */
    fun getAccountRegister(
        userId: String,
        accountId: String,
        startDate: String? = null,
        endDate: String? = null
    ): AccountRegisterResponse {
        return transaction {
            val userUuid = UUID.fromString(userId)
            val accountUuid = UUID.fromString(accountId)
            
            // Verify account exists and belongs to user
            val account = Accounts.selectAll()
                .where { (Accounts.id eq accountUuid) and (Accounts.userId eq userUuid) }
                .singleOrNull()
                ?: throw NotFoundException("Account not found")
            
            // Get all splits for this account joined with transactions
            var query = (Splits innerJoin Transactions)
                .selectAll()
                .where { Splits.accountId eq accountUuid }
            
            if (startDate != null) {
                val start = LocalDate.parse(startDate)
                query = query.andWhere { Transactions.date greaterEq start }
            }
            
            if (endDate != null) {
                val end = LocalDate.parse(endDate)
                query = query.andWhere { Transactions.date lessEq end }
            }
            
            val splits = query
                .orderBy(Transactions.date to SortOrder.ASC, Transactions.createdAt to SortOrder.ASC)
                .toList()
            
            // Calculate running balance
            var runningBalance = BigDecimal.ZERO
            val entries = splits.map { row ->
                val amount = row[Splits.amount]
                runningBalance += amount
                
                // Get other accounts in this transaction
                val otherAccounts = (Splits innerJoin Accounts)
                    .selectAll()
                    .where { 
                        (Splits.transactionId eq row[Transactions.id]) and 
                        (Splits.accountId neq accountUuid)
                    }
                    .map { it[Accounts.fullName] }
                
                AccountRegisterEntry(
                    transactionId = row[Transactions.id].toString(),
                    date = row[Transactions.date].toString(),
                    description = row[Transactions.description],
                    amount = amount.toPlainString(),
                    balance = runningBalance.toPlainString(),
                    otherAccounts = otherAccounts
                )
            }
            
            AccountRegisterResponse(
                accountId = accountUuid.toString(),
                accountName = account[Accounts.fullName],
                entries = entries,
                openingBalance = "0", // TODO: Support opening balance
                closingBalance = runningBalance.toPlainString()
            )
        }
    }
    
    // Helper functions
    
    private fun getTransactionById(transactionId: UUID): TransactionResponse {
        val txn = Transactions.selectAll()
            .where { Transactions.id eq transactionId }
            .single()
        
        val splits = (Splits innerJoin Accounts)
            .selectAll()
            .where { Splits.transactionId eq transactionId }
            .map { row ->
                SplitResponse(
                    id = row[Splits.id].toString(),
                    accountId = row[Splits.accountId].toString(),
                    accountName = row[Accounts.fullName],
                    amount = row[Splits.amount].toPlainString(),
                    currency = row[Splits.currency].name,
                    memo = row[Splits.memo]
                )
            }
        
        return TransactionResponse(
            id = txn[Transactions.id].toString(),
            date = txn[Transactions.date].toString(),
            description = txn[Transactions.description],
            notes = txn[Transactions.notes],
            splits = splits,
            createdAt = txn[Transactions.createdAt].toString(),
            updatedAt = txn[Transactions.updatedAt].toString()
        )
    }
    
    private fun validateTransaction(request: CreateTransactionRequest) {
        if (request.description.isBlank()) {
            throw BadRequestException("Transaction description cannot be blank")
        }
        
        try {
            LocalDate.parse(request.date)
        } catch (e: Exception) {
            throw BadRequestException("Invalid date format. Use ISO-8601 format: YYYY-MM-DD")
        }
        
        validateSplits(request.splits)
    }
    
    private fun validateSplits(splits: List<SplitRequest>) {
        if (splits.size < 2) {
            throw ValidationException("A transaction must have at least 2 splits")
        }
        
        // Validate each split
        splits.forEach { split ->
            try {
                UUID.fromString(split.accountId)
            } catch (e: Exception) {
                throw BadRequestException("Invalid account ID: ${split.accountId}")
            }
            
            try {
                BigDecimal(split.amount)
            } catch (e: Exception) {
                throw BadRequestException("Invalid amount: ${split.amount}")
            }
            
            try {
                Currency.valueOf(split.currency.uppercase())
            } catch (e: Exception) {
                throw BadRequestException("Invalid currency: ${split.currency}. Must be one of: USD, EUR, RSD")
            }
        }
        
        // THE FUNDAMENTAL RULE: splits must sum to zero
        // Group by currency and verify each currency sums to zero
        val byCurrency = splits.groupBy { it.currency.uppercase() }
        byCurrency.forEach { (currency, currencySplits) ->
            val sum = currencySplits.sumOf { BigDecimal(it.amount) }
            if (sum.compareTo(BigDecimal.ZERO) != 0) {
                throw ValidationException(
                    "Transaction splits for $currency must sum to zero. Current sum: $sum"
                )
            }
        }
    }
}

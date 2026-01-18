package com.bugdigger.piggybank.service

import com.bugdigger.piggybank.api.dto.*
import com.bugdigger.piggybank.data.tables.AccountType
import com.bugdigger.piggybank.data.tables.Accounts
import com.bugdigger.piggybank.data.tables.Currency
import com.bugdigger.piggybank.data.tables.NormalBalance
import com.bugdigger.piggybank.data.tables.Splits
import com.bugdigger.piggybank.plugins.BadRequestException
import com.bugdigger.piggybank.plugins.ConflictException
import com.bugdigger.piggybank.plugins.NotFoundException
import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.math.BigDecimal
import java.util.UUID

class AccountService {
    
    /**
     * Create a new account
     */
    fun createAccount(userId: String, request: CreateAccountRequest): AccountResponse {
        validateCreateAccount(request)
        
        return transaction {
            val userUuid = UUID.fromString(userId)
            val parentUuid = request.parentId?.let { UUID.fromString(it) }
            
            // If parent is specified, verify it exists and belongs to user
            val parentAccount = if (parentUuid != null) {
                Accounts.selectAll()
                    .where { (Accounts.id eq parentUuid) and (Accounts.userId eq userUuid) }
                    .singleOrNull()
                    ?: throw NotFoundException("Parent account not found")
            } else null
            
            // Verify account type matches parent's type (if parent exists)
            val accountType = AccountType.valueOf(request.type.uppercase())
            if (parentAccount != null) {
                val parentType = parentAccount[Accounts.type]
                if (parentType != accountType) {
                    throw BadRequestException("Account type must match parent account type ($parentType)")
                }
            }
            
            // Check for duplicate name under same parent
            val existingAccount = Accounts.selectAll()
                .where { 
                    (Accounts.userId eq userUuid) and 
                    (Accounts.parentId eq parentUuid) and 
                    (Accounts.name eq request.name)
                }
                .singleOrNull()
            
            if (existingAccount != null) {
                throw ConflictException("An account named '${request.name}' already exists under this parent")
            }
            
            // Build full name (path from root)
            val fullName = buildFullName(parentAccount, request.name)
            
            val currency = Currency.valueOf(request.currency.uppercase())
            val now = Clock.System.now()
            val accountId = UUID.randomUUID()
            
            val normalBal = NormalBalance.fromAccountType(accountType)
            
            Accounts.insert {
                it[Accounts.id] = accountId
                it[Accounts.userId] = userUuid
                it[Accounts.parentId] = parentUuid
                it[name] = request.name
                it[Accounts.fullName] = fullName
                it[type] = accountType
                it[normalBalance] = normalBal
                it[Accounts.currency] = currency
                it[description] = request.description
                it[placeholder] = request.placeholder
                it[createdAt] = now
                it[updatedAt] = now
            }
            
            AccountResponse(
                id = accountId.toString(),
                parentId = parentUuid?.toString(),
                name = request.name,
                fullName = fullName,
                type = accountType.name,
                normalBalance = normalBal.name,
                currency = currency.name,
                description = request.description,
                placeholder = request.placeholder,
                createdAt = now.toString()
            )
        }
    }
    
    /**
     * Get all accounts for a user
     */
    fun getAccounts(userId: String): List<AccountResponse> {
        return transaction {
            val userUuid = UUID.fromString(userId)
            
            Accounts.selectAll()
                .where { Accounts.userId eq userUuid }
                .orderBy(Accounts.fullName)
                .map { row -> row.toAccountResponse() }
        }
    }
    
    /**
     * Get a single account by ID
     */
    fun getAccount(userId: String, accountId: String): AccountResponse {
        return transaction {
            val userUuid = UUID.fromString(userId)
            val accountUuid = UUID.fromString(accountId)
            
            Accounts.selectAll()
                .where { (Accounts.id eq accountUuid) and (Accounts.userId eq userUuid) }
                .singleOrNull()
                ?.toAccountResponse()
                ?: throw NotFoundException("Account not found")
        }
    }
    
    /**
     * Update an account
     */
    fun updateAccount(userId: String, accountId: String, request: UpdateAccountRequest): AccountResponse {
        return transaction {
            val userUuid = UUID.fromString(userId)
            val accountUuid = UUID.fromString(accountId)
            
            val account = Accounts.selectAll()
                .where { (Accounts.id eq accountUuid) and (Accounts.userId eq userUuid) }
                .singleOrNull()
                ?: throw NotFoundException("Account not found")
            
            val newParentUuid = request.parentId?.let { UUID.fromString(it) }
            val newName = request.name ?: account[Accounts.name]
            
            // If parent is changing, verify new parent exists and belongs to user
            if (request.parentId != null && request.parentId != account[Accounts.parentId]?.toString()) {
                val newParent = Accounts.selectAll()
                    .where { (Accounts.id eq newParentUuid!!) and (Accounts.userId eq userUuid) }
                    .singleOrNull()
                    ?: throw NotFoundException("New parent account not found")
                
                // Verify types match
                if (newParent[Accounts.type] != account[Accounts.type]) {
                    throw BadRequestException("Cannot move account to a parent of different type")
                }
                
                // Prevent circular references
                if (isDescendantOf(newParentUuid!!, accountUuid)) {
                    throw BadRequestException("Cannot move account under its own descendant")
                }
            }
            
            // Build new full name
            val parentAccount = newParentUuid?.let { parentId ->
                Accounts.selectAll()
                    .where { Accounts.id eq parentId }
                    .singleOrNull()
            }
            val newFullName = buildFullName(parentAccount, newName)
            
            val now = Clock.System.now()
            
            Accounts.update({ Accounts.id eq accountUuid }) {
                if (request.name != null) it[name] = request.name
                if (request.description != null) it[description] = request.description
                if (request.parentId != null) it[parentId] = newParentUuid
                it[fullName] = newFullName
                it[updatedAt] = now
            }
            
            // Update full names of all descendants
            updateDescendantFullNames(accountUuid, newFullName)
            
            // Fetch and return updated account
            Accounts.selectAll()
                .where { Accounts.id eq accountUuid }
                .single()
                .toAccountResponse()
        }
    }
    
    /**
     * Delete an account
     */
    fun deleteAccount(userId: String, accountId: String) {
        transaction {
            val userUuid = UUID.fromString(userId)
            val accountUuid = UUID.fromString(accountId)
            
            val account = Accounts.selectAll()
                .where { (Accounts.id eq accountUuid) and (Accounts.userId eq userUuid) }
                .singleOrNull()
                ?: throw NotFoundException("Account not found")
            
            // Check if account has any splits
            val hasSplits = Splits.selectAll()
                .where { Splits.accountId eq accountUuid }
                .count() > 0
            
            if (hasSplits) {
                throw BadRequestException("Cannot delete account with existing transactions")
            }
            
            // Check if account has children
            val hasChildren = Accounts.selectAll()
                .where { Accounts.parentId eq accountUuid }
                .count() > 0
            
            if (hasChildren) {
                throw BadRequestException("Cannot delete account with child accounts")
            }
            
            Accounts.deleteWhere { Accounts.id eq accountUuid }
        }
    }
    
    /**
     * Get account balance (sum of all splits)
     */
    fun getAccountBalance(userId: String, accountId: String): BigDecimal {
        return transaction {
            val userUuid = UUID.fromString(userId)
            val accountUuid = UUID.fromString(accountId)
            
            // Verify account exists and belongs to user
            val account = Accounts.selectAll()
                .where { (Accounts.id eq accountUuid) and (Accounts.userId eq userUuid) }
                .singleOrNull()
                ?: throw NotFoundException("Account not found")
            
            // Sum all splits for this account
            Splits.select(Splits.amount.sum())
                .where { Splits.accountId eq accountUuid }
                .singleOrNull()
                ?.get(Splits.amount.sum())
                ?: BigDecimal.ZERO
        }
    }
    
    /**
     * Get accounts as a tree structure
     */
    fun getAccountTree(userId: String): List<AccountTreeNode> {
        return transaction {
            val userUuid = UUID.fromString(userId)
            
            val allAccounts = Accounts.selectAll()
                .where { Accounts.userId eq userUuid }
                .toList()
            
            val accountBalances = mutableMapOf<UUID, BigDecimal>()
            
            // Calculate balances for all accounts
            for (account in allAccounts) {
                val accountId = account[Accounts.id].value
                val balance = Splits.select(Splits.amount.sum())
                    .where { Splits.accountId eq accountId }
                    .singleOrNull()
                    ?.get(Splits.amount.sum())
                    ?: BigDecimal.ZERO
                accountBalances[accountId] = balance
            }
            
            // Build tree starting from root accounts
            val rootAccounts = allAccounts.filter { it[Accounts.parentId] == null }
            rootAccounts.map { buildTreeNode(it, allAccounts, accountBalances) }
        }
    }
    
    /**
     * Create default accounts for a new user
     */
    fun createDefaultAccounts(userId: String) {
        transaction {
            val userUuid = UUID.fromString(userId)
            val now = Clock.System.now()
            
            // Define default account structure
            val defaultAccounts = listOf(
                // Assets
                DefaultAccount("Assets", AccountType.ASSET, Currency.USD, true, null, listOf(
                    DefaultAccount("Cash", AccountType.ASSET, Currency.USD, false, null),
                    DefaultAccount("Bank", AccountType.ASSET, Currency.USD, true, null, listOf(
                        DefaultAccount("Checking", AccountType.ASSET, Currency.USD, false, null)
                    )),
                    DefaultAccount("Investments", AccountType.ASSET, Currency.USD, true, null)
                )),
                // Liabilities
                DefaultAccount("Liabilities", AccountType.LIABILITY, Currency.USD, true, null, listOf(
                    DefaultAccount("Credit Card", AccountType.LIABILITY, Currency.USD, false, null),
                    DefaultAccount("Loans", AccountType.LIABILITY, Currency.USD, true, null)
                )),
                // Equity
                DefaultAccount("Equity", AccountType.EQUITY, Currency.USD, true, null, listOf(
                    DefaultAccount("Opening Balances", AccountType.EQUITY, Currency.USD, false, null),
                    DefaultAccount("Retained Earnings", AccountType.EQUITY, Currency.USD, false, null)
                )),
                // Income
                DefaultAccount("Income", AccountType.INCOME, Currency.USD, true, null, listOf(
                    DefaultAccount("Salary", AccountType.INCOME, Currency.USD, false, null),
                    DefaultAccount("Interest", AccountType.INCOME, Currency.USD, false, null),
                    DefaultAccount("Other Income", AccountType.INCOME, Currency.USD, false, null)
                )),
                // Expenses
                DefaultAccount("Expenses", AccountType.EXPENSE, Currency.USD, true, null, listOf(
                    DefaultAccount("Food", AccountType.EXPENSE, Currency.USD, true, null, listOf(
                        DefaultAccount("Groceries", AccountType.EXPENSE, Currency.USD, false, null),
                        DefaultAccount("Restaurants", AccountType.EXPENSE, Currency.USD, false, null)
                    )),
                    DefaultAccount("Housing", AccountType.EXPENSE, Currency.USD, true, null, listOf(
                        DefaultAccount("Rent", AccountType.EXPENSE, Currency.USD, false, null),
                        DefaultAccount("Utilities", AccountType.EXPENSE, Currency.USD, false, null)
                    )),
                    DefaultAccount("Transportation", AccountType.EXPENSE, Currency.USD, false, null),
                    DefaultAccount("Entertainment", AccountType.EXPENSE, Currency.USD, false, null)
                ))
            )
            
            // Recursively create accounts
            fun createAccountRecursive(default: DefaultAccount, parentId: UUID?, parentFullName: String?) {
                val accountId = UUID.randomUUID()
                val fullName = if (parentFullName != null) "$parentFullName:${default.name}" else default.name
                
                Accounts.insert {
                    it[Accounts.id] = accountId
                    it[Accounts.userId] = userUuid
                    it[Accounts.parentId] = parentId
                    it[name] = default.name
                    it[Accounts.fullName] = fullName
                    it[type] = default.type
                    it[normalBalance] = NormalBalance.fromAccountType(default.type)
                    it[currency] = default.currency
                    it[description] = default.description
                    it[placeholder] = default.placeholder
                    it[createdAt] = now
                    it[updatedAt] = now
                }
                
                // Create children
                default.children.forEach { child ->
                    createAccountRecursive(child, accountId, fullName)
                }
            }
            
            defaultAccounts.forEach { createAccountRecursive(it, null, null) }
        }
    }
    
    // Helper functions
    
    private fun buildFullName(parentAccount: ResultRow?, name: String): String {
        return if (parentAccount != null) {
            "${parentAccount[Accounts.fullName]}:$name"
        } else {
            name
        }
    }
    
    private fun isDescendantOf(potentialDescendantId: UUID, ancestorId: UUID): Boolean {
        var currentId: UUID? = potentialDescendantId
        while (currentId != null) {
            if (currentId == ancestorId) return true
            currentId = Accounts.selectAll()
                .where { Accounts.id eq currentId!! }
                .singleOrNull()
                ?.get(Accounts.parentId)?.value
        }
        return false
    }
    
    private fun updateDescendantFullNames(parentId: UUID, parentFullName: String) {
        val children = Accounts.selectAll()
            .where { Accounts.parentId eq parentId }
            .toList()
        
        for (child in children) {
            val newFullName = "$parentFullName:${child[Accounts.name]}"
            Accounts.update({ Accounts.id eq child[Accounts.id] }) {
                it[fullName] = newFullName
            }
            updateDescendantFullNames(child[Accounts.id].value, newFullName)
        }
    }
    
    private fun buildTreeNode(
        account: ResultRow,
        allAccounts: List<ResultRow>,
        balances: Map<UUID, BigDecimal>
    ): AccountTreeNode {
        val children = allAccounts.filter { it[Accounts.parentId]?.value == account[Accounts.id].value }
        val childNodes = children.map { buildTreeNode(it, allAccounts, balances) }
        
        // Calculate total balance including children
        val ownBalance = balances[account[Accounts.id].value] ?: BigDecimal.ZERO
        val childrenBalance = childNodes.sumOf { BigDecimal(it.balance) }
        val totalBalance = ownBalance + childrenBalance
        
        return AccountTreeNode(
            account = account.toAccountResponse(),
            balance = totalBalance.toPlainString(),
            children = childNodes
        )
    }
    
    private fun ResultRow.toAccountResponse(): AccountResponse {
        return AccountResponse(
            id = this[Accounts.id].toString(),
            parentId = this[Accounts.parentId]?.toString(),
            name = this[Accounts.name],
            fullName = this[Accounts.fullName],
            type = this[Accounts.type].name,
            normalBalance = this[Accounts.normalBalance].name,
            currency = this[Accounts.currency].name,
            description = this[Accounts.description],
            placeholder = this[Accounts.placeholder],
            createdAt = this[Accounts.createdAt].toString()
        )
    }
    
    private fun validateCreateAccount(request: CreateAccountRequest) {
        if (request.name.isBlank()) {
            throw BadRequestException("Account name cannot be blank")
        }
        if (request.name.contains(":")) {
            throw BadRequestException("Account name cannot contain ':'")
        }
        try {
            AccountType.valueOf(request.type.uppercase())
        } catch (e: IllegalArgumentException) {
            throw BadRequestException("Invalid account type: ${request.type}. Must be one of: ASSET, LIABILITY, EQUITY, INCOME, EXPENSE")
        }
        try {
            Currency.valueOf(request.currency.uppercase())
        } catch (e: IllegalArgumentException) {
            throw BadRequestException("Invalid currency: ${request.currency}. Must be one of: USD, EUR, RSD")
        }
    }
    
    private data class DefaultAccount(
        val name: String,
        val type: AccountType,
        val currency: Currency,
        val placeholder: Boolean,
        val description: String?,
        val children: List<DefaultAccount> = emptyList()
    )
}

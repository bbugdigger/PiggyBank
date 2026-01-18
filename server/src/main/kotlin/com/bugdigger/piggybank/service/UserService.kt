package com.bugdigger.piggybank.service

import at.favre.lib.crypto.bcrypt.BCrypt
import com.bugdigger.piggybank.api.dto.*
import com.bugdigger.piggybank.config.JwtConfig
import com.bugdigger.piggybank.data.tables.Users
import com.bugdigger.piggybank.plugins.BadRequestException
import com.bugdigger.piggybank.plugins.ConflictException
import com.bugdigger.piggybank.plugins.UnauthorizedException
import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

class UserService(private val accountService: AccountService) {
    
    /**
     * Register a new user
     */
    fun register(request: RegisterRequest): AuthResponse {
        validateRegistration(request)
        
        val userId = transaction {
            // Check if username already exists
            val existingUser = Users.selectAll()
                .where { Users.username eq request.username }
                .singleOrNull()
            
            if (existingUser != null) {
                throw ConflictException("Username '${request.username}' is already taken")
            }
            
            // Check if email already exists (if provided)
            if (request.email != null) {
                val existingEmail = Users.selectAll()
                    .where { Users.email eq request.email }
                    .singleOrNull()
                
                if (existingEmail != null) {
                    throw ConflictException("Email '${request.email}' is already registered")
                }
            }
            
            // Hash password
            val passwordHash = BCrypt.withDefaults().hashToString(12, request.password.toCharArray())
            
            // Create user
            val now = Clock.System.now()
            val id = UUID.randomUUID()
            
            Users.insert {
                it[Users.id] = id
                it[username] = request.username
                it[email] = request.email
                it[Users.passwordHash] = passwordHash
                it[createdAt] = now
                it[updatedAt] = now
            }
            
            id
        }
        
        // Create default accounts for the new user
        accountService.createDefaultAccounts(userId.toString())
        
        // Generate token
        val token = JwtConfig.generateToken(userId.toString(), request.username)
        
        return AuthResponse(
            token = token,
            userId = userId.toString(),
            username = request.username
        )
    }
    
    /**
     * Login an existing user
     */
    fun login(request: LoginRequest): AuthResponse {
        val user = transaction {
            Users.selectAll()
                .where { Users.username eq request.username }
                .singleOrNull()
        } ?: throw UnauthorizedException("Invalid username or password")
        
        val passwordHash = user[Users.passwordHash]
        val result = BCrypt.verifyer().verify(request.password.toCharArray(), passwordHash)
        
        if (!result.verified) {
            throw UnauthorizedException("Invalid username or password")
        }
        
        val token = JwtConfig.generateToken(
            user[Users.id].toString(),
            user[Users.username]
        )
        
        return AuthResponse(
            token = token,
            userId = user[Users.id].toString(),
            username = user[Users.username]
        )
    }
    
    /**
     * Get user by ID
     */
    fun getUserById(userId: String): UserResponse? {
        return transaction {
            Users.selectAll()
                .where { Users.id eq UUID.fromString(userId) }
                .singleOrNull()
                ?.let { row ->
                    UserResponse(
                        id = row[Users.id].toString(),
                        username = row[Users.username],
                        email = row[Users.email],
                        createdAt = row[Users.createdAt].toString()
                    )
                }
        }
    }
    
    private fun validateRegistration(request: RegisterRequest) {
        if (request.username.isBlank()) {
            throw BadRequestException("Username cannot be blank")
        }
        if (request.username.length < 3) {
            throw BadRequestException("Username must be at least 3 characters")
        }
        if (request.username.length > 50) {
            throw BadRequestException("Username cannot exceed 50 characters")
        }
        if (!request.username.matches(Regex("^[a-zA-Z0-9_]+$"))) {
            throw BadRequestException("Username can only contain letters, numbers, and underscores")
        }
        if (request.password.length < 8) {
            throw BadRequestException("Password must be at least 8 characters")
        }
        if (request.email != null && !request.email.contains("@")) {
            throw BadRequestException("Invalid email format")
        }
    }
}

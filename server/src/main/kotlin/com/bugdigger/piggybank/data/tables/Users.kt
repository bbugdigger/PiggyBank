package com.bugdigger.piggybank.data.tables

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

/**
 * Users table - stores user authentication and profile information
 */
object Users : UUIDTable("users") {
    val username = varchar("username", 50).uniqueIndex()
    val email = varchar("email", 255).nullable().uniqueIndex()
    val passwordHash = varchar("password_hash", 255)
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
}

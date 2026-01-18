package com.bugdigger.piggybank.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequest(
    val username: String,
    val password: String,
    val email: String? = null
)

@Serializable
data class LoginRequest(
    val username: String,
    val password: String
)

@Serializable
data class AuthResponse(
    val token: String,
    val userId: String,
    val username: String
)

@Serializable
data class UserResponse(
    val id: String,
    val username: String,
    val email: String?,
    val createdAt: String
)

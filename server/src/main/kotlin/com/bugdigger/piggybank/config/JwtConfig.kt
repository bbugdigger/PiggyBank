package com.bugdigger.piggybank.config

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import java.util.*

object JwtConfig {
    private val secret = System.getenv("JWT_SECRET") ?: "piggybank-dev-secret-change-in-production"
    private val issuer = System.getenv("JWT_ISSUER") ?: "piggybank"
    private val audience = System.getenv("JWT_AUDIENCE") ?: "piggybank-users"
    private val validityInMs = System.getenv("JWT_VALIDITY_MS")?.toLongOrNull() ?: 3600000L // 1 hour default
    private val algorithm = Algorithm.HMAC256(secret)
    
    val verifier: JWTVerifier = JWT
        .require(algorithm)
        .withAudience(audience)
        .withIssuer(issuer)
        .build()
    
    fun getAudience(): String = audience
    fun getIssuer(): String = issuer
    fun getRealm(): String = "piggybank"
    
    /**
     * Generate a JWT token for a user
     */
    fun generateToken(userId: String, username: String): String = JWT.create()
        .withSubject("Authentication")
        .withIssuer(issuer)
        .withAudience(audience)
        .withClaim("userId", userId)
        .withClaim("username", username)
        .withExpiresAt(Date(System.currentTimeMillis() + validityInMs))
        .sign(algorithm)
}

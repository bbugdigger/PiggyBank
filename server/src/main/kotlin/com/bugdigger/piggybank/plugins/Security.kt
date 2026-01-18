package com.bugdigger.piggybank.plugins

import com.bugdigger.piggybank.config.JwtConfig
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*

fun Application.configureSecurity() {
    install(Authentication) {
        jwt("auth-jwt") {
            realm = JwtConfig.getRealm()
            verifier(JwtConfig.verifier)
            validate { credential ->
                val userId = credential.payload.getClaim("userId").asString()
                val username = credential.payload.getClaim("username").asString()
                if (userId != null && username != null) {
                    JWTPrincipal(credential.payload)
                } else {
                    null
                }
            }
        }
    }
}

/**
 * Extension function to get the current user's ID from the JWT principal
 */
fun ApplicationCall.getUserId(): String? {
    return principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asString()
}

/**
 * Extension function to get the current user's username from the JWT principal
 */
fun ApplicationCall.getUsername(): String? {
    return principal<JWTPrincipal>()?.payload?.getClaim("username")?.asString()
}

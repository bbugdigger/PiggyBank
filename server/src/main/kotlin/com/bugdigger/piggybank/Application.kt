package com.bugdigger.piggybank

import com.bugdigger.piggybank.api.routes.accountRoutes
import com.bugdigger.piggybank.api.routes.authRoutes
import com.bugdigger.piggybank.api.routes.transactionRoutes
import com.bugdigger.piggybank.config.DatabaseConfig
import com.bugdigger.piggybank.plugins.*
import com.bugdigger.piggybank.service.AccountService
import com.bugdigger.piggybank.service.TransactionService
import com.bugdigger.piggybank.service.UserService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun main() {
    embeddedServer(
        Netty,
        port = System.getenv("PORT")?.toIntOrNull() ?: 8080,
        host = "0.0.0.0",
        module = Application::module
    ).start(wait = true)
}

fun Application.module() {
    // Initialize database
    DatabaseConfig.init()
    
    // Install plugins
    configureSerialization()
    configureHTTP()
    configureSecurity()
    configureStatusPages()
    
    // Create services
    val accountService = AccountService()
    val userService = UserService(accountService)
    val transactionService = TransactionService()
    
    // Configure routes
    routing {
        // Health check endpoint
        get("/health") {
            call.respond(HttpStatusCode.OK, mapOf("status" to "healthy"))
        }
        
        // API info endpoint
        get("/api") {
            call.respond(HttpStatusCode.OK, mapOf(
                "name" to "PiggyBank API",
                "version" to "1.0.0",
                "description" to "Personal finance management with double-entry accounting"
            ))
        }
        
        // Register routes
        authRoutes(userService)
        accountRoutes(accountService)
        transactionRoutes(transactionService)
    }
}

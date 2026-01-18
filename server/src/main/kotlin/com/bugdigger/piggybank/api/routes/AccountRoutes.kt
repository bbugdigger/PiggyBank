package com.bugdigger.piggybank.api.routes

import com.bugdigger.piggybank.api.dto.CreateAccountRequest
import com.bugdigger.piggybank.api.dto.UpdateAccountRequest
import com.bugdigger.piggybank.plugins.getUserId
import com.bugdigger.piggybank.plugins.UnauthorizedException
import com.bugdigger.piggybank.service.AccountService
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.accountRoutes(accountService: AccountService) {
    authenticate("auth-jwt") {
        route("/api/accounts") {
            
            /**
             * GET /api/accounts
             * Get all accounts for the current user
             */
            get {
                val userId = call.getUserId() ?: throw UnauthorizedException()
                val accounts = accountService.getAccounts(userId)
                call.respond(HttpStatusCode.OK, accounts)
            }
            
            /**
             * GET /api/accounts/tree
             * Get accounts as a tree structure
             */
            get("/tree") {
                val userId = call.getUserId() ?: throw UnauthorizedException()
                val tree = accountService.getAccountTree(userId)
                call.respond(HttpStatusCode.OK, tree)
            }
            
            /**
             * GET /api/accounts/{id}
             * Get a single account by ID
             */
            get("/{id}") {
                val userId = call.getUserId() ?: throw UnauthorizedException()
                val accountId = call.parameters["id"]!!
                val account = accountService.getAccount(userId, accountId)
                call.respond(HttpStatusCode.OK, account)
            }
            
            /**
             * GET /api/accounts/{id}/balance
             * Get the balance for an account
             */
            get("/{id}/balance") {
                val userId = call.getUserId() ?: throw UnauthorizedException()
                val accountId = call.parameters["id"]!!
                val balance = accountService.getAccountBalance(userId, accountId)
                call.respond(HttpStatusCode.OK, mapOf("balance" to balance.toPlainString()))
            }
            
            /**
             * POST /api/accounts
             * Create a new account
             */
            post {
                val userId = call.getUserId() ?: throw UnauthorizedException()
                val request = call.receive<CreateAccountRequest>()
                val account = accountService.createAccount(userId, request)
                call.respond(HttpStatusCode.Created, account)
            }
            
            /**
             * PUT /api/accounts/{id}
             * Update an account
             */
            put("/{id}") {
                val userId = call.getUserId() ?: throw UnauthorizedException()
                val accountId = call.parameters["id"]!!
                val request = call.receive<UpdateAccountRequest>()
                val account = accountService.updateAccount(userId, accountId, request)
                call.respond(HttpStatusCode.OK, account)
            }
            
            /**
             * DELETE /api/accounts/{id}
             * Delete an account
             */
            delete("/{id}") {
                val userId = call.getUserId() ?: throw UnauthorizedException()
                val accountId = call.parameters["id"]!!
                accountService.deleteAccount(userId, accountId)
                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}

package com.bugdigger.piggybank.api.routes

import com.bugdigger.piggybank.api.dto.CreateTransactionRequest
import com.bugdigger.piggybank.api.dto.UpdateTransactionRequest
import com.bugdigger.piggybank.plugins.getUserId
import com.bugdigger.piggybank.plugins.UnauthorizedException
import com.bugdigger.piggybank.service.TransactionService
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.transactionRoutes(transactionService: TransactionService) {
    authenticate("auth-jwt") {
        route("/api/transactions") {
            
            /**
             * GET /api/transactions
             * Get all transactions for the current user
             * Query params: startDate, endDate, page, pageSize
             */
            get {
                val userId = call.getUserId() ?: throw UnauthorizedException()
                val startDate = call.request.queryParameters["startDate"]
                val endDate = call.request.queryParameters["endDate"]
                val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
                val pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull() ?: 50
                
                val response = transactionService.getTransactions(
                    userId = userId,
                    startDate = startDate,
                    endDate = endDate,
                    page = page,
                    pageSize = pageSize
                )
                call.respond(HttpStatusCode.OK, response)
            }
            
            /**
             * GET /api/transactions/{id}
             * Get a single transaction by ID
             */
            get("/{id}") {
                val userId = call.getUserId() ?: throw UnauthorizedException()
                val transactionId = call.parameters["id"]!!
                val transaction = transactionService.getTransaction(userId, transactionId)
                call.respond(HttpStatusCode.OK, transaction)
            }
            
            /**
             * POST /api/transactions
             * Create a new transaction
             */
            post {
                val userId = call.getUserId() ?: throw UnauthorizedException()
                val request = call.receive<CreateTransactionRequest>()
                val transaction = transactionService.createTransaction(userId, request)
                call.respond(HttpStatusCode.Created, transaction)
            }
            
            /**
             * PUT /api/transactions/{id}
             * Update a transaction
             */
            put("/{id}") {
                val userId = call.getUserId() ?: throw UnauthorizedException()
                val transactionId = call.parameters["id"]!!
                val request = call.receive<UpdateTransactionRequest>()
                val transaction = transactionService.updateTransaction(userId, transactionId, request)
                call.respond(HttpStatusCode.OK, transaction)
            }
            
            /**
             * DELETE /api/transactions/{id}
             * Delete a transaction
             */
            delete("/{id}") {
                val userId = call.getUserId() ?: throw UnauthorizedException()
                val transactionId = call.parameters["id"]!!
                transactionService.deleteTransaction(userId, transactionId)
                call.respond(HttpStatusCode.NoContent)
            }
        }
        
        /**
         * GET /api/accounts/{id}/transactions
         * Get transactions for a specific account (account register view)
         */
        route("/api/accounts/{id}/transactions") {
            get {
                val userId = call.getUserId() ?: throw UnauthorizedException()
                val accountId = call.parameters["id"]!!
                val startDate = call.request.queryParameters["startDate"]
                val endDate = call.request.queryParameters["endDate"]
                
                val register = transactionService.getAccountRegister(
                    userId = userId,
                    accountId = accountId,
                    startDate = startDate,
                    endDate = endDate
                )
                call.respond(HttpStatusCode.OK, register)
            }
        }
    }
}

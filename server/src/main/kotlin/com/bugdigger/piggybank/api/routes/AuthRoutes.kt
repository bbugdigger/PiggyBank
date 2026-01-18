package com.bugdigger.piggybank.api.routes

import com.bugdigger.piggybank.api.dto.LoginRequest
import com.bugdigger.piggybank.api.dto.RegisterRequest
import com.bugdigger.piggybank.plugins.getUserId
import com.bugdigger.piggybank.plugins.NotFoundException
import com.bugdigger.piggybank.service.UserService
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.authRoutes(userService: UserService) {
    route("/api/auth") {
        
        /**
         * POST /api/auth/register
         * Register a new user
         */
        post("/register") {
            val request = call.receive<RegisterRequest>()
            val response = userService.register(request)
            call.respond(HttpStatusCode.Created, response)
        }
        
        /**
         * POST /api/auth/login
         * Login and receive JWT token
         */
        post("/login") {
            val request = call.receive<LoginRequest>()
            val response = userService.login(request)
            call.respond(HttpStatusCode.OK, response)
        }
        
        /**
         * GET /api/auth/me
         * Get current user info (requires authentication)
         */
        authenticate("auth-jwt") {
            get("/me") {
                val userId = call.getUserId() 
                    ?: throw NotFoundException("User not found")
                
                val user = userService.getUserById(userId)
                    ?: throw NotFoundException("User not found")
                
                call.respond(HttpStatusCode.OK, user)
            }
        }
    }
}

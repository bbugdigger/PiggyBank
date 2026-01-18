package com.bugdigger.piggybank.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import kotlinx.serialization.Serializable

@Serializable
data class ErrorResponse(
    val error: String,
    val message: String,
    val statusCode: Int
)

/**
 * Custom exceptions for the application
 */
open class AppException(message: String, val statusCode: HttpStatusCode) : Exception(message)

class BadRequestException(message: String) : AppException(message, HttpStatusCode.BadRequest)
class UnauthorizedException(message: String = "Unauthorized") : AppException(message, HttpStatusCode.Unauthorized)
class ForbiddenException(message: String = "Forbidden") : AppException(message, HttpStatusCode.Forbidden)
class NotFoundException(message: String) : AppException(message, HttpStatusCode.NotFound)
class ConflictException(message: String) : AppException(message, HttpStatusCode.Conflict)
class ValidationException(message: String) : AppException(message, HttpStatusCode.UnprocessableEntity)

fun Application.configureStatusPages() {
    install(StatusPages) {
        exception<AppException> { call, cause ->
            call.respond(
                cause.statusCode,
                ErrorResponse(
                    error = cause.statusCode.description,
                    message = cause.message ?: "An error occurred",
                    statusCode = cause.statusCode.value
                )
            )
        }
        
        exception<Throwable> { call, cause ->
            call.application.environment.log.error("Unhandled exception", cause)
            call.respond(
                HttpStatusCode.InternalServerError,
                ErrorResponse(
                    error = "Internal Server Error",
                    message = "An unexpected error occurred",
                    statusCode = 500
                )
            )
        }
    }
}

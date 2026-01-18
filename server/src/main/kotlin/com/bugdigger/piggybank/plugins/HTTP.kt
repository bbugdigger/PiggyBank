package com.bugdigger.piggybank.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.calllogging.*
import org.slf4j.event.Level

fun Application.configureHTTP() {
    install(CORS) {
        // Allow requests from any origin during development
        // In production, you'd want to restrict this to specific origins
        anyHost()
        
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Patch)
        
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Accept)
        
        allowCredentials = true
    }
    
    install(CallLogging) {
        level = Level.INFO
    }
}

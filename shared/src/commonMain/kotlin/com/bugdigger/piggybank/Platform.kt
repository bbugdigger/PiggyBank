package com.bugdigger.piggybank

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
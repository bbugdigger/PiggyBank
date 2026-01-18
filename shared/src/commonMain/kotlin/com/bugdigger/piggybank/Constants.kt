package com.bugdigger.piggybank

/**
 * Server configuration constants
 */
object ServerConfig {
    const val DEFAULT_PORT = 8080
    const val DEFAULT_HOST = "localhost"
    
    fun getBaseUrl(host: String = DEFAULT_HOST, port: Int = DEFAULT_PORT): String {
        return "http://$host:$port"
    }
}

/**
 * API endpoints
 */
object ApiEndpoints {
    const val AUTH_REGISTER = "/api/auth/register"
    const val AUTH_LOGIN = "/api/auth/login"
    const val AUTH_ME = "/api/auth/me"
    
    const val ACCOUNTS = "/api/accounts"
    const val ACCOUNTS_TREE = "/api/accounts/tree"
    
    const val TRANSACTIONS = "/api/transactions"
    
    const val EXCHANGE_RATES = "/api/exchange-rates"
    
    const val RECURRING_TRANSACTIONS = "/api/recurring-transactions"
    
    const val REPORTS_BALANCE_SHEET = "/api/reports/balance-sheet"
    const val REPORTS_INCOME_STATEMENT = "/api/reports/income-statement"
}

// Keep legacy constant for backward compatibility
const val SERVER_PORT = ServerConfig.DEFAULT_PORT

package com.bugdigger.piggybank.ui.util

/**
 * Currency formatting and conversion utilities
 * Cross-platform compatible for Kotlin Multiplatform
 */
object CurrencyUtils {
    
    // Hardcoded exchange rates for MVP (to RSD)
    // In production, these would come from the server
    private val exchangeRatesToRSD = mapOf(
        "RSD" to 1.0,
        "EUR" to 117.5,  // 1 EUR = ~117.5 RSD
        "USD" to 108.0   // 1 USD = ~108 RSD
    )
    
    /**
     * Format amount with currency symbol
     */
    fun formatWithCurrency(amount: Double, currency: String): String {
        val absAmount = kotlin.math.abs(amount)
        val formatted = formatNumber(absAmount)
        val sign = if (amount < 0) "-" else ""
        
        return when (currency) {
            "USD" -> "$sign$$formatted"
            "EUR" -> "$sign$formatted EUR"
            "RSD" -> "$sign$formatted RSD"
            else -> "$sign$formatted $currency"
        }
    }
    
    /**
     * Format amount with currency symbol (from string)
     */
    fun formatWithCurrency(amountStr: String, currency: String): String {
        val amount = amountStr.toDoubleOrNull() ?: 0.0
        return formatWithCurrency(amount, currency)
    }
    
    /**
     * Format number with thousands separator and 2 decimal places
     */
    fun formatNumber(amount: Double): String {
        val absAmount = kotlin.math.abs(amount)
        
        // Format with 2 decimal places
        val intPart = absAmount.toLong()
        val decPart = ((absAmount - intPart) * 100).toLong()
        
        // Add thousands separators
        val intStr = intPart.toString()
        val withSeparators = buildString {
            intStr.forEachIndexed { index, c ->
                if (index > 0 && (intStr.length - index) % 3 == 0) {
                    append(',')
                }
                append(c)
            }
        }
        
        val sign = if (amount < 0) "-" else ""
        return "$sign$withSeparators.${decPart.toString().padStart(2, '0')}"
    }
    
    /**
     * Format number from string
     */
    fun formatNumber(amountStr: String): String {
        val amount = amountStr.toDoubleOrNull() ?: 0.0
        return formatNumber(amount)
    }
    
    /**
     * Convert amount from one currency to another
     */
    fun convert(
        amount: Double, 
        fromCurrency: String, 
        toCurrency: String
    ): Double {
        if (fromCurrency == toCurrency) return amount
        
        // Convert to RSD first, then to target currency
        val rateFromRSD = exchangeRatesToRSD[fromCurrency] ?: 1.0
        val rateToRSD = exchangeRatesToRSD[toCurrency] ?: 1.0
        
        return amount * rateFromRSD / rateToRSD
    }
    
    /**
     * Convert amount from string
     */
    fun convert(
        amountStr: String, 
        fromCurrency: String, 
        toCurrency: String
    ): Double {
        val amount = amountStr.toDoubleOrNull() ?: 0.0
        return convert(amount, fromCurrency, toCurrency)
    }
    
    /**
     * Convert and format with target currency
     */
    fun convertAndFormat(
        amountStr: String,
        fromCurrency: String,
        toCurrency: String
    ): String {
        val converted = convert(amountStr, fromCurrency, toCurrency)
        return formatWithCurrency(converted, toCurrency)
    }
    
    /**
     * Get currency symbol
     */
    fun getSymbol(currency: String): String = when (currency) {
        "USD" -> "$"
        "EUR" -> "EUR"
        "RSD" -> "RSD"
        else -> currency
    }
    
    /**
     * Get exchange rate from currency to RSD
     */
    fun getExchangeRateToRSD(currency: String): Double {
        return exchangeRatesToRSD[currency] ?: 1.0
    }
    
    /**
     * Default display currency for aggregates
     */
    const val DEFAULT_CURRENCY = "RSD"
}

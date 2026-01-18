package com.bugdigger.piggybank.data.tables

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.kotlin.datetime.date
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

/**
 * ExchangeRates table - stores currency exchange rates
 * 
 * Exchange rates are stored as: 1 unit of fromCurrency = rate units of toCurrency
 * 
 * Example: If 1 USD = 117.50 RSD, then:
 *   fromCurrency = USD
 *   toCurrency = RSD
 *   rate = 117.50
 * 
 * To convert 100 USD to RSD: 100 * 117.50 = 11,750 RSD
 */
object ExchangeRates : UUIDTable("exchange_rates") {
    val fromCurrency = enumerationByName<Currency>("from_currency", 10)
    val toCurrency = enumerationByName<Currency>("to_currency", 10)
    val rate = decimal("rate", 19, 8) // High precision for exchange rates
    val date = date("date")
    val rateSource = varchar("source", 50).nullable() // e.g., "manual", "api", etc.
    val createdAt = timestamp("created_at")
    
    init {
        // Index for looking up rates by currency pair and date
        index("idx_exchange_rates_currencies_date", false, fromCurrency, toCurrency, date)
        // Unique constraint: only one rate per currency pair per day
        uniqueIndex("idx_exchange_rates_unique", fromCurrency, toCurrency, date)
    }
}

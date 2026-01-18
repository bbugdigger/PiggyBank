package com.bugdigger.piggybank.api.dto

import kotlinx.serialization.Serializable

@Serializable
data class CreateExchangeRateRequest(
    val fromCurrency: String, // USD, EUR, RSD
    val toCurrency: String,
    val rate: String, // BigDecimal as string
    val date: String, // ISO-8601 date: YYYY-MM-DD
    val source: String? = "manual"
)

@Serializable
data class ExchangeRateResponse(
    val id: String,
    val fromCurrency: String,
    val toCurrency: String,
    val rate: String,
    val date: String,
    val source: String?,
    val createdAt: String
)

@Serializable
data class ConvertCurrencyRequest(
    val amount: String,
    val fromCurrency: String,
    val toCurrency: String,
    val date: String? = null // If null, use latest rate
)

@Serializable
data class ConvertCurrencyResponse(
    val originalAmount: String,
    val fromCurrency: String,
    val convertedAmount: String,
    val toCurrency: String,
    val rate: String,
    val rateDate: String
)

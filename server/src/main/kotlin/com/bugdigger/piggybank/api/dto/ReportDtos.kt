package com.bugdigger.piggybank.api.dto

import kotlinx.serialization.Serializable

@Serializable
data class ReportLineItem(
    val accountId: String,
    val accountName: String,
    val accountFullName: String,
    val amount: String,
    val currency: String,
    val children: List<ReportLineItem> = emptyList()
)

@Serializable
data class BalanceSheetResponse(
    val asOfDate: String,
    val assets: List<ReportLineItem>,
    val totalAssets: String,
    val liabilities: List<ReportLineItem>,
    val totalLiabilities: String,
    val equity: List<ReportLineItem>,
    val totalEquity: String,
    val netWorth: String, // Assets - Liabilities (should equal Equity)
    val currency: String
)

@Serializable
data class IncomeStatementResponse(
    val startDate: String,
    val endDate: String,
    val income: List<ReportLineItem>,
    val totalIncome: String,
    val expenses: List<ReportLineItem>,
    val totalExpenses: String,
    val netIncome: String, // Income - Expenses
    val currency: String
)

package com.bugdigger.piggybank.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * PiggyBank color palette - GnuCash inspired
 */
object PiggyBankColors {
    // Primary brand colors
    val Primary = Color(0xFF4CAF50)          // Green
    val PrimaryDark = Color(0xFF388E3C)      // Dark green
    val PrimaryLight = Color(0xFFC8E6C9)     // Light green
    
    // Secondary
    val Secondary = Color(0xFF2196F3)        // Blue
    val SecondaryDark = Color(0xFF1976D2)
    
    // Background colors
    val Background = Color(0xFFFAFAFA)       // Light gray background
    val Surface = Color(0xFFFFFFFF)          // White
    val SurfaceVariant = Color(0xFFF5F5F5)   // Slightly darker white
    
    // Text colors
    val OnPrimary = Color(0xFFFFFFFF)        // White on primary
    val OnBackground = Color(0xFF212121)     // Almost black
    val OnSurface = Color(0xFF212121)
    val OnSurfaceVariant = Color(0xFF757575) // Gray text
    
    // Error/Warning
    val Error = Color(0xFFD32F2F)            // Red
    val Warning = Color(0xFFFFA000)          // Amber
    val Success = Color(0xFF388E3C)          // Green
    
    // ============ GnuCash-style Register Colors ============
    
    // Zebra striping for table rows
    val ZebraLight = Color(0xFFE8F5E9)       // Very light green
    val ZebraWhite = Color(0xFFFFFFFF)       // White
    
    // Selection
    val SelectedRow = Color(0xFFFFFF99)      // Yellow highlight
    val SelectedRowBorder = Color(0xFFCCCC00) // Darker yellow for border
    
    // Table header
    val TableHeader = Color(0xFF4CAF50)       // Green header
    val TableHeaderText = Color(0xFFFFFFFF)   // White text
    
    // Cell borders
    val CellBorder = Color(0xFFE0E0E0)        // Light gray
    val CellBorderFocused = Color(0xFF4CAF50) // Green when focused
    
    // Account type colors (for tree view indicators)
    val AssetColor = Color(0xFF2196F3)        // Blue
    val LiabilityColor = Color(0xFFF44336)    // Red
    val EquityColor = Color(0xFF9C27B0)       // Purple
    val IncomeColor = Color(0xFF4CAF50)       // Green
    val ExpenseColor = Color(0xFFFF9800)      // Orange
    
    // Transaction indicators
    val DepositColor = Color(0xFF2E7D32)      // Dark green for deposits
    val WithdrawalColor = Color(0xFFC62828)   // Dark red for withdrawals
    val VoidedTransaction = Color(0xFFBDBDBD) // Gray for voided
    
    // Reconciliation status
    val ReconcileNew = Color(0xFF757575)      // Gray - not reconciled
    val ReconcileCleared = Color(0xFF2196F3)  // Blue - cleared
    val ReconcileReconciled = Color(0xFF4CAF50) // Green - reconciled
}

// Extension to get color for account type
fun accountTypeColor(type: String): Color = when (type.uppercase()) {
    "ASSET" -> PiggyBankColors.AssetColor
    "LIABILITY" -> PiggyBankColors.LiabilityColor
    "EQUITY" -> PiggyBankColors.EquityColor
    "INCOME" -> PiggyBankColors.IncomeColor
    "EXPENSE" -> PiggyBankColors.ExpenseColor
    else -> PiggyBankColors.OnSurface
}

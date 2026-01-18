package com.bugdigger.piggybank.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Light theme colors
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF2E7D32),          // Forest green - money/prosperity
    onPrimary = Color.White,
    primaryContainer = Color(0xFFA5D6A7),
    onPrimaryContainer = Color(0xFF1B5E20),
    
    secondary = Color(0xFF558B2F),         // Light green
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFC5E1A5),
    onSecondaryContainer = Color(0xFF33691E),
    
    tertiary = Color(0xFF00897B),          // Teal for accents
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF80CBC4),
    onTertiaryContainer = Color(0xFF004D40),
    
    background = Color(0xFFF5F5F5),
    onBackground = Color(0xFF1C1B1F),
    
    surface = Color.White,
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFFE7E0EC),
    onSurfaceVariant = Color(0xFF49454F),
    
    error = Color(0xFFB00020),
    onError = Color.White,
    errorContainer = Color(0xFFFCD8DF),
    onErrorContainer = Color(0xFF8C0017),
    
    outline = Color(0xFF79747E),
    outlineVariant = Color(0xFFCAC4D0)
)

// Dark theme colors
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF81C784),           // Light green for dark mode
    onPrimary = Color(0xFF1B5E20),
    primaryContainer = Color(0xFF2E7D32),
    onPrimaryContainer = Color(0xFFA5D6A7),
    
    secondary = Color(0xFFAED581),
    onSecondary = Color(0xFF33691E),
    secondaryContainer = Color(0xFF558B2F),
    onSecondaryContainer = Color(0xFFC5E1A5),
    
    tertiary = Color(0xFF4DB6AC),
    onTertiary = Color(0xFF004D40),
    tertiaryContainer = Color(0xFF00897B),
    onTertiaryContainer = Color(0xFF80CBC4),
    
    background = Color(0xFF1C1B1F),
    onBackground = Color(0xFFE6E1E5),
    
    surface = Color(0xFF2B2930),
    onSurface = Color(0xFFE6E1E5),
    surfaceVariant = Color(0xFF49454F),
    onSurfaceVariant = Color(0xFFCAC4D0),
    
    error = Color(0xFFCF6679),
    onError = Color(0xFF000000),
    errorContainer = Color(0xFF8C0017),
    onErrorContainer = Color(0xFFFCD8DF),
    
    outline = Color(0xFF938F99),
    outlineVariant = Color(0xFF49454F)
)

@Composable
fun PiggyBankTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}

// Extension colors for accounting
object AccountingColors {
    val Positive = Color(0xFF2E7D32)   // Green for positive amounts
    val Negative = Color(0xFFC62828)   // Red for negative amounts
    val Neutral = Color(0xFF424242)    // Gray for zero
    
    val Asset = Color(0xFF1976D2)      // Blue
    val Liability = Color(0xFFC62828)  // Red
    val Equity = Color(0xFF7B1FA2)     // Purple
    val Income = Color(0xFF2E7D32)     // Green
    val Expense = Color(0xFFE65100)    // Orange
}

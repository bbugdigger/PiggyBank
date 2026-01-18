package com.bugdigger.piggybank.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = PiggyBankColors.Primary,
    onPrimary = PiggyBankColors.OnPrimary,
    primaryContainer = PiggyBankColors.PrimaryLight,
    onPrimaryContainer = PiggyBankColors.PrimaryDark,
    
    secondary = PiggyBankColors.Secondary,
    onSecondary = PiggyBankColors.OnPrimary,
    secondaryContainer = PiggyBankColors.Secondary.copy(alpha = 0.1f),
    onSecondaryContainer = PiggyBankColors.SecondaryDark,
    
    background = PiggyBankColors.Background,
    onBackground = PiggyBankColors.OnBackground,
    
    surface = PiggyBankColors.Surface,
    onSurface = PiggyBankColors.OnSurface,
    surfaceVariant = PiggyBankColors.SurfaceVariant,
    onSurfaceVariant = PiggyBankColors.OnSurfaceVariant,
    
    error = PiggyBankColors.Error,
    onError = PiggyBankColors.OnPrimary,
    
    outline = PiggyBankColors.CellBorder,
    outlineVariant = PiggyBankColors.CellBorder.copy(alpha = 0.5f)
)

private val DarkColorScheme = darkColorScheme(
    primary = PiggyBankColors.PrimaryLight,
    onPrimary = PiggyBankColors.PrimaryDark,
    primaryContainer = PiggyBankColors.PrimaryDark,
    onPrimaryContainer = PiggyBankColors.PrimaryLight,
    
    secondary = PiggyBankColors.Secondary,
    onSecondary = PiggyBankColors.OnPrimary,
    
    background = PiggyBankColors.OnBackground,
    onBackground = PiggyBankColors.Background,
    
    surface = PiggyBankColors.OnSurface,
    onSurface = PiggyBankColors.Surface,
    surfaceVariant = PiggyBankColors.OnSurfaceVariant,
    onSurfaceVariant = PiggyBankColors.SurfaceVariant,
    
    error = PiggyBankColors.Error,
    onError = PiggyBankColors.OnPrimary,
    
    outline = PiggyBankColors.CellBorder,
    outlineVariant = PiggyBankColors.CellBorder.copy(alpha = 0.5f)
)

@Composable
fun PiggyBankTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // For MVP, we'll use light theme only (like GnuCash)
    // Dark theme support can be added later
    val colorScheme = LightColorScheme
    
    MaterialTheme(
        colorScheme = colorScheme,
        typography = PiggyBankTypography,
        content = content
    )
}

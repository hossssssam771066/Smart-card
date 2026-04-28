package com.smartcardscanner.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val PrimaryBlue = Color(0xFF1565C0)
val PrimaryLight = Color(0xFF42A5F5)
val PrimaryDark = Color(0xFF0D47A1)
val Secondary = Color(0xFF26A69A)
val Background = Color(0xFFF5F7FA)
val Surface = Color(0xFFFFFFFF)
val ErrorRed = Color(0xFFD32F2F)
val SuccessGreen = Color(0xFF2E7D32)
val WarningOrange = Color(0xFFF57F17)
val OnPrimary = Color(0xFFFFFFFF)
val OnBackground = Color(0xFF1A1A2E)
val OnSurface = Color(0xFF333333)
val CardBorder = Color(0xFFE0E0E0)
val LightGray = Color(0xFFF0F0F0)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryBlue,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryLight,
    secondary = Secondary,
    background = Background,
    surface = Surface,
    error = ErrorRed,
    onBackground = OnBackground,
    onSurface = OnSurface,
    outline = CardBorder
)

@Composable
fun SmartCardScannerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        content = content
    )
}

package com.codecrafters.kehdona.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

/**
 * KehDoNa uses a single dark, brand-led colour scheme. Dynamic (wallpaper-based)
 * colour is intentionally not used so the brand looks the same on every device.
 */
private val KehDoNaColorScheme = darkColorScheme(
    primary = Rose400,
    onPrimary = Night950,
    primaryContainer = Plum700,
    onPrimaryContainer = Blush200,
    inversePrimary = Rose500,
    secondary = Lavender300,
    onSecondary = Night950,
    secondaryContainer = Violet700,
    onSecondaryContainer = Lavender200,
    tertiary = Blush200,
    onTertiary = Night950,
    tertiaryContainer = Plum800,
    onTertiaryContainer = Blush200,
    background = Night950,
    onBackground = Ink,
    surface = Night900,
    onSurface = Ink,
    surfaceVariant = Night800,
    onSurfaceVariant = InkMuted,
    surfaceTint = Rose400,
    surfaceDim = Night950,
    surfaceBright = Night700,
    surfaceContainerLowest = Night950,
    surfaceContainerLow = Night900,
    surfaceContainer = Night850,
    surfaceContainerHigh = Night800,
    surfaceContainerHighest = Night700,
    inverseSurface = Cream50,
    inverseOnSurface = InkDark,
    outline = InkSubtle,
    outlineVariant = Night700,
    error = ErrorRose,
    onError = OnErrorRose,
    errorContainer = Plum800,
    onErrorContainer = Blush200,
    scrim = Night950,
)

@Composable
fun KehDoNaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = KehDoNaColorScheme,
        typography = KehDoNaTypography,
        shapes = KehDoNaShapes,
        content = content,
    )
}

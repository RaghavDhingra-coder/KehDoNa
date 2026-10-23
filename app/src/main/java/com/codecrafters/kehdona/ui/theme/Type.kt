package com.codecrafters.kehdona.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.codecrafters.kehdona.R

/** Expressive serif used for display text and headlines. Bundled, so it never fails to load. */
val Fraunces = FontFamily(
    Font(R.font.fraunces_medium, FontWeight.Medium),
    Font(R.font.fraunces_semibold, FontWeight.SemiBold),
    Font(R.font.fraunces_medium_italic, FontWeight.Medium, FontStyle.Italic),
)

/** Highly readable sans-serif for body copy, labels and buttons. */
val PlusJakartaSans = FontFamily(
    Font(R.font.plus_jakarta_sans_regular, FontWeight.Normal),
    Font(R.font.plus_jakarta_sans_medium, FontWeight.Medium),
    Font(R.font.plus_jakarta_sans_semibold, FontWeight.SemiBold),
    Font(R.font.plus_jakarta_sans_bold, FontWeight.Bold),
)

private fun display(size: Int, lineHeight: Int, tracking: Double = -0.5) = TextStyle(
    fontFamily = Fraunces,
    fontWeight = FontWeight.Medium,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = tracking.sp,
)

private fun sans(size: Int, lineHeight: Int, weight: FontWeight, tracking: Double = 0.0) = TextStyle(
    fontFamily = PlusJakartaSans,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = tracking.sp,
)

val KehDoNaTypography = Typography(
    displayLarge = display(54, 58, -1.2),
    displayMedium = display(44, 48, -1.0),
    displaySmall = display(36, 41, -0.8),
    headlineLarge = display(32, 38, -0.6),
    headlineMedium = display(28, 34, -0.4),
    headlineSmall = display(24, 30, -0.2),
    titleLarge = sans(20, 26, FontWeight.SemiBold),
    titleMedium = sans(16, 22, FontWeight.SemiBold, 0.1),
    titleSmall = sans(14, 20, FontWeight.SemiBold, 0.1),
    bodyLarge = sans(16, 25, FontWeight.Normal, 0.1),
    bodyMedium = sans(14, 21, FontWeight.Normal, 0.15),
    bodySmall = sans(12, 17, FontWeight.Normal, 0.2),
    labelLarge = sans(15, 20, FontWeight.SemiBold, 0.1),
    labelMedium = sans(12, 16, FontWeight.SemiBold, 0.4),
    // Used for small uppercase eyebrows and badges.
    labelSmall = sans(11, 14, FontWeight.Bold, 1.4),
)

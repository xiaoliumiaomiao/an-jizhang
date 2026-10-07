package com.xiaoliumiaomiao.anjizhang.ui
import androidx.compose.material3.*
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val Green = Color(0xFF176B5B)
private val Red = Color(0xFFB33C38)
private val Ink = Color(0xFF17212F)
private val Paper = Color(0xFFFFFFFF)
private val Ground = Color(0xFFF5F7FB)

private val LedgerColors = lightColorScheme(
    primary = Green, onPrimary = Paper, primaryContainer = Color(0xFFE2F1ED), onPrimaryContainer = Ink,
    inversePrimary = Color(0xFF8DD6C3), secondary = Green, onSecondary = Paper,
    secondaryContainer = Color(0xFFE2F1ED), onSecondaryContainer = Ink,
    tertiary = Green, onTertiary = Paper, tertiaryContainer = Color(0xFFE2F1ED), onTertiaryContainer = Ink,
    background = Ground, onBackground = Ink, surface = Paper, onSurface = Ink,
    surfaceVariant = Ground, onSurfaceVariant = Color(0xFF526174), surfaceTint = Green,
    inverseSurface = Ink, inverseOnSurface = Paper, error = Red, onError = Paper,
    errorContainer = Color(0xFFFFE9E7), onErrorContainer = Color(0xFF651D1B),
    outline = Color(0xFF718094), outlineVariant = Color(0xFFDCE3ED), scrim = Color.Black,
    surfaceBright = Paper, surfaceDim = Color(0xFFDCE3ED), surfaceContainer = Ground,
    surfaceContainerHigh = Color(0xFFEBEFF6), surfaceContainerHighest = Color(0xFFE2E8F1),
    surfaceContainerLow = Color(0xFFF9FAFD), surfaceContainerLowest = Paper
)

private val LedgerDarkColors = darkColorScheme(
    primary = Color(0xFF8DD6C3), onPrimary = Color(0xFF00382C),
    primaryContainer = Color(0xFF105143), onPrimaryContainer = Color(0xFFB1F2DF),
    inversePrimary = Green, secondary = Color(0xFF8DD6C3), onSecondary = Color(0xFF00382C),
    secondaryContainer = Color(0xFF244D43), onSecondaryContainer = Color(0xFFB1F2DF),
    tertiary = Color(0xFFB2C8E4), onTertiary = Color(0xFF18324D),
    tertiaryContainer = Color(0xFF304A65), onTertiaryContainer = Color(0xFFD2E4FF),
    background = Color(0xFF101820), onBackground = Color(0xFFE7EEF5),
    surface = Ink, onSurface = Color(0xFFE7EEF5), surfaceVariant = Color(0xFF273340),
    onSurfaceVariant = Color(0xFFBDCAD8), surfaceTint = Color(0xFF8DD6C3),
    inverseSurface = Color(0xFFE7EEF5), inverseOnSurface = Ink,
    error = Color(0xFFFFB4AB), onError = Color(0xFF690005),
    errorContainer = Color(0xFF8C1D18), onErrorContainer = Color(0xFFFFDAD6),
    outline = Color(0xFF8D9CAC), outlineVariant = Color(0xFF3D4C5A), scrim = Color.Black,
    surfaceBright = Color(0xFF35414D), surfaceDim = Color(0xFF101820),
    surfaceContainer = Color(0xFF1C2834), surfaceContainerHigh = Color(0xFF273340),
    surfaceContainerHighest = Color(0xFF323F4C), surfaceContainerLow = Ink,
    surfaceContainerLowest = Color(0xFF0B131B)
)

private fun text(size: Int, height: Int, weight: FontWeight = FontWeight.Normal) = TextStyle(
    fontFamily = FontFamily.SansSerif, fontSize = size.sp, lineHeight = height.sp, fontWeight = weight
)

private val LedgerTypography = Typography(
    headlineSmall = text(24, 32, FontWeight.SemiBold), headlineMedium = text(28, 36, FontWeight.Bold),
    titleLarge = text(20, 28, FontWeight.SemiBold), titleMedium = text(16, 24, FontWeight.Medium),
    bodyLarge = text(16, 24), bodyMedium = text(16, 24), bodySmall = text(14, 22),
    labelLarge = text(14, 20, FontWeight.Medium), labelMedium = text(12, 18, FontWeight.Medium),
    labelSmall = text(11, 16, FontWeight.Medium)
)

@Composable fun LedgerTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) LedgerDarkColors else LedgerColors,
        typography = LedgerTypography, content = content)
}

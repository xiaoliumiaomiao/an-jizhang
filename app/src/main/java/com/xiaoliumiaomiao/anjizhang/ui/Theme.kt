package com.xiaoliumiaomiao.anjizhang.ui
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val Blue = Color(0xFF2457C5)
private val Green = Color(0xFF176B5B)
private val Red = Color(0xFFB33C38)
private val Ink = Color(0xFF17212F)
private val Paper = Color(0xFFFFFFFF)
private val Ground = Color(0xFFF5F7FB)

private val LedgerColors = lightColorScheme(
    primary = Blue, onPrimary = Paper, primaryContainer = Color(0xFFE7EEFF), onPrimaryContainer = Ink,
    inversePrimary = Color(0xFFB4C7FF), secondary = Green, onSecondary = Paper,
    secondaryContainer = Color(0xFFE2F1ED), onSecondaryContainer = Ink,
    tertiary = Blue, onTertiary = Paper, tertiaryContainer = Color(0xFFE7EEFF), onTertiaryContainer = Ink,
    background = Ground, onBackground = Ink, surface = Paper, onSurface = Ink,
    surfaceVariant = Ground, onSurfaceVariant = Color(0xFF526174), surfaceTint = Blue,
    inverseSurface = Ink, inverseOnSurface = Paper, error = Red, onError = Paper,
    errorContainer = Color(0xFFFFE9E7), onErrorContainer = Color(0xFF651D1B),
    outline = Color(0xFF718094), outlineVariant = Color(0xFFDCE3ED), scrim = Color.Black,
    surfaceBright = Paper, surfaceDim = Color(0xFFDCE3ED), surfaceContainer = Ground,
    surfaceContainerHigh = Color(0xFFEBEFF6), surfaceContainerHighest = Color(0xFFE2E8F1),
    surfaceContainerLow = Color(0xFFF9FAFD), surfaceContainerLowest = Paper
)

private fun text(size: Int, height: Int, weight: FontWeight = FontWeight.Normal) = TextStyle(
    fontFamily = FontFamily.SansSerif, fontSize = size.sp, lineHeight = height.sp, fontWeight = weight
)

private val LedgerTypography = Typography(
    headlineSmall = text(24, 32, FontWeight.SemiBold), headlineMedium = text(28, 36, FontWeight.Bold),
    titleLarge = text(20, 28, FontWeight.SemiBold), titleMedium = text(16, 24, FontWeight.Medium),
    bodyLarge = text(16, 24), bodyMedium = text(14, 22), bodySmall = text(12, 18),
    labelLarge = text(14, 20, FontWeight.Medium), labelMedium = text(12, 18, FontWeight.Medium),
    labelSmall = text(11, 16, FontWeight.Medium)
)

@Composable fun LedgerTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = LedgerColors, typography = LedgerTypography, content = content)
}

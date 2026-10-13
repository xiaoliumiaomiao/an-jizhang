package com.xiaoliumiaomiao.anjizhang.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Pine = Color(0xFF206D63)
private val Clay = Color(0xFFA54A36)
private val Ink = Color(0xFF21332F)
private val Paper = Color(0xFFFFFFFF)
private val Ground = Color(0xFFF4F5F2)

private val LedgerColors = lightColorScheme(
    primary = Pine, onPrimary = Paper,
    primaryContainer = Color(0xFFE2EEE7), onPrimaryContainer = Color(0xFF143F37),
    inversePrimary = Color(0xFF9BD4C4),
    secondary = Pine, onSecondary = Paper,
    secondaryContainer = Color(0xFFE2EEE7), onSecondaryContainer = Color(0xFF143F37),
    tertiary = Clay, onTertiary = Paper,
    tertiaryContainer = Color(0xFFF7E7DF), onTertiaryContainer = Color(0xFF6B2F21),
    background = Ground, onBackground = Ink,
    surface = Paper, onSurface = Ink,
    surfaceVariant = Color(0xFFEDF0EB), onSurfaceVariant = Color(0xFF586A62),
    surfaceTint = Pine,
    inverseSurface = Ink, inverseOnSurface = Ground,
    error = Color(0xFFBA332C), onError = Paper,
    errorContainer = Color(0xFFFFE8E4), onErrorContainer = Color(0xFF6F1915),
    outline = Color(0xFF788A80), outlineVariant = Color(0xFFDCE3DE), scrim = Color.Black,
    surfaceBright = Paper, surfaceDim = Color(0xFFDCE3DE),
    surfaceContainer = Color(0xFFEEF1EC),
    surfaceContainerHigh = Color(0xFFE8EDE7), surfaceContainerHighest = Color(0xFFE2E8E1),
    surfaceContainerLow = Color(0xFFF8FAF6), surfaceContainerLowest = Paper
)

private val LedgerDarkColors = darkColorScheme(
    primary = Color(0xFF9BD4C4), onPrimary = Color(0xFF083B31),
    primaryContainer = Color(0xFF205447), onPrimaryContainer = Color(0xFFC4EBDD),
    inversePrimary = Pine,
    secondary = Color(0xFF9BD4C4), onSecondary = Color(0xFF083B31),
    secondaryContainer = Color(0xFF244D41), onSecondaryContainer = Color(0xFFC4EBDD),
    tertiary = Color(0xFFF0B29E), onTertiary = Color(0xFF562518),
    tertiaryContainer = Color(0xFF703829), onTertiaryContainer = Color(0xFFFFDACC),
    background = Color(0xFF101A17), onBackground = Color(0xFFE5EEEA),
    surface = Color(0xFF182622), onSurface = Color(0xFFE5EEEA),
    surfaceVariant = Color(0xFF263831), onSurfaceVariant = Color(0xFFB8C9BF),
    surfaceTint = Color(0xFF9BD4C4),
    inverseSurface = Color(0xFFE5EEEA), inverseOnSurface = Ink,
    error = Color(0xFFFFB4AB), onError = Color(0xFF690005),
    errorContainer = Color(0xFF8C1D18), onErrorContainer = Color(0xFFFFDAD6),
    outline = Color(0xFF889F92), outlineVariant = Color(0xFF3B4E43), scrim = Color.Black,
    surfaceBright = Color(0xFF344A3F), surfaceDim = Color(0xFF101A17),
    surfaceContainer = Color(0xFF1D2C25),
    surfaceContainerHigh = Color(0xFF273B31), surfaceContainerHighest = Color(0xFF31463B),
    surfaceContainerLow = Color(0xFF182622), surfaceContainerLowest = Color(0xFF0B1410)
)

private fun text(size: Int, height: Int, weight: FontWeight = FontWeight.Normal) = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontSize = size.sp, lineHeight = height.sp, fontWeight = weight
)

private val LedgerTypography = Typography(
    displayLarge = text(48, 56, FontWeight.SemiBold),
    displayMedium = text(40, 48, FontWeight.SemiBold),
    displaySmall = text(32, 40, FontWeight.SemiBold),
    headlineLarge = text(32, 40, FontWeight.SemiBold),
    headlineMedium = text(28, 36, FontWeight.Bold),
    headlineSmall = text(24, 32, FontWeight.SemiBold),
    titleLarge = text(20, 28, FontWeight.SemiBold),
    titleMedium = text(16, 24, FontWeight.Medium),
    titleSmall = text(14, 20, FontWeight.Medium),
    bodyLarge = text(16, 24), bodyMedium = text(16, 24), bodySmall = text(14, 22),
    labelLarge = text(14, 20, FontWeight.Medium),
    labelMedium = text(12, 18, FontWeight.Medium),
    labelSmall = text(11, 16, FontWeight.Medium)
)

@Composable
fun LedgerTheme(preferences: UiPreferences = UiPreferences(), content: @Composable () -> Unit) {
    val dark = resolvedDark(preferences.theme, isSystemInDarkTheme())
    CompositionLocalProvider(LocalResolvedDark provides dark, LocalHandedness provides preferences.hand) {
        MaterialTheme(
            colorScheme = if (dark) LedgerDarkColors else LedgerColors,
            typography = LedgerTypography,
            shapes = Shapes(
                extraSmall = RoundedCornerShape(8.dp),
                small = RoundedCornerShape(12.dp),
                medium = RoundedCornerShape(18.dp),
                large = RoundedCornerShape(24.dp),
                extraLarge = RoundedCornerShape(24.dp)
            ),
            content = content
        )
    }
}

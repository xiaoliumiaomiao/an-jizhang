package com.xiaoliumiaomiao.anjizhang.ui

import androidx.compose.runtime.staticCompositionLocalOf

enum class ThemeMode { SYSTEM, LIGHT, DARK }
enum class Handedness { RIGHT, LEFT }

data class UiPreferences(
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val hand: Handedness = Handedness.RIGHT,
)

val LocalResolvedDark = staticCompositionLocalOf { false }
val LocalHandedness = staticCompositionLocalOf { Handedness.RIGHT }

fun resolvedDark(theme: ThemeMode, systemDark: Boolean): Boolean = when (theme) {
    ThemeMode.SYSTEM -> systemDark
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}

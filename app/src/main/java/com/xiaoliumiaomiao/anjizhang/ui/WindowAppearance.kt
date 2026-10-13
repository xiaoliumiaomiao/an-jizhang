package com.xiaoliumiaomiao.anjizhang.ui

import android.view.View
import android.view.Window
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowInsetsControllerCompat

enum class DialogWindowAppearance { FULL_SCREEN, FLOATING }

fun applyActivityWindowAppearance(activity: ComponentActivity, dark: Boolean, backgroundArgb: Int) {
    activity.window.setBackgroundDrawable(ColorDrawable(backgroundArgb))
    activity.enableEdgeToEdge(
        statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark },
        navigationBarStyle = SystemBarStyle.auto(backgroundArgb, backgroundArgb) { dark },
    )
}

@Suppress("DEPRECATION")
fun applyDialogWindowAppearance(
    window: Window,
    dark: Boolean,
    backgroundArgb: Int,
    appearance: DialogWindowAppearance,
) {
    window.setBackgroundDrawable(ColorDrawable(
        if (appearance == DialogWindowAppearance.FULL_SCREEN) backgroundArgb else Color.TRANSPARENT
    ))
    window.statusBarColor = Color.TRANSPARENT
    // API26 cannot rely on transparent modern navigation contrast handling.
    window.navigationBarColor = if (Build.VERSION.SDK_INT >= 29) Color.TRANSPARENT else backgroundArgb
    if (Build.VERSION.SDK_INT >= 29) {
        window.isStatusBarContrastEnforced = false
        window.isNavigationBarContrastEnforced = true
    }
    WindowInsetsControllerCompat(window, window.decorView).apply {
        isAppearanceLightStatusBars = !dark
        isAppearanceLightNavigationBars = !dark
    }
    // The caller owns decorFits, dim/scrim, corner surfaces and input/inset behavior.
}

internal fun nearestDialogWindow(view: View): Window? {
    var current: View? = view
    while (current != null) {
        if (current is DialogWindowProvider) return current.window
        current = current.parent as? View
    }
    return null
}

@Composable
fun SyncActivityWindowAppearance(activity: ComponentActivity) {
    val dark = LocalResolvedDark.current
    val background = MaterialTheme.colorScheme.background.toArgb()
    SideEffect { applyActivityWindowAppearance(activity, dark, background) }
}

/** Must be called inside each Dialog's content so LocalView identifies that Dialog. */
@Composable
fun SyncDialogWindowAppearance(appearance: DialogWindowAppearance) {
    val view = LocalView.current
    val dark = LocalResolvedDark.current
    val background = MaterialTheme.colorScheme.background.toArgb()
    SideEffect {
        nearestDialogWindow(view)?.let { applyDialogWindowAppearance(it, dark, background, appearance) }
    }
}

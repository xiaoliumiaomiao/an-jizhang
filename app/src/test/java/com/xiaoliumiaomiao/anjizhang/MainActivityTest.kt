package com.xiaoliumiaomiao.anjizhang

import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import androidx.core.view.WindowInsetsControllerCompat
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26])
class MainActivityTest {
    @Test fun launcherStartsWithoutCrashing() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        assertFalse(controller.get().isFinishing)
        controller.pause().stop().destroy()
    }

    @Test @Config(qualifiers = "night")
    fun nightWindowHasDarkBackgroundAndReadableSystemBarIcons() = assertWindowAppearance(true)

    @Test @Config(qualifiers = "notnight")
    fun lightWindowHasLightBackgroundAndReadableSystemBarIcons() = assertWindowAppearance(false)

    @Suppress("DEPRECATION")
    private fun assertWindowAppearance(dark: Boolean) {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        try {
            val activity = controller.get()
            assertEquals("Night fixture must affect native resources", dark,
                activity.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES)
            val background = activity.window.decorView.background
            assertTrue("Transparent system bars need a real solid window background", background is ColorDrawable)
            assertEquals("Window background must match the active ledger palette",
                if (dark) 0xFF101A17.toInt() else 0xFFF4F5F2.toInt(), (background as ColorDrawable).color)
            assertEquals("Status bar must reveal the window background", Color.TRANSPARENT, activity.window.statusBarColor)
            val icons = WindowInsetsControllerCompat(activity.window, activity.window.decorView)
            assertEquals("Status icons must contrast with the window background", !dark, icons.isAppearanceLightStatusBars)
            assertEquals("Navigation icons must follow native night mode", !dark, icons.isAppearanceLightNavigationBars)
        } finally { controller.pause().stop().destroy() }
    }
}

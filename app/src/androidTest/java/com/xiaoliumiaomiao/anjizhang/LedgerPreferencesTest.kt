package com.xiaoliumiaomiao.anjizhang

import android.content.res.Configuration
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.inspector.WindowInspector
import androidx.compose.ui.test.*
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowInsetsControllerCompat
import com.xiaoliumiaomiao.anjizhang.data.UiPreferencesStore
import com.xiaoliumiaomiao.anjizhang.data.LedgerDatabase
import com.xiaoliumiaomiao.anjizhang.domain.*
import com.xiaoliumiaomiao.anjizhang.ui.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class LedgerPreferencesTest : V018DeviceScenario() {
    private fun assertActualWindow(window: Window, dark: Boolean) {
        val appearance = WindowInsetsControllerCompat(window, window.decorView)
        assertEquals("Actual status-bar icons follow the chosen theme", !dark, appearance.isAppearanceLightStatusBars)
        assertEquals("Actual navigation-bar icons follow the chosen theme", !dark, appearance.isAppearanceLightNavigationBars)
        assertTrue("Actual window has a color background", window.decorView.background is ColorDrawable)
        assertEquals("Actual window palette follows the chosen theme",
            if (dark) 0xFF101A17.toInt() else 0xFFF4F5F2.toInt(), (window.decorView.background as ColorDrawable).color)
    }

    private fun focusedDialogWindow(tag: String): Window {
        val target = compose.awaitNativeDialogFocus(compose.onNodeWithTag(tag))
        return compose.runOnIdle {
            val roots = WindowInspector.getGlobalWindowViews().filter { it.isAttachedToWindow && it.isShown && it.hasWindowFocus() }
            assertEquals("There is one focused native editor window", 1, roots.size)
            val providers = mutableListOf<DialogWindowProvider>()
            fun visit(view: View) {
                if (view is DialogWindowProvider) providers += view
                if (view is ViewGroup) for (index in 0 until view.childCount) visit(view.getChildAt(index))
            }
            visit(roots.single())
            assertEquals("The focused window is a real Compose Dialog", 1, providers.size)
            providers.single().window.also {
                assertNotSame(compose.activity.window, it)
                assertSame(roots.single(), it.decorView)
                assertSame("The focused window owns the requested Dialog node", target, it)
            }
        }
    }

    private fun forceThemeAgainstSystem(dark: Boolean) {
        val previousNight = shell("cmd uimode night")
        try {
            shell("cmd uimode night ${if (dark) "no" else "yes"}")
            runBlocking { UiPreferencesStore.getInstance(compose.activity.applicationContext)
                .setTheme(if (dark) ThemeMode.DARK else ThemeMode.LIGHT).getOrThrow() }
            compose.activityRule.scenario.recreate()
            ready()
            compose.runOnIdle {
                assertEquals("The system theme deliberately opposes the manual choice",
                    if (dark) Configuration.UI_MODE_NIGHT_NO else Configuration.UI_MODE_NIGHT_YES,
                    compose.activity.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK)
                assertActualWindow(compose.activity.window, dark)
            }
            openGlobalEditor()
            val editorWindow = focusedDialogWindow("editor-shell")
            compose.runOnIdle { assertActualWindow(editorWindow, dark) }
            compose.onNodeWithTag("editor-date-select").performScrollTo().performClick()
            compose.onNodeWithTag("calendar-confirm").assertIsDisplayed()
            val floating = focusedDialogWindow("calendar-confirm")
            compose.runOnIdle {
                val appearance = WindowInsetsControllerCompat(floating, floating.decorView)
                assertEquals(!dark, appearance.isAppearanceLightStatusBars)
                assertEquals(!dark, appearance.isAppearanceLightNavigationBars)
                assertEquals(Color.TRANSPARENT, (floating.decorView.background as ColorDrawable).color)
            }
            compose.onNodeWithTag("calendar-cancel").performClick()
            compose.onNodeWithText("取消").performScrollTo().performClick()
        } finally {
            val mode = previousNight.substringAfter(": ").lowercase()
            check(mode in listOf("auto", "yes", "no"))
            shell("cmd uimode night $mode")
        }
    }

    @Test fun manualDarkWithSystemLightUpdatesActivityAndEditorWindows() = forceThemeAgainstSystem(true)
    @Test fun manualLightWithSystemDarkUpdatesActivityAndEditorWindows() = forceThemeAgainstSystem(false)

    @Test fun handPreferencePersistsAndMovesPhysicalActionsAndMeasuredFabAtFontTwo() {
        ready()
        val store = UiPreferencesStore.getInstance(compose.activity.applicationContext)
        compose.onNodeWithTag("nav-settings").performClick()
        compose.onNodeWithTag("settings-open-appearance").performScrollTo().performClick()
        compose.onNodeWithTag("hand-left").performScrollTo().performClick()
        compose.waitUntil(10000) { store.state.value.hand == Handedness.LEFT }
        compose.onNodeWithTag("hand-left").assertIsSelected()
        compose.activityRule.scenario.recreate(); ready()
        compose.onNodeWithTag("settings-appearance-list").assertIsDisplayed()
        compose.onNodeWithTag("hand-left").performScrollTo().assertIsSelected()
        val actualPersisted = UiPreferencesStore(compose.activity.getSharedPreferences("ui-preferences", Context.MODE_PRIVATE))
        assertEquals(Handedness.LEFT, actualPersisted.state.value.hand)
        fun fabIsOnPhysicalSide(left: Boolean) {
            val fab = compose.onNodeWithTag("add-entry").assertIsDisplayed().assertIsEnabled().fetchSemanticsNode().boundsInRoot
            val root = compose.onNodeWithTag("settings-appearance-list").fetchSemanticsNode().boundsInRoot
            assertTrue("Physical FAB side follows the confirmed hand", if (left) fab.center.x < root.center.x else fab.center.x > root.center.x)
        }
        fabIsOnPhysicalSide(true)
        compose.onNodeWithTag("hand-right").performScrollTo().performClick()
        compose.waitUntil(10000) { store.state.value.hand == Handedness.RIGHT }
        fabIsOnPhysicalSide(false)
        compose.onNodeWithTag("hand-left").performScrollTo().performClick()
        compose.waitUntil(10000) { store.state.value.hand == Handedness.LEFT }
        compose.onNodeWithTag("settings-child-back").performScrollTo().performClick()
        compose.onNodeWithTag("settings-open-backup").performScrollTo().performClick()
        openGlobalEditor()
        compose.onNodeWithText("取消").performScrollTo().performClick()
        compose.onNodeWithTag("settings-backup-list").assertIsDisplayed()
        compose.onNodeWithTag("settings-child-back").performScrollTo().performClick()
        val accounts = (0 until 8).map { Account(UUID.randomUUID().toString(), "合成账户尾部测试${it}", AccountKind.ASSET, AccountSubtype.CASH, sortOrder = it) }
        LedgerDatabase(compose.activity).use { database -> accounts.forEach(database::saveAccount) }
        val vm = model()
        val previousRevision = vm.state.value.snapshotRevision
        compose.runOnIdle { vm.load() }
        compose.waitUntil(10000) { !vm.state.value.busy && vm.state.value.snapshotRevision > previousRevision }
        val oldFont = shell("settings get system font_scale")
        fun override(kind: String) = shell("wm $kind").lineSequence().firstOrNull { it.startsWith("Override ") }?.substringAfter(": ")
        val oldSize = override("size"); val oldDensity = override("density")
        try {
            shell("wm size 720x1280"); shell("wm density 320"); shell("settings put system font_scale 2.0")
            compose.activityRule.scenario.recreate(); ready()
            compose.waitUntil(10000) { compose.activity.resources.configuration.fontScale == 2f && compose.activity.resources.configuration.screenWidthDp == 360 }
            compose.onNodeWithTag("nav-accounts").assertIsDisplayed().performClick()
            compose.onNodeWithTag("accounts-list").assertIsDisplayed()
            val last = accounts.last()
            val row = hasTestTag("account-row-${last.id}")
            compose.onNodeWithTag("accounts-list").performScrollToNode(row).performTouchInput { swipeUp() }
            compose.waitUntil(10000) { compose.onAllNodes(hasText("余额 ${Money.format(0)}") and hasAnyAncestor(row), useUnmergedTree = true).fetchSemanticsNodes().size == 1 }
            val listNode = compose.onNodeWithTag("accounts-list").fetchSemanticsNode()
            val fabNode = compose.onNodeWithTag("add-entry").assertIsDisplayed().fetchSemanticsNode()
            listOf(last.name, "现金", "余额 ${Money.format(0)}").forEach { text ->
                val node = compose.onNode(hasText(text) and hasAnyAncestor(row), useUnmergedTree = true).assertIsDisplayed().fetchSemanticsNode()
                compose.runOnIdle {
                    val full = androidx.compose.ui.geometry.Rect(node.positionInRoot, androidx.compose.ui.geometry.Size(node.size.width.toFloat(), node.size.height.toFloat()))
                    val viewport = listNode.boundsInRoot; val fab = fabNode.boundsInRoot
                    assertTrue("Actual last text is fully visible at font 2", full.left >= viewport.left && full.right <= viewport.right && full.top >= viewport.top && full.bottom <= viewport.bottom)
                    assertFalse("Actual last text does not overlap measured left FAB", full.left < fab.right && full.right > fab.left && full.top < fab.bottom && full.bottom > fab.top)
                    assertTrue(fab.center.x < viewport.center.x)
                }
            }
            compose.onNodeWithTag("accounts-list").performScrollToNode(hasTestTag("account-add"))
            compose.onNodeWithTag("account-add").performClick()
            val save = compose.onNodeWithTag("account-editor-save").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
            val cancel = compose.onNodeWithTag("account-editor-save-cancel").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
            assertTrue("Left hand moves the actual primary action before cancel", save.left < cancel.left || save.top < cancel.top)
            compose.onNodeWithTag("account-editor-save-cancel").performClick()
        } finally {
            shell(if (oldSize == null) "wm size reset" else "wm size $oldSize")
            shell(if (oldDensity == null) "wm density reset" else "wm density $oldDensity")
            shell(if (oldFont == "null" || oldFont.isEmpty()) "settings delete system font_scale" else "settings put system font_scale $oldFont")
            compose.activityRule.scenario.recreate()
            assertEquals(oldSize, override("size")); assertEquals(oldDensity, override("density"))
            assertEquals(oldFont, shell("settings get system font_scale"))
        }
    }
}

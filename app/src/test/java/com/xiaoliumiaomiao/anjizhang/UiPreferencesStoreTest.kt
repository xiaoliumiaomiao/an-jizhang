package com.xiaoliumiaomiao.anjizhang

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowInsetsControllerCompat
import androidx.test.core.app.ApplicationProvider
import com.xiaoliumiaomiao.anjizhang.data.UiPreferencesStore
import com.xiaoliumiaomiao.anjizhang.ui.Handedness
import com.xiaoliumiaomiao.anjizhang.ui.ThemeMode
import com.xiaoliumiaomiao.anjizhang.ui.UiPreferences
import com.xiaoliumiaomiao.anjizhang.ui.resolvedDark
import com.xiaoliumiaomiao.anjizhang.ui.DialogWindowAppearance
import com.xiaoliumiaomiao.anjizhang.ui.applyActivityWindowAppearance
import com.xiaoliumiaomiao.anjizhang.ui.applyDialogWindowAppearance
import com.xiaoliumiaomiao.anjizhang.ui.nearestDialogWindow
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Collections
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26])
class UiPreferencesStoreTest {
    @Test fun emptyPreferencesUseSafeDefaults() {
        assertEquals(UiPreferences(), UiPreferencesStore(ControlledPreferences()).state.value)
    }

    @Test fun validSavedValuesLoadSynchronously() {
        val prefs = ControlledPreferences(mapOf("theme_mode" to "dark", "handedness" to "left"))
        assertEquals(UiPreferences(ThemeMode.DARK, Handedness.LEFT), UiPreferencesStore(prefs).state.value)
    }

    @Test fun unknownThemeDoesNotDiscardValidHand() {
        val prefs = ControlledPreferences(mapOf("theme_mode" to "future", "handedness" to "left"))
        assertEquals(UiPreferences(ThemeMode.SYSTEM, Handedness.LEFT), UiPreferencesStore(prefs).state.value)
    }

    @Test fun unknownHandDoesNotDiscardValidTheme() {
        val prefs = ControlledPreferences(mapOf("theme_mode" to "light", "handedness" to "future"))
        assertEquals(UiPreferences(ThemeMode.LIGHT, Handedness.RIGHT), UiPreferencesStore(prefs).state.value)
    }

    @Test fun wrongTypedThemeDoesNotDiscardValidHand() {
        val prefs = ControlledPreferences(mapOf("theme_mode" to 2, "handedness" to "left"))
        assertEquals(UiPreferences(ThemeMode.SYSTEM, Handedness.LEFT), UiPreferencesStore(prefs).state.value)
    }

    @Test fun wrongTypedHandDoesNotDiscardValidTheme() {
        val prefs = ControlledPreferences(mapOf("theme_mode" to "dark", "handedness" to true))
        assertEquals(UiPreferences(ThemeMode.DARK, Handedness.RIGHT), UiPreferencesStore(prefs).state.value)
    }

    @Test fun resolvedThemeUsesManualChoiceWithoutChangingSystemInput() {
        for (system in listOf(false, true)) {
            assertEquals(system, resolvedDark(ThemeMode.SYSTEM, system))
            assertFalse(resolvedDark(ThemeMode.LIGHT, system))
            assertTrue(resolvedDark(ThemeMode.DARK, system))
        }
    }

    @Test fun successReturnsOnlyAfterBothKeysAndStateAreConfirmed() = runBlocking {
        val prefs = ControlledPreferences()
        val store = UiPreferencesStore(prefs)
        assertEquals(UiPreferences(ThemeMode.DARK), store.setTheme(ThemeMode.DARK).getOrThrow())
        assertEquals(mapOf("theme_mode" to "dark", "handedness" to "right"), prefs.disk)
        assertEquals(UiPreferences(ThemeMode.DARK), store.state.value)
        assertEquals(UiPreferences(ThemeMode.DARK, Handedness.LEFT), store.setHand(Handedness.LEFT).getOrThrow())
        assertEquals(mapOf("theme_mode" to "dark", "handedness" to "left"), prefs.disk)
        assertEquals(store.state.value, UiPreferencesStore(ControlledPreferences(prefs.disk)).state.value)
    }

    @Test fun realSharedPreferencesSuccessfulCommitCanBeReadByReconstructedTestStore() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<Context>()
        val prefs = app.getSharedPreferences("preferences-test-${UUID.randomUUID()}", Context.MODE_PRIVATE)
        val store = UiPreferencesStore(prefs)
        store.setHand(Handedness.LEFT).getOrThrow()
        store.setTheme(ThemeMode.LIGHT).getOrThrow()
        assertEquals("light", prefs.getString("theme_mode", null))
        assertEquals("left", prefs.getString("handedness", null))
        assertEquals(store.state.value, UiPreferencesStore(prefs).state.value)
    }

    @Test fun failedCommitPollutesMemoryButIndependentHandWriteUsesConfirmedTheme() = runBlocking {
        val prefs = ControlledPreferences(results = listOf(false, true))
        val store = UiPreferencesStore(prefs)
        assertTrue(store.setTheme(ThemeMode.DARK).isFailure)
        assertEquals("dark", prefs.getString("theme_mode", null))
        assertEquals(emptyMap<String, Any?>(), prefs.disk)
        assertEquals(UiPreferences(), store.state.value)
        assertEquals(UiPreferences(hand = Handedness.LEFT), store.setHand(Handedness.LEFT).getOrThrow())
        assertEquals(mapOf("theme_mode" to "system", "handedness" to "left"), prefs.disk)
        assertEquals(store.state.value, UiPreferencesStore(ControlledPreferences(prefs.disk)).state.value)
    }

    @Test fun dirtyEqualRequestStillCommitsConfirmedValuesAndCanRetryAgain() = runBlocking {
        val prefs = ControlledPreferences(results = listOf(false, false, true))
        val store = UiPreferencesStore(prefs)
        assertTrue(store.setTheme(ThemeMode.DARK).isFailure)
        assertTrue(store.setTheme(ThemeMode.SYSTEM).isFailure)
        assertEquals(UiPreferences(), store.state.value)
        assertTrue(store.setTheme(ThemeMode.SYSTEM).isSuccess)
        assertEquals(3, prefs.writes.size)
        assertEquals(mapOf("theme_mode" to "system", "handedness" to "right"), prefs.disk)
    }

    @Test fun exceptionDuringCommitIsReportedAndMutexRemainsRetryable() = runBlocking {
        val prefs = ControlledPreferences(results = listOf(IllegalStateException("synthetic disk failure"), true))
        val store = UiPreferencesStore(prefs)
        assertTrue(store.setTheme(ThemeMode.DARK).isFailure)
        assertEquals(UiPreferences(), store.state.value)
        assertEquals(UiPreferences(hand = Handedness.LEFT), store.setHand(Handedness.LEFT).getOrThrow())
        assertEquals(mapOf("theme_mode" to "system", "handedness" to "left"), prefs.disk)
    }

    @Test fun settersShareMutexAndCopyLatestConfirmedState() = runBlocking {
        val gate = CommitGate()
        val prefs = ControlledPreferences(gate = gate)
        val store = UiPreferencesStore(prefs)
        val theme = async(Dispatchers.Default) { store.setTheme(ThemeMode.DARK) }
        try {
            assertTrue("First write must reach controlled barrier", gate.started.await(3, TimeUnit.SECONDS))
            assertEquals(UiPreferences(), store.state.value)
            val hand = async(start = CoroutineStart.UNDISPATCHED) { store.setHand(Handedness.LEFT) }
            assertEquals(1, prefs.writes.size)
            gate.release.countDown()
            theme.await().getOrThrow()
            hand.await().getOrThrow()
            assertEquals(listOf(
                mapOf("theme_mode" to "dark", "handedness" to "right"),
                mapOf("theme_mode" to "dark", "handedness" to "left"),
            ), prefs.writes.toList())
            assertEquals(UiPreferences(ThemeMode.DARK, Handedness.LEFT), store.state.value)
        } finally { gate.release.countDown(); theme.cancelAndJoin() }
    }

    @Test fun cancellingMutexWaiterDoesNotWrite() = runBlocking {
        val gate = CommitGate()
        val prefs = ControlledPreferences(gate = gate)
        val store = UiPreferencesStore(prefs)
        val theme = async(Dispatchers.Default) { store.setTheme(ThemeMode.DARK) }
        try {
            assertTrue(gate.started.await(3, TimeUnit.SECONDS))
            val waiter = launch(start = CoroutineStart.UNDISPATCHED) { store.setHand(Handedness.LEFT) }
            waiter.cancelAndJoin()
            assertEquals(1, prefs.writes.size)
            gate.release.countDown()
            theme.await().getOrThrow()
            assertEquals(UiPreferences(ThemeMode.DARK), store.state.value)
            assertEquals(1, prefs.writes.size)
        } finally { gate.release.countDown(); theme.cancelAndJoin() }
    }

    @Test fun cancellingDuringCommitFinishesDiskAndPublicationBeforeCancellationReturns() = runBlocking {
        val gate = CommitGate()
        val prefs = ControlledPreferences(gate = gate)
        val store = UiPreferencesStore(prefs)
        val saving = async(Dispatchers.Default) { store.setTheme(ThemeMode.DARK) }
        try {
            assertTrue(gate.started.await(3, TimeUnit.SECONDS))
            saving.cancel()
            gate.release.countDown()
            saving.join()
            try { saving.await(); fail("Cancelled caller must receive CancellationException") }
            catch (_: CancellationException) { /* Cancellation is not a failed preference save. */ }
            assertEquals(mapOf("theme_mode" to "dark", "handedness" to "right"), prefs.disk)
            assertEquals(UiPreferences(ThemeMode.DARK), store.state.value)
            store.setHand(Handedness.LEFT).getOrThrow()
            assertEquals(UiPreferences(ThemeMode.DARK, Handedness.LEFT), store.state.value)
        } finally { gate.release.countDown(); saving.cancelAndJoin() }
    }

    @Test fun processFactoryReusesStoreForApplicationAndWrappedActivityContext() {
        val app = ApplicationProvider.getApplicationContext<Context>()
        val controller = Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val first = UiPreferencesStore.getInstance(app)
            assertSame(first, UiPreferencesStore.getInstance(ContextWrapper(controller.get())))
            assertSame(first, UiPreferencesStore.getInstance(app))
        } finally { controller.pause().stop().destroy() }
    }

    @Test @Config(qualifiers = "notnight")
    fun activityForcedDarkUpdatesRealWindowWithoutChangingLightSystemConfiguration() =
        assertManualActivityWindow(true)

    @Test @Config(qualifiers = "night")
    fun activityForcedLightUpdatesRealWindowWithoutChangingDarkSystemConfiguration() =
        assertManualActivityWindow(false)

    @Suppress("DEPRECATION")
    private fun assertManualActivityWindow(dark: Boolean) {
        val controller = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        try {
            val activity = controller.get()
            val beforeNight = activity.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
            assertEquals(if (dark) Configuration.UI_MODE_NIGHT_NO else Configuration.UI_MODE_NIGHT_YES, beforeNight)
            val background = if (dark) 0xFF101A17.toInt() else 0xFFF4F5F2.toInt()
            applyActivityWindowAppearance(activity, dark, background)
            assertEquals(background, (activity.window.decorView.background as ColorDrawable).color)
            assertEquals(Color.TRANSPARENT, activity.window.statusBarColor)
            assertEquals("API26 navigation needs a matching readable base", background, activity.window.navigationBarColor)
            val icons = WindowInsetsControllerCompat(activity.window, activity.window.decorView)
            assertEquals(!dark, icons.isAppearanceLightStatusBars)
            assertEquals(!dark, icons.isAppearanceLightNavigationBars)
            assertEquals(beforeNight, activity.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK)
        } finally { controller.pause().stop().destroy() }
    }

    @Suppress("DEPRECATION")
    @Test fun fullScreenDialogUpdatesPassedWindowAndLeavesActivityAlone() {
        val controller = Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val activity = controller.get()
            activity.window.setBackgroundDrawable(ColorDrawable(Color.MAGENTA))
            val window = Dialog(activity).window!!
            assertNotSame(activity.window, window)
            val darkBackground = 0xFF101A17.toInt()
            applyDialogWindowAppearance(window, true, darkBackground, DialogWindowAppearance.FULL_SCREEN)
            assertTrue("Full editor needs a solid palette window background", window.decorView.background is ColorDrawable)
            assertEquals(darkBackground, (window.decorView.background as ColorDrawable).color)
            assertEquals(darkBackground, window.navigationBarColor)
            val icons = WindowInsetsControllerCompat(window, window.decorView)
            assertFalse(icons.isAppearanceLightStatusBars)
            assertFalse(icons.isAppearanceLightNavigationBars)
            val lightBackground = 0xFFF4F5F2.toInt()
            applyDialogWindowAppearance(window, false, lightBackground, DialogWindowAppearance.FULL_SCREEN)
            assertEquals(lightBackground, (window.decorView.background as ColorDrawable).color)
            assertEquals(lightBackground, window.navigationBarColor)
            assertTrue(icons.isAppearanceLightStatusBars)
            assertTrue(icons.isAppearanceLightNavigationBars)
            assertEquals(Color.TRANSPARENT, window.statusBarColor)
            assertEquals(Color.MAGENTA, (activity.window.decorView.background as ColorDrawable).color)
        } finally { controller.pause().stop().destroy() }
    }

    @Suppress("DEPRECATION")
    @Test fun floatingDialogKeepsTransparentBackgroundDimInsetsAndInputFlags() {
        val controller = Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val window = Dialog(controller.get()).window!!
            // Install the themed decor before assigning the controlled dim/input fixture.
            window.decorView
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            window.attributes = window.attributes.apply { dimAmount = 0.42f }
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            val originalFlags = window.attributes.flags
            val originalInput = window.attributes.softInputMode
            val layoutMask = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            val originalLayout = window.decorView.systemUiVisibility and layoutMask
            val background = 0xFFF4F5F2.toInt()
            assertEquals("Controlled dim fixture must exist before helper", 0.42f, window.attributes.dimAmount, 0f)
            applyDialogWindowAppearance(window, false, background, DialogWindowAppearance.FLOATING)
            assertTrue("Floating window needs an explicit transparent background", window.decorView.background is ColorDrawable)
            assertEquals(Color.TRANSPARENT, (window.decorView.background as ColorDrawable).color)
            val retainedFlagMask = WindowManager.LayoutParams.FLAG_DIM_BEHIND or WindowManager.LayoutParams.FLAG_FULLSCREEN
            assertEquals(originalFlags and retainedFlagMask, window.attributes.flags and retainedFlagMask)
            assertEquals(0.42f, window.attributes.dimAmount, 0f)
            assertEquals(originalInput, window.attributes.softInputMode)
            assertEquals(originalLayout, window.decorView.systemUiVisibility and layoutMask)
            assertEquals(background, window.navigationBarColor)
            val icons = WindowInsetsControllerCompat(window, window.decorView)
            assertTrue(icons.isAppearanceLightStatusBars)
            assertTrue(icons.isAppearanceLightNavigationBars)
        } finally { controller.pause().stop().destroy() }
    }

    @Test fun nearestProviderUsesSelfOrClosestDialogAndNeverFallsBackToActivity() {
        val controller = Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val activity = controller.get()
            val lowerWindow = Dialog(activity).window!!
            val upperWindow = Dialog(activity).window!!
            val lower = ProviderView(activity, lowerWindow)
            val middle = FrameLayout(activity)
            val upper = ProviderView(activity, upperWindow)
            val leaf = View(activity)
            lower.addView(middle)
            middle.addView(upper)
            upper.addView(leaf)
            assertSame(upperWindow, nearestDialogWindow(leaf))
            assertSame(upperWindow, nearestDialogWindow(upper))
            assertSame(lowerWindow, nearestDialogWindow(middle))
            assertNull(nearestDialogWindow(View(activity)))
        } finally { controller.pause().stop().destroy() }
    }

    private class ProviderView(context: Context, override val window: Window) :
        FrameLayout(context), DialogWindowProvider

    @Suppress("DEPRECATION")
    @Test @Config(sdk = [35])
    fun modernDialogUsesTransparentBarsAndSupportedContrastProperties() {
        val controller = Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val window = Dialog(controller.get()).window!!
            window.isStatusBarContrastEnforced = true
            window.isNavigationBarContrastEnforced = false
            window.statusBarColor = Color.MAGENTA
            window.navigationBarColor = Color.MAGENTA
            applyDialogWindowAppearance(window, true, 0xFF101A17.toInt(), DialogWindowAppearance.FLOATING)
            assertFalse(window.isStatusBarContrastEnforced)
            assertTrue(window.isNavigationBarContrastEnforced)
            assertEquals(Color.TRANSPARENT, window.statusBarColor)
            assertEquals(Color.TRANSPARENT, window.navigationBarColor)
        } finally { controller.pause().stop().destroy() }
    }

    private class CommitGate {
        val started = CountDownLatch(1)
        val release = CountDownLatch(1)
    }

    /** A disk fault seam: commit updates memory first, as Android does, and only then succeeds/fails. */
    private class ControlledPreferences(
        initial: Map<String, Any?> = emptyMap(),
        results: List<Any> = emptyList(),
        private val gate: CommitGate? = null,
    ) : SharedPreferences {
        private val memory = initial.toMutableMap()
        private val outcomes = ArrayDeque(results)
        val writes: MutableList<Map<String, Any?>> = Collections.synchronizedList(mutableListOf())
        @Volatile var disk: Map<String, Any?> = initial.toMap()
            private set
        override fun getAll(): MutableMap<String, *> = synchronized(memory) { memory.toMutableMap() }
        override fun getString(key: String, defValue: String?): String? = synchronized(memory) {
            if (memory.containsKey(key)) memory[key] as String? else defValue
        }
        @Suppress("UNCHECKED_CAST")
        override fun getStringSet(key: String, defValues: MutableSet<String>?): MutableSet<String>? =
            synchronized(memory) { memory[key] as? MutableSet<String> ?: defValues }
        override fun getInt(key: String, defValue: Int): Int = synchronized(memory) { memory[key] as? Int ?: defValue }
        override fun getLong(key: String, defValue: Long): Long = synchronized(memory) { memory[key] as? Long ?: defValue }
        override fun getFloat(key: String, defValue: Float): Float = synchronized(memory) { memory[key] as? Float ?: defValue }
        override fun getBoolean(key: String, defValue: Boolean): Boolean = synchronized(memory) { memory[key] as? Boolean ?: defValue }
        override fun contains(key: String): Boolean = synchronized(memory) { memory.containsKey(key) }
        override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) = Unit
        override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) = Unit
        override fun edit(): SharedPreferences.Editor = object : SharedPreferences.Editor {
            private val changes = mutableMapOf<String, Any?>()
            private var clear = false
            override fun putString(key: String, value: String?) = apply { changes[key] = value }
            override fun putStringSet(key: String, values: MutableSet<String>?) = apply { changes[key] = values?.toSet() }
            override fun putInt(key: String, value: Int) = apply { changes[key] = value }
            override fun putLong(key: String, value: Long) = apply { changes[key] = value }
            override fun putFloat(key: String, value: Float) = apply { changes[key] = value }
            override fun putBoolean(key: String, value: Boolean) = apply { changes[key] = value }
            override fun remove(key: String) = apply { changes[key] = null }
            override fun clear() = apply { clear = true }
            override fun apply() { commit() }
            override fun commit(): Boolean {
                val candidate = synchronized(memory) {
                    if (clear) memory.clear()
                    changes.forEach { (key, value) -> if (value == null) memory.remove(key) else memory[key] = value }
                    memory.toMap().also { writes.add(it) }
                }
                if (writes.size == 1 && gate != null) {
                    gate.started.countDown()
                    check(gate.release.await(5, TimeUnit.SECONDS)) { "Controlled commit barrier timed out" }
                }
                val outcome = synchronized(outcomes) { if (outcomes.isEmpty()) true else outcomes.removeFirst() }
                if (outcome is RuntimeException) throw outcome
                if (outcome == true) disk = candidate
                return outcome == true
            }
        }
    }
}

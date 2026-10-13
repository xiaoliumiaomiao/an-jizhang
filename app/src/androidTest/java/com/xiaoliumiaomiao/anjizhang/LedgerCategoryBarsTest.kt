package com.xiaoliumiaomiao.anjizhang

import android.content.res.Configuration
import android.os.SystemClock
import android.util.Log
import android.view.Choreographer
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import com.xiaoliumiaomiao.anjizhang.domain.Entry
import com.xiaoliumiaomiao.anjizhang.domain.EntryKind
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class LedgerCategoryBarsTest : StatisticsDeviceScenario() {
    private fun row(id: Int, kind: EntryKind, cents: Long, name: String, date: String) =
        Entry(statsId(id), kind, cents, name, LocalDate.parse(date), "比例条合成测试")

    private val fractionFixture = listOf(
        row(601, EntryKind.EXPENSE, 6000, " 主类 ", "2025-05-01"),
        row(602, EntryKind.EXPENSE, 3000, "主类", "2025-05-02"),
        row(603, EntryKind.EXPENSE, 3000, "次类", "2025-05-03"),
        row(604, EntryKind.INCOME, 7500, "主类", "2025-05-01"),
        row(605, EntryKind.INCOME, 2500, "次类", "2025-05-02"),
        row(606, EntryKind.EXPENSE, 9000, "主类", "2025-06-01"),
        row(607, EntryKind.INCOME, 2500, "主类", "2025-06-01"),
        row(608, EntryKind.EXPENSE, 90000, "主类", "2024-05-01"),
        row(609, EntryKind.INCOME, 90000, "主类", "2024-05-01")
    )

    private fun bar(name: String, kind: EntryKind): SemanticsNodeInteraction {
        category(name, kind)
        return compose.onNodeWithTag("stats-category-bar-${kind.name}-$name", useUnmergedTree = true)
            .assertIsDisplayed()
    }

    private fun assertShare(name: String, kind: EntryKind, fraction: Float, amount: String,
        percentage: String, count: Int) {
        val categoryTag = "stats-category-${kind.name}-$name"
        category(name, kind).assertTextContains("$name  ¥$amount").assertTextContains("$percentage · ${count}笔")
        readable("$name  ¥$amount", categoryTag)
        readable("$percentage · ${count}笔", categoryTag)
        val node = bar(name, kind)
        val type = if (kind == EntryKind.INCOME) "收入" else "支出"
        node.assertContentDescriptionEquals("${name}占本期${type}总额")
        node.assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, percentage))
        val range = node.fetchSemanticsNode().config[SemanticsProperties.ProgressBarRangeInfo]
        assertEquals(ProgressBarRangeInfo(fraction, 0f..1f), range)
        assertTrue("Finite category share", range.current.isFinite())
        assertTrue(range.current in 0f..1f)
        assertTrue("Share semantics must describe recorded amounts",
            node.fetchSemanticsNode().config[SemanticsProperties.ContentDescription].none { "加载" in it || "进度" in it })

        // Inspect the rendered middle scanline: semantics alone cannot prove a proportional graphic.
        val pixels = node.captureToImage().toPixelMap()
        assertTrue("Positive category bar dimensions", pixels.width >= 100 && pixels.height >= 2)
        val dark = compose.activity.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        val fill = when {
            kind == EntryKind.EXPENSE && dark -> 0xFFF0B29E.toInt()
            kind == EntryKind.EXPENSE -> 0xFFA54A36.toInt()
            dark -> 0xFF9BD4C4.toInt()
            else -> 0xFF206D63.toInt()
        }
        val y = pixels.height / 2
        val filled = (0 until pixels.width).count { pixels[it, y].toArgb() == fill }
        assertEquals("Rendered filled width follows this type's full-period total",
            fraction * pixels.width, filled.toFloat(), 2f)
        touchTarget(category(name, kind))
    }

    @Test fun monthlyBarsUseIndependentTypeTotalsAndKeepExactDrill() {
        openStats()
        replaceFixture(fractionFixture)
        overview("100.00", "120.00", "-20.00")
        assertShare("主类", EntryKind.EXPENSE, .75f, "90.00", "75.0%", 2)
        assertShare("次类", EntryKind.EXPENSE, .25f, "30.00", "25.0%", 1)
        categoryOrder(listOf("主类", "次类"))
        // Ranking traversal ends at the list bottom; capture the actual category chart.
        compose.onNodeWithTag("stats-list").performScrollToKey("category-title")
        compose.onNodeWithTag("stats-category-bar-EXPENSE-主类", useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithTag("stats-category-bar-EXPENSE-次类", useUnmergedTree = true).assertIsDisplayed()
        captureLedgerScreen("v016-category-month-expense")
        drill("主类")
        allDrillIds(listOf(602, 601))
        drillBack()
        kind(EntryKind.INCOME)
        assertShare("主类", EntryKind.INCOME, .75f, "75.00", "75.0%", 1)
        assertShare("次类", EntryKind.INCOME, .25f, "25.00", "25.0%", 1)
        categoryOrder(listOf("主类", "次类"), EntryKind.INCOME)
        // Ranking traversal ends at the list bottom; capture the actual category chart.
        compose.onNodeWithTag("stats-list").performScrollToKey("category-title")
        compose.onNodeWithTag("stats-category-bar-INCOME-主类", useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithTag("stats-category-bar-INCOME-次类", useUnmergedTree = true).assertIsDisplayed()
        captureLedgerScreen("v016-category-month-income")
        drill("主类", EntryKind.INCOME)
        allDrillIds(listOf(604))
    }

    @Test fun annualBarsUseFullYearTypeTotalsAndUnroundedVisualFractions() {
        openStats()
        replaceFixture(fractionFixture)
        modeYear()
        year(2025)
        overview("125.00", "210.00", "-85.00", true)
        assertShare("主类", EntryKind.EXPENSE, 6f / 7f, "180.00", "85.7%", 3)
        assertShare("次类", EntryKind.EXPENSE, 1f / 7f, "30.00", "14.3%", 1)
        captureLedgerScreen("v016-category-year-expense")
        drill("主类")
        allDrillIds(listOf(606, 602, 601))
        drillBack()
        kind(EntryKind.INCOME)
        assertShare("主类", EntryKind.INCOME, .8f, "100.00", "80.0%", 2)
        assertShare("次类", EntryKind.INCOME, .2f, "25.00", "20.0%", 1)
        captureLedgerScreen("v016-category-year-income")
        drill("主类", EntryKind.INCOME)
        allDrillIds(listOf(607, 604))
    }

    private fun noBars() {
        awaitAnalysis()
        compose.onAllNodes(SemanticsMatcher("category share bar") {
            it.config.getOrElse(SemanticsProperties.TestTag) { "" }.startsWith("stats-category-bar-")
        }, useUnmergedTree = true).assertCountEquals(0)
    }

    @Test fun singleCategoryIsFullWidthAndEmptyMonthsAndYearsHaveNoInventedBars() {
        openStats()
        replaceFixture(listOf(row(620, EntryKind.EXPENSE, 1, "唯一分类", "2025-05-01")))
        assertShare("唯一分类", EntryKind.EXPENSE, 1f, "0.01", "100.0%", 1)
        captureLedgerScreen("v016-category-single")
        kind(EntryKind.INCOME)
        text("这个月还没有收入")
        noBars()
        month(2025, 7)
        for (type in EntryKind.entries) {
            kind(type)
            text(if (type == EntryKind.INCOME) "这个月还没有收入" else "这个月还没有支出")
            noBars()
        }
        captureLedgerScreen("v016-category-empty-month")
        modeYear()
        year(2026)
        for (type in EntryKind.entries) {
            kind(type)
            text(if (type == EntryKind.INCOME) "这一年还没有收入" else "这一年还没有支出")
            noBars()
        }
        captureLedgerScreen("v016-category-empty-year")
    }

    private fun screenOverride(kind: String) = shell("wm $kind").lineSequence()
        .firstOrNull { it.startsWith("Override ") }?.substringAfter(": ")

    private data class CategoryConfiguration(val fontScale: Float, val widthDp: Int, val densityDpi: Int, val nightMode: Int) {
        fun matches(actual: CategoryConfiguration) = kotlin.math.abs(actual.fontScale - fontScale) < .01f &&
            actual.widthDp == widthDp && actual.densityDpi == densityDpi && actual.nightMode == nightMode
    }

    private fun categoryConfiguration(config: Configuration) = CategoryConfiguration(
        config.fontScale, config.screenWidthDp, config.densityDpi, config.uiMode and Configuration.UI_MODE_NIGHT_MASK)

    private fun awaitStableConfiguration(expected: CategoryConfiguration, phase: String, deadline: Long): MainActivity {
        val registry = InstrumentationRegistry.getInstrumentation()
        val completed = CountDownLatch(1)
        val observing = AtomicBoolean(true)
        val stable = AtomicReference<MainActivity?>()
        val failure = AtomicReference<Throwable?>()
        val observed = AtomicReference("no frame observed")
        var previous: MainActivity? = null
        var previousConfig: CategoryConfiguration? = null
        var previousFrame = -1L
        var choreographer: Choreographer? = null
        lateinit var callback: Choreographer.FrameCallback
        callback = Choreographer.FrameCallback { frameTime ->
            if (!observing.get()) return@FrameCallback
            try {
                val resumed = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED)
                    .filterIsInstance<MainActivity>()
                val activity = resumed.singleOrNull()
                val actual = activity?.let { categoryConfiguration(it.resources.configuration) }
                val ready = activity != null && !activity.isDestroyed && !activity.isFinishing &&
                    activity.window.decorView.isAttachedToWindow && activity.window.decorView.isShown &&
                    activity.hasWindowFocus() && actual != null && expected.matches(actual)
                observed.set("resumed=" + resumed.size + " activity=" + System.identityHashCode(activity) + " config=" + actual)
                if (ready && activity === previous && actual == previousConfig && frameTime > previousFrame) {
                    stable.set(activity)
                    completed.countDown()
                } else {
                    previous = if (ready) activity else null
                    previousConfig = if (ready) actual else null
                    previousFrame = frameTime
                }
                if (completed.count > 0 && SystemClock.elapsedRealtime() < deadline) choreographer?.postFrameCallback(callback)
            } catch (callbackFailure: Throwable) {
                failure.compareAndSet(null, callbackFailure)
                completed.countDown()
            }
        }
        try {
            registry.runOnMainSync {
                choreographer = Choreographer.getInstance()
                choreographer!!.postFrameCallback(callback)
            }
            val remaining = deadline - SystemClock.elapsedRealtime()
            val signalled = remaining > 0 && completed.await(remaining, TimeUnit.MILLISECONDS)
            failure.get()?.let { throw it }
            assertTrue("$phase stable RESUMED configuration must fit one total 10000 ms budget; expected=$expected; " + observed.get(), signalled)
            val activity = requireNotNull(stable.get())
            registry.runOnMainSync {
                try {
                    val current = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED)
                        .filterIsInstance<MainActivity>().singleOrNull()
                    assertSame("$phase must retain the activity observed across two actual frames", activity, current)
                    assertFalse("$phase activity must remain alive", activity.isDestroyed || activity.isFinishing)
                    assertTrue("$phase attached focused window", activity.window.decorView.isAttachedToWindow &&
                        activity.window.decorView.isShown && activity.hasWindowFocus())
                    assertTrue("$phase exact configuration", expected.matches(categoryConfiguration(activity.resources.configuration)))
                } catch (finalCheckFailure: Throwable) { failure.compareAndSet(null, finalCheckFailure) }
            }
            failure.get()?.let { throw it }
            Log.i("LedgerCategoryConfig", "$phase config-ready expected=$expected; " + observed.get())
            return activity
        } finally {
            observing.set(false)
            registry.runOnMainSync { choreographer?.removeFrameCallback(callback) }
        }
    }

    private fun recreateConfigured(expected: CategoryConfiguration, phase: String) {
        val deadline = SystemClock.elapsedRealtime() + 10000L
        Log.i("LedgerCategoryConfig", "$phase start expected=$expected")
        val before = awaitStableConfiguration(expected, "$phase before-recreate", deadline)
        Log.i("LedgerCategoryConfig", "$phase recreate activity=" + System.identityHashCode(before))
        compose.activityRule.scenario.recreate()
        val after = awaitStableConfiguration(expected, "$phase after-recreate", deadline)
        assertNotSame("$phase explicit recreation must create a new activity", before, after)
        Log.i("LedgerCategoryConfig", "$phase complete activity=" + System.identityHashCode(after))
    }

    private fun largeCategoryScenario(scale: Float, dark: Boolean) {
        emulatorOnly()
        val oldScale = shell("settings get system font_scale")
        val oldSize = screenOverride("size")
        val oldDensity = screenOverride("density")
        val oldNight = shell("cmd uimode night")
        val oldConfiguration = compose.runOnIdle { categoryConfiguration(Configuration(compose.activity.resources.configuration)) }
        val wasNight = oldConfiguration.nightMode == Configuration.UI_MODE_NIGHT_YES
        val name = "一二三四五六七八九十一二三四五六七八九十"
        val image = "v016-category-font-${scale.toString().replace('.', '-')}-${if (dark) "dark" else "light"}"
        var scenarioFailure: Throwable? = null
        try {
            Log.i("LedgerCategoryConfig", "$image setup-settings")
            shell("wm size 720x1280")
            shell("wm density 320")
            shell("settings put system font_scale $scale")
            shell("cmd uimode night ${if (dark) "yes" else "no"}")
            recreateConfigured(CategoryConfiguration(scale, 360, 320,
                if (dark) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO), "$image setup")
            assertEquals("720x1280", screenOverride("size"))
            assertEquals("320", screenOverride("density"))
            openStats()
            replaceFixture(listOf(
                row(630, EntryKind.EXPENSE, 99999999999, name, "2025-05-01"),
                row(631, EntryKind.EXPENSE, 33333333333, "小项", "2025-05-02")
            ))
            assertShare(name, EntryKind.EXPENSE, .75f, "999999999.99", "75.0%", 1)
            captureLedgerScreen("$image-month")
            assertShare("小项", EntryKind.EXPENSE, .25f, "333333333.33", "25.0%", 1)
            modeYear()
            assertShare(name, EntryKind.EXPENSE, .75f, "999999999.99", "75.0%", 1)
            captureLedgerScreen("$image-year")
            drill(name)
            allDrillIds(listOf(630))
            drillAmountSingleLine(statsId(630), "-¥999999999.99")
            captureLedgerScreen("$image-drill")
        } catch (failure: Throwable) {
            scenarioFailure = failure
            runCatching { captureLedgerScreen("$image-failure-before-reset") }
                .exceptionOrNull()?.let(failure::addSuppressed)
            throw failure
        } finally {
            try {
                Log.i("LedgerCategoryConfig", "$image restore-settings expected=$oldConfiguration")
                shell(if (oldSize == null) "wm size reset" else "wm size $oldSize")
                shell(if (oldDensity == null) "wm density reset" else "wm density $oldDensity")
                shell(if (oldScale == "null" || oldScale.isEmpty()) "settings delete system font_scale" else "settings put system font_scale $oldScale")
                val oldMode = oldNight.substringAfter(": ").lowercase()
                shell("cmd uimode night ${if (oldMode in listOf("auto", "yes", "no")) oldMode else if (wasNight) "yes" else "no"}")
                recreateConfigured(oldConfiguration, "$image restore")
                assertEquals(oldSize, screenOverride("size"))
                assertEquals(oldDensity, screenOverride("density"))
                assertEquals(oldScale, shell("settings get system font_scale"))
                assertEquals(oldNight, shell("cmd uimode night"))
            } catch (restoreFailure: Throwable) {
                val original = scenarioFailure
                if (original == null) throw restoreFailure else original.addSuppressed(restoreFailure)
            }
        }
    }

    @Test fun categoryBarsAt360DpFontOneRemainReadable() = largeCategoryScenario(1f, false)
    @Test fun categoryBarsAt360DpFontOnePointThreeRemainReadable() = largeCategoryScenario(1.3f, false)
    @Test fun categoryBarsAt360DpFontTwoDarkRemainReadable() = largeCategoryScenario(2f, true)
}

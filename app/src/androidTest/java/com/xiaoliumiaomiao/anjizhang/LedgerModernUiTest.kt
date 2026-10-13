package com.xiaoliumiaomiao.anjizhang

import android.content.res.Configuration
import android.os.Build
import android.os.SystemClock
import android.util.Log
import android.view.Choreographer
import android.view.View
import android.view.ViewGroup
import android.view.inspector.WindowInspector
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowInsetsControllerCompat
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import com.xiaoliumiaomiao.anjizhang.domain.EntryKind
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert.*
import org.junit.Test

/** Exact expectations are independent literals; all fixtures are guarded synthetic ci-ledger data. */
class LedgerModernUiTest : StatisticsDeviceScenario() {
    private fun richMonth() {
        openStats()
        replaceFixture(modernUiFixture)
        overview("21950.00", "5402.00", "16548.00")
    }

    private fun dailyChart() = tag("stats-daily-chart").assertContentDescriptionEquals(
        "收入和支出每日趋势图，从1日到31日；各日准确金额可展开图表数据")

    private fun annualChart() = tag("stats-annual-chart").assertContentDescriptionEquals(
        "收入和支出年度双柱图，从1月到12月；各月准确金额见下方文字")

    private fun expandData() {
        val toggle = tag("stats-chart-data-toggle").assertIsEnabled()
        toggle.assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "已收起"))
        toggle.performClick()
        toggle.assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "已展开"))
    }

    private fun pointText(date: String, value: String) {
        val row = "stats-chart-point-$date"
        tag(row).assertIsDisplayed()
        compose.onNode(hasText(value) and hasAnyAncestor(hasTestTag(row)), useUnmergedTree = true)
            .assertIsDisplayed().assertTextEquals(value)
    }

    /** Inspect the actual Canvas rather than accepting accessible text as evidence of a chart. */
    private fun trendInk(tag: String, hasIncome: Boolean, hasExpense: Boolean) {
        val chart = this.tag(tag).assertIsDisplayed()
        val pixels = chart.captureToImage().toPixelMap()
        assertTrue("A chart must render a nonzero area", pixels.width >= 100 && pixels.height >= 50)
        val night = compose.activity.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        val incomeColor = if (night) 0xFF9BD4C4.toInt() else 0xFF206D63.toInt()
        val expenseColor = if (night) 0xFFF0B29E.toInt() else 0xFFA54A36.toInt()
        var incomePixels = 0
        var expensePixels = 0
        for (y in 0 until pixels.height) for (x in 0 until pixels.width) {
            when (pixels[x, y].toArgb()) {
                incomeColor -> incomePixels++
                expenseColor -> expensePixels++
            }
        }
        if (hasIncome) assertTrue("Recorded income must have visible ink", incomePixels > 0)
        else assertEquals("No invented income line, points, or bars", 0, incomePixels)
        if (hasExpense) assertTrue("Recorded expense must have visible ink", expensePixels > 0)
        else assertEquals("No invented expense line, points, or bars", 0, expensePixels)
    }

    private fun donut(total: String) = tag("stats-category-donut-chart").assert(
        SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, total))

    private fun legend(kind: EntryKind, name: String, percentage: String): SemanticsNodeInteraction =
        tag("stats-donut-category-${kind.name}-$name").assert(
            SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, percentage))

    /** Extra review images must include the complete real Canvas, not a clipped legend or data row. */
    private fun completeChart(tag: String) {
        val chart = this.tag(tag).assertIsDisplayed().fetchSemanticsNode()
        val viewport = compose.onNodeWithTag("stats-list").fetchSemanticsNode().boundsInRoot
        val bounds = chart.boundsInRoot
        assertTrue("Whole $tag Canvas must be visible", bounds.width >= chart.size.width - 1f &&
            bounds.height >= chart.size.height - 1f && bounds.left >= viewport.left - 1f &&
            bounds.right <= viewport.right + 1f && bounds.top >= viewport.top - 1f &&
            bounds.bottom <= viewport.bottom + 1f)
    }

    /** Read the actual focused Compose Dialog window; checking only the Activity misses this regression. */
    private fun assertDarkEditorSystemBars() {
        assertTrue("Public native window inspection requires API 29 or newer", Build.VERSION.SDK_INT >= 29)
        compose.onNodeWithTag("editor-shell").assertIsDisplayed()
        compose.runOnIdle {
            assertEquals("Native editor fixture must be in night mode", Configuration.UI_MODE_NIGHT_YES,
                compose.activity.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK)
            val roots = WindowInspector.getGlobalWindowViews().filter {
                it.isAttachedToWindow && it.isShown && it.hasWindowFocus()
            }
            assertEquals("Editor must have one actual focused native window", 1, roots.size)
            val providers = mutableListOf<DialogWindowProvider>()
            fun visit(view: View) {
                if (view is DialogWindowProvider) providers += view
                if (view is ViewGroup) for (index in 0 until view.childCount) visit(view.getChildAt(index))
            }
            visit(roots.single())
            assertEquals("Focused editor must expose its real Compose Dialog window", 1, providers.size)
            val window = providers.single().window
            assertNotSame("Editor Dialog window must be independent of Activity", compose.activity.window, window)
            assertSame("Inspect the focused editor window, not a synthetic dialog", roots.single(), window.decorView)
            val icons = WindowInsetsControllerCompat(window, window.decorView)
            assertFalse("Dark editor status icons must contrast with its actual native window", icons.isAppearanceLightStatusBars)
            assertFalse("Dark editor navigation icons must contrast with its actual native window", icons.isAppearanceLightNavigationBars)
        }
    }

    @Test fun dailyAndAnnualDataRowsKeepExactAmountsAndMonthNavigation() {
        richMonth()
        dailyChart()
        trendInk("stats-daily-chart", hasIncome = true, hasExpense = true)
        expandData()
        pointText("2025-05-01", "收入 ¥0.00")
        pointText("2025-05-01", "支出 ¥3600.00")
        pointText("2025-05-01", "结余 ¥-3600.00")
        pointText("2025-05-03", "收入 ¥18500.00")
        pointText("2025-05-03", "支出 ¥0.00")
        pointText("2025-05-05", "支出 ¥159.30")
        pointText("2025-05-05", "结余 ¥-159.30")
        captureLedgerScreen("v017-modern-month-exact-data")

        modeYear()
        year(2025)
        overview("117750.00", "27102.00", "90648.00", annual = true)
        annualChart()
        trendInk("stats-annual-chart", hasIncome = true, hasExpense = true)
        expandData()
        pointText("2025-01-01", "收入 ¥16000.00")
        pointText("2025-01-01", "支出 ¥3000.00")
        pointText("2025-01-01", "结余 ¥13000.00")
        pointText("2025-05-01", "收入 ¥21950.00")
        pointText("2025-05-01", "支出 ¥5402.00")
        pointText("2025-05-01", "结余 ¥16548.00")
        captureLedgerScreen("v017-modern-year-exact-data")
        tag("stats-chart-point-2025-05-01").assertHasClickAction().performClick()
        tag("month-select").assertTextContains("2025年5月")
        overview("21950.00", "5402.00", "16548.00")
        dailyChart()
    }

    @Test fun donutUsesIndependentTypeTotalsAndDrillsExactOriginalCategories() {
        richMonth()
        donut("支出总额 ¥5402.00")
        val meal = legend(EntryKind.EXPENSE, "餐饮", "8.8%")
        touchTarget(meal)
        captureLedgerScreen("v017-modern-donut-expense")
        meal.assertHasClickAction().performClick()
        allDrillIds(listOf(721, 720)) // Trim aliases merge; 餐饮外卖 must not match.
        drillBack()
        kind(EntryKind.INCOME)
        donut("收入总额 ¥21950.00")
        val salary = legend(EntryKind.INCOME, "工资", "84.3%")
        touchTarget(salary)
        captureLedgerScreen("v017-modern-donut-income")
        salary.assertHasClickAction().performClick()
        allDrillIds(listOf(710))
    }

    @Test fun emptyAndSingleDayChartsKeepZerosAndNegativeBalanceAcrossPeriods() {
        openStats()
        replaceFixture(emptyList())
        overview("0.00", "0.00", "0.00")
        dailyChart()
        trendInk("stats-daily-chart", hasIncome = false, hasExpense = false)
        text("这个月还没有记录，暂无趋势")
        donut("支出总额 ¥0.00")
        text("暂无支出分类")
        expandData()
        pointText("2025-05-01", "收入 ¥0.00")
        pointText("2025-05-01", "支出 ¥0.00")
        pointText("2025-05-01", "结余 ¥0.00")
        captureLedgerScreen("v017-modern-empty-month")

        // Collapse explicitly before replacing data in the same selected period.
        tag("stats-chart-data-toggle").performClick()
        replaceFixture(listOf(uiRow(900, EntryKind.EXPENSE, 10001, "唯一分类", "2025-05-15")))
        overview("0.00", "100.01", "-100.01")
        dailyChart()
        trendInk("stats-daily-chart", hasIncome = false, hasExpense = true)
        completeChart("stats-daily-chart")
        captureLedgerScreen("v017-modern-single-negative-month-chart")
        donut("支出总额 ¥100.01")
        legend(EntryKind.EXPENSE, "唯一分类", "100.0%")
        expandData()
        pointText("2025-05-14", "支出 ¥0.00")
        pointText("2025-05-15", "收入 ¥0.00")
        pointText("2025-05-15", "支出 ¥100.01")
        pointText("2025-05-15", "结余 ¥-100.01")
        captureLedgerScreen("v017-modern-single-negative-month")
        modeYear()
        year(2025)
        overview("0.00", "100.01", "-100.01", annual = true)
        annualChart()
        trendInk("stats-annual-chart", hasIncome = false, hasExpense = true)
        expandData()
        pointText("2025-01-01", "支出 ¥0.00")
        pointText("2025-05-01", "支出 ¥100.01")
        pointText("2025-05-01", "结余 ¥-100.01")
        year(2026)
        overview("0.00", "0.00", "0.00", annual = true)
        trendInk("stats-annual-chart", hasIncome = false, hasExpense = false)
        text("这一年还没有记录，暂无趋势")
        donut("支出总额 ¥0.00")
        captureLedgerScreen("v017-modern-empty-year")
        completeChart("stats-annual-chart")
        captureLedgerScreen("v017-modern-empty-year-chart")
    }

    private fun screenOverride(kind: String) = shell("wm $kind").lineSequence()
        .firstOrNull { it.startsWith("Override ") }?.substringAfter(": ")

    private data class ModernConfiguration(val fontScale: Float, val widthDp: Int, val densityDpi: Int, val nightMode: Int) {
        fun matches(actual: ModernConfiguration) = kotlin.math.abs(actual.fontScale - fontScale) < .01f &&
            actual.widthDp == widthDp && actual.densityDpi == densityDpi && actual.nightMode == nightMode
    }

    private fun modernConfiguration(config: Configuration) = ModernConfiguration(
        config.fontScale, config.screenWidthDp, config.densityDpi, config.uiMode and Configuration.UI_MODE_NIGHT_MASK)

    private fun awaitStableConfiguration(expected: ModernConfiguration, phase: String, deadline: Long): MainActivity {
        val registry = InstrumentationRegistry.getInstrumentation()
        val completed = CountDownLatch(1)
        val observing = AtomicBoolean(true)
        val stable = AtomicReference<MainActivity?>()
        val failure = AtomicReference<Throwable?>()
        val observed = AtomicReference("no frame observed")
        var previous: MainActivity? = null
        var previousConfig: ModernConfiguration? = null
        var previousFrame = -1L
        var choreographer: Choreographer? = null
        lateinit var callback: Choreographer.FrameCallback
        callback = Choreographer.FrameCallback { frameTime ->
            if (!observing.get()) return@FrameCallback
            try {
                val resumed = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED)
                    .filterIsInstance<MainActivity>()
                val activity = resumed.singleOrNull()
                val actual = activity?.let { modernConfiguration(it.resources.configuration) }
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
                    assertTrue("$phase exact configuration", expected.matches(modernConfiguration(activity.resources.configuration)))
                } catch (finalCheckFailure: Throwable) { failure.compareAndSet(null, finalCheckFailure) }
            }
            failure.get()?.let { throw it }
            Log.i("LedgerModernConfig", "$phase config-ready expected=$expected; " + observed.get())
            return activity
        } finally {
            observing.set(false)
            registry.runOnMainSync { choreographer?.removeFrameCallback(callback) }
        }
    }

    private fun recreateConfigured(expected: ModernConfiguration, phase: String) {
        val deadline = SystemClock.elapsedRealtime() + 10000L
        Log.i("LedgerModernConfig", "$phase start expected=$expected")
        val before = awaitStableConfiguration(expected, "$phase before-recreate", deadline)
        Log.i("LedgerModernConfig", "$phase recreate activity=" + System.identityHashCode(before))
        compose.activityRule.scenario.recreate()
        val after = awaitStableConfiguration(expected, "$phase after-recreate", deadline)
        assertNotSame("$phase explicit recreation must create a new activity", before, after)
        Log.i("LedgerModernConfig", "$phase complete activity=" + System.identityHashCode(after))
    }

    private fun keypadTarget(key: String) {
        val interaction = compose.onNodeWithTag("amount-key-$key").performScrollTo().assertIsDisplayed().assertIsEnabled()
        val node = interaction.fetchSemanticsNode()
        val panel = compose.onNodeWithTag("amount-keypad-panel").fetchSemanticsNode()
        val density = compose.activity.resources.displayMetrics.density
        val bounds = node.boundsInRoot
        val viewport = panel.boundsInRoot
        assertTrue("48dp keypad target", bounds.width >= 48f * density - 1 && bounds.height >= 48f * density - 1)
        assertTrue("Whole keypad target stays reachable in its panel", bounds.left >= viewport.left - 1 &&
            bounds.right <= viewport.right + 1 && bounds.top >= viewport.top - 1 && bounds.bottom <= viewport.bottom + 1)
    }

    private fun largeModernScenario(scale: Float, dark: Boolean) {
        emulatorOnly()
        val oldScale = shell("settings get system font_scale")
        val oldSize = screenOverride("size")
        val oldDensity = screenOverride("density")
        val oldNight = shell("cmd uimode night")
        val oldConfiguration = compose.runOnIdle { modernConfiguration(Configuration(compose.activity.resources.configuration)) }
        val wasNight = oldConfiguration.nightMode == Configuration.UI_MODE_NIGHT_YES
        val name = "一二三四五六七八九十一二三四五六七八九十"
        val image = "v017-modern-font-${scale.toString().replace('.', '-')}-${if (dark) "dark" else "light"}"
        val fixture = listOf(
            uiRow(910, EntryKind.EXPENSE, 99999999999, name, "2025-05-01", "备注".repeat(100)),
            uiRow(911, EntryKind.INCOME, 1, "工资", "2025-05-01")
        )
        var scenarioFailure: Throwable? = null
        try {
            shell("wm size 720x1280")
            shell("wm density 320")
            shell("settings put system font_scale $scale")
            shell("cmd uimode night ${if (dark) "yes" else "no"}")
            recreateConfigured(ModernConfiguration(scale, 360, 320,
                if (dark) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO), "$image setup")
            assertEquals("720x1280", screenOverride("size"))
            assertEquals("320", screenOverride("density"))
            openStats()
            replaceFixture(fixture)
            overview("0.01", "999999999.99", "-999999999.98")
            readable("支出 ¥999999999.99", "stats-overview")
            dailyChart()
            trendInk("stats-daily-chart", hasIncome = true, hasExpense = true)
            captureLedgerScreen("$image-month-chart")
            donut("支出总额 ¥999999999.99")
            val category = legend(EntryKind.EXPENSE, name, "100.0%")
            touchTarget(category)
            captureLedgerScreen("$image-donut")
            if (dark) {
                completeChart("stats-category-donut-chart")
                captureLedgerScreen("$image-donut-chart")
                category.performScrollTo().assertIsDisplayed()
            }
            category.performClick()
            allDrillIds(listOf(910))
            drillAmountSingleLine(statsId(910), "-¥999999999.99")
            captureLedgerScreen("$image-drill")
            drillBack()
            compose.onNodeWithTag("nav-ledger").performClick()
            tag("entry-${statsId(910)}").assertIsDisplayed()
            captureLedgerScreen("$image-ledger")
            detail(910)
            edit()
            compose.setAmountWithKeypad("999999999.99")
            compose.assertSystemImeVisible(false)
            keypadTarget("0")
            keypadTarget("dot")
            keypadTarget("backspace")
            captureLedgerScreen("$image-editor-amount")
            if (dark) {
                compose.onNodeWithText("编辑账目").performScrollTo().assertIsDisplayed()
                compose.onNodeWithContentDescription("关闭编辑器").assertIsDisplayed()
                compose.onNodeWithTag("amount-keypad-panel").assertIsDisplayed()
                compose.onNodeWithTag("amount-key-clear").performScrollTo().assertIsDisplayed()
                compose.onNodeWithTag("amount-keypad-close").assertIsDisplayed()
                captureLedgerScreen("$image-editor-amount-top")
                assertDarkEditorSystemBars()
            }
            compose.onNodeWithTag("amount-keypad-close").performScrollTo().performClick()
            compose.onNodeWithText("取消").performScrollTo().performClick()
            compose.waitUntil(10000) { compose.onAllNodesWithTag("editor-form").fetchSemanticsNodes().isEmpty() }
            compose.onNodeWithTag("detail-back").performClick()
            touchTarget(compose.onNodeWithTag("nav-settings"))
            compose.onNodeWithTag("nav-settings").performClick()
            compose.onNodeWithTag("settings-list").performScrollToIndex(0)
            captureLedgerScreen("$image-settings")
            assertEquals("Viewing charts and cancelling the editor preserves every field", fixture.sortedBy { it.id }, entries().sortedBy { it.id })
        } catch (failure: Throwable) {
            scenarioFailure = failure
            runCatching { captureLedgerScreen("$image-failure-before-reset") }.exceptionOrNull()?.let(failure::addSuppressed)
            throw failure
        } finally {
            try {
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

    @Test fun modernChartsAndEditorAt360DpFontOnePointThreeRemainReachable() = largeModernScenario(1.3f, false)
    @Test fun modernChartsAndEditorAt360DpFontTwoDarkRemainReachable() = largeModernScenario(2f, true)
}

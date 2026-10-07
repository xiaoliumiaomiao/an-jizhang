package com.xiaoliumiaomiao.anjizhang

import android.content.res.Configuration
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.*
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.graphics.ColorUtils
import androidx.test.platform.app.InstrumentationRegistry
import com.xiaoliumiaomiao.anjizhang.domain.*
import java.time.LocalDate
import java.io.File
import org.junit.Assert.*
import org.junit.Test

class LedgerStatisticsDisplayTest : StatisticsDeviceScenario() {
    private fun override(kind: String) = shell("wm $kind").lineSequence().firstOrNull { it.startsWith("Override ") }?.substringAfter(": ")
    private fun fontScenario(scale: Float) {
        emulatorOnly()
        val oldScale = shell("settings get system font_scale"); val oldSize = override("size"); val oldDensity = override("density")
        try {
            shell("wm size 720x1280"); shell("wm density 320"); shell("settings put system font_scale $scale")
            compose.activityRule.scenario.recreate()
            compose.waitUntil(10000) { kotlin.math.abs(compose.activity.resources.configuration.fontScale - scale) < .01f && compose.activity.resources.configuration.screenWidthDp == 360 }
            assertEquals("720x1280", override("size")); assertEquals("320", override("density"))
            openStats(); overview("100.01", "16.42", "83.59")
            listOf("stats-mode-month", "stats-mode-year", "stats-kind-expense", "stats-kind-income").forEach { touchTarget(tag(it)) }
            readable("本月结余 ¥83.59"); metrics(8, "2.05")
            sectionText("stats-comparison", "本期：2025-05-01 至 2025-05-31")
            sectionText("stats-comparison", "对比：2025-04-01 至 2025-04-30")
            captureLedgerScreen("s2-font-${scale.toString().replace('.', '-')}-month")
            listOf("餐饮", "交通", "餐饮外卖", "Dining", "dining", "末项").forEach { touchTarget(category(it)) }
            tag("stats-big-entry-${statsId(3)}").performClick(); compose.onNodeWithTag("entry-detail").assertIsDisplayed()
            touchTarget(compose.onNodeWithTag("detail-back")); compose.onNodeWithTag("detail-back").performClick()
            modeYear(); touchTarget(tag("stats-year-select")); overview("350.01", "45.42", "304.59", true)
            tag("stats-annual-chart").assertContentDescriptionEquals("收入和支出年度双柱图，从1月到12月；各月准确金额见下方文字")
            listOf(1, 4, 8, 12).forEach { month ->
                tag("stats-chart-month-$month").assertTextEquals(month.toString())
                readable(month.toString())
            }
            captureLedgerScreen("s2-font-${scale.toString().replace('.', '-')}-annual")
            (1..12).forEach { touchTarget(tag("stats-month-$it")); tag("stats-month-$it").assertTextContains("${it}月") }
            text("仅根据已记录账目，无记录不代表无消费")
            captureLedgerScreen("s2-font-${scale.toString().replace('.', '-')}-annual-bottom")
            val longName = "一二三四五六七八九十一二三四五六七八九十"
            replaceFixture(listOf(Entry(statsId(401), EntryKind.EXPENSE, 99999999999, longName, LocalDate.of(2025, 5, 1), "最大合法金额")))
            tag("stats-mode-month").performClick()
            readable("支出 ¥999999999.99")
            category(longName).assertTextContains("$longName  ¥999999999.99")
            readable("$longName  ¥999999999.99", categoryTag = "stats-category-EXPENSE-$longName")
            drill(longName); touchTarget(compose.onNodeWithTag("stats-drill-back")); allDrillIds(listOf(401))
            tag("entry-${statsId(401)}").assertTextContains("-¥999999999.99")
            captureLedgerScreen("s2-font-${scale.toString().replace('.', '-')}-drill")
            compose.onNodeWithTag("stats-drill-back").performClick()
            tag("stats-mode-year").performClick(); tag("stats-month-12").performClick()
            tag("month-select").assertTextContains("2025年12月")
        } catch (failure: Throwable) {
            // Preserve the failing 360dp/font configuration before finally restores it.
            runCatching { captureLedgerScreen("s2-font-${scale.toString().replace('.', '-')}-failure-before-reset") }
                .exceptionOrNull()?.let(failure::addSuppressed)
            throw failure
        } finally {
            shell(if (oldSize == null) "wm size reset" else "wm size $oldSize")
            shell(if (oldDensity == null) "wm density reset" else "wm density $oldDensity")
            shell(if (oldScale == "null" || oldScale.isEmpty()) "settings delete system font_scale" else "settings put system font_scale $oldScale")
            compose.activityRule.scenario.recreate()
            assertEquals(oldSize, override("size")); assertEquals(oldDensity, override("density")); assertEquals(oldScale, shell("settings get system font_scale"))
        }
    }
    @Test fun fontOneAt360DpCanReadAndOperateMonthlyAnnualAndDrill() = fontScenario(1f)
    @Test fun fontOnePointThreeAt360DpCanReadAndOperateMonthlyAnnualAndDrill() = fontScenario(1.3f)
    @Test fun fontTwoAt360DpCanReadAndOperateMonthlyAnnualAndDrill() = fontScenario(2f)

    @Test fun emptyTypesAndThemesShowHonestZeroStatesAndReadableNativeBars() {
        emulatorOnly(); openStats()
        val oldNight = shell("cmd uimode night")
        val wasNight = compose.activity.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        try {
            listOf(true, false).forEach { dark ->
                shell("cmd uimode night ${if (dark) "yes" else "no"}"); compose.activityRule.scenario.recreate()
                compose.waitUntil(10000) { (compose.activity.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES) == dark }
                modeYear(); year(2025); overview("350.01", "45.42", "304.59", true)
                val expected = if (dark) 0xFF101820.toInt() else 0xFFF5F7FB.toInt()
                val pixels = compose.onRoot().captureToImage().toPixelMap()
                var matches = 0
                for (y in 0 until pixels.height step 8) for (x in 0 until pixels.width step 8) {
                    if (android.graphics.Color.rgb((pixels[x,y].red * 255).toInt(), (pixels[x,y].green * 255).toInt(), (pixels[x,y].blue * 255).toInt()) == expected) matches++
                }
                assertTrue("Active theme background is rendered", matches > 100)
                compose.runOnIdle {
                    val window = compose.activity.window; val icons = WindowInsetsControllerCompat(window, window.decorView)
                    assertEquals(!dark, icons.isAppearanceLightStatusBars); assertEquals(!dark, icons.isAppearanceLightNavigationBars)
                }
                captureLedgerScreen(if (dark) "s2-dark-annual" else "s2-light-annual")
                val statusHeight = compose.runOnIdle {
                    WindowInsetsCompat.toWindowInsetsCompat(requireNotNull(compose.activity.window.decorView.rootWindowInsets))
                        .getInsets(WindowInsetsCompat.Type.statusBars()).top
                }
                val imageName = if (dark) "s2-dark-annual" else "s2-light-annual"
                val native = requireNotNull(BitmapFactory.decodeFile(File(
                    InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null), "v013-screens/$imageName.png").absolutePath))
                try {
                    assertTrue(statusHeight > 0 && statusHeight < native.height)
                    var background = 0; var clock = 0
                    for (y in 0 until statusHeight) for (x in 0 until native.width) {
                        val pixel = native.getPixel(x, y)
                        if (pixel == expected) background++
                        if (x < native.width * 2 / 5 && ColorUtils.calculateContrast(pixel, expected) >= 3.0) clock++
                    }
                    assertTrue("Native status background follows theme", background > native.width * statusHeight * 3 / 4)
                    assertTrue("Native clock remains readable", clock > 20)
                } finally { native.recycle() }
                tag("stats-annual-chart").assertContentDescriptionEquals("收入和支出年度双柱图，从1月到12月；各月准确金额见下方文字")
                captureLedgerScreen(if (dark) "s2-dark-chart" else "s2-light-chart")
                replaceFixture(emptyList()); overview("0.00", "0.00", "0.00", true)
                for (type in EntryKind.entries) {
                    kind(type); tag("stats-metrics").assertTextContains("记录 0 笔").assertTextContains("平均每笔 —")
                    text(if (type == EntryKind.INCOME) "这一年还没有收入" else "这一年还没有支出")
                    text("暂无较大记录")
                }
                (1..12).forEach { tag("stats-month-$it").assertTextContains("无记录").assertTextContains("收入 ¥0.00").assertTextContains("支出 ¥0.00") }
                captureLedgerScreen(if (dark) "s2-dark-empty" else "s2-light-empty")
                tag("stats-mode-month").performClick(); text("这个月还没有收入")
                kind(EntryKind.EXPENSE); text("这个月还没有支出")
                replaceFixture(listOf(statisticsFixture[0])); overview("100.00", "0.00", "100.00")
                text("这个月还没有支出"); kind(EntryKind.INCOME); metrics(1, "100.00")
                replaceFixture(listOf(statisticsFixture[2])); text("这个月还没有收入")
                kind(EntryKind.EXPENSE); metrics(1, "12.34")
                replaceFixture(statisticsFixture)
            }
        } finally {
            val oldMode = oldNight.substringAfter(": ").lowercase()
            shell("cmd uimode night ${if (oldMode in listOf("auto", "yes", "no")) oldMode else if (wasNight) "yes" else "no"}")
            compose.activityRule.scenario.recreate(); assertEquals(oldNight, shell("cmd uimode night"))
        }
    }
}

package com.xiaoliumiaomiao.anjizhang

import android.content.Context
import android.content.res.Configuration
import android.os.ParcelFileDescriptor
import android.view.KeyEvent
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.xiaoliumiaomiao.anjizhang.data.LedgerDatabase
import com.xiaoliumiaomiao.anjizhang.domain.*
import com.xiaoliumiaomiao.anjizhang.ui.LedgerViewModel
import java.time.YearMonth
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain

class LedgerS1Test {
    val compose = createAndroidComposeRule<MainActivity>()
    private val month = YearMonth.now()
    private val records = (1..12).map { index -> Entry("01500000-0000-0000-0000-${index.toString().padStart(12, '0')}",
        EntryKind.EXPENSE, index * 101L, "最近分类$index", month.atDay(index), "合成备注$index") }
    private val seed = object : ExternalResource() {
        override fun before() {
            val context = ApplicationProvider.getApplicationContext<Context>()
            context.deleteDatabase("ledger.db")
            LedgerDatabase(context).use { db -> records.forEach(db::save) }
        }
    }
    @get:Rule val rules: RuleChain = RuleChain.outerRule(seed).around(compose)
    private fun model() = compose.runOnIdle { ViewModelProvider(compose.activity)[LedgerViewModel::class.java] }
    private fun ready() { val vm = model(); compose.waitUntil(10000) { !vm.state.value.busy && vm.state.value.entries.size == records.size }; assertNull(vm.state.value.error) }
    private fun open() { ready(); compose.onNodeWithTag("add-entry").assertIsDisplayed().performClick(); compose.onNodeWithTag("editor-form").assertIsDisplayed() }
    private fun field(label: String, text: String) { compose.onNodeWithText(label).performScrollTo().performTextReplacement(text) }
    private fun save() { compose.onNodeWithText("保存").performScrollTo().assertIsEnabled().performClick() }
    private fun unchanged() { LedgerDatabase(ApplicationProvider.getApplicationContext<Context>()).use { assertEquals(records.toSet(), it.listEntries().toSet()) } }
    private fun back() = InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)

    @Test fun emptyCategoryIsRejectedWithoutCreatingRecord() {
        open(); compose.setAmountWithKeypad("0.01"); save()
        compose.onNodeWithText("请选择或填写分类").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("editor-form").assertExists(); unchanged()
    }
    @Test fun dirtyBackConfirmationAndBaselineSurviveRotation() {
        open(); compose.setAmountWithKeypad("12.34")
        field("分类（可自定义）", "自定义草稿"); compose.closeKeyboard()
        back(); compose.onNodeWithTag("discard-draft").assertIsDisplayed()
        compose.activityRule.scenario.recreate()
        compose.onNodeWithText("放弃未保存的修改？").assertIsDisplayed()
        compose.onNodeWithText("继续编辑").performClick()
        compose.assertAmountDraft("12.34")
        compose.onNodeWithText("分类（可自定义）").performScrollTo().assertTextContains("自定义草稿")
        compose.onNodeWithText("取消").performScrollTo().performClick()
        compose.onNodeWithText("放弃修改").performClick()
        compose.onNodeWithTag("editor-form").assertDoesNotExist(); unchanged()
        open(); compose.onNodeWithText("取消").performScrollTo().performClick()
        compose.onNodeWithTag("discard-draft").assertDoesNotExist()
        compose.onNodeWithTag("editor-form").assertDoesNotExist()
    }
    @Test fun noteToggleRetainsTextAndCustomCategoryAcrossKindAndRotation() {
        open(); compose.onNodeWithText("备注（可选，最多200字）").assertDoesNotExist()
        compose.onNodeWithTag("note-toggle").performScrollTo().assertTextContains("添加备注").performClick()
        compose.onNodeWithText("备注（可选，最多200字）").performScrollTo().performTouchInput { click() }
        compose.assertSystemImeVisible(true); field("备注（可选，最多200字）", "保留备注")
        compose.closeKeyboard(); compose.onNodeWithTag("note-toggle").performScrollTo().performClick()
        compose.onNodeWithText("备注（可选，最多200字）").assertDoesNotExist()
        field("分类（可自定义）", "自定义内容"); compose.closeKeyboard()
        compose.onNode(hasText("收入") and isSelectable()).performScrollTo().performClick()
        compose.onNodeWithText("分类（可自定义）").performScrollTo().assertTextContains("自定义内容")
        compose.activityRule.scenario.recreate()
        compose.onNodeWithText("备注（可选，最多200字）").assertDoesNotExist()
        compose.onNodeWithTag("note-toggle").performScrollTo().performClick()
        compose.onNodeWithText("备注（可选，最多200字）").performScrollTo().assertTextContains("保留备注")
        compose.onNodeWithText("分类（可自定义）").performScrollTo().assertTextContains("自定义内容"); unchanged()
    }
    @Test fun recentCategoriesUseSixNewestDistinctForCurrentKind() {
        open(); compose.onNodeWithTag("recent-categories").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("最近分类12").performScrollTo().performClick()
        compose.onNodeWithText("分类（可自定义）").performScrollTo().assertTextContains("最近分类12")
        compose.onNodeWithText("最近分类7").assertExists(); compose.onNodeWithText("最近分类6").assertDoesNotExist()
        compose.onNode(hasText("收入") and isSelectable()).performScrollTo().performClick()
        compose.onNodeWithText("最近分类12").assertDoesNotExist()
        compose.onNodeWithText("分类（可自定义）").performScrollTo().assertTextContains("最近分类12"); unchanged()
    }
    @Test fun monthPickerConfirmsBoundariesKeepsQueryAndCancelKeepsMonth() {
        ready(); compose.onNodeWithTag("ledger-list").performScrollToNode(hasText("搜索分类或备注"))
        compose.onNodeWithText("搜索分类或备注").performTextReplacement("最近")
        compose.closeKeyboard(); compose.onNodeWithText("仅支出").performClick()
        fun picker(year: String, selectedMonth: Int) {
            compose.onNodeWithTag("ledger-list").performScrollToNode(hasTestTag("month-select"))
            compose.onNodeWithTag("month-select").assertIsDisplayed().performClick()
            compose.onNodeWithTag("month-picker").assertIsDisplayed()
            compose.onNodeWithText("年份（1900–9999）").performTextReplacement(year); compose.closeKeyboard()
            compose.onNodeWithTag("month-$selectedMonth").performScrollTo().performClick()
        }
        picker("1900", 1); compose.onNodeWithText("确定").performClick()
        compose.onNodeWithTag("month-select").assertTextContains("1900年1月")
        compose.onNodeWithText("上月").assertIsNotEnabled()
        picker("9999", 12); compose.onNodeWithText("确定").performClick()
        compose.onNodeWithTag("month-select").assertTextContains("9999年12月")
        compose.onNodeWithText("下月").assertIsNotEnabled()
        picker("1899", 2); compose.onNodeWithText("确定").performClick()
        compose.onNodeWithText("年份请输入1900至9999").assertIsDisplayed()
        compose.onNodeWithText("取消").performClick()
        compose.onNodeWithTag("month-select").assertTextContains("9999年12月")
        compose.onNodeWithTag("ledger-list").performScrollToNode(hasText("搜索分类或备注"))
        compose.onNodeWithText("搜索分类或备注").assertTextContains("最近")
        compose.onNodeWithText("仅支出").assertIsSelected(); unchanged()
    }
    private fun shell(command: String): String = ParcelFileDescriptor.AutoCloseInputStream(
        InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(command)).bufferedReader().use { it.readText().trim() }
    private fun emulatorOnly() {
        assertEquals("true", InstrumentationRegistry.getArguments().getString("ledgerCi"))
        assertEquals("1", shell("getprop ro.kernel.qemu"))
        assertEquals("ci-ledger", shell("getprop ro.boot.qemu.avd_name").ifEmpty { shell("getprop ro.kernel.qemu.avd_name") })
    }
    @Test fun systemNightModeUsesDarkAndLightBackgrounds() {
        emulatorOnly(); ready()
        val wasNight = compose.activity.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        try {
            listOf(true, false).forEach { dark ->
                shell("cmd uimode night ${if (dark) "yes" else "no"}"); compose.activityRule.scenario.recreate()
                compose.waitUntil(10000) { (compose.activity.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES) == dark }
                compose.waitForIdle()
                val expected = if (dark) 0xFF101820.toInt() else 0xFFF5F7FB.toInt()
                val pixels = compose.onRoot().captureToImage().toPixelMap(); var matches = 0
                for (y in 0 until pixels.height step 8) for (x in 0 until pixels.width step 8) {
                    if (android.graphics.Color.rgb((pixels[x,y].red*255).toInt(), (pixels[x,y].green*255).toInt(), (pixels[x,y].blue*255).toInt()) == expected) matches++
                }
                assertTrue("Rendered ${if(dark) "dark" else "light"} palette, matches=$matches", matches > 100)
                captureLedgerScreen(if(dark) "s1-dark" else "s1-light")
            }
        } finally { shell("cmd uimode night ${if(wasNight) "yes" else "no"}"); compose.activityRule.scenario.recreate() }
    }
    private fun fontViewport(scale: Float) {
        emulatorOnly(); val old = shell("settings get system font_scale")
        try {
            shell("settings put system font_scale $scale"); compose.activityRule.scenario.recreate()
            compose.waitUntil(10000) { kotlin.math.abs(compose.activity.resources.configuration.fontScale - scale) < .01f }; ready()
            listOf("nav-stats", "nav-settings", "nav-ledger").forEach { compose.onNodeWithTag(it).assertIsDisplayed().performClick().assertIsSelected() }
            compose.onNodeWithTag("ledger-list").performScrollToIndex(0)
            val button = compose.onNodeWithTag("add-entry").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
            val viewport = compose.onNodeWithTag("ledger-list").fetchSemanticsNode().boundsInRoot
            assertTrue("List viewport must end above add button at font $scale: $viewport / $button", viewport.bottom <= button.top)
            compose.onNodeWithTag("ledger-list").performScrollToNode(hasTestTag("entry-${records.first().id}"))
            val row = compose.onNodeWithTag("entry-${records.first().id}").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
            assertTrue(row.bottom <= button.top); compose.onNodeWithTag("entry-${records.first().id}").performClick()
            compose.onNodeWithTag("add-entry").assertDoesNotExist()
            compose.onNodeWithText("编辑").performScrollTo().performClick()
            compose.onNodeWithTag("add-entry").assertDoesNotExist()
            compose.onNodeWithText("保存").performScrollTo().assertIsDisplayed()
            captureLedgerScreen("s1-font-${scale.toString().replace('.', '-')}")
            compose.onNodeWithText("取消").performScrollTo().performClick()
            compose.onNodeWithTag("discard-draft").assertDoesNotExist(); unchanged()
        } finally { shell(if(old == "null") "settings delete system font_scale" else "settings put system font_scale $old"); compose.activityRule.scenario.recreate() }
    }
    @Test fun fontOneHasUncoveredViewportAndReachableActions() = fontViewport(1f)
    @Test fun fontOnePointThreeHasUncoveredViewportAndReachableActions() = fontViewport(1.3f)
    @Test fun fontTwoHasUncoveredViewportAndReachableActions() = fontViewport(2f)
}

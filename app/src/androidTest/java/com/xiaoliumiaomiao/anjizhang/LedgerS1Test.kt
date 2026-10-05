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
import com.xiaoliumiaomiao.anjizhang.ui.LedgerUiState
import kotlinx.coroutines.flow.MutableStateFlow
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
    private fun assertPanelGeometry(): Float {
        val panel = compose.onNodeWithTag("amount-keypad-panel").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        val form = compose.onNodeWithTag("editor-form").fetchSemanticsNode().boundsInRoot
        val shell = compose.onNodeWithTag("editor-shell").fetchSemanticsNode().boundsInRoot
        // editor-shell excludes system-bar insets; allow 2dp only for native pixel rounding.
        val tolerance = compose.activity.resources.displayMetrics.density * 2f
        assertTrue("Bottom panel must end at editor usable bottom: $panel / $shell", kotlin.math.abs(panel.bottom - shell.bottom) <= tolerance)
        assertTrue("Form viewport must end above panel, without overlap: $form / $panel", form.bottom <= panel.top + tolerance)
        assertTrue("Panel must leave at least half the usable editor for the form: $panel / $shell", panel.height <= shell.height * .52f)
        compose.onNodeWithText("保存").performScrollTo().assertIsDisplayed()
        val afterScroll = compose.onNodeWithTag("amount-keypad-panel").fetchSemanticsNode().boundsInRoot
        assertTrue("Panel position must stay fixed while form scrolls", kotlin.math.abs(afterScroll.top - panel.top) <= tolerance && kotlin.math.abs(afterScroll.bottom - panel.bottom) <= tolerance)
        return form.height
    }

    @Test fun emptyCategoryIsRejectedWithoutCreatingRecord() {
        open(); compose.setAmountWithKeypad("0.01"); save()
        compose.onNodeWithText("请选择或填写分类").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("editor-form").assertExists(); unchanged()
    }
    @Test fun amountPanelPopsOnDemandBackClosesItBeforeDiscardAndNoteUsesIme() {
        open(); compose.onNodeWithTag("amount-keypad").assertDoesNotExist()
        compose.setAmountWithKeypad("12.34")
        compose.onNodeWithTag("amount-keypad-panel").assertIsDisplayed()
        val openFormHeight = assertPanelGeometry()
        compose.assertSystemImeVisible(false)
        back(); compose.onNodeWithTag("amount-keypad").assertDoesNotExist()
        compose.onNodeWithTag("discard-draft").assertDoesNotExist()
        assertTrue("Closing panel must enlarge actual form viewport", compose.onNodeWithTag("editor-form").fetchSemanticsNode().boundsInRoot.height > openFormHeight)
        compose.assertAmountDraft("12.34")
        compose.onNodeWithText("金额（元）").performScrollTo().performTouchInput { click() }
        compose.onNodeWithTag("amount-keypad-panel").assertIsDisplayed()
        compose.onNodeWithTag("note-toggle").performScrollTo().performClick()
        compose.onNodeWithText("备注（可选，最多200字）").performScrollTo().performTouchInput { click() }
        compose.assertSystemImeVisible(true); field("备注（可选，最多200字）", "面板交接备注")
        compose.onNodeWithTag("amount-keypad-panel").assertDoesNotExist()
        compose.onNodeWithText("金额（元）").performScrollTo().performTouchInput { click() }
        compose.assertSystemImeVisible(false)
        compose.onNodeWithTag("amount-keypad-panel").assertIsDisplayed()
        assertPanelGeometry(); compose.assertAmountDraft("12.34")
        compose.onNodeWithText("分类（可自定义）").performScrollTo().performTouchInput { click() }
        compose.assertSystemImeVisible(true); compose.onNodeWithTag("amount-keypad-panel").assertDoesNotExist()
        field("分类（可自定义）", "面板交接分类")
        compose.onNodeWithText("金额（元）").performScrollTo().performTouchInput { click() }
        compose.assertSystemImeVisible(false); compose.onNodeWithTag("amount-keypad-panel").assertIsDisplayed()
        compose.onNodeWithText("日期（yyyy-MM-dd）").performScrollTo().performTouchInput { click() }
        compose.assertSystemImeVisible(true); compose.onNodeWithTag("amount-keypad-panel").assertDoesNotExist()
        field("日期（yyyy-MM-dd）", "2020-02-29")
        compose.onNodeWithText("金额（元）").performScrollTo().performTouchInput { click() }
        compose.assertSystemImeVisible(false); compose.onNodeWithTag("amount-keypad-panel").assertIsDisplayed()
        compose.pressAmountKey("backspace"); compose.assertAmountDraft("12.3")
        compose.activityRule.scenario.recreate()
        compose.onNodeWithTag("amount-keypad-panel").assertIsDisplayed()
        compose.assertAmountDraft("12.3")
        compose.onNodeWithTag("amount-keypad-close").performScrollTo().performClick()
        compose.onNodeWithTag("amount-keypad").assertDoesNotExist()
        compose.onNodeWithText("备注（可选，最多200字）").performScrollTo().assertTextContains("面板交接备注")
        compose.onNodeWithText("分类（可自定义）").performScrollTo().assertTextContains("面板交接分类")
        compose.onNodeWithText("日期（yyyy-MM-dd）").performScrollTo().assertTextContains("2020-02-29")
        back(); compose.onNodeWithTag("discard-draft").assertIsDisplayed()
        compose.onNodeWithText("放弃修改").performClick(); unchanged()
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
        compose.closeKeyboard(); compose.onNodeWithTag("discard-draft").assertDoesNotExist()
        compose.onNodeWithTag("note-toggle").performScrollTo().performClick()
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
    @Test fun detailReadsLatestEntryAndDoesNotRecreateMissingId() {
        ready(); val original = records.last()
        compose.onNodeWithTag("ledger-list").performScrollToNode(hasTestTag("entry-${original.id}"))
        compose.onNodeWithTag("entry-${original.id}").performClick()
        val vm = model(); val updated = original.copy(category = "更新分类", note = "读取最新备注")
        val saved = vm.save(updated)
        compose.waitUntil(10000) { !vm.state.value.busy && vm.state.value.completedOperationId == saved }
        compose.onNodeWithText("分类：更新分类").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("备注：读取最新备注").performScrollTo().assertIsDisplayed()
        val deleted = vm.delete(updated)
        compose.waitUntil(10000) { !vm.state.value.busy && vm.state.value.completedOperationId == deleted }
        compose.onNodeWithText("这笔账目已不存在").assertIsDisplayed()
        compose.onNodeWithText("编辑").assertDoesNotExist(); compose.onNodeWithText("删除").assertDoesNotExist()
        compose.activityRule.scenario.recreate()
        compose.onNodeWithText("这笔账目已不存在").assertIsDisplayed()
        compose.onNodeWithTag("detail-back").performClick()
        assertEquals(records.filter { it.id != original.id }.toSet(), vm.state.value.entries.toSet())
        LedgerDatabase(ApplicationProvider.getApplicationContext<Context>()).use { assertEquals(vm.state.value.entries.toSet(), it.listEntries().toSet()) }
    }
    @Test fun restoredDetailWithControlledPendingReadDoesNotAppearMissing() {
        ready(); val original = records.last()
        compose.onNodeWithTag("ledger-list").performScrollToNode(hasTestTag("entry-${original.id}"))
        compose.onNodeWithTag("entry-${original.id}").performClick()
        val vm = model(); val loaded = vm.state.value
        // Drive the real UI with the VM's initial busy/empty state. This is a controlled
        // state fixture, not a database blocking test or an Android process-kill claim.
        @Suppress("UNCHECKED_CAST")
        val mutable = LedgerViewModel::class.java.getDeclaredField("mutable").apply { isAccessible = true }.get(vm) as MutableStateFlow<LedgerUiState>
        try {
            compose.runOnIdle { mutable.value = loaded.copy(entries = emptyList(), busy = true) }
            compose.activityRule.scenario.recreate()
            compose.onNodeWithText("正在读取账目…").assertIsDisplayed()
            compose.onNodeWithText("这笔账目已不存在").assertDoesNotExist()
            compose.onNodeWithText("编辑").assertDoesNotExist()
            compose.runOnIdle { mutable.value = loaded }
            compose.onNodeWithText("分类：${original.category}").performScrollTo().assertIsDisplayed()
            unchanged()
        } finally { compose.runOnIdle { mutable.value = loaded } }
    }
    @Test fun exportWarningShowsCapacityAndHidesGlobalEntryAction() {
        ready(); compose.onNodeWithTag("nav-settings").performClick()
        compose.onNodeWithTag("settings-list").performScrollToNode(hasText("导出备份"))
        compose.onNodeWithText("导出备份").performClick()
        compose.onNodeWithText("导出明文备份").assertIsDisplayed()
        compose.onNode(hasText("50000", substring = true) and hasText("10 MiB", substring = true) and hasAnyAncestor(isDialog())).assertIsDisplayed()
        compose.onNodeWithTag("add-entry").assertDoesNotExist()
        compose.onNodeWithText("取消").performClick()
        compose.onNodeWithTag("add-entry").assertIsDisplayed(); unchanged()
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
        fun override(kind: String) = shell("wm $kind").lineSequence().firstOrNull { it.startsWith("Override ") }?.substringAfter(": ")
        val oldSize = override("size"); val oldDensity = override("density")
        try {
            shell("wm size 720x1280"); shell("wm density 320")
            shell("settings put system font_scale $scale"); compose.activityRule.scenario.recreate()
            compose.waitUntil(10000) { kotlin.math.abs(compose.activity.resources.configuration.fontScale - scale) < .01f && compose.activity.resources.configuration.screenWidthDp == 360 }
            assertEquals("720x1280", override("size")); assertEquals("320", override("density"))
            assertEquals(360, compose.activity.resources.configuration.screenWidthDp); ready()
            listOf("nav-stats", "nav-settings", "nav-ledger").forEach { compose.onNodeWithTag(it).assertIsDisplayed().performClick().assertIsSelected() }
            compose.onNodeWithTag("ledger-list").performScrollToIndex(0)
            val button = compose.onNodeWithTag("add-entry").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
            val viewport = compose.onNodeWithTag("ledger-list").fetchSemanticsNode().boundsInRoot
            android.util.Log.i("LedgerS1", "font=$scale wm=${override("size")} density=${override("density")} configurationWidthDp=${compose.activity.resources.configuration.screenWidthDp} viewportPx=$viewport")
            assertTrue("List viewport must end above add button at font $scale: $viewport / $button", viewport.bottom <= button.top)
            compose.onNodeWithTag("ledger-list").performScrollToNode(hasTestTag("entry-${records.first().id}"))
            val row = compose.onNodeWithTag("entry-${records.first().id}").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
            assertTrue(row.bottom <= button.top); compose.onNodeWithTag("entry-${records.first().id}").performClick()
            compose.onNodeWithTag("add-entry").assertDoesNotExist()
            compose.onNodeWithText("编辑").performScrollTo().performClick()
            compose.onNodeWithTag("add-entry").assertDoesNotExist()
            compose.onNodeWithText("保存").performScrollTo().assertIsDisplayed()
            compose.setAmountWithKeypad("0.01")
            val openHeight = assertPanelGeometry()
            "0123456789".forEach { key ->
                compose.pressAmountKey("clear"); compose.assertAmountDraft("")
                compose.pressAmountKey(key.toString()); compose.assertAmountDraft(key.toString())
            }
            compose.pressAmountKey("backspace"); compose.assertAmountDraft("")
            compose.pressAmountKey("dot"); compose.assertAmountDraft("0.")
            compose.pressAmountKey("1"); compose.assertAmountDraft("0.1")
            compose.pressAmountKey("clear"); compose.assertAmountDraft("")
            captureLedgerScreen("s1-keypad-font-${scale.toString().replace('.', '-')}")
            compose.onNodeWithTag("amount-keypad-close").performScrollTo().assertIsDisplayed().performClick()
            compose.onNodeWithTag("amount-keypad").assertDoesNotExist()
            assertTrue(compose.onNodeWithTag("editor-form").fetchSemanticsNode().boundsInRoot.height > openHeight)
            compose.onNodeWithText("保存").performScrollTo().assertIsDisplayed()
            captureLedgerScreen("s1-font-${scale.toString().replace('.', '-')}")
            compose.onNodeWithText("取消").performScrollTo().performClick()
            compose.onNodeWithTag("discard-draft").assertIsDisplayed()
            compose.onNodeWithText("放弃修改").performClick(); unchanged()
        } finally {
            shell(if(oldSize == null) "wm size reset" else "wm size $oldSize")
            shell(if(oldDensity == null) "wm density reset" else "wm density $oldDensity")
            shell(if(old == "null" || old.isEmpty()) "settings delete system font_scale" else "settings put system font_scale $old")
            compose.activityRule.scenario.recreate()
            assertEquals(oldSize, override("size")); assertEquals(oldDensity, override("density"))
            assertEquals(old, shell("settings get system font_scale"))
        }
    }
    @Test fun fontOneHasUncoveredViewportAndReachableActions() = fontViewport(1f)
    @Test fun fontOnePointThreeHasUncoveredViewportAndReachableActions() = fontViewport(1.3f)
    @Test fun fontTwoHasUncoveredViewportAndReachableActions() = fontViewport(2f)
}

package com.xiaoliumiaomiao.anjizhang
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import com.xiaoliumiaomiao.anjizhang.ui.LedgerViewModel
import kotlinx.coroutines.sync.Mutex
import org.junit.Assert.assertTrue
import org.junit.Assert.assertSame
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain

class LedgerSmokeTest {
    val compose = createAndroidComposeRule<MainActivity>()
    private val cleanDatabase = object : ExternalResource() {
        override fun before() { ApplicationProvider.getApplicationContext<Context>().deleteDatabase("ledger.db") }
    }
    @get:Rule val rules: RuleChain = RuleChain.outerRule(cleanDatabase).around(compose)
    // Compose idling does not wait for the ViewModel's initial Dispatchers.IO load.
    // Wait for the user-visible action to be enabled rather than clicking a disabled button.
    private fun readyEntryAction(): SemanticsNodeInteraction {
        val ready = hasText("记一笔") and hasClickAction() and isEnabled()
        compose.waitUntil(10000) { compose.onAllNodes(ready).fetchSemanticsNodes().isNotEmpty() }
        return compose.onAllNodes(ready)[0].assertIsDisplayed().assertIsEnabled()
    }
    private fun openEditor() {
        readyEntryAction().performClick()
        compose.waitUntil(10000) { compose.onAllNodesWithText("金额（元）").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("金额（元）").assertExists()
    }
    private fun revealLedgerText(value: String): SemanticsNodeInteraction {
        compose.onNode(hasScrollAction()).performScrollToNode(hasText(value))
        return compose.onNodeWithText(value).assertIsDisplayed()
    }
    @Test fun emptyLedgerAndNavigation() {
        readyEntryAction()
        compose.onNodeWithText("安记账").assertIsDisplayed()
        revealLedgerText("这个月还没有账目")
        compose.onAllNodesWithText("记一笔")[0].assertIsDisplayed()
        compose.onNodeWithText("统计").performClick()
        compose.onNodeWithText("支出分类").assertIsDisplayed()
        compose.onNodeWithText("设置").performClick()
        compose.onNodeWithText("本地隐私").assertIsDisplayed()
        compose.onNodeWithText("整本备份最多50000条且不超过10 MiB，目前不支持分批导出。备注等内容也占用容量；接近上限时请先保存备份。")
            .performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("版本 0.1.2").performScrollTo().assertIsDisplayed()
    }
    @Test fun editorInputSurvivesActivityRecreation() {
        openEditor()
        compose.onNodeWithText("金额（元）").performTextInput("12.34")
        compose.activityRule.scenario.recreate()
        compose.onNodeWithText("金额（元）").assertTextContains("12.34")
        compose.onNodeWithText("取消").performScrollTo().performClick()
        compose.onNodeWithText("统计").performClick()
        compose.activityRule.scenario.recreate()
        compose.onNodeWithText("支出分类").assertIsDisplayed()
    }
    @Test fun invalidAmountCannotSaveThenOneCentExpenseAppears() {
        openEditor()
        compose.onNodeWithText("金额（元）").performTextInput("0")
        compose.onNodeWithText("保存").performScrollTo().performClick()
        // Validation inserts a new row into the scrollable form after the save-button scroll.
        // Reveal that row by a real scroll, preserving the actual-display assertion.
        compose.onNodeWithText("金额请输入0.01至999999999.99，最多两位小数").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("金额（元）").assertTextContains("0")
        assertTrue("非法金额不能生成账目", ledgerModel().state.value.entries.isEmpty())
        compose.onNodeWithText("金额（元）").performScrollTo().performTextReplacement("0.01")
        compose.onNodeWithText("保存").performScrollTo().performClick()
        val savedModel = ledgerModel()
        compose.waitUntil(10000) {
            savedModel.state.value.entries.any { it.amountCents == 1L && it.kind == com.xiaoliumiaomiao.anjizhang.domain.EntryKind.EXPENSE } &&
                compose.onAllNodesWithText("金额（元）").fetchSemanticsNodes().isEmpty()
        }
        revealLedgerText("-¥0.01")
        revealLedgerText("支出 ¥0.01")
    }
    // Hold only the existing worker serialization lock, so recreation precedes actual SQL work.
    // No production test hook or artificial data seed is used.
    private fun closeEditorKeyboard() {
        compose.closeKeyboard()
    }
    private fun ledgerModel(): LedgerViewModel {
        var model: LedgerViewModel? = null
        compose.runOnIdle { model = ViewModelProvider(compose.activity)[LedgerViewModel::class.java] }
        return model!!
    }
    private fun dumpPhase(phase: String, model: LedgerViewModel) {
        android.util.Log.e("LedgerSmoke", "$phase activity=${System.identityHashCode(compose.activity)} model=${System.identityHashCode(model)} state=${model.state.value} jobs=${model.viewModelScope.coroutineContext[Job]?.children?.count()}")
        val roots = compose.onAllNodes(isRoot(), useUnmergedTree = true)
        roots.fetchSemanticsNodes().indices.forEach { roots[it].printToLog("LedgerSmoke") }
    }
    private fun awaitSubmitted(phase: String, model: LedgerViewModel, previousJobs: Set<Job>) {
        // With the serialization lock held, a NEW active child cannot complete SQL work.
        // Its presence proves onSave/onDelete dispatched work; holding Mutex alone does not.
        try {
            compose.waitUntil(10000) {
                model.viewModelScope.coroutineContext[Job]?.children?.any { it.isActive && it !in previousJobs } == true
            }
            dumpPhase("$phase submitted", model)
        } catch (failure: Throwable) { dumpPhase("$phase NOT submitted", model); throw failure }
    }
    private fun awaitDialogClosed(label: String, phase: String, model: LedgerViewModel) {
        try { compose.waitUntil(10000) { compose.onAllNodesWithText(label).fetchSemanticsNodes().isEmpty() } }
        catch (failure: Throwable) { dumpPhase("$phase dialog timeout", model); throw AssertionError("$phase dialog did not close; state=${model.state.value}", failure) }
    }
    private fun pauseWorker(): Mutex {
        var model: LedgerViewModel? = null
        compose.runOnIdle { model = ViewModelProvider(compose.activity)[LedgerViewModel::class.java] }
        compose.waitUntil(10000) { !model!!.state.value.busy }
        val field = LedgerViewModel::class.java.getDeclaredField("lock").apply { isAccessible = true }
        return (field.get(model) as Mutex).also { assertTrue(it.tryLock()) }
    }
    @Test fun saveAndDeleteCompleteAfterActivityRecreationAndOldResultDoesNotCloseNewEditor() {
        openEditor()
        compose.onNodeWithText("金额（元）").performTextInput("12.34")
        closeEditorKeyboard()
        compose.onNodeWithText("金额（元）").assertTextContains("12.34")
        val saveModel = ledgerModel()
        val saveLock = pauseWorker()
        val priorSaveJobs = saveModel.viewModelScope.coroutineContext[Job]?.children?.toSet().orEmpty()
        try {
            compose.onNodeWithText("保存").performScrollTo().assertIsDisplayed().assertIsEnabled().performClick()
            awaitSubmitted("save", saveModel, priorSaveJobs)
            compose.activityRule.scenario.recreate()
            assertSame("recreation must retain saving ViewModel", saveModel, ledgerModel())
            dumpPhase("save after recreation", saveModel)
            compose.onNodeWithText("金额（元）").assertTextContains("12.34")
        } finally { saveLock.unlock() }
        awaitDialogClosed("金额（元）", "save", saveModel)
        revealLedgerText("-¥12.34")
        openEditor()
        compose.onNodeWithText("金额（元）").assertIsDisplayed()
        compose.onNodeWithText("取消").performScrollTo().performClick()
        revealLedgerText("删除").performClick()
        val deleteModel = ledgerModel()
        val deleteLock = pauseWorker()
        val priorDeleteJobs = deleteModel.viewModelScope.coroutineContext[Job]?.children?.toSet().orEmpty()
        try {
            compose.onNodeWithText("确认删除").performClick()
            awaitSubmitted("delete", deleteModel, priorDeleteJobs)
            compose.activityRule.scenario.recreate()
            assertSame("recreation must retain deleting ViewModel", deleteModel, ledgerModel())
            dumpPhase("delete after recreation", deleteModel)
            compose.onNodeWithText("删除账目？").assertIsDisplayed()
        } finally { deleteLock.unlock() }
        awaitDialogClosed("删除账目？", "delete", deleteModel)
        revealLedgerText("这个月还没有账目")
    }
    @Test fun databaseSaveFailureAfterRecreationKeepsEditorInputs() {
        openEditor()
        compose.onNodeWithText("金额（元）").performTextInput("12.34")
        val workerLock = pauseWorker()
        try {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val db = android.database.sqlite.SQLiteDatabase.openDatabase(context.getDatabasePath("ledger.db").path, null, android.database.sqlite.SQLiteDatabase.OPEN_READWRITE)
            try { db.execSQL("DROP TABLE entries") } finally { db.close() }
            compose.onNodeWithText("保存").performScrollTo().performClick()
            compose.activityRule.scenario.recreate()
        } finally { workerLock.unlock() }
        compose.waitUntil(10000) { compose.onAllNodes(hasText("操作失败：", substring = true)).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("金额（元）").assertTextContains("12.34")
        compose.onNodeWithText("取消").performScrollTo().performClick()
    }
    @Test fun reselectingExpenseKeepsCustomCategory() {
        openEditor()
        compose.onNodeWithText("分类（可自定义）").performTextReplacement("自定义分类")
        compose.onNodeWithText("支出").performScrollTo().performClick()
        compose.onNodeWithText("分类（可自定义）").assertTextContains("自定义分类")
    }
}

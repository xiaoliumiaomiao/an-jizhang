package com.xiaoliumiaomiao.anjizhang
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import com.xiaoliumiaomiao.anjizhang.ui.LedgerViewModel
import kotlinx.coroutines.sync.Mutex
import org.junit.Assert.assertTrue
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
    @Test fun emptyLedgerAndNavigation() {
        compose.onNodeWithText("安记账").assertIsDisplayed()
        compose.onNodeWithText("这个月还没有账目").assertIsDisplayed()
        compose.onAllNodesWithText("记一笔")[0].assertIsDisplayed()
        compose.onNodeWithText("统计").performClick()
        compose.onNodeWithText("支出分类").assertIsDisplayed()
        compose.onNodeWithText("设置").performClick()
        compose.onNodeWithText("本地隐私").assertIsDisplayed()
    }
    @Test fun editorInputSurvivesActivityRecreation() {
        compose.onAllNodesWithText("记一笔")[0].performClick()
        compose.onNodeWithText("金额（元）").performTextInput("12.34")
        compose.activityRule.scenario.recreate()
        compose.onNodeWithText("金额（元）").assertTextContains("12.34")
        compose.onNodeWithText("取消").performScrollTo().performClick()
        compose.onNodeWithText("统计").performClick()
        compose.activityRule.scenario.recreate()
        compose.onNodeWithText("支出分类").assertIsDisplayed()
    }
    @Test fun invalidAmountCannotSaveThenOneCentExpenseAppears() {
        compose.onAllNodesWithText("记一笔")[0].performClick()
        compose.onNodeWithText("金额（元）").performTextInput("0")
        compose.onNodeWithText("保存").performScrollTo().performClick()
        compose.onNodeWithText("金额请输入0.01至999999999.99，最多两位小数").assertIsDisplayed()
        compose.onNodeWithText("金额（元）").performTextReplacement("0.01")
        compose.onNodeWithText("保存").performScrollTo().performClick()
        compose.waitUntil(10000) { compose.onAllNodesWithText("-¥0.01").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("-¥0.01").assertIsDisplayed()
        compose.onNodeWithText("支出 ¥0.01").assertIsDisplayed()
    }
    // Hold only the existing worker serialization lock, so recreation precedes actual SQL work.
    // No production test hook or artificial data seed is used.
    private fun pauseWorker(): Mutex {
        var model: LedgerViewModel? = null
        compose.runOnIdle { model = ViewModelProvider(compose.activity)[LedgerViewModel::class.java] }
        compose.waitUntil(10000) { !model!!.state.value.busy }
        val field = LedgerViewModel::class.java.getDeclaredField("lock").apply { isAccessible = true }
        return (field.get(model) as Mutex).also { assertTrue(it.tryLock()) }
    }
    @Test fun saveAndDeleteCompleteAfterActivityRecreationAndOldResultDoesNotCloseNewEditor() {
        compose.onAllNodesWithText("记一笔")[0].performClick()
        compose.onNodeWithText("金额（元）").performTextInput("12.34")
        val saveLock = pauseWorker()
        try {
            compose.onNodeWithText("保存").performScrollTo().performClick()
            compose.activityRule.scenario.recreate()
            compose.onNodeWithText("金额（元）").assertTextContains("12.34")
        } finally { saveLock.unlock() }
        compose.waitUntil(10000) { compose.onAllNodesWithText("金额（元）").fetchSemanticsNodes().isEmpty() }
        compose.onNodeWithText("-¥12.34").assertIsDisplayed()
        compose.onAllNodesWithText("记一笔")[0].performClick()
        compose.onNodeWithText("金额（元）").assertIsDisplayed()
        compose.onNodeWithText("取消").performScrollTo().performClick()
        compose.onNodeWithText("删除").performScrollTo().performClick()
        val deleteLock = pauseWorker()
        try {
            compose.onNodeWithText("确认删除").performClick()
            compose.activityRule.scenario.recreate()
            compose.onNodeWithText("删除账目？").assertIsDisplayed()
        } finally { deleteLock.unlock() }
        compose.waitUntil(10000) { compose.onAllNodesWithText("删除账目？").fetchSemanticsNodes().isEmpty() }
        compose.onNodeWithText("这个月还没有账目").assertIsDisplayed()
    }
    @Test fun databaseSaveFailureAfterRecreationKeepsEditorInputs() {
        compose.onAllNodesWithText("记一笔")[0].performClick()
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
        compose.onAllNodesWithText("记一笔")[0].performClick()
        compose.onNodeWithText("分类（可自定义）").performTextReplacement("自定义分类")
        compose.onNodeWithText("支出").performScrollTo().performClick()
        compose.onNodeWithText("分类（可自定义）").assertTextContains("自定义分类")
    }
}

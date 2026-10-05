package com.xiaoliumiaomiao.anjizhang

import android.content.Context
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.text.AnnotatedString
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ApplicationProvider
import com.xiaoliumiaomiao.anjizhang.data.LedgerDatabase
import com.xiaoliumiaomiao.anjizhang.domain.*
import com.xiaoliumiaomiao.anjizhang.ui.LedgerViewModel
import java.time.YearMonth
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain

class LedgerQueryTest {
    private fun activeList():SemanticsNodeInteraction {
        val tag=listOf("ledger-list","stats-list","settings-list").single{compose.onAllNodesWithTag(it).fetchSemanticsNodes().isNotEmpty()}
        return compose.onNodeWithTag(tag)
    }
    private val month = YearMonth.now()
    private val seeds = listOf(
        Entry("01200000-0000-0000-0000-000000000001", EntryKind.EXPENSE, 1234L, "午餐", month.atDay(2), "团队午餐"),
        Entry("01200000-0000-0000-0000-000000000002", EntryKind.INCOME, 5000L, "奖金", month.atDay(3), "午餐报销"),
        Entry("01200000-0000-0000-0000-000000000003", EntryKind.EXPENSE, 100L, "交通", month.atDay(1), "地铁"),
        Entry("01200000-0000-0000-0000-000000000004", EntryKind.EXPENSE, 200L, "午餐", month.minusMonths(1).atEndOfMonth(), "上月午餐")
    )
    val compose = createAndroidComposeRule<MainActivity>()
    private val seedDatabase = object : ExternalResource() {
        override fun before() {
            val context = ApplicationProvider.getApplicationContext<Context>()
            context.deleteDatabase("ledger.db")
            val db = LedgerDatabase(context)
            try { seeds.forEach(db::save) } finally { db.close() }
        }
    }
    @get:Rule val rules: RuleChain = RuleChain.outerRule(seedDatabase).around(compose)

    private fun model(): LedgerViewModel {
        var result: LedgerViewModel? = null
        compose.runOnIdle { result = ViewModelProvider(compose.activity)[LedgerViewModel::class.java] }
        return result!!
    }
    private fun ready() {
        val vm = model()
        compose.waitUntil(10000) { !vm.state.value.busy && vm.state.value.entries.size == seeds.size }
        assertNull(vm.state.value.error)
    }
    private fun reveal(matcher: SemanticsMatcher): SemanticsNodeInteraction {
        activeList().performScrollToNode(matcher)
        return compose.onNode(matcher).assertIsDisplayed()
    }
    private fun text(value: String) = reveal(hasText(value))
    private fun query(value: String) {
        text("搜索分类或备注").performClick().performTextReplacement(value)
        closeIme()
    }
    private fun closeIme() {
        compose.closeKeyboard()
    }
    private fun count(visible: Int, total: Int) { text("显示 $visible / 本月 $total 条") }
    private fun summary(income: String, expense: String, balance: String) {
        text("收入 ¥$income")
        text("支出 ¥$expense")
        text("本月结余 ¥$balance")
    }
    private fun assertQuery(value: String, kind: String) {
        text("搜索分类或备注").assert(SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString(value)))
        text(kind).assertIsSelected()
    }
    private fun row(id: String) = reveal(hasTestTag("entry-$id"))
    private fun rowButton(id:String,label:String) {
        row(id).performClick()
        compose.onNodeWithTag("entry-detail").assertIsDisplayed()
        compose.onNodeWithText(label).performScrollTo().assertIsDisplayed().performClick()
    }
    private fun saveEditor() {
        compose.onNodeWithText("保存").performScrollTo().assertIsDisplayed().assertIsEnabled().performClick()
        compose.waitUntil(10000) { compose.onAllNodesWithText("金额（元）").fetchSemanticsNodes().isEmpty() }
        assertNull(model().state.value.error)
    }
    private fun add(category: String, amount: String) {
        compose.onAllNodes(hasText("记一笔") and hasClickAction() and isEnabled())[0].performClick()
        compose.setAmountWithKeypad(amount)
        compose.onNodeWithText("分类（可自定义）").performScrollTo().performTextReplacement(category)
        compose.onNodeWithText("备注（可选，最多200字）").performScrollTo().performTextInput("匹配词")
        closeIme()
        saveEditor()
    }

    @Test fun combinesKeywordAndTypeClearsBothAndKeepsFullMonthStatistics() {
        ready()
        query("午餐")
        count(2, 3)
        row(seeds[0].id)
        row(seeds[1].id)
        text("仅支出").performClick()
        count(1, 3)
        row(seeds[0].id)
        summary("50.00", "13.34", "36.66")
        compose.onNodeWithTag("nav-stats").performClick()
        text("午餐  ¥12.34")
        text("交通  ¥1.00")
        compose.onNodeWithTag("nav-ledger").performClick()
        text("清除筛选").performClick()
        assertQuery("", "全部类型")
        count(3, 3)
        row(seeds[2].id)
        summary("50.00", "13.34", "36.66")
    }

    @Test fun retainsQueryAndKindAcrossNavigationRecreationAndMonthSwitch() {
        ready()
        query(" 午餐 ")
        text("仅支出").performClick()
        compose.onNodeWithTag("nav-settings").performClick()
        compose.activityRule.scenario.recreate()
        text("备份与恢复")
        compose.onNodeWithTag("nav-ledger").performClick()
        assertQuery(" 午餐 ", "仅支出")
        count(1, 3)
        text("上月").performClick()
        assertQuery(" 午餐 ", "仅支出")
        count(1, 1)
        row(seeds[3].id)
        compose.activityRule.scenario.recreate()
        assertQuery(" 午餐 ", "仅支出")
        count(1, 1)
        text("本月").performClick()
        count(1, 3)
        row(seeds[0].id)
    }

    @Test fun addingEditingAndDeletingRefreshesMatchingListWithoutLosingQuery() {
        ready()
        query("匹配词")
        text("仅支出").performClick()
        count(0, 3)
        add("试验甲", "12.34")
        count(1, 4)
        add("试验乙", "2.00")
        count(2, 5)
        val first = model().state.value.entries.single { it.category == "试验甲" }
        val second = model().state.value.entries.single { it.category == "试验乙" }
        rowButton(first.id, "编辑")
        compose.onNodeWithText("备注（可选，最多200字）").performScrollTo().performClick().performTextReplacement("已改为其他备注")
        closeIme()
        saveEditor()
        count(1, 5)
        assertEquals("已改为其他备注", model().state.value.entries.single { it.id == first.id }.note)
        row(second.id)
        rowButton(second.id, "删除")
        compose.onNodeWithText("确认删除").assertIsDisplayed().performClick()
        compose.waitUntil(10000) { compose.onAllNodesWithText("删除账目？").fetchSemanticsNodes().isEmpty() }
        assertFalse(model().state.value.entries.any { it.id == second.id })
        count(0, 4)
        text("没有符合条件的账目")
        assertQuery("匹配词", "仅支出")
        summary("50.00", "25.68", "24.32")
        text("清除筛选").performClick()
        count(4, 4)
        row(first.id)
    }

    @Test fun distinguishesEmptyMonthFromNoMatchesAndClearKeepsMonth() {
        ready()
        query("不存在")
        text("仅收入").performClick()
        count(0, 3)
        text("没有符合条件的账目")
        text("下月").performClick()
        count(0, 0)
        text("这个月还没有账目")
        compose.onNodeWithText("没有符合条件的账目").assertDoesNotExist()
        text("清除筛选").performClick()
        assertQuery("", "全部类型")
        val next = month.plusMonths(1)
        text("${next.year}年${next.monthValue}月")
        count(0, 0)
        text("上月").performClick()
        count(3, 3)
        row(seeds[0].id)
    }
}

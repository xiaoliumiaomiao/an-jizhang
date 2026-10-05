package com.xiaoliumiaomiao.anjizhang

import android.content.Context
import android.os.SystemClock
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ApplicationProvider
import com.xiaoliumiaomiao.anjizhang.data.LedgerDatabase
import com.xiaoliumiaomiao.anjizhang.domain.Entry
import com.xiaoliumiaomiao.anjizhang.domain.EntryKind
import com.xiaoliumiaomiao.anjizhang.ui.LedgerViewModel
import java.time.YearMonth
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain

class LedgerInputFeedbackTest {
    private val month = YearMonth.now()
    private val first = Entry("01400000-0000-0000-0000-000000000001", EntryKind.EXPENSE,
        1234L, "删除目标", month.atDay(2), "只删除这笔合成账目")
    private val second = Entry("01400000-0000-0000-0000-000000000002", EntryKind.INCOME,
        5678L, "保留目标", month.atDay(3), "这笔全部字段必须保留")
    val compose = createAndroidComposeRule<MainActivity>()
    private val seed = object : ExternalResource() {
        override fun before() {
            val context = ApplicationProvider.getApplicationContext<Context>()
            context.deleteDatabase("ledger.db")
            LedgerDatabase(context).use { db -> db.save(first); db.save(second) }
        }
    }
    @get:Rule val rules: RuleChain = RuleChain.outerRule(seed).around(compose)

    private fun model(): LedgerViewModel = compose.runOnIdle {
        ViewModelProvider(compose.activity)[LedgerViewModel::class.java]
    }

    private fun databaseEntries(): List<Entry> =
        LedgerDatabase(ApplicationProvider.getApplicationContext<Context>()).use { it.listEntries() }

    private fun ready() {
        val vm = model()
        compose.waitUntil(10000) { !vm.state.value.busy && vm.state.value.entries.size == 2 }
        assertNull(vm.state.value.error)
        assertEquals(setOf(first, second), vm.state.value.entries.toSet())
    }

    private fun openEditor() {
        ready()
        compose.onNodeWithTag("add-entry").assertIsDisplayed().assertIsEnabled().performClick()
        compose.onNodeWithTag("editor-form").assertIsDisplayed()
    }

    private fun save() {
        compose.onNodeWithText("保存").performScrollTo().assertIsDisplayed().assertIsEnabled().performClick()
    }

    private fun invalidAmountMustNotSave() {
        save()
        compose.onNodeWithText("金额请输入0.01至999999999.99，最多两位小数")
            .performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("editor-form").assertExists()
        assertEquals(setOf(first, second), databaseEntries().toSet())
        assertEquals(setOf(first, second), model().state.value.entries.toSet())
    }

    @Test fun deletedFeedbackAppearsThenClearsWithoutChangingOtherEntry() {
        ready()
        compose.onNodeWithTag("ledger-list").performScrollToNode(hasTestTag("entry-${first.id}"))
        compose.onNodeWithTag("entry-${first.id}").assertIsDisplayed().performClick()
        compose.onNodeWithTag("entry-detail").assertIsDisplayed()
        compose.onNodeWithText("删除").performScrollTo().assertIsDisplayed().performClick()
        compose.onNodeWithText("删除账目？").assertIsDisplayed()
        compose.onNodeWithText("确认删除").assertIsEnabled().performClick()
        compose.waitUntil(10000) {
            compose.onAllNodesWithText("已删除").fetchSemanticsNodes().size == 1
        }
        compose.onNodeWithText("已删除").assertIsDisplayed()
        val observedAt = SystemClock.elapsedRealtime()
        val vm = model()
        val feedback = vm.state.value
        assertEquals("已删除", feedback.message)
        assertNotNull("成功反馈必须有稳定身份", feedback.messageId)
        assertNotNull("成功反馈必须有原始截止时间", feedback.messageExpiresAt)
        val messageId = feedback.messageId!!
        val expiresAt = feedback.messageExpiresAt!!
        compose.activityRule.scenario.recreate()
        assertSame("反馈重建必须保留同一 VM", vm, model())
        val restoredFeedback = vm.state.value
        if (restoredFeedback.message != null) {
            assertEquals("已删除", restoredFeedback.message)
            assertEquals("重建不能换成功反馈身份", messageId, restoredFeedback.messageId)
            assertEquals("重建不能延长原截止时间", expiresAt, restoredFeedback.messageExpiresAt)
        } else {
            assertTrue("反馈只能在原截止时间之后被清除", SystemClock.elapsedRealtime() >= expiresAt)
            assertNull(restoredFeedback.messageId)
            assertNull(restoredFeedback.messageExpiresAt)
        }
        compose.waitUntil(10000) {
            compose.onAllNodesWithText("已删除").fetchSemanticsNodes().isEmpty()
        }
        // The product expires at 3 seconds; allow native scheduling/observation overhead.
        assertTrue("Deletion feedback must expire, not remain indefinitely",
            SystemClock.elapsedRealtime() - observedAt <= 5000L)
        compose.onNodeWithText("已删除").assertDoesNotExist()
        compose.onNodeWithTag("entry-detail").assertDoesNotExist()
        compose.onNodeWithText("删除账目？").assertDoesNotExist()
        compose.onNodeWithTag("nav-ledger").assertIsSelected()
        compose.onNodeWithTag("ledger-list").performScrollToNode(hasTestTag("entry-${second.id}"))
        compose.onNodeWithTag("entry-${second.id}").assertIsDisplayed()
        assertEquals(listOf(second), model().state.value.entries)
        assertEquals(listOf(second), databaseEntries())
        captureLedgerScreen("deleted-feedback-cleared")
    }

    @Test fun amountKeypadHandlesDecimalBackspaceClearAndSavesOneCentWithoutIme() {
        openEditor()
        compose.setAmountWithKeypad("12.34")
        compose.assertSystemImeVisible(false)
        captureLedgerScreen("amount-keypad")
        compose.pressAmountKey("backspace"); compose.assertAmountDraft("12.3")
        compose.pressAmountKey("4"); compose.assertAmountDraft("12.34")
        compose.pressAmountKey("clear"); compose.assertAmountDraft("")
        compose.pressAmountKey("backspace"); compose.assertAmountDraft("")
        invalidAmountMustNotSave()
        val errorMustRemainUntil = SystemClock.elapsedRealtime() + 3100L
        compose.waitUntil(10000) { SystemClock.elapsedRealtime() >= errorMustRemainUntil }
        compose.onNodeWithText("金额请输入0.01至999999999.99，最多两位小数")
            .performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("editor-form").assertExists()
        assertEquals(setOf(first, second), databaseEntries().toSet())
        assertEquals(setOf(first, second), model().state.value.entries.toSet())
        compose.setAmountWithKeypad(".")
        invalidAmountMustNotSave()
        compose.setAmountWithKeypad("0")
        invalidAmountMustNotSave()
        compose.setAmountWithKeypad("0.01")
        compose.assertSystemImeVisible(false)
        save()
        val vm = model()
        compose.waitUntil(10000) {
            !vm.state.value.busy && vm.state.value.entries.size == 3 &&
                compose.onAllNodesWithTag("editor-form").fetchSemanticsNodes().isEmpty()
        }
        assertNull(vm.state.value.error)
        val saved = vm.state.value.entries.single { it.id != first.id && it.id != second.id }
        assertEquals(1L, saved.amountCents)
        assertEquals(EntryKind.EXPENSE, saved.kind)
        assertEquals(setOf(first, second, saved), databaseEntries().toSet())
        assertEquals(setOf(first, second), vm.state.value.entries.filter { it.id != saved.id }.toSet())
        compose.assertSystemImeVisible(false)
    }

    @Test fun noteUsesSystemImeAmountHidesItAndDraftSurvivesRecreation() {
        openEditor()
        compose.setAmountWithKeypad("12.34")
        compose.assertSystemImeVisible(false)
        val note = "备注仍用系统键盘\n重建后保留"
        compose.onNodeWithText("备注（可选，最多200字）").performScrollTo().assertIsDisplayed()
            .performTouchInput { click() }
        compose.assertSystemImeVisible(true)
        compose.onNodeWithText("备注（可选，最多200字）").performTextReplacement(note)
        compose.onNodeWithText("金额（元）").performScrollTo().assertIsDisplayed()
            .performTouchInput { click() }
        compose.assertSystemImeVisible(false)
        compose.assertAmountDraft("12.34")
        compose.pressAmountKey("backspace"); compose.assertAmountDraft("12.3")
        compose.pressAmountKey("4"); compose.assertAmountDraft("12.34")
        compose.onNodeWithText("备注（可选，最多200字）").assertIsNotFocused().assertTextContains(note)
        compose.onNode(hasText("收入") and isSelectable()).performScrollTo().performClick()
        compose.onNodeWithText("分类（可自定义）").performScrollTo().performTouchInput { click() }
        compose.onNodeWithText("分类（可自定义）").performTextReplacement("输入回归分类")
        compose.closeKeyboard()
        compose.onNodeWithText("日期（yyyy-MM-dd）").performScrollTo().performTouchInput { click() }
        compose.onNodeWithText("日期（yyyy-MM-dd）").performTextReplacement("2020-02-29")
        compose.closeKeyboard()
        val vm = model()
        compose.activityRule.scenario.recreate()
        assertSame(vm, model())
        compose.onNode(hasText("收入") and isSelectable()).performScrollTo().assertIsSelected()
        compose.assertAmountDraft("12.34")
        compose.onNodeWithText("日期（yyyy-MM-dd）").performScrollTo().assertTextContains("2020-02-29")
        compose.onNodeWithText("分类（可自定义）").performScrollTo().assertTextContains("输入回归分类")
        compose.onNodeWithText("备注（可选，最多200字）").performScrollTo().assertTextContains(note)
        compose.pressAmountKey("backspace"); compose.assertAmountDraft("12.3")
        compose.pressAmountKey("4"); compose.assertAmountDraft("12.34")
        compose.assertSystemImeVisible(false)
        assertEquals(setOf(first, second), databaseEntries().toSet())
        assertEquals(setOf(first, second), vm.state.value.entries.toSet())
    }
}

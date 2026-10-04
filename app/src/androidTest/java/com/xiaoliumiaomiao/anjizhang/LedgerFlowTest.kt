package com.xiaoliumiaomiao.anjizhang
import android.content.Context
import android.view.KeyEvent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.text.AnnotatedString
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
class LedgerFlowTest {
    private val month=YearMonth.now()
    private val first=Entry("01300000-0000-0000-0000-000000000011",EntryKind.EXPENSE,1234,"餐饮",month.atDay(2),"匹配词甲")
    private val second=Entry("01300000-0000-0000-0000-000000000012",EntryKind.EXPENSE,5678,"交通",month.atDay(3),"匹配词乙")
    val compose=createAndroidComposeRule<MainActivity>()
    private val seed=object:ExternalResource(){override fun before(){
        val context=ApplicationProvider.getApplicationContext<Context>();context.deleteDatabase("ledger.db")
        LedgerDatabase(context).use{it.save(first);it.save(second)}
    }}
    @get:Rule val rules:RuleChain=RuleChain.outerRule(seed).around(compose)
    private fun model():LedgerViewModel { var result:LedgerViewModel?=null;compose.runOnIdle{result=ViewModelProvider(compose.activity)[LedgerViewModel::class.java]};return result!! }
    private fun ready(){val vm=model();compose.waitUntil(10000){!vm.state.value.busy&&vm.state.value.entries.size==2};assertNull(vm.state.value.error)}
    private fun reveal(tag:String,text:String):SemanticsNodeInteraction {compose.onNodeWithTag(tag).performScrollToNode(hasText(text));return compose.onNodeWithText(text).assertIsDisplayed()}
    private fun open(entry:Entry){compose.onNodeWithTag("ledger-list").performScrollToNode(hasTestTag("entry-${entry.id}"));compose.onNodeWithTag("entry-${entry.id}").assertIsDisplayed().performClick();compose.onNodeWithTag("entry-detail").assertIsDisplayed()}
    private fun set(label:String,value:String){compose.onNodeWithText(label).performScrollTo().performTextReplacement(value)}
    @Test fun detailCancelPreservesQueryAndDoesNotLeakDraftIntoAnotherEntry(){
        ready()
        compose.onNodeWithTag("ledger-list").assertIsDisplayed();captureLedgerScreen("ledger")
        val ledger=compose.onNodeWithTag("ledger-list")
        val lastRow=hasTestTag("entry-${first.id}")
        ledger.performScrollToNode(lastRow)
        // Reach the real end padding rather than accepting a row partly under the floating action.
        ledger.performTouchInput { swipeUp() }
        val viewport=ledger.getBoundsInRoot()
        val fab=compose.onNodeWithTag("add-entry").assertIsDisplayed().getUnclippedBoundsInRoot()
        listOf("-"+Money.format(first.amountCents),first.category,first.note).forEach { text->
            val lastText=compose.onNode(hasText(text) and hasAnyAncestor(lastRow),useUnmergedTree=true)
            lastText.assertIsDisplayed()
            val bounds=lastText.getUnclippedBoundsInRoot()
            assertTrue("Last entry text $text must fit fully in the visible list: $bounds / $viewport",
                bounds.left>=viewport.left && bounds.top>=viewport.top &&
                    bounds.right<=viewport.right && bounds.bottom<=viewport.bottom)
            assertFalse("Last entry text $text overlaps the add-entry action: $bounds / $fab",
                bounds.left<fab.right && bounds.right>fab.left &&
                    bounds.top<fab.bottom && bounds.bottom>fab.top)
        }
        captureLedgerScreen("ledger-last-entry")
        reveal("ledger-list","搜索分类或备注").performTextReplacement("不存在的合成关键词")
        compose.closeKeyboard();reveal("ledger-list","没有符合条件的账目");captureLedgerScreen("no-matches")
        reveal("ledger-list","搜索分类或备注").performClick().performTextReplacement(" 匹配词 ");compose.closeKeyboard()
        reveal("ledger-list","仅支出").performClick();open(first)
        captureLedgerScreen("detail")
        compose.onNodeWithText("编辑").performScrollTo().performClick()
        set("分类（可自定义）","未保存分类");compose.closeKeyboard()
        compose.onNodeWithText("取消").performScrollTo().performClick()
        // Native Back targets the focused window; Compose idling alone does not wait for Dialog focus transfer.
        compose.waitUntil(10000) {
            val editorClosed=compose.onAllNodesWithTag("editor-form").fetchSemanticsNodes().isEmpty()
            val mainWindowFocused=compose.runOnIdle { compose.activity.hasWindowFocus() }
            editorClosed && mainWindowFocused
        }
        compose.onNodeWithText("分类：餐饮").assertIsDisplayed()
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        compose.waitUntil(10000) {
            compose.onAllNodesWithTag("ledger-list").fetchSemanticsNodes().size==1 &&
                compose.onAllNodesWithTag("entry-detail").fetchSemanticsNodes().isEmpty()
        }
        reveal("ledger-list","搜索分类或备注").assertTextContains(" 匹配词 ")
        reveal("ledger-list","仅支出").assertIsSelected()
        assertEquals(listOf(first,second).toSet(),model().state.value.entries.toSet())
        open(second);compose.onNodeWithText("编辑").performScrollTo().performClick()
        compose.onNodeWithText("分类（可自定义）").performScrollTo().assertTextContains("交通")
        compose.onNodeWithText("金额（元）").performScrollTo().assertTextContains("56.78")
        compose.onNodeWithText("备注（可选，最多200字）").performScrollTo().assertTextContains("匹配词乙")
    }
    @Test fun allEditorFieldsSurviveRecreationAndSaveKeepsStableId(){
        ready();open(first);compose.onNodeWithText("编辑").performScrollTo().performClick()
        compose.onNodeWithText("收入").performScrollTo().performClick()
        set("金额（元）","987654321.09");set("日期（yyyy-MM-dd）","2020-02-29")
        set("分类（可自定义）","自定义收入");set("备注（可选，最多200字）","完整备注\n第二行")
        compose.closeKeyboard()
        compose.onNodeWithText("金额（元）").performScrollTo().assertIsDisplayed();captureLedgerScreen("editor")
        val vm=model();compose.activityRule.scenario.recreate();assertSame(vm,model())
        compose.onNodeWithText("收入").performScrollTo().assertIsSelected()
        compose.onNodeWithText("金额（元）").performScrollTo().assertTextContains("987654321.09")
        compose.onNodeWithText("日期（yyyy-MM-dd）").performScrollTo().assertTextContains("2020-02-29")
        compose.onNodeWithText("分类（可自定义）").performScrollTo().assertTextContains("自定义收入")
        compose.onNodeWithText("备注（可选，最多200字）").performScrollTo().assertTextContains("完整备注\n第二行")
        compose.onNodeWithText("保存").performScrollTo().performClick()
        compose.waitUntil(10000){compose.onAllNodesWithTag("editor-form").fetchSemanticsNodes().isEmpty()}
        compose.onNodeWithTag("nav-ledger").assertIsSelected()
        assertEquals(first.copy(kind=EntryKind.INCOME,amountCents=98765432109,category="自定义收入",date=java.time.LocalDate.of(2020,2,29),note="完整备注\n第二行"),vm.state.value.entries.single{it.id==first.id})
        assertEquals(second,vm.state.value.entries.single{it.id==second.id})
    }
    @Test fun newEntryFromStatisticsReturnsThereAndNextSessionStartsFresh(){
        ready();compose.onNodeWithTag("nav-settings").performClick()
        val before=model().state.value.entries.toList()
        reveal("settings-list","导出备份").performClick()
        compose.onNodeWithText("导出明文备份").assertIsDisplayed()
        compose.onNodeWithText("取消").performClick()
        reveal("settings-list","恢复备份").performClick()
        compose.onNodeWithText("合并恢复账目").assertIsDisplayed()
        compose.onNodeWithText("取消").performClick()
        assertEquals(before,model().state.value.entries)
        compose.onNodeWithTag("nav-stats").performClick()
        compose.onNodeWithTag("stats-list").assertIsDisplayed();captureLedgerScreen("statistics")
        compose.onNodeWithTag("nav-settings").performClick()
        compose.onNodeWithTag("settings-list").assertIsDisplayed();captureLedgerScreen("settings")
        compose.onNodeWithTag("nav-stats").performClick()
        compose.onNodeWithTag("add-entry").performClick()
        set("金额（元）","77.77");set("分类（可自定义）","取消草稿");compose.closeKeyboard()
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        compose.onNodeWithTag("nav-stats").assertIsSelected();assertEquals(2,model().state.value.entries.size)
        compose.onNodeWithTag("add-entry").performClick()
        compose.onNodeWithText("金额（元）").assert(SemanticsMatcher.expectValue(SemanticsProperties.EditableText,AnnotatedString("")))
        compose.onNodeWithText("日期（yyyy-MM-dd）").performScrollTo().assertTextContains(java.time.LocalDate.now().toString())
        compose.onNodeWithText("分类（可自定义）").performScrollTo().assertTextContains("餐饮")
    }
}

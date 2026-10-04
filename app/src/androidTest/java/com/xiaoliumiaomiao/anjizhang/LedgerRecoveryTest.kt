package com.xiaoliumiaomiao.anjizhang

import android.app.Application
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewModelScope
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.xiaoliumiaomiao.anjizhang.data.LedgerDatabase
import com.xiaoliumiaomiao.anjizhang.domain.Entry
import com.xiaoliumiaomiao.anjizhang.domain.EntryKind
import com.xiaoliumiaomiao.anjizhang.ui.LedgerScreen
import com.xiaoliumiaomiao.anjizhang.ui.LedgerTheme
import com.xiaoliumiaomiao.anjizhang.ui.LedgerViewModel
import java.time.YearMonth
import kotlinx.coroutines.Job
import kotlinx.coroutines.sync.Mutex
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Saved Compose UI restored with a fresh VM; this fixture does not kill an Android process. */
class LedgerRecoveryTest {
    @get:Rule val compose = createComposeRule()
    private val application: Application
        get() = ApplicationProvider.getApplicationContext()
    private val month = YearMonth.now()
    private val original = Entry(
        "01320000-0000-0000-0000-000000000001", EntryKind.EXPENSE, 1234L,
        "餐饮", month.atDay(2), "恢复前的合成备注"
    )
    private val untouched = Entry(
        "01320000-0000-0000-0000-000000000002", EntryKind.INCOME, 5000L,
        "奖金", month.atDay(3), "不能误删的合成记录"
    )

    private inner class RecoveryFixture {
        private val tester = StateRestorationTester(compose)
        private val stores = mutableListOf<ViewModelStore>()
        private lateinit var firstStore: ViewModelStore
        lateinit var activeModel: LedgerViewModel
            private set
        private var heldLock: Mutex? = null

        private fun createModel(store: ViewModelStore): LedgerViewModel = ViewModelProvider(
            store, ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[LedgerViewModel::class.java]

        fun mount(entries: List<Entry>): LedgerViewModel {
            application.deleteDatabase("ledger.db")
            LedgerDatabase(application).use { database -> entries.forEach(database::save) }
            firstStore = ViewModelStore().also(stores::add)
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                activeModel = createModel(firstStore)
            }
            // activeModel is an ordinary variable. Changing it must not recompose before saving.
            tester.setContent { LedgerTheme { LedgerScreen(activeModel, {}, {}, {}) } }
            awaitReady(activeModel, entries)
            return activeModel
        }

        fun pauseWorker(model: LedgerViewModel) {
            compose.waitUntil(10000) { !model.state.value.busy }
            val field = LedgerViewModel::class.java.getDeclaredField("lock").apply { isAccessible = true }
            heldLock = (field.get(model) as Mutex).also {
                assertTrue("the existing SQL serialization lock must be held", it.tryLock())
            }
        }

        fun restoreWithFreshModel(entries: List<Entry>): LedgerViewModel {
            val oldModel = activeModel
            val oldJob = oldModel.viewModelScope.coroutineContext[Job]!!
            compose.runOnIdle {
                firstStore.clear() // Cancels the real submitted worker while it awaits the old lock.
                val freshStore = ViewModelStore().also(stores::add)
                activeModel = createModel(freshStore)
            }
            tester.emulateSavedInstanceStateRestore()
            val freshModel = activeModel
            assertNotSame("restoration must use a new ViewModel", oldModel, freshModel)
            assertTrue("the old ViewModel job must be cancelled", oldJob.isCancelled)
            awaitReady(freshModel, entries)
            assertNull(freshModel.state.value.completedOperationId)
            assertNull(freshModel.state.value.failedOperationId)
            return freshModel
        }

        fun close() {
            // onCleared's database-close coroutine also awaits the old lock; never wait while held.
            heldLock?.unlock()
            heldLock = null
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                stores.forEach(ViewModelStore::clear)
            }
        }
    }

    private fun awaitReady(model: LedgerViewModel, entries: List<Entry>) {
        compose.waitUntil(10000) {
            !model.state.value.busy && model.state.value.entries.toSet() == entries.toSet()
        }
        assertNull(model.state.value.error)
    }

    private fun openDetail(entry: Entry) {
        compose.onNodeWithTag("ledger-list").performScrollToNode(hasTestTag("entry-${entry.id}"))
        compose.onNodeWithTag("entry-${entry.id}").assertIsDisplayed().performClick()
        compose.onNodeWithTag("entry-detail").assertIsDisplayed()
    }

    private fun setField(label: String, value: String) {
        compose.onNodeWithText(label).performScrollTo().assertIsDisplayed().performTextReplacement(value)
    }

    private fun assertField(label: String, value: String) {
        compose.onNodeWithText(label).performScrollTo().assertIsDisplayed().assertTextContains(value)
    }

    private fun saveAction(): SemanticsNodeInteraction =
        compose.onNode(hasText("保存") or hasText("保存中…"))

    private fun awaitSubmitted(phase: String, model: LedgerViewModel, previousJobs: Set<Job>) {
        try {
            compose.waitUntil(10000) {
                model.viewModelScope.coroutineContext[Job]?.children?.any {
                    it.isActive && it !in previousJobs
                } == true
            }
            assertEquals("only the submitted $phase may be queued", 1,
                model.viewModelScope.coroutineContext[Job]?.children?.count {
                    it.isActive && it !in previousJobs
                })
        } catch (failure: Throwable) {
            android.util.Log.e("LedgerRecovery", "$phase NOT submitted; state=${model.state.value}", failure)
            compose.onAllNodes(isRoot(), useUnmergedTree = true).fetchSemanticsNodes().indices.forEach {
                compose.onAllNodes(isRoot(), useUnmergedTree = true)[it].printToLog("LedgerRecovery")
            }
            throw failure
        }
    }

    private fun assertDatabase(entries: List<Entry>) {
        LedgerDatabase(application).use { database ->
            assertEquals(entries.sortedBy { it.id }, database.listEntries().sortedBy { it.id })
        }
    }

    @Test fun queuedSaveRestoredWithNewViewModelKeepsDraftAndCanRetrySameId() {
        val fixture = RecoveryFixture()
        try {
            val firstModel = fixture.mount(listOf(original))
            openDetail(original)
            compose.onNodeWithText("编辑").performScrollTo().assertIsDisplayed().performClick()
            val draft = original.copy(amountCents = 5678L, category = "恢复后的分类", note = "恢复草稿\n第二行")
            setField("金额（元）", "56.78")
            setField("分类（可自定义）", draft.category)
            setField("备注（可选，最多200字）", draft.note)
            compose.closeKeyboard()
            fixture.pauseWorker(firstModel)
            val previousJobs = firstModel.viewModelScope.coroutineContext[Job]?.children?.toSet().orEmpty()
            saveAction().performScrollTo().assertIsDisplayed().assertIsEnabled().performClick()
            awaitSubmitted("save", firstModel, previousJobs)
            saveAction().performScrollTo().assertIsDisplayed().assertIsNotEnabled()
            compose.onNodeWithText("取消").performScrollTo().assertIsDisplayed().assertIsNotEnabled()
            assertDatabase(listOf(original))

            val freshModel = fixture.restoreWithFreshModel(listOf(original))
            compose.onNodeWithTag("editor-form").assertIsDisplayed()
            compose.onNodeWithText("支出").performScrollTo().assertIsDisplayed().assertIsSelected()
            assertField("金额（元）", "56.78")
            assertField("日期（yyyy-MM-dd）", draft.date.toString())
            assertField("分类（可自定义）", draft.category)
            assertField("备注（可选，最多200字）", draft.note)
            // A fresh VM cannot finish the old pending ID. The restored actions must unlock.
            saveAction().performScrollTo().assertIsDisplayed().assertIsEnabled()
            compose.onNodeWithText("取消").performScrollTo().assertIsDisplayed().assertIsEnabled()
            saveAction().performScrollTo().assertIsDisplayed().performClick()
            compose.waitUntil(10000) {
                !freshModel.state.value.busy && freshModel.state.value.entries == listOf(draft) &&
                    compose.onAllNodesWithTag("editor-form").fetchSemanticsNodes().isEmpty()
            }
            assertNull(freshModel.state.value.error)
            assertNotNull(freshModel.state.value.completedOperationId)
            assertEquals(original.id, freshModel.state.value.entries.single().id)
            assertDatabase(listOf(draft))
            compose.onNodeWithTag("nav-ledger").assertIsSelected()
        } finally { fixture.close() }
    }

    @Test fun queuedDeleteRestoredWithNewViewModelCanCancelThenDeleteOnlyTarget() {
        val fixture = RecoveryFixture()
        try {
            val entries = listOf(original, untouched)
            val firstModel = fixture.mount(entries)
            openDetail(original)
            compose.onNodeWithText("删除").performScrollTo().assertIsDisplayed().performClick()
            fixture.pauseWorker(firstModel)
            val previousJobs = firstModel.viewModelScope.coroutineContext[Job]?.children?.toSet().orEmpty()
            compose.onNodeWithText("确认删除").assertIsDisplayed().assertIsEnabled().performClick()
            awaitSubmitted("delete", firstModel, previousJobs)
            compose.onNodeWithText("确认删除").assertIsDisplayed().assertIsNotEnabled()
            compose.onNodeWithText("取消").assertIsDisplayed().assertIsNotEnabled()
            assertDatabase(entries)

            val freshModel = fixture.restoreWithFreshModel(entries)
            compose.onNodeWithText("删除账目？").assertIsDisplayed()
            compose.onNodeWithText("确认删除").assertIsDisplayed().assertIsEnabled()
            compose.onNodeWithText("取消").assertIsDisplayed().assertIsEnabled().performClick()
            compose.waitUntil(10000) {
                compose.onAllNodesWithText("删除账目？").fetchSemanticsNodes().isEmpty()
            }
            compose.onNodeWithTag("entry-detail").assertIsDisplayed()
            assertEquals(entries.toSet(), freshModel.state.value.entries.toSet())
            assertDatabase(entries)
            compose.onNodeWithText("删除").performScrollTo().assertIsDisplayed().assertIsEnabled().performClick()
            compose.onNodeWithText("确认删除").assertIsDisplayed().assertIsEnabled().performClick()
            compose.waitUntil(10000) {
                !freshModel.state.value.busy && freshModel.state.value.entries == listOf(untouched) &&
                    compose.onAllNodesWithText("删除账目？").fetchSemanticsNodes().isEmpty()
            }
            assertNull(freshModel.state.value.error)
            assertNotNull(freshModel.state.value.completedOperationId)
            assertDatabase(listOf(untouched))
            compose.onNodeWithTag("nav-ledger").assertIsSelected()
            compose.onNodeWithTag("ledger-list").performScrollToNode(hasTestTag("entry-${untouched.id}"))
            compose.onNodeWithTag("entry-${untouched.id}").assertIsDisplayed()
        } finally { fixture.close() }
    }
}

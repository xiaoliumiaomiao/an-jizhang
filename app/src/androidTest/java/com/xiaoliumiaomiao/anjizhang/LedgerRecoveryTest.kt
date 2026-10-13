package com.xiaoliumiaomiao.anjizhang

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewModelScope
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.xiaoliumiaomiao.anjizhang.data.LedgerDatabase
import com.xiaoliumiaomiao.anjizhang.data.BackupCodec
import com.xiaoliumiaomiao.anjizhang.domain.Entry
import com.xiaoliumiaomiao.anjizhang.domain.EntryKind
import com.xiaoliumiaomiao.anjizhang.ui.LedgerScreen
import com.xiaoliumiaomiao.anjizhang.ui.LedgerTheme
import com.xiaoliumiaomiao.anjizhang.ui.LedgerViewModel
import com.xiaoliumiaomiao.anjizhang.ui.LedgerDocumentResult
import com.xiaoliumiaomiao.anjizhang.ui.UiOperationSession
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
        var restoreOwner = ""
            private set
        private var documentResult by mutableStateOf<LedgerDocumentResult?>(null)
        fun deliverRestore(uri: Uri) = compose.runOnIdle { documentResult = LedgerDocumentResult(restoreOwner, uri.toString(), false) }

        private fun createModel(store: ViewModelStore): LedgerViewModel = ViewModelProvider(
            store, ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[LedgerViewModel::class.java]

        fun mount(entries: List<Entry>): LedgerViewModel {
            application.deleteDatabase("ledger.db")
            LedgerDatabase(application).use { database -> database.seedLegacyFixture(entries) }
            firstStore = ViewModelStore().also(stores::add)
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                activeModel = createModel(firstStore)
            }
            // activeModel is an ordinary variable. Changing it must not recompose before saving.
            tester.setContent { LedgerTheme { LedgerScreen(activeModel, {}, {}, {},
                onSelectImport = { restoreOwner = it }, documentResult = documentResult,
                onDocumentConsumed = { documentResult = null }) } }
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
        when(label) {
            "editor-category-current" -> compose.setCategoryThroughDirectory(value)
            "editor-date-select" -> compose.selectEditorDate(value)
            else -> {
                val field = compose.onNodeWithText(label).performScrollTo()
                compose.awaitNativeDialogFocus(field)
                field.performClick().performTextReplacement(value)
            }
        }
    }

    private fun assertField(label: String, value: String) {
        when(label) { "editor-date-select" -> compose.assertEditorDate(value); "editor-category-current" -> compose.assertSelectedCategory(value); else -> compose.onNodeWithText(label).performScrollTo().assertIsDisplayed().assertTextContains(value) }
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

    @Test fun queuedSaveFreshSnapshotResolvesUncommittedPayloadAndRetriesSameId() {
        val fixture = RecoveryFixture()
        try {
            val firstModel = fixture.mount(listOf(original))
            openDetail(original)
            compose.onNodeWithText("编辑").performScrollTo().assertIsDisplayed().performClick()
            val draft = original.copy(amountCents = 5678L, category = "恢复后的分类", note = "恢复草稿\n第二行")
            compose.setAmountWithKeypad("56.78")
            setField("editor-category-current", draft.category)
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
            compose.onNode(hasText("支出") and isSelectable()).performScrollTo().assertIsDisplayed().assertIsSelected()
            assertField("金额（元）", "56.78")
            assertField("editor-date-select", draft.date.toString())
            assertField("editor-category-current", draft.category)
            assertField("备注（可选，最多200字）", draft.note)
            // The new VM has actually read every entity; the unchanged complete baseline proves this queued write did not commit.
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

    @Test fun queuedDeleteFreshSnapshotResolvesUncommittedPayloadThenDeletesOnlyTarget() {
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

    @Test fun unknownRestoreNeedsItsOwnExplicitFreshReadBeforeReturningToBackupPage() {
        val fixture = RecoveryFixture()
        val backup = java.io.File(application.cacheDir, "synthetic-unknown-restore.json")
        try {
            val old = fixture.mount(listOf(original))
            backup.writeText(BackupCodec.encode(listOf(untouched)), Charsets.UTF_8)
            compose.onNodeWithTag("nav-settings").performClick()
            compose.onNodeWithTag("settings-open-backup").performScrollTo().performClick()
            compose.onNodeWithTag("backup-import").performClick()
            compose.onNodeWithTag("document-select").assertIsEnabled().performClick()
            assertTrue(fixture.restoreOwner.isNotEmpty())
            fixture.pauseWorker(old)
            val jobs = old.viewModelScope.coroutineContext[Job]!!.children.toSet()
            fixture.deliverRestore(Uri.fromFile(backup))
            awaitSubmitted("restore", old, jobs)
            val pending = compose.runOnIdle { UiOperationSession.forViewModel(old, old.instanceId).value }
            assertEquals("RESTORE", pending.operation)
            assertEquals(listOf(Uri.fromFile(backup).toString()), pending.payload)
            assertTrue(pending.attempt.isNotEmpty())
            val fresh = fixture.restoreWithFreshModel(listOf(original))
            compose.onNodeWithTag("restore-acknowledge-unknown").assertDoesNotExist()
            compose.onNodeWithTag("operation-retry-read").assertIsDisplayed().assertIsEnabled().performClick()
            compose.waitUntil(10000) { !fresh.state.value.busy && !fresh.state.value.readError &&
                compose.onAllNodesWithTag("restore-acknowledge-unknown").fetchSemanticsNodes().size == 1 }
            compose.onNodeWithText("上次恢复结果无法确认。账本已重新读取；重新选择相同备份时，相同记录会跳过，冲突会拒绝。").performScrollTo().assertIsDisplayed()
            compose.onNodeWithTag("restore-acknowledge-unknown").assertIsEnabled().performClick()
            compose.onNodeWithTag("settings-backup-list").assertIsDisplayed()
            compose.onNodeWithTag("restore-result-unconfirmed").assertIsDisplayed()
            assertDatabase(listOf(original))
            assertEquals(listOf(original), fresh.state.value.entries)
            assertTrue(fresh.state.value.committedResults.isEmpty())
            assertNull(fresh.state.value.completedOperationId)
            compose.onNodeWithTag("backup-import").assertIsEnabled().performClick()
            compose.onNodeWithText("合并恢复账目").assertIsDisplayed()
            compose.onNodeWithText("取消").performClick()
        } finally { fixture.close(); assertTrue(!backup.exists() || backup.delete()) }
    }
}

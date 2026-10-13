package com.xiaoliumiaomiao.anjizhang

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.lifecycle.viewModelScope
import com.xiaoliumiaomiao.anjizhang.data.LedgerDatabase
import com.xiaoliumiaomiao.anjizhang.domain.*
import com.xiaoliumiaomiao.anjizhang.ui.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.sync.Mutex
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class LedgerAccountsTest : V018DeviceScenario() {
    private fun field(tag: String, raw: String) {
        val node = compose.onNodeWithTag(tag)
        if (tag != "editor-category-search") node.performScrollTo()
        if (tag == "settings-category-search") compose.awaitNativeActivityFocus(compose.activity, node)
        else compose.awaitNativeDialogFocus(node)
        node.assertIsDisplayed().assertIsEnabled().performClick().performTextReplacement(raw)
        compose.closeKeyboard()
    }
    private fun create(name: String, initial: String? = null, liability: Boolean = false): Account {
        val vm = model()
        val previous = vm.state.value.accounts.map { it.id }.toSet()
        compose.onNodeWithTag("accounts-list").performScrollToNode(hasTestTag("account-add"))
        compose.onNodeWithTag("account-add").assertIsEnabled().performClick()
        field("account-name", name)
        if (liability) compose.onNodeWithTag("account-kind-liability").performScrollTo().performClick()
        initial?.let { field("account-initial-balance", it) }
        compose.onNodeWithTag("account-editor-save").assertIsEnabled().performClick()
        compose.waitUntil(10000) { !vm.state.value.busy && vm.state.value.accounts.any { it.id !in previous } &&
            compose.onAllNodesWithTag("account-editor").fetchSemanticsNodes().isEmpty() }
        assertNull(vm.state.value.error)
        return vm.state.value.accounts.single { it.id !in previous }
    }
    private fun openAccount(account: Account) {
        compose.onNodeWithTag("accounts-list").performScrollToNode(hasTestTag("account-row-${account.id}"))
        compose.onNodeWithTag("account-row-${account.id}").assertIsDisplayed().performClick()
        compose.onNodeWithTag("account-detail-list").assertIsDisplayed().performScrollToIndex(0)
    }
    private fun backAccounts() {
        compose.onNodeWithTag("account-detail-list").performScrollToIndex(0)
        compose.onNodeWithTag("account-detail-back").performScrollTo().performClick()
    }
    private fun adjust(account: Account) {
        compose.onNodeWithTag("account-detail-list").performScrollToIndex(0)
        compose.onNodeWithTag("account-detail-balance").assertExists()
        compose.waitUntil(10000) { compose.onAllNodes(hasTestTag("account-detail-balance") and hasText("正在计算余额…")).fetchSemanticsNodes().isEmpty() }
        compose.onNodeWithTag("account-adjust").performScrollTo().assertIsEnabled().performClick()
        compose.onNodeWithTag("balance-editor").assertIsDisplayed()
        compose.onNodeWithText("调整 ${account.name} 的余额").assertIsDisplayed()
    }
    private fun saveBalance() {
        val vm = model()
        val before = vm.state.value.adjustments.size
        compose.onNodeWithTag("balance-editor-save").assertIsEnabled().performClick()
        compose.waitUntil(10000) { !vm.state.value.busy && vm.state.value.adjustments.size == before + 1 &&
            compose.onAllNodesWithTag("balance-editor").fetchSemanticsNodes().isEmpty() }
        assertNull(vm.state.value.error)
    }
    private fun currentSession(): UiOperationSession {
        val vm = model()
        return compose.runOnIdle { UiOperationSession.forViewModel(vm, vm.instanceId) }
    }
    private fun assertReceiptsConsumed() {
        val vm = model()
        compose.waitUntil(10000) { vm.state.value.committedResults.isEmpty() }
    }
    @Test fun fourRootDestinationsUseOneGlobalNonSelectableFab() {
        ready()
        compose.onNodeWithTag("add-entry").assertIsDisplayed().assertIsEnabled()
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Selected))
        listOf("ledger", "stats", "accounts", "settings").forEach { destination ->
            compose.onNodeWithTag("nav-$destination").assertIsDisplayed().assertHasClickAction().performClick()
            compose.onNodeWithTag("nav-$destination").assertIsSelected()
            compose.onAllNodesWithTag("add-entry").assertCountEquals(1)
            compose.onNodeWithTag("add-entry").assertIsDisplayed().assertIsEnabled()
        }
    }

    @Test fun accountRootOpensCreateDialogAndKeepsFabBlockedUntilDismissed() {
        ready()
        compose.onNodeWithTag("nav-accounts").assertIsDisplayed().performClick()
        compose.onNodeWithTag("accounts-list").assertIsDisplayed()
        compose.onNodeWithTag("account-add").performScrollTo().assertIsDisplayed().performClick()
        compose.onNodeWithTag("account-editor").assertIsDisplayed()
        compose.onNodeWithTag("add-entry").assertDoesNotExist()
        compose.onNodeWithTag("account-editor-save-cancel").performClick()
        compose.onNodeWithTag("accounts-list").assertIsDisplayed()
        compose.onNodeWithTag("nav-accounts").assertIsSelected()
        compose.onNodeWithTag("add-entry").assertIsDisplayed().assertIsEnabled()
    }

    @Test fun initialAndNoInitialAccountsConsumeTheirOwnReceiptsWithoutCreatingLedgerEntries() {
        ready(); compose.onNodeWithTag("nav-accounts").performClick()
        val cash = create("合成现金", "100.00")
        val bank = create("合成银行")
        val debt = create("合成欠款", "12.34", liability = true)
        val zero = create("合成零余额", "0")
        val state = model().state.value
        assertTrue(state.entries.isEmpty()); assertTrue(state.transfers.isEmpty())
        assertEquals(3, state.adjustments.size)
        val initial = state.adjustments.single { it.accountId == cash.id }
        assertEquals(AdjustmentReason.INITIAL, initial.reason)
        assertEquals(LocalDate.now(), initial.date)
        assertEquals(10000L, initial.deltaCents); assertEquals(10000L, initial.targetBalanceCents)
        assertTrue(state.adjustments.none { it.accountId == bank.id })
        assertEquals(0L, state.adjustments.single { it.accountId == zero.id }.deltaCents)
        assertEquals(0L, state.adjustments.single { it.accountId == zero.id }.targetBalanceCents)
        assertEquals(AdjustmentReason.INITIAL, state.adjustments.single { it.accountId == zero.id }.reason)
        assertEquals(-1234L, state.adjustments.single { it.accountId == debt.id }.deltaCents)
        assertEquals(mapOf(cash.id to 10000L, bank.id to 0L, debt.id to -1234L, zero.id to 0L), AccountBalances.balances(state.snapshot, LocalDate.now()))
        assertReceiptsConsumed()
        compose.waitUntil(10000) { compose.onAllNodes(hasTestTag("accounts-net") and hasText(Money.format(8766))).fetchSemanticsNodes().size == 1 }
        compose.onNodeWithTag("accounts-net").performScrollTo().assertTextEquals(Money.format(8766))
        compose.onNodeWithTag("accounts-debt").assertTextEquals("负债 ${Money.format(1234)}")
        compose.onNodeWithTag("add-entry").assertIsDisplayed().assertIsEnabled()
    }

    @Test fun liabilityCalibrationPrefillsDebtAndPreservesZeroModeDraftAcrossRotation() {
        ready(); compose.onNodeWithTag("nav-accounts").performClick()
        val account = create("合成信用卡", "12.34", liability = true)
        openAccount(account); adjust(account)
        compose.onNodeWithTag("balance-target").performScrollTo().assertTextContains("12.34")
        compose.onNodeWithTag("balance-target-debt").assertIsSelected()
        compose.onNodeWithTag("balance-target-overpayment").performClick()
        field("balance-target", "0"); field("balance-note", "零余额草稿")
        val holder = currentSession(); val owner = holder.value.owner
        compose.activityRule.scenario.recreate(); ready()
        assertSame(holder, currentSession()); assertEquals(owner, currentSession().value.owner)
        compose.onNodeWithTag("balance-target").performScrollTo().assertTextContains("0")
        compose.onNodeWithTag("balance-target-overpayment").assertIsSelected()
        compose.onNodeWithTag("balance-note").performScrollTo().assertTextContains("零余额草稿")
        saveBalance(); assertReceiptsConsumed()
        val zero = model().state.value.adjustments.single { it.reason == AdjustmentReason.CALIBRATION }
        assertEquals(account.id, zero.accountId); assertEquals(LocalDate.now(), zero.date)
        assertEquals(1234L, zero.deltaCents); assertEquals(0L, zero.targetBalanceCents)
        adjust(account)
        compose.onNodeWithTag("balance-target").performScrollTo().assertTextContains("0.00")
        compose.onNodeWithTag("balance-target-debt").assertIsSelected()
        field("balance-target", "-1")
        compose.onNodeWithTag("balance-editor-save").performClick()
        compose.onNodeWithText("余额请输入有效金额，最多两位小数，负债金额不能带负号").performScrollTo().assertIsDisplayed()
        assertEquals(2, model().state.value.adjustments.size)
        field("balance-target", "20.01")
        compose.onNodeWithTag("balance-target-overpayment").performScrollTo().performClick()
        saveBalance(); adjust(account)
        compose.onNodeWithTag("balance-target").performScrollTo().assertTextContains("20.01")
        compose.onNodeWithTag("balance-target-overpayment").assertIsSelected()
        compose.onNodeWithTag("balance-editor-save-cancel").performClick()
        compose.onNodeWithTag("account-detail-balance").performScrollTo().assertTextEquals("溢缴 ${Money.format(2001)}")
        val vm = model()
        val positive = vm.state.value.adjustments.single { it.targetBalanceCents == 2001L }
        compose.onNodeWithTag("account-detail-list").performScrollToNode(hasTestTag("account-adjustment-delete-${positive.id}"))
        compose.onNodeWithTag("account-adjustment-delete-${positive.id}").performClick()
        compose.onNodeWithTag("account-event-delete-save").performClick()
        compose.waitUntil(10000) { !vm.state.value.busy && vm.state.value.adjustments.none { it.id == positive.id } &&
            compose.onAllNodesWithTag("account-event-delete").fetchSemanticsNodes().isEmpty() }
        compose.onNodeWithTag("account-detail-list").performScrollToIndex(0)
        compose.waitUntil(10000) { compose.onAllNodes(hasTestTag("account-detail-balance") and hasText("无欠款 ${Money.format(0)}")).fetchSemanticsNodes().size == 1 }
        assertReceiptsConsumed()
    }

    @Test fun assetCalibrationUsesBalanceRangeAndHistoricalEntryKeepsAccountDetailOrigin() {
        ready(); compose.onNodeWithTag("nav-accounts").performClick()
        val account = create("合成资产", "-5.00")
        openAccount(account); adjust(account)
        compose.onNodeWithTag("balance-target").performScrollTo().assertTextContains("-5.00")
        field("balance-target", "1000000000.01")
        saveBalance()
        assertEquals(100000000001L, AccountBalances.balances(model().state.value.snapshot, LocalDate.now()).getValue(account.id))
        openGlobalEditor(); compose.setAmountWithKeypad("1.23")
        compose.addAndSelectCategory("关联历史合成类")
        compose.onNodeWithTag("add-entry").assertDoesNotExist()
        compose.assertAmountDraft("1.23")
        compose.onNodeWithTag("editor-account-select").performScrollTo().performClick()
        compose.onNodeWithTag("editor-account-${account.id}").performScrollTo().performClick()
        val historical = LocalDate.now().minusDays(1)
        compose.selectEditorDate(historical.toString())
        compose.onNodeWithText("保存").performScrollTo().assertIsEnabled().performClick()
        val vm = model()
        compose.waitUntil(10000) { !vm.state.value.busy && vm.state.value.entries.size == 1 &&
            compose.onAllNodesWithTag("editor-shell").fetchSemanticsNodes().isEmpty() }
        val entry = vm.state.value.entries.single()
        assertEquals(account.id, entry.accountId); assertEquals("关联历史合成类", entry.category)
        assertEquals(historical, entry.date); assertEquals(123L, entry.amountCents)
        compose.onNodeWithTag("nav-accounts").assertIsSelected()
        compose.onNodeWithTag("account-detail-list").assertIsDisplayed()
        compose.onNodeWithTag("account-detail-balance").performScrollTo().assertTextEquals("余额 ${Money.format(99999999878L)}")
        compose.onNodeWithTag("account-detail-list").performScrollToNode(hasTestTag("account-entry-${entry.id}"))
        compose.onNodeWithTag("account-entry-${entry.id}").performClick()
        compose.onNodeWithTag("entry-detail").assertIsDisplayed()
        openGlobalEditor()
        compose.onNodeWithText("取消").performScrollTo().performClick()
        compose.onNodeWithTag("entry-detail").assertIsDisplayed()
        compose.onNodeWithTag("detail-back").performClick()
        compose.onNodeWithTag("account-detail-list").assertIsDisplayed()
        compose.onNodeWithTag("account-detail-list").performScrollToNode(hasTestTag("account-entry-${entry.id}"))
        compose.onNodeWithTag("account-entry-${entry.id}").performClick()
        compose.onNodeWithText("编辑").performScrollTo().performClick()
        compose.setAmountWithKeypad("2.00")
        compose.onNodeWithText("保存").performScrollTo().performClick()
        compose.waitUntil(10000) { !vm.state.value.busy && vm.state.value.entries.single().amountCents == 200L &&
            compose.onAllNodesWithTag("editor-shell").fetchSemanticsNodes().isEmpty() }
        assertEquals(entry.id, vm.state.value.entries.single().id)
        assertEquals(account.id, vm.state.value.entries.single().accountId)
        compose.onNodeWithTag("account-detail-list").performScrollToIndex(0)
        compose.waitUntil(10000) { compose.onAllNodes(hasTestTag("account-detail-balance") and hasText("余额 ${Money.format(99999999801L)}")).fetchSemanticsNodes().size == 1 }
        compose.onNodeWithTag("account-detail-list").performScrollToNode(hasTestTag("account-entry-${entry.id}"))
        compose.onNodeWithTag("account-entry-${entry.id}").performClick()
        compose.onNodeWithText("删除").performScrollTo().performClick()
        compose.onNodeWithTag("entry-delete-confirm").performClick()
        compose.waitUntil(10000) { !vm.state.value.busy && vm.state.value.entries.isEmpty() &&
            compose.onAllNodesWithTag("entry-delete-confirm").fetchSemanticsNodes().isEmpty() }
        compose.onNodeWithTag("account-detail-list").performScrollToIndex(0)
        compose.waitUntil(10000) { compose.onAllNodes(hasTestTag("account-detail-balance") and hasText("余额 ${Money.format(100000000001L)}")).fetchSemanticsNodes().size == 1 }
        backAccounts()
        assertReceiptsConsumed()
    }

    @Test fun transfersEditDeleteAndArchiveKeepIncomeAndExpenseUnchanged() {
        ready(); compose.onNodeWithTag("nav-accounts").performClick()
        val from = create("合成转出", "100.00"); val to = create("合成转入")
        compose.onNodeWithTag("account-transfer").performScrollTo().performClick()
        compose.onNodeWithTag("transfer-from-${from.id}").performScrollTo().performClick()
        compose.onNodeWithTag("transfer-to-${to.id}").performScrollTo().performClick()
        field("transfer-amount", "10.00")
        compose.onNodeWithTag("transfer-editor-save").performClick()
        val vm = model()
        compose.waitUntil(10000) { !vm.state.value.busy && vm.state.value.transfers.size == 1 &&
            compose.onAllNodesWithTag("transfer-editor").fetchSemanticsNodes().isEmpty() }
        val transfer = vm.state.value.transfers.single()
        assertEquals(from.id, transfer.fromAccountId); assertEquals(to.id, transfer.toAccountId)
        assertEquals(1000L, transfer.amountCents); assertTrue(vm.state.value.entries.isEmpty())
        assertEquals(9000L, AccountBalances.balances(vm.state.value.snapshot, LocalDate.now()).getValue(from.id))
        openAccount(from)
        compose.onNodeWithTag("account-detail-list").performScrollToNode(hasTestTag("account-transfer-edit-${transfer.id}"))
        compose.onNodeWithTag("account-transfer-edit-${transfer.id}").performClick()
        field("transfer-amount", "20.00")
        compose.onNodeWithTag("transfer-editor-save").performClick()
        compose.waitUntil(10000) { !vm.state.value.busy && vm.state.value.transfers.single().amountCents == 2000L &&
            compose.onAllNodesWithTag("transfer-editor").fetchSemanticsNodes().isEmpty() }
        assertEquals(transfer.id, vm.state.value.transfers.single().id)
        compose.onNodeWithTag("account-detail-list").performScrollToNode(hasTestTag("account-transfer-delete-${transfer.id}"))
        compose.onNodeWithTag("account-transfer-delete-${transfer.id}").performClick()
        compose.onNodeWithTag("account-event-delete-save").performClick()
        compose.waitUntil(10000) { !vm.state.value.busy && vm.state.value.transfers.isEmpty() &&
            compose.onAllNodesWithTag("account-event-delete").fetchSemanticsNodes().isEmpty() }
        compose.onNodeWithTag("account-detail-list").performScrollToIndex(0)
        compose.onNodeWithTag("account-archive").performScrollTo().performClick()
        compose.waitUntil(10000) { !vm.state.value.busy && vm.state.value.accounts.single { it.id == from.id }.archived }
        compose.onNodeWithTag("account-adjust").performScrollTo().assertIsNotEnabled()
        backAccounts()
        compose.onNodeWithTag("account-show-archived").performScrollTo().performClick()
        compose.onNodeWithTag("accounts-list").performScrollToNode(hasTestTag("account-row-${from.id}"))
        compose.onNodeWithTag("account-row-${from.id}").assertTextContains("合成转出（已归档）")
        assertTrue(vm.state.value.entries.isEmpty())
        assertEquals(AccountTotals(10000, 0, 10000), AccountBalances.totals(vm.state.value.snapshot, LocalDate.now()))
        assertReceiptsConsumed()
    }

    @Test fun rejectedCreatePreservesRawInputsAndRetryDoesNotDuplicateItsBusinessIds() {
        ready(); compose.onNodeWithTag("nav-accounts").performClick()
        LedgerDatabase(compose.activity).use { it.writableDatabase.execSQL("CREATE TRIGGER reject_synthetic_account BEFORE INSERT ON accounts BEGIN SELECT RAISE(ABORT,'synthetic-private-detail'); END") }
        try {
            compose.onNodeWithTag("account-add").performScrollTo().performClick()
            field("account-name", "合成失败重试"); field("account-initial-balance", "12.34")
            field("account-initial-note", "保持原始备注")
            compose.onNodeWithTag("account-editor-save").performClick()
            val vm = model()
            compose.waitUntil(10000) { !vm.state.value.busy && vm.state.value.failedOperationId != null }
            val payload = currentSession().value.payload
            val accountId = payload[0]; val initialId = payload[6]
            assertTrue(vm.state.value.accounts.isEmpty()); assertTrue(vm.state.value.adjustments.isEmpty())
            assertFalse(vm.state.value.error.orEmpty().contains("synthetic-private-detail"))
            compose.onNodeWithTag("account-name").performScrollTo().assertTextContains("合成失败重试")
            compose.onNodeWithTag("account-initial-balance").performScrollTo().assertTextContains("12.34")
            compose.onNodeWithTag("account-initial-note").performScrollTo().assertTextContains("保持原始备注")
            compose.onNodeWithTag("account-editor-save").assertIsEnabled()
            LedgerDatabase(compose.activity).use { it.writableDatabase.execSQL("DROP TRIGGER reject_synthetic_account") }
            compose.onNodeWithTag("account-editor-save").performClick()
            compose.waitUntil(10000) { !vm.state.value.busy && vm.state.value.accounts.size == 1 &&
                compose.onAllNodesWithTag("account-editor").fetchSemanticsNodes().isEmpty() }
            assertEquals(accountId, vm.state.value.accounts.single().id)
            assertEquals(initialId, vm.state.value.adjustments.single().id)
            assertEquals(LocalDate.parse(payload[9]), vm.state.value.adjustments.single().date)
            assertReceiptsConsumed()
        } finally { LedgerDatabase(compose.activity).use { it.writableDatabase.execSQL("DROP TRIGGER IF EXISTS reject_synthetic_account") } }
    }

    @Test fun committedReadFailureStaysFrozenAcrossRotationUntilItsExactQueuedReadCompletes() {
        ready(); compose.onNodeWithTag("nav-accounts").performClick()
        LedgerDatabase(compose.activity).use { it.writableDatabase.execSQL("CREATE TRIGGER corrupt_synthetic_read AFTER INSERT ON adjustments WHEN NEW.reason='INITIAL' AND NEW.note='read-failure-marker' BEGIN UPDATE adjustments SET date='invalid-date' WHERE id=NEW.id; END") }
        var held: Mutex? = null
        var repairDate = LocalDate.now().toString()
        try {
            compose.onNodeWithTag("account-add").performScrollTo().performClick()
            field("account-name", "合成提交读错"); field("account-initial-balance", "5.00")
            field("account-initial-note", "read-failure-marker")
            compose.onNodeWithTag("account-editor-save").performClick()
            val vm = model()
            compose.waitUntil(10000) { !vm.state.value.busy && vm.state.value.readError && vm.state.value.committedResults.size == 1 }
            val receipt = vm.state.value.committedResults.single()
            assertEquals(LedgerOperation.CREATE_ACCOUNT, receipt.operation); assertTrue(receipt.committed); assertFalse(receipt.snapshotReady)
            val adjustment = requireNotNull(receipt.adjustment)
            repairDate = adjustment.date.toString()
            val live = currentSession(); val payload = live.value.payload; val owner = live.value.owner
            LedgerDatabase(compose.activity).use { database ->
                database.readableDatabase.rawQuery("SELECT id FROM accounts", null).use { cursor -> assertEquals(1, cursor.count); assertTrue(cursor.moveToFirst()); assertEquals(payload[0], cursor.getString(0)) }
                database.readableDatabase.rawQuery("SELECT id,date FROM adjustments", null).use { cursor -> assertEquals(1, cursor.count); assertTrue(cursor.moveToFirst()); assertEquals(adjustment.id, cursor.getString(0)); assertEquals("invalid-date", cursor.getString(1)) }
            }
            compose.onNodeWithTag("account-name").performScrollTo().assertIsNotEnabled()
            compose.onNodeWithTag("account-kind-liability").performScrollTo().assertIsNotEnabled()
            compose.onNodeWithTag("account-editor-save").assertIsNotEnabled()
            compose.onNodeWithTag("account-editor-save-cancel").assertIsNotEnabled()
            compose.onNodeWithTag("add-entry").assertDoesNotExist()
            LedgerDatabase(compose.activity).use {
                it.writableDatabase.execSQL("DROP TRIGGER corrupt_synthetic_read")
                it.writableDatabase.execSQL("UPDATE adjustments SET date=? WHERE id=?", arrayOf(adjustment.date.toString(), adjustment.id))
            }
            val lock = LedgerViewModel::class.java.getDeclaredField("lock").apply { isAccessible = true }.get(vm) as Mutex
            assertTrue(lock.tryLock()); held = lock
            val before = vm.viewModelScope.coroutineContext[Job]!!.children.toSet()
            compose.onNodeWithTag("operation-retry-read").performScrollTo().assertIsEnabled().performClick()
            compose.waitUntil(10000) { live.value.readToken.isNotEmpty() && vm.viewModelScope.coroutineContext[Job]!!.children.any { it.isActive && it !in before } }
            val readToken = live.value.readToken
            val revision = vm.state.value.snapshotRevision
            assertEquals(1, vm.viewModelScope.coroutineContext[Job]!!.children.count { it.isActive && it !in before })
            compose.onNodeWithTag("operation-retry-read").assertIsNotEnabled()
            compose.activityRule.scenario.recreate()
            assertSame(vm, model()); assertSame(live, currentSession())
            assertEquals(readToken, live.value.readToken); assertEquals(owner, live.value.owner)
            assertEquals(payload, live.value.payload); assertEquals(receipt.operationId, live.value.attempt)
            compose.onNodeWithTag("account-editor-save").assertIsNotEnabled()
            lock.unlock(); held = null
            compose.waitUntil(10000) { !vm.state.value.busy && !vm.state.value.readError &&
                compose.onAllNodesWithTag("account-editor").fetchSemanticsNodes().isEmpty() }
            assertEquals(payload[0], vm.state.value.accounts.single().id)
            assertEquals(adjustment, vm.state.value.adjustments.single())
            assertTrue(vm.state.value.snapshotRevision > revision)
            assertTrue(live.value.readToken.isEmpty()); assertTrue(live.value.owner.isEmpty())
            assertReceiptsConsumed(); compose.onNodeWithTag("add-entry").assertIsDisplayed().assertIsEnabled()
        } finally {
            held?.unlock()
            LedgerDatabase(compose.activity).use {
                it.writableDatabase.execSQL("DROP TRIGGER IF EXISTS corrupt_synthetic_read")
                it.writableDatabase.execSQL("UPDATE adjustments SET date=? WHERE date='invalid-date' AND note='read-failure-marker'", arrayOf(repairDate))
            }
        }
    }

    @Test fun actualCalendarSelectsLeapAndRangeBoundariesWithoutLosingParentDraft() {
        ready(); openGlobalEditor(); compose.setAmountWithKeypad("12.34")
        compose.selectCategory("餐饮")
        val owner = currentSession().value.owner
        compose.selectEditorDate("2000-02-29")
        compose.selectEditorDate("1900-01-01", confirm = false)
        compose.assertEditorDate("2000-02-29")
        compose.selectEditorDate("9999-12-31")
        compose.activityRule.scenario.recreate(); ready()
        compose.assertEditorDate("9999-12-31"); compose.assertAmountDraft("12.34")
        compose.assertSelectedCategory("餐饮"); assertEquals(owner, currentSession().value.owner)
        compose.onNodeWithTag("add-entry").assertDoesNotExist()
        compose.onNodeWithText("取消").performScrollTo().performClick()
        compose.onNodeWithTag("discard-draft").assertIsDisplayed()
        compose.onNodeWithText("放弃修改").performClick()
        assertTrue(model().state.value.entries.isEmpty())
        compose.onNodeWithTag("add-entry").assertIsDisplayed().assertIsEnabled()
    }

    @Test fun categoryChildAndSettingsRenameArchiveAndFilteredReorderKeepHistoricalName() {
        ready(); openGlobalEditor(); compose.setAmountWithKeypad("1.00")
        compose.addAndSelectCategory("历史目录合成名")
        compose.onNodeWithText("最近使用").assertDoesNotExist()
        compose.onNodeWithText("快捷分类").assertDoesNotExist()
        compose.onNodeWithTag("add-entry").assertDoesNotExist()
        compose.onNodeWithText("保存").performScrollTo().performClick()
        val vm = model()
        compose.waitUntil(10000) { !vm.state.value.busy && vm.state.value.entries.size == 1 &&
            compose.onAllNodesWithTag("editor-shell").fetchSemanticsNodes().isEmpty() }
        val original = vm.state.value.entries.single()
        val category = vm.state.value.categories.single { it.name == original.category && it.kind == original.kind }
        compose.onNodeWithTag("nav-settings").performClick()
        compose.onNodeWithTag("settings-open-categories").performScrollTo().performClick()
        field("settings-category-search", "历史目录合成名")
        compose.onNodeWithTag("settings-categories-list").performScrollToNode(hasTestTag("category-rename-${category.id}"))
        compose.onNodeWithTag("category-rename-${category.id}").performClick()
        field("category-name", "历史目录合成名已改")
        compose.onNodeWithTag("category-editor-save").performClick()
        compose.waitUntil(10000) { !vm.state.value.busy && vm.state.value.categories.single { it.id == category.id }.name == "历史目录合成名已改" &&
            compose.onAllNodesWithTag("category-editor").fetchSemanticsNodes().isEmpty() }
        assertEquals(original, vm.state.value.entries.single())
        val before = vm.state.value.categories.filter { it.kind == category.kind && !it.archived }.sortedWith(compareBy({ it.sortOrder }, { it.id })).map { it.id }
        assertEquals(category.id, before.last())
        compose.onNodeWithTag("settings-categories-list").performScrollToNode(hasTestTag("category-up-${category.id}"))
        compose.onNodeWithTag("category-up-${category.id}").assertIsEnabled().performClick()
        val expected = before.toMutableList().apply { val last = removeAt(lastIndex); add(lastIndex, last) }
        compose.waitUntil(10000) { !vm.state.value.busy && vm.state.value.categories.filter { it.kind == category.kind && !it.archived }.sortedWith(compareBy({ it.sortOrder }, { it.id })).map { it.id } == expected }
        assertReceiptsConsumed()
        compose.onNodeWithTag("settings-categories-list").performScrollToNode(hasTestTag("category-archive-${category.id}"))
        compose.onNodeWithTag("category-archive-${category.id}").performClick()
        compose.waitUntil(10000) { !vm.state.value.busy && vm.state.value.categories.single { it.id == category.id }.archived }
        assertReceiptsConsumed(); assertEquals(original, vm.state.value.entries.single())
        openGlobalEditor()
        compose.onNodeWithTag("editor-category-all").performScrollTo().performClick()
        field("editor-category-search", "历史目录合成名已改")
        compose.onNodeWithTag("editor-category-row-${category.id}").assertDoesNotExist()
        compose.onNodeWithText("没有匹配的活动类别").assertIsDisplayed()
        compose.onNode(hasText("返回") and hasAnyAncestor(isDialog() and hasAnyDescendant(hasTestTag("editor-category-list")))).performClick()
        compose.onNodeWithText("取消").performScrollTo().performClick()
        compose.onNodeWithTag("settings-categories-list").assertIsDisplayed()
        assertEquals(original, vm.state.value.entries.single())
    }
}

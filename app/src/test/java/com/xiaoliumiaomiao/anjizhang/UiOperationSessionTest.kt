package com.xiaoliumiaomiao.anjizhang

import com.xiaoliumiaomiao.anjizhang.ui.*
import org.junit.Assert.*
import org.junit.Test

class UiOperationSessionTest {
    @Test fun aFreshVmReleasesOnlyTheCancelledExportJobAndKeepsUriAndUnconfirmedResult() {
        fun pending(operation: String, attempt: String = ""): UiOperationSession {
            val oldVm = object : androidx.lifecycle.ViewModel() {}
            val store = androidx.lifecycle.ViewModelStore().also { it.put("old", oldVm) }
            val old = UiOperationSession.forViewModel(oldVm, "old")
            old.acquire("owner", "export", emptyList(), "old", false)
            old.latchSubmit("owner", operation, listOf("content://synthetic/backup"), false)
            if (attempt.isNotEmpty()) old.bindAttempt("owner", attempt, "old")
            val saved = old.saved()
            store.clear()
            return UiOperationSession.forViewModel(object : androidx.lifecycle.ViewModel() {}, "new", UiOperationSession.restored(saved))
        }
        val exporting = pending("EXPORT")
        assertFalse("The old VM's cancelled IO Job cannot keep a fresh VM permanently sending", exporting.submitBlocked("owner", false))
        assertEquals("owner", exporting.value.owner)
        assertEquals(listOf("content://synthetic/backup"), exporting.value.payload)
        assertTrue(exporting.value.attempt.isEmpty())
        assertFalse(exporting.value.unknown)
        assertEquals("无法确认上次导出结果，请检查备份文件；可返回后重新导出。", exporting.value.error)
        assertTrue(pending("EXPORT_SELECT").submitBlocked("owner", false))
        val restore = pending("RESTORE", "actual-write-attempt")
        assertTrue(restore.value.unknown); assertTrue(restore.value.frozen)
        assertEquals("actual-write-attempt", restore.value.attempt)
        assertTrue(restore.submitBlocked("owner", false))
    }
    @Test fun aReadyReceiptCannotCloseTheOwnerBeforeItsExactReadJobHasTerminated() {
        val session = opened()
        session.latchSubmit("parent", "CREATE_ACCOUNT", listOf("account-id", "request-id"), false)
        session.bindAttempt("parent", "attempt", "vm")
        val committed = CommittedOperationResult("attempt", LedgerOperation.CREATE_ACCOUNT)
        session.observe(LedgerUiState(readError = true, committedResults = listOf(committed)), "vm")
        session.beginRead("parent", "read", false)
        val job = kotlinx.coroutines.Job()
        session.trackRead(job, kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Unconfined), "parent", "read")
        val ready = committed.copy(snapshotReady = true)
        assertEquals(ready, session.observe(LedgerUiState(committedResults = listOf(ready)), "vm"))
        assertFalse("Snapshot publication precedes the actual read Job terminal state", session.complete("parent", "attempt"))
        assertEquals("parent", session.value.owner)
        assertEquals("read", session.value.readToken)
        job.complete()
        assertEquals("", session.value.readToken)
        assertTrue(session.complete("parent", "attempt"))
        assertFalse(session.openBlocked(false))
    }
    @Test fun registeringAnAlreadyCompletedReadAndAnOldLateCompletionCannotReleaseANewToken() {
        val session = opened()
        val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Unconfined)
        val completed = kotlinx.coroutines.Job().also { it.complete() }
        assertTrue(session.beginRead("parent", "first", false))
        session.trackRead(completed, scope, "parent", "first")
        assertEquals("", session.value.readToken)
        val late = kotlinx.coroutines.Job()
        session.beginRead("parent", "old", false)
        session.trackRead(late, scope, "parent", "old")
        session.endRead("old")
        assertTrue(session.beginRead("parent", "new", false))
        late.complete()
        assertEquals("new", session.value.readToken)
    }
    @Test fun viewModelClearClosesOnceWithoutThrowingOrLosingPendingWritePayload() {
        val store = androidx.lifecycle.ViewModelStore()
        val vm = object : androidx.lifecycle.ViewModel() {}
        store.put("actual", vm)
        val session = UiOperationSession.forViewModel(vm, "vm")
        assertSame(session, UiOperationSession.forViewModel(vm, "vm"))
        assertTrue(session.acquire("owner", "account", listOf("raw"), "vm", false))
        session.latchSubmit("owner", "CREATE_ACCOUNT", listOf("id", "request-id", "full raw"), false)
        session.bindAttempt("owner", "attempt", "vm")
        store.clear()
        session.close()
        assertTrue(session.openBlocked(false))
        assertEquals("attempt", session.value.attempt)
        assertEquals(listOf("id", "request-id", "full raw"), session.value.payload)
        assertFalse(session.beginRead("owner", "read", false))
    }
    @Test fun unknownRestoreRequiresItsOwnSuccessfulExplicitReadBeforeAcknowledgement() {
        val old = UiOperationSession()
        old.acquire("restore-owner", "restore", emptyList(), "old", false)
        old.latchSubmit("restore-owner", "RESTORE", listOf("content://synthetic/backup"), false)
        old.bindAttempt("restore-owner", "restore-attempt", "old")
        val fresh = UiOperationSession.forViewModel(object : androidx.lifecycle.ViewModel() {}, "new", UiOperationSession.restored(old.saved()))
        assertFalse(fresh.acknowledgeUnknownRestore("restore-owner", false, false))
        fresh.beginRead("restore-owner", "read", false)
        fresh.confirmRestoreRead("restore-owner", true)
        assertFalse(fresh.acknowledgeUnknownRestore("restore-owner", false, false))
        fresh.endRead("read")
        fresh.confirmRestoreRead("restore-owner", false)
        assertFalse(fresh.acknowledgeUnknownRestore("restore-owner", false, false))
        fresh.confirmRestoreRead("restore-owner", true)
        assertFalse(fresh.acknowledgeUnknownRestore("other-owner", false, false))
        assertFalse(fresh.acknowledgeUnknownRestore("restore-owner", true, false))
        assertFalse(fresh.acknowledgeUnknownRestore("restore-owner", false, true))
        assertEquals("restore-attempt", fresh.value.attempt)
        assertEquals(listOf("content://synthetic/backup"), fresh.value.payload)
        assertTrue(fresh.acknowledgeUnknownRestore("restore-owner", false, false))
        assertEquals("", fresh.value.owner)
        assertFalse(fresh.acknowledgeUnknownRestore("restore-owner", false, false))
    }
    @Test fun restoreAcknowledgementCannotUnlockAnUnknownEditableRequest() {
        val old = opened()
        old.latchSubmit("parent", "SAVE_ENTRY", listOf("id", "raw-date", "payload"), false)
        old.bindAttempt("parent", "attempt", "vm")
        val fresh = UiOperationSession.forViewModel(object : androidx.lifecycle.ViewModel() {}, "new", UiOperationSession.restored(old.saved()))
        fresh.confirmRestoreRead("parent", true)
        assertFalse(fresh.acknowledgeUnknownRestore("parent", false, false))
        assertEquals(old.value.payload, fresh.value.payload)
        assertTrue(fresh.value.unknown)
    }
    @Test fun rebindingSameVmKeepsTheLiveOwnerAndReadLatchInsteadOfTheSavedCopy() {
        val vm = object : androidx.lifecycle.ViewModel() {}
        val live = UiOperationSession.forViewModel(vm, "vm")
        assertTrue(live.acquire("owner", "entry", listOf("original"), "vm", false))
        assertTrue(live.beginRead("owner", "read", false))
        val restored = UiOperationSession.restored(live.saved())
        assertSame(live, UiOperationSession.forViewModel(vm, "vm", restored))
        assertTrue(live.openBlocked(false))
        assertEquals("read", live.value.readToken)
    }
    @Test fun aFreshVmDropsOnlyTheOldReadTokenAndRetainsUnknownWriteIdentity() {
        val old = opened()
        old.latchSubmit("parent", "SAVE_ENTRY", listOf("business-id", "2020-02-29", "raw"), false)
        old.bindAttempt("parent", "attempt", "vm")
        old.observe(LedgerUiState(readError = true, committedResults = listOf(
            CommittedOperationResult("attempt", LedgerOperation.SAVE_ENTRY))), "vm")
        old.beginRead("parent", "old-read", false)
        val fresh = UiOperationSession.forViewModel(object : androidx.lifecycle.ViewModel() {}, "new-vm", UiOperationSession.restored(old.saved()))
        assertEquals("", fresh.value.readToken)
        assertEquals("attempt", fresh.value.attempt)
        assertEquals(old.value.payload, fresh.value.payload)
        assertTrue(fresh.value.unknown)
        assertTrue(fresh.value.frozen)
        assertTrue(fresh.beginRead("parent", "new-read", false))
    }
    @Test fun aReadLatchReleasesOnlyWhenTheExactJobTerminatesAndCloseNeverReportsSuccess() {
        val session = opened()
        val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Unconfined)
        val job = kotlinx.coroutines.Job()
        assertTrue(session.beginRead("parent", "read", false))
        session.trackRead(job, scope, "parent", "read")
        assertEquals("read", session.value.readToken)
        job.complete()
        assertEquals("", session.value.readToken)
        session.latchSubmit("parent", "SAVE_ENTRY", listOf("id", "payload"), false)
        session.bindAttempt("parent", "attempt", "vm")
        session.close()
        assertTrue(session.openBlocked(false))
        assertEquals("attempt", session.value.attempt)
        assertEquals(listOf("id", "payload"), session.value.payload)
        assertFalse(session.beginRead("parent", "again", false))
    }
    private fun opened(): UiOperationSession = UiOperationSession().also {
        assertTrue(it.acquire("parent", "entry", listOf("source-id", "raw original"), "vm", false))
    }
    @Test fun twoOpensInTheSameFrameHaveExactlyOneOwner() {
        val session = UiOperationSession()
        assertTrue(session.acquire("one", "entry", emptyList(), "vm", false))
        assertFalse(session.acquire("two", "account", emptyList(), "vm", false))
        assertEquals("one", session.value.owner)
        assertTrue(session.openBlocked(false))
        assertFalse(session.submitBlocked("one", false))
    }
    @Test fun twoSubmitsBeforeVmBusyChangesKeepTheOriginalFrozenPayload() {
        val session = opened()
        assertTrue(session.latchSubmit("parent", "SAVE_ENTRY", listOf("stable-business-id", "raw", "2020-02-29"), false))
        assertFalse(session.latchSubmit("parent", "SAVE_ENTRY", listOf("other-id", "changed"), false))
        session.bindAttempt("parent", "attempt", "vm")
        assertEquals(listOf("stable-business-id", "raw", "2020-02-29"), session.value.payload)
        assertEquals("attempt", session.value.attempt)
        assertFalse(session.dismiss("parent", false))
    }
    @Test fun childCloseResumesOwnParentSaveAndNeverReleasesGlobalOwner() {
        val session = opened()
        assertTrue(session.acquireChild("parent", "child", "category", listOf("EXPENSE"), false))
        assertTrue(session.submitBlocked("parent", false))
        assertFalse(session.acquire("other", "entry", emptyList(), "vm", false))
        assertTrue(session.dismiss("child", false))
        assertEquals("parent", session.value.owner)
        assertFalse(session.openBlocked(false).not())
        assertFalse(session.submitBlocked("parent", false))
        assertTrue(session.latchSubmit("parent", "SAVE_ENTRY", listOf("business"), false))
    }
    @Test fun unrelatedResultsCannotCloseOrUnlockCurrentAttempt() {
        val session = opened()
        session.latchSubmit("parent", "SAVE_ENTRY", listOf("business"), false)
        session.bindAttempt("parent", "mine", "vm")
        val old = CommittedOperationResult("old", LedgerOperation.SAVE_ENTRY, snapshotReady = true)
        assertNull(session.observe(LedgerUiState(committedResults = listOf(old), failedOperationId = "old"), "vm"))
        assertEquals("mine", session.value.attempt)
        assertFalse(session.complete("parent", "old"))
        assertTrue(session.submitBlocked("parent", false))
    }
    @Test fun committedReadFailureFreezesAllInputsButOwnReadCanRunOnlyOnceUntilExactCompletion() {
        val session = opened()
        session.latchSubmit("parent", "CREATE_ACCOUNT", listOf("account-id", "initial-id", "2020-02-29"), false)
        session.bindAttempt("parent", "mine", "vm")
        val committed = CommittedOperationResult("mine", LedgerOperation.CREATE_ACCOUNT, adjustment = null)
        assertNull(session.observe(LedgerUiState(readError = true, failedOperationId = "mine", committedResults = listOf(committed)), "vm"))
        assertTrue(session.value.frozen)
        assertFalse(session.dismiss("parent", false))
        assertTrue(session.beginRead("parent", "read-one", false))
        assertFalse(session.beginRead("parent", "read-two", false))
        assertFalse(session.endRead("old-token"))
        assertTrue(session.value.frozen)
        assertTrue(session.endRead("read-one"))
        val ready = committed.copy(snapshotReady = true)
        assertEquals(ready, session.observe(LedgerUiState(committedResults = listOf(ready)), "vm"))
        assertTrue(session.complete("parent", "mine"))
        assertFalse(session.openBlocked(false))
    }
    @Test fun uncommittedFailureUnlocksRawDraftWithoutReplacingBusinessIdentity() {
        val session = opened()
        val payload = listOf("stable", "unfinished raw", "1900-01-01")
        session.latchSubmit("parent", "ADJUST_BALANCE", payload, false)
        session.bindAttempt("parent", "failed", "vm")
        assertNull(session.observe(LedgerUiState(failedOperationId = "failed", error = "安全文案"), "vm"))
        assertFalse(session.value.frozen)
        assertEquals("failed", session.value.failedAttempt)
        assertEquals(payload, session.value.payload)
        assertFalse(session.submitBlocked("parent", false))
        assertTrue(session.clearFailed("parent", false))
        assertEquals("parent", session.value.owner)
    }
    @Test fun saveableUnknownOutcomeRetainsOriginalUuidPayloadAndOnlyReadCanEstablishDisposition() {
        val session = opened()
        val payload = listOf("account-id", "initial-id", "raw", "2020-02-29")
        session.latchSubmit("parent", "CREATE_ACCOUNT", payload, false)
        session.bindAttempt("parent", "attempt", "old-vm")
        val restored = UiOperationSession.restored(session.saved())
        assertEquals(session.value, restored.value)
        assertNull(restored.observe(LedgerUiState(), "new-vm"))
        assertTrue(restored.value.unknown)
        assertTrue(restored.value.frozen)
        assertEquals(payload, restored.value.payload)
        assertEquals("attempt", restored.value.attempt)
        assertFalse(restored.latchSubmit("parent", "CREATE_ACCOUNT", listOf("new-id"), false))
        assertTrue(restored.beginRead("parent", "read", false))
        assertTrue(restored.endRead("read"))
        assertTrue(restored.resolveUnknown(false, "new-vm"))
        assertEquals(payload, restored.value.payload)
        assertEquals("parent", restored.value.owner)
        assertFalse(restored.value.frozen)
        assertFalse(restored.submitBlocked("parent", false))
    }
    @Test fun readFailureReleasesOnlyReadLatchAndKeepsUnknownFormFrozen() {
        val session = opened()
        session.latchSubmit("parent", "SAVE_TRANSFER", listOf("stable-transfer", "full original"), false)
        session.bindAttempt("parent", "attempt", "old-vm")
        session.observe(LedgerUiState(), "new-vm")
        assertTrue(session.beginRead("parent", "read", false))
        assertTrue(session.endRead("read"))
        assertFalse(session.resolveUnknown(null, "new-vm"))
        assertTrue(session.value.unknown)
        assertTrue(session.value.frozen)
        assertEquals(listOf("stable-transfer", "full original"), session.value.payload)
        assertTrue(session.beginRead("parent", "retry", false))
    }
}

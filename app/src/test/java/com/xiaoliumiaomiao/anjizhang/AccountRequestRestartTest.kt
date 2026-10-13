package com.xiaoliumiaomiao.anjizhang

import com.xiaoliumiaomiao.anjizhang.ui.*
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class AccountRequestRestartTest {
    private val oldDate = LocalDate.of(2026, 10, 12)
    private val newDate = oldDate.plusDays(1)
    private fun accountDraft() = AccountEditorState.from(null, oldDate).copy(
        name = "  原始账户  ", initialBalance = "001.00", note = "原始备注\n保留")
    private fun balanceDraft() = BalanceEditorState.create("account", oldDate).copy(
        targetBalance = "-001.00", note = "原始备注\n保留")
    private fun failedSession() = UiOperationSession().apply {
        assertTrue(acquire("form", "account", emptyList(), "vm", false))
        assertTrue(latchSubmit("form", "CREATE_ACCOUNT", listOf("id", "name", "ASSET", "CASH", "false", "0",
            "request", "id", "100", oldDate.toString(), "note", "INITIAL"), false))
        bindAttempt("form", "failed", "vm")
        observe(LedgerUiState(failedOperationId = "failed", error = "失败"), "vm")
    }

    @Test fun deniedAccountRestartKeepsAllDraftFieldsAndDoesNotReadToday() {
        val original = accountDraft()
        var dateReads = 0
        val actual = original.restartForToday({ false }, { dateReads++; newDate })
        assertEquals(original, actual)
        assertEquals(0, dateReads)
    }

    @Test fun deniedBalanceRestartKeepsAllDraftFieldsAndDoesNotReadToday() {
        val original = balanceDraft()
        var dateReads = 0
        val actual = original.restartForToday({ false }, { dateReads++; newDate })
        assertEquals(original, actual)
        assertEquals(0, dateReads)
    }

    @Test fun accountRestartGetsOneLivePermitBeforeChangingBusinessIds() {
        val original = accountDraft()
        var draft = original
        val session = failedSession()
        var dateReads = 0
        val permit = { session.clearFailed("form", false) }
        draft = draft.restartForToday(permit, { dateReads++; newDate })
        val first = draft
        draft = draft.restartForToday(permit, { dateReads++; newDate.plusDays(1) })
        assertEquals(first, draft)
        assertEquals(1, dateReads)
        assertNotEquals(original.id, first.id)
        assertNotEquals(original.initialRequestId, first.initialRequestId)
        assertEquals(newDate.toString(), first.date)
        assertEquals(original, first.copy(id = original.id, initialRequestId = original.initialRequestId, date = original.date))
        assertEquals("form", session.value.owner)
    }

    @Test fun balanceRestartGetsOneLivePermitBeforeChangingRequestId() {
        val original = balanceDraft()
        var draft = original
        val session = failedSession()
        var dateReads = 0
        val permit = { session.clearFailed("form", false) }
        draft = draft.restartForToday(permit, { dateReads++; newDate })
        val first = draft
        draft = draft.restartForToday(permit, { dateReads++; newDate.plusDays(1) })
        assertEquals(first, draft)
        assertEquals(1, dateReads)
        assertNotEquals(original.requestId, first.requestId)
        assertEquals(original, first.copy(requestId = original.requestId, date = original.date))
    }

    @Test fun liveRestartRejectsUnmatchedFailureAndCommittedReceipt() {
        val failed = LedgerUiState(failedOperationId = "failed", error = "失败")
        val session = failedSession()
        assertFalse(session.canRestartFailed("form", failed.copy(failedOperationId = "other"), newDate))
        assertFalse(session.canRestartFailed("form", failed.copy(committedResults = listOf(
            CommittedOperationResult("failed", LedgerOperation.CREATE_ACCOUNT))), newDate))
        assertEquals("failed", session.value.failedAttempt)
    }

    @Test fun liveRestartChecksCurrentDateAndReadOrBusyState() {
        val failed = LedgerUiState(failedOperationId = "failed", error = "失败")
        val session = failedSession()
        assertFalse(session.canRestartFailed("form", failed, oldDate))
        assertFalse(session.canRestartFailed("form", failed.copy(busy = true), newDate))
        assertFalse(session.canRestartFailed("form", failed.copy(readError = true), newDate))
        assertFalse(session.canRestartFailed("other", failed, newDate))
        assertTrue(session.canRestartFailed("form", failed, newDate))
        assertTrue(session.clearFailed("form", false))
        assertFalse(session.canRestartFailed("form", failed, newDate))
    }
}

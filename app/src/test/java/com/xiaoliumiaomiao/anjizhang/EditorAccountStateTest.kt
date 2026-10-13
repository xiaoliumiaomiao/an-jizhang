package com.xiaoliumiaomiao.anjizhang

import com.xiaoliumiaomiao.anjizhang.domain.*
import com.xiaoliumiaomiao.anjizhang.ui.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.util.UUID

class EditorAccountStateTest {
    private val today = LocalDate.of(2026, 10, 12)
    private val scope = object : SaverScope { override fun canBeSaved(value: Any) = true }
    private fun <T : Any> roundTrip(saver: Saver<T, Any>, value: T): T =
        saver.restore(requireNotNull(with(saver) { scope.save(value) }))!!
    @Test fun editingSourceSnapshotRoundTripKeepsAccountLink() {
        val entry = Entry(UUID.randomUUID().toString(), EntryKind.INCOME, 1234, " 工资 ",
            LocalDate.of(2026, 10, 12), " \n 合成备注 ", UUID.randomUUID().toString())
        assertEquals(entry, entryFromSnapshot(entrySnapshot(entry)))
    }

    @Test fun accountOnlyDraftChangeIsDirtyAndRevertingRestoresCancelBaseline() {
        val initial = EntryEditorState.from(null, today)
        assertNull(initial.accountId)
        val changed = initial.copy(accountId = UUID.randomUUID().toString())
        assertTrue(changed.hasChangesFrom(initial))
        assertFalse(changed.copy(accountId = null).hasChangesFrom(initial))
        assertEquals(initial.id, changed.id)
    }

    @Test fun fromEntryAndRawDraftSaverPreserveAccountAndIncompleteInputs() {
        val entry = Entry(UUID.randomUUID().toString(), EntryKind.EXPENSE, 1234, " 餐饮 ", today, " \n 备注 ", UUID.randomUUID().toString())
        val initial = EntryEditorState.from(entry, today)
        assertEquals(entry.accountId, initial.accountId)
        val draft = initial.copy(amount = "0.", date = "2026-1", note = " \t 未完成 ")
        assertEquals(draft, roundTrip(EntryEditorStateSaver, draft))
        assertEquals(initial, roundTrip(EntryEditorStateSaver, initial))
        assertTrue(roundTrip(EntryEditorStateSaver, draft).hasChangesFrom(roundTrip(EntryEditorStateSaver, initial)))
    }

    @Test fun bothEntrySaverPathsRestoreOldSixFieldsAsNullAndRoundTripSeventh() {
        val legacy = listOf(UUID.randomUUID().toString(), "INCOME", "12.34", " 工资 ", today.toString(), " \n 原文 ")
        val draft = EntryEditorStateSaver.restore(legacy)!!
        assertNull(draft.accountId)
        assertEquals(legacy, listOf(draft.id, draft.kind.name, draft.amount, draft.category, draft.date, draft.note))
        assertNull(entryFromSnapshot(legacy.toMutableList().also { it[2] = "1234" })!!.accountId)
        listOf<String?>(null, UUID.randomUUID().toString()).forEach { accountId ->
            val savedDraft = roundTrip(EntryEditorStateSaver, draft.copy(accountId = accountId))
            assertEquals(accountId, savedDraft.accountId)
            val entry = Entry(draft.id, draft.kind, 1234, draft.category, today, draft.note, accountId)
            assertEquals(entry, entryFromSnapshot(entrySnapshot(entry)))
        }
        assertNull(entryFromSnapshot(emptyList()))
    }

    @Test fun accountInitialDraftSaverKeepsStableAccountAndRequestIdentityAndDate() {
        val initial = AccountEditorState.from(null, today)
        assertNotEquals(initial.id, initial.initialRequestId)
        val draft = initial.copy(name = " 原始名称 ", initialBalance = "-0.01", note = " \n 原始备注 ")
        val restored = roundTrip(AccountEditorStateSaver, draft)
        assertEquals(draft, restored)
        assertEquals(Account(draft.id, draft.name, draft.kind, draft.subtype, false, 0), restored.toAccount())
        assertEquals(BalanceAdjustmentRequest(draft.initialRequestId, draft.id, -1, today, draft.note, AdjustmentReason.INITIAL), restored.initialRequest())
        assertEquals(today.toString(), restored.date)
        assertNull(initial.initialRequest())
        assertNull(draft.copy(initialBalance = "0.").initialRequest())
    }

    @Test fun calibrationDraftSaverKeepsOriginalRequestAcrossRecreationAndLaterDate() {
        val initial = BalanceEditorState.create(UUID.randomUUID().toString(), today)
        val draft = initial.copy(targetBalance = "-12.34", note = " \t 原文 ")
        val restored = roundTrip(BalanceEditorStateSaver, draft)
        assertEquals(draft, restored)
        assertEquals(BalanceAdjustmentRequest(draft.requestId, draft.accountId, -1234, today, draft.note), restored.request())
        assertEquals(initial.requestId, draft.copy(targetBalance = "0.").requestId)
        assertNull(draft.copy(targetBalance = "0.").request())
        assertNull(draft.copy(date = "2026-1").request())
        assertNull(draft.copy(date = "2026-02-30").request())
    }

    @Test fun transferDraftSaverKeepsBusinessUuidDateEndpointsAndRawInputs() {
        val transfer = AccountTransfer(UUID.randomUUID().toString(), UUID.randomUUID().toString(), UUID.randomUUID().toString(), 1234, today, " 原文 ")
        val initial = TransferEditorState.from(transfer, today.plusDays(1))
        assertEquals(transfer, initial.transfer())
        assertEquals(initial, roundTrip(TransferEditorStateSaver, initial))
        val draft = initial.copy(amount = "0.", date = "2026-1", note = " \n 未完成 ")
        assertEquals(draft, roundTrip(TransferEditorStateSaver, draft))
        assertNull(draft.transfer())
        assertEquals(transfer.id, draft.id)
        assertNotEquals(transfer.id, TransferEditorState.from(null, today).id)
    }
}

package com.xiaoliumiaomiao.anjizhang

import com.xiaoliumiaomiao.anjizhang.domain.Entry
import com.xiaoliumiaomiao.anjizhang.domain.EntryKind
import com.xiaoliumiaomiao.anjizhang.ui.EntryEditorState
import java.time.LocalDate
import java.util.UUID
import org.junit.Assert.*
import org.junit.Test

class EntryEditorStateTest {
    private val today = LocalDate.of(2026, 10, 5)
    private val initial = EntryEditorState("stable-id", EntryKind.EXPENSE, "12.34", "餐饮", "2026-10-05", "午饭")

    @Test fun newDraftStartsWithEmptyCategoryAndStableUuid() {
        val draft = EntryEditorState.from(null, today)
        assertEquals(EntryKind.EXPENSE, draft.kind)
        assertEquals("", draft.amount)
        assertEquals("", draft.category)
        assertEquals(today.toString(), draft.date)
        assertEquals("", draft.note)
        assertTrue(draft.id.matches(Regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")))
        assertEquals(draft.id, UUID.fromString(draft.id).toString())
        assertEquals(draft.id, draft.copy(amount = "0.").id)
        assertNotEquals(draft.id, EntryEditorState.from(null, today).id)
    }

    @Test fun editingKeepsIdKindDateAndExactIntegerAmount() {
        val entry = Entry("existing-id", EntryKind.INCOME, 99999999999L, "工资", LocalDate.of(1900, 1, 1), "月薪")
        val draft = EntryEditorState.from(entry, today)
        assertEquals("existing-id", draft.id)
        assertEquals(EntryKind.INCOME, draft.kind)
        assertEquals("999999999.99", draft.amount)
        assertEquals("1900-01-01", draft.date)
        assertEquals("工资", draft.category)
        assertEquals("月薪", draft.note)
        assertEquals(draft, EntryEditorState.from(entry, today.plusDays(1)))
        assertEquals("0.01", EntryEditorState.from(entry.copy(amountCents = 1L), today).amount)
    }

    @Test fun editingDoesNotTrimBlankCategoryOrNote() {
        val entry = Entry("blank-id", EntryKind.EXPENSE, 100L, " \t ", today, " \n ")
        val draft = EntryEditorState.from(entry, today)
        assertEquals(entry.category, draft.category)
        assertEquals(entry.note, draft.note)
        assertEquals(entry.id, draft.id)
    }

    @Test fun rawDraftCopyKeepsIncompleteAndInvalidInputs() {
        val draft = initial.copy(amount = "0.", category = "  餐饮  ", date = "2026-1", note = " \n 备注 \t")
        assertEquals("0.", draft.amount)
        assertEquals("  餐饮  ", draft.category)
        assertEquals("2026-1", draft.date)
        assertEquals(" \n 备注 \t", draft.note)
        assertEquals(initial.id, draft.id)
    }

    @Test fun unchangedContentAndDifferentSessionIdAreNotChanges() {
        assertFalse(initial.hasChangesFrom(initial.copy()))
        assertFalse(initial.copy(id = "different-session-id").hasChangesFrom(initial))
    }

    @Test fun changingOnlyKindIsAChange() {
        assertTrue(initial.copy(kind = EntryKind.INCOME).hasChangesFrom(initial))
    }

    @Test fun changingOnlyAmountIsAChange() {
        assertTrue(initial.copy(amount = "12.3").hasChangesFrom(initial))
    }

    @Test fun changingOnlyCategoryIsAChange() {
        assertTrue(initial.copy(category = "交通").hasChangesFrom(initial))
    }

    @Test fun changingOnlyDateIsAChange() {
        assertTrue(initial.copy(date = "2026-10-04").hasChangesFrom(initial))
    }

    @Test fun changingOnlyNoteIsAChange() {
        assertTrue(initial.copy(note = "晚饭").hasChangesFrom(initial))
    }

    @Test fun rawWhitespaceChangeCountsAndRevertingRemovesChanges() {
        val changed = initial.copy(category = " 餐饮 ")
        assertTrue(changed.hasChangesFrom(initial))
        assertFalse(changed.copy(category = initial.category).hasChangesFrom(initial))
    }
}

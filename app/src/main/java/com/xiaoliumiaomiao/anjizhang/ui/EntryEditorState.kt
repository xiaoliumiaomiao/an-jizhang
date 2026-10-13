package com.xiaoliumiaomiao.anjizhang.ui

import com.xiaoliumiaomiao.anjizhang.domain.Entry
import com.xiaoliumiaomiao.anjizhang.domain.EntryKind
import com.xiaoliumiaomiao.anjizhang.domain.Money
import java.time.LocalDate
import java.util.UUID

data class EntryEditorState(
    val id: String,
    val kind: EntryKind,
    val amount: String,
    val category: String,
    val date: String,
    val note: String,
    val accountId: String? = null,
) {
    fun hasChangesFrom(initial: EntryEditorState): Boolean =
        kind != initial.kind || amount != initial.amount || category != initial.category ||
            date != initial.date || note != initial.note || accountId != initial.accountId

    companion object {
        fun from(entry: Entry?, today: LocalDate): EntryEditorState = if (entry == null) {
            EntryEditorState(UUID.randomUUID().toString(), EntryKind.EXPENSE, "", "", today.toString(), "")
        } else {
            EntryEditorState(entry.id, entry.kind, Money.plain(entry.amountCents), entry.category,
                entry.date.toString(), entry.note, entry.accountId)
        }
    }
}

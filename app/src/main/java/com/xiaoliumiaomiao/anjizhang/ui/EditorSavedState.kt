package com.xiaoliumiaomiao.anjizhang.ui

import androidx.compose.runtime.saveable.listSaver
import com.xiaoliumiaomiao.anjizhang.domain.Entry
import com.xiaoliumiaomiao.anjizhang.domain.EntryKind
import java.time.LocalDate

/** Only six primitive fields for one editor session, independent of backup JSON. */
internal val EntryEditorStateSaver = listSaver<EntryEditorState, String>(
    save = { listOf(it.id, it.kind.name, it.amount, it.category, it.date, it.note) },
    restore = { EntryEditorState(it[0], EntryKind.valueOf(it[1]), it[2], it[3], it[4], it[5]) }
)

internal fun entrySnapshot(entry: Entry?): List<String> = entry?.let {
    listOf(it.id, it.kind.name, it.amountCents.toString(), it.category, it.date.toString(), it.note)
}.orEmpty()

internal fun entryFromSnapshot(fields: List<String>): Entry? = if (fields.isEmpty()) null else
    Entry(fields[0], EntryKind.valueOf(fields[1]), fields[2].toLong(), fields[3], LocalDate.parse(fields[4]), fields[5])

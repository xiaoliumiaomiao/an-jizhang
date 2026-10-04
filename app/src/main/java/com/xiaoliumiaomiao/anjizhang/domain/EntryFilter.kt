package com.xiaoliumiaomiao.anjizhang.domain

import java.time.YearMonth

object EntryFilter {
    fun apply(entries: List<Entry>, month: YearMonth, query: String = "", kind: EntryKind? = null): List<Entry> {
        val needle = query.trim()
        return entries.filter { entry ->
            YearMonth.from(entry.date) == month &&
                (kind == null || entry.kind == kind) &&
                (needle.isEmpty() || entry.category.contains(needle, ignoreCase = true) ||
                    entry.note.contains(needle, ignoreCase = true))
        }
    }
}

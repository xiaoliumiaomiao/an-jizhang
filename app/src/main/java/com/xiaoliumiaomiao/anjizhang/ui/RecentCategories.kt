package com.xiaoliumiaomiao.anjizhang.ui

import com.xiaoliumiaomiao.anjizhang.domain.Entry
import com.xiaoliumiaomiao.anjizhang.domain.EntryKind

object RecentCategories {
    fun forKind(entries: List<Entry>, kind: EntryKind): List<String> = entries
        .filter { it.kind == kind }
        .sortedWith(compareByDescending<Entry> { it.date }.thenBy { it.id })
        .map { it.category.trim() }
        .distinct()
        .take(6)
}

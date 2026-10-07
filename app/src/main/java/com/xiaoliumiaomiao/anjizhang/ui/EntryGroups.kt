package com.xiaoliumiaomiao.anjizhang.ui

import com.xiaoliumiaomiao.anjizhang.domain.Entry
import java.time.LocalDate

data class EntryDay(val date: LocalDate, val entries: List<Entry>)

object EntryGroups {
    fun forDisplay(entries: List<Entry>): List<EntryDay> = entries.groupBy { it.date }
        .toSortedMap(compareByDescending { it })
        .map { (date, rows) -> EntryDay(date, rows.toList()) }
}

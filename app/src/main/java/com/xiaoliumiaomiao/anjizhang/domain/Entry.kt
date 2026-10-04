package com.xiaoliumiaomiao.anjizhang.domain
import java.time.LocalDate
enum class EntryKind { EXPENSE, INCOME }
data class Entry(val id: String, val kind: EntryKind, val amountCents: Long, val category: String, val date: LocalDate, val note: String = "")

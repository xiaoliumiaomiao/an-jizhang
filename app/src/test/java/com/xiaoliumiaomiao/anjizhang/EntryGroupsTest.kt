package com.xiaoliumiaomiao.anjizhang

import com.xiaoliumiaomiao.anjizhang.domain.Entry
import com.xiaoliumiaomiao.anjizhang.domain.EntryKind
import com.xiaoliumiaomiao.anjizhang.ui.EntryGroups
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class EntryGroupsTest {
    private fun entry(n: Int, day: String) = Entry(
        "01300000-0000-0000-0000-${n.toString().padStart(12, '0')}",
        EntryKind.EXPENSE,
        n.toLong(),
        "分类$n",
        LocalDate.parse(day),
        "备注\n$n"
    )

    @Test fun emptyInputHasNoGroups() {
        assertTrue(EntryGroups.forDisplay(emptyList()).isEmpty())
    }

    @Test fun groupsNewestFirstAndKeepsSameDayOrder() {
        val a = entry(1, "2026-10-02")
        val b = entry(2, "2026-10-03")
        val c = entry(3, "2026-10-02")
        val actual = EntryGroups.forDisplay(listOf(a, b, c))

        assertEquals(listOf(b.date, a.date), actual.map { it.date })
        assertEquals(listOf(a, c), actual.last().entries)
    }

    @Test fun leavesInputAndEveryEntryUnchanged() {
        val original = mutableListOf(entry(2, "2026-10-01"), entry(1, "2026-10-03"))
        val snapshot = original.toList()
        val grouped = EntryGroups.forDisplay(original)

        assertEquals(snapshot, original)
        assertEquals(snapshot.toSet(), grouped.flatMap { it.entries }.toSet())
        snapshot.forEach { expected ->
            assertSame(expected, grouped.flatMap { it.entries }.single { it.id == expected.id })
        }
    }

    @Test fun groupsAcrossMonthAndYearBoundariesWithoutDroppingRows() {
        val rows = listOf(
            entry(1, "2027-01-01"), entry(2, "2026-12-31"),
            entry(3, "1900-01-01"), entry(4, "9999-12-31")
        )

        assertEquals(
            listOf("9999-12-31", "2027-01-01", "2026-12-31", "1900-01-01"),
            EntryGroups.forDisplay(rows).map { it.date.toString() }
        )
        assertEquals(4, EntryGroups.forDisplay(rows).sumOf { it.entries.size })
    }
}

package com.xiaoliumiaomiao.anjizhang

import com.xiaoliumiaomiao.anjizhang.domain.*
import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.*
import org.junit.Test

class EntryFilterTest {
    private val month = YearMonth.of(2026, 10)
    private fun entry(n: Int, category: String = "餐饮", note: String = "", kind: EntryKind = EntryKind.EXPENSE,
                      date: LocalDate = month.atDay(1)): Entry =
        Entry("00000000-0000-0000-0000-${n.toString().padStart(12, '0')}", kind, n.toLong(), category, date, note)
    private val expense = entry(1, "午餐", "团队 Lunch")
    private val income = entry(2, "奖金", "午餐报销", EntryKind.INCOME)
    private val other = entry(3, "交通", "地铁")
    private val entries = listOf(expense, income, other)

    @Test fun selectedMonthIncludesBothEdgesAndExcludesAdjacentDays() {
        val before = entry(4, date = month.atDay(1).minusDays(1))
        val first = entry(5, date = month.atDay(1))
        val last = entry(6, date = month.atEndOfMonth())
        val after = entry(7, date = month.plusMonths(1).atDay(1))
        assertEquals(listOf(first, last), EntryFilter.apply(listOf(before, first, last, after), month))
    }
    @Test fun emptyQueryAndNullKindReturnAllSelectedMonthEntries() {
        assertEquals(entries, EntryFilter.apply(entries, month))
    }
    @Test fun whitespaceOnlyQueryIsUnrestricted() {
        assertEquals(entries, EntryFilter.apply(entries, month, " \t\n "))
    }
    @Test fun queryTrimsOnlyEdgesAndPreservesInternalWhitespace() {
        val one = entry(4, note = "a  b")
        val two = entry(5, note = "a b")
        assertEquals(listOf(one), EntryFilter.apply(listOf(one, two), month, "  a  b\n"))
    }
    @Test fun categoryOrNoteCanMatchWithoutDuplicatingAnEntry() {
        val both = entry(4, "午餐", "午餐")
        assertEquals(listOf(expense, income, both), EntryFilter.apply(entries + both, month, "午餐"))
    }
    @Test fun typeOnlySupportsExpenseIncomeAndAll() {
        assertEquals(listOf(expense, other), EntryFilter.apply(entries, month, kind = EntryKind.EXPENSE))
        assertEquals(listOf(income), EntryFilter.apply(entries, month, kind = EntryKind.INCOME))
        assertEquals(entries, EntryFilter.apply(entries, month, kind = null))
    }
    @Test fun keywordAndTypeAreBothRequired() {
        assertEquals(listOf(expense), EntryFilter.apply(entries, month, "午餐", EntryKind.EXPENSE))
        assertEquals(listOf(income), EntryFilter.apply(entries, month, "午餐", EntryKind.INCOME))
        assertTrue(EntryFilter.apply(entries, month, "地铁", EntryKind.INCOME).isEmpty())
    }
    @Test fun asciiCaseIsIgnored() {
        assertEquals(listOf(expense), EntryFilter.apply(entries, month, "lUnCh"))
    }
    @Test fun chineseAndLineBreakAreLiteralSubstrings() {
        val target = entry(4, note = "中文\n备注")
        assertEquals(listOf(target), EntryFilter.apply(listOf(target, other), month, "文\n备"))
    }
    @Test fun percentUnderscoreAndDotAreLiteral() {
        val percent = entry(4, note = "50%")
        val underscore = entry(5, note = "a_b")
        val dot = entry(6, note = "a.b")
        val plain = entry(7, note = "axb")
        val all = listOf(percent, underscore, dot, plain)
        assertEquals(listOf(percent), EntryFilter.apply(all, month, "%"))
        assertEquals(listOf(underscore), EntryFilter.apply(all, month, "_"))
        assertEquals(listOf(dot), EntryFilter.apply(all, month, "."))
        assertTrue(EntryFilter.apply(all, month, ".*").isEmpty())
    }
    @Test fun noMatchReturnsEmptyList() {
        assertTrue(EntryFilter.apply(entries, month, "不存在").isEmpty())
    }
    @Test fun filteringPreservesOrderObjectsAndOriginalInput() {
        val source = mutableListOf(income, other, expense)
        val snapshot = source.toList()
        val result = EntryFilter.apply(source, month, "午餐")
        assertEquals(listOf(income, expense), result)
        assertSame(income, result[0])
        assertSame(expense, result[1])
        assertEquals(snapshot, source)
    }
    @Test fun unicodeCharacterCaseWorksWithoutNormalizationOrExpansion() {
        val accent = entry(4, note = "CAFÉ")
        val sharpS = entry(5, note = "straße")
        val all = listOf(accent, sharpS)
        assertEquals(listOf(accent), EntryFilter.apply(all, month, "café"))
        assertTrue(EntryFilter.apply(all, month, "cafe\u0301").isEmpty())
        assertTrue(EntryFilter.apply(all, month, "STRASSE").isEmpty())
    }
    @Test fun fullLengthNoteAndPaddedQueryRemainSearchable() {
        val full = entry(4, note = "中".repeat(200))
        assertEquals(listOf(full), EntryFilter.apply(listOf(full), month, "  ${full.note}  "))
        assertTrue(EntryFilter.apply(listOf(full), month, full.note + "中").isEmpty())
    }
}

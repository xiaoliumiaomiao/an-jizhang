package com.xiaoliumiaomiao.anjizhang

import com.xiaoliumiaomiao.anjizhang.domain.Entry
import com.xiaoliumiaomiao.anjizhang.domain.EntryKind
import com.xiaoliumiaomiao.anjizhang.ui.RecentCategories
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class RecentCategoriesTest {
    private val today = LocalDate.of(2026, 10, 5)
    private fun entry(id: String, category: String, date: LocalDate = today, kind: EntryKind = EntryKind.EXPENSE) =
        Entry(id, kind, 100L, category, date, "  original note  ")

    @Test fun newestDateComesFirstAndSameDayUsesAscendingId() {
        val entries = listOf(entry("z", "旧分类", today.minusDays(1)), entry("b", "交通"), entry("a", "餐饮"))
        assertEquals(listOf("餐饮", "交通", "旧分类"), RecentCategories.forKind(entries, EntryKind.EXPENSE))
        assertEquals(listOf("餐饮", "交通", "旧分类"), RecentCategories.forKind(entries.reversed(), EntryKind.EXPENSE))
    }

    @Test fun incomeAndExpenseCategoriesStaySeparate() {
        val entries = listOf(entry("a", "工资", kind = EntryKind.INCOME), entry("b", "餐饮"),
            entry("c", "奖金", today.minusDays(1), EntryKind.INCOME))
        assertEquals(listOf("餐饮"), RecentCategories.forKind(entries, EntryKind.EXPENSE))
        assertEquals(listOf("工资", "奖金"), RecentCategories.forKind(entries, EntryKind.INCOME))
    }

    @Test fun trimsAndDeduplicatesKeepingTheMostRecentPosition() {
        val entries = listOf(entry("d", "餐饮", today.minusDays(2)), entry("c", "交通", today.minusDays(1)),
            entry("b", "\t餐饮 "), entry("a", "  购物  "))
        assertEquals(listOf("购物", "餐饮", "交通"), RecentCategories.forKind(entries, EntryKind.EXPENSE))
    }

    @Test fun limitsToSixAfterDeduplication() {
        val entries = (1..8).map { entry("id-$it", "类别$it", today.minusDays(it.toLong())) } +
            entry("latest-duplicate", " 类别1 ")
        assertEquals((1..6).map { "类别$it" }, RecentCategories.forKind(entries, EntryKind.EXPENSE))
    }

    @Test fun calculationLeavesInputOrderAndEveryEntryFieldUnchanged() {
        val entries = mutableListOf(entry("z", " 餐饮 ", today.minusDays(1)), entry("a", " 交通 "),
            entry("b", " 工资 ", kind = EntryKind.INCOME))
        val before = entries.map { it.copy() }
        assertEquals(listOf("交通", "餐饮"), RecentCategories.forKind(entries, EntryKind.EXPENSE))
        assertEquals(before, entries)
    }

    @Test fun emptyAndOtherKindOnlyInputsGiveNoRecentCategories() {
        assertEquals(emptyList<String>(), RecentCategories.forKind(emptyList(), EntryKind.EXPENSE))
        assertEquals(emptyList<String>(), RecentCategories.forKind(listOf(entry("a", "工资", kind = EntryKind.INCOME)), EntryKind.EXPENSE))
    }
}

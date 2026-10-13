package com.xiaoliumiaomiao.anjizhang.domain

import java.time.LocalDate
import java.time.YearMonth

data class TypeAnalysis(
    val totalCents: Long,
    val entryCount: Int,
    val categories: List<CategoryStatistics>,
    val largestEntries: List<Entry>
)

data class TypeComparison(val currentCents: Long, val previousCents: Long, val currentCount: Int, val previousCount: Int) {
    val deltaCents: Long get() = Math.subtractExact(currentCents, previousCents)
}

data class PeriodComparison(
    val currentStart: LocalDate,
    val currentEnd: LocalDate,
    val previousStart: LocalDate,
    val previousEnd: LocalDate,
    val income: TypeComparison,
    val expense: TypeComparison
)

data class PeriodAnalysis(
    val period: StatisticsPeriod,
    val income: TypeAnalysis,
    val expense: TypeAnalysis,
    val comparison: PeriodComparison?,
    val annual: AnnualSummary?,
    val hasFutureEntries: Boolean
) {
    val balanceCents: Long get() = Math.subtractExact(income.totalCents, expense.totalCents)

    companion object {
        fun calculate(entries: List<Entry>, period: StatisticsPeriod, today: LocalDate): PeriodAnalysis {
            val income = TypeAccumulator()
            val expense = TypeAccumulator()
            val annual = if (period.month == null) AnnualBuckets(period.year) else null
            val windows = comparisonWindows(period, today)
            val currentIncome = ComparisonTotals()
            val currentExpense = ComparisonTotals()
            val previousIncome = ComparisonTotals()
            val previousExpense = ComparisonTotals()
            var hasFutureEntries = false

            entries.forEach { entry ->
                val selected = period.contains(entry.date)
                val previous = windows != null && entry.date >= windows.previousStart && entry.date <= windows.previousEnd
                if (selected || previous) {
                    LedgerRules.validate(entry)
                    if (selected) {
                        if (entry.kind == EntryKind.INCOME) income.add(entry) else expense.add(entry)
                        annual?.add(entry)
                        if (entry.date > today) hasFutureEntries = true
                        if (windows != null && entry.date <= windows.currentEnd) {
                            if (entry.kind == EntryKind.INCOME) currentIncome.add(entry) else currentExpense.add(entry)
                        }
                    } else {
                        if (entry.kind == EntryKind.INCOME) previousIncome.add(entry) else previousExpense.add(entry)
                    }
                }
            }
            val comparison = windows?.let {
                PeriodComparison(it.currentStart, it.currentEnd, it.previousStart, it.previousEnd,
                    TypeComparison(currentIncome.amount, previousIncome.amount, currentIncome.count, previousIncome.count),
                    TypeComparison(currentExpense.amount, previousExpense.amount, currentExpense.count, previousExpense.count))
            }
            return PeriodAnalysis(period, income.result(), expense.result(), comparison, annual?.result(), hasFutureEntries)
        }

        fun drillDown(entries: List<Entry>, period: StatisticsPeriod, kind: EntryKind, category: String): List<Entry> {
            val name = category.trim()
            return entries.filter { it.kind == kind && period.contains(it.date) && it.category.trim() == name }
                .onEach { LedgerRules.validate(it) }
                .sortedWith(compareByDescending<Entry> { it.date }.thenBy { it.id })
        }

        private fun comparisonWindows(period: StatisticsPeriod, today: LocalDate): ComparisonWindows? {
            if (period.startDate > today) return null
            val previous = if (period.month == null) {
                if (period.year == 1900) return null
                StatisticsPeriod.annual(period.year - 1)
            } else {
                if (period.year == 1900 && period.month == 1) return null
                StatisticsPeriod.monthly(YearMonth.of(period.year, period.month).minusMonths(1))
            }
            if (period.endDate < today) {
                return ComparisonWindows(period.startDate, period.endDate, previous.startDate, previous.endDate)
            }
            // Current periods compare the same elapsed day count, including leap-year shifts.
            val elapsedDays = if (period.month == null) today.dayOfYear else today.dayOfMonth
            val previousDays = if (previous.month == null) previous.endDate.dayOfYear else previous.endDate.dayOfMonth
            val days = minOf(elapsedDays, previousDays).toLong()
            return ComparisonWindows(period.startDate, period.startDate.plusDays(days - 1),
                previous.startDate, previous.startDate.plusDays(days - 1))
        }
    }
}

private data class ComparisonWindows(
    val currentStart: LocalDate,
    val currentEnd: LocalDate,
    val previousStart: LocalDate,
    val previousEnd: LocalDate
)

private class ComparisonTotals {
    var amount = 0L
        private set
    var count = 0
        private set

    fun add(entry: Entry) {
        amount = Math.addExact(amount, entry.amountCents)
        count = Math.addExact(count, 1)
    }
}

private class TypeAccumulator {
    private var total = 0L
    private var count = 0
    private val categories = mutableMapOf<String, CategoryStatistics>()
    private val largest = mutableListOf<Entry>()
    private val largestOrder = compareByDescending<Entry> { it.amountCents }
        .thenByDescending { it.date }.thenBy { it.id }

    fun add(entry: Entry) {
        total = Math.addExact(total, entry.amountCents)
        count = Math.addExact(count, 1)
        val name = entry.category.trim()
        val category = categories[name]
        categories[name] = CategoryStatistics(name,
            Math.addExact(category?.amountCents ?: 0L, entry.amountCents),
            Math.addExact(category?.entryCount ?: 0, 1))
        // Keep only three entries instead of sorting the entire selected period.
        largest.add(entry)
        largest.sortWith(largestOrder)
        if (largest.size > 3) largest.removeAt(3)
    }

    fun result() = TypeAnalysis(total, count,
        categories.values.sortedWith(compareByDescending<CategoryStatistics> { it.amountCents }.thenBy { it.category }),
        largest.toList())
}

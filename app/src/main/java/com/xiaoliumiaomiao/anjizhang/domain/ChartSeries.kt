package com.xiaoliumiaomiao.anjizhang.domain

import java.time.LocalDate

/** A calendar bucket, not an individual ledger entry. Aggregated amounts may reach Long.MAX_VALUE. */
data class ChartPoint(val date: LocalDate, val incomeCents: Long, val expenseCents: Long, val entryCount: Int) {
    init {
        require(incomeCents >= 0 && expenseCents >= 0) { "类型合计不能为负数" }
        require(entryCount >= 0) { "记录数不能为负数" }
    }
    val balanceCents: Long get() = Math.subtractExact(incomeCents, expenseCents)
}

/** The caller chooses the matching calendar bucket before adding the validated entry. */
internal fun ChartPoint.withEntry(entry: Entry): ChartPoint {
    LedgerRules.validate(entry)
    return copy(
        incomeCents = if (entry.kind == EntryKind.INCOME) Math.addExact(incomeCents, entry.amountCents) else incomeCents,
        expenseCents = if (entry.kind == EntryKind.EXPENSE) Math.addExact(expenseCents, entry.amountCents) else expenseCents,
        entryCount = Math.addExact(entryCount, 1))
}

/** Exact full-period series. Missing calendar buckets are zero; no monetary value uses floating point. */
data class ChartSeries(val period: StatisticsPeriod, val points: List<ChartPoint>,
    val totalIncomeCents: Long, val totalExpenseCents: Long, val entryCount: Int) {
    init {
        require(totalIncomeCents >= 0 && totalExpenseCents >= 0) { "期间合计不能为负数" }
        require(entryCount >= 0) { "记录数不能为负数" }
    }
    val totalBalanceCents: Long get() = Math.subtractExact(totalIncomeCents, totalExpenseCents)

    companion object {
        fun calculate(entries: List<Entry>, period: StatisticsPeriod): ChartSeries {
            val size = if (period.month == null) 12 else period.endDate.dayOfMonth
            val points = MutableList(size) { index ->
                val date = if (period.month == null) LocalDate.of(period.year, index + 1, 1)
                    else period.startDate.plusDays(index.toLong())
                ChartPoint(date, 0, 0, 0)
            }
            entries.forEach { entry ->
                if (period.contains(entry.date)) {
                    val index = if (period.month == null) entry.date.monthValue - 1 else entry.date.dayOfMonth - 1
                    points[index] = points[index].withEntry(entry)
                }
            }
            var income = 0L
            var expense = 0L
            var count = 0
            points.forEach { point ->
                income = Math.addExact(income, point.incomeCents)
                expense = Math.addExact(expense, point.expenseCents)
                count = Math.addExact(count, point.entryCount)
            }
            return ChartSeries(period, points.toList(), income, expense, count)
        }
    }
}

/** A null category is an honest aggregate of remaining categories and never a drill-down category. */
data class CategoryChartSlice(val category: String?, val amountCents: Long, val entryCount: Int,
    val categoryCount: Int = 1) {
    init {
        require(amountCents > 0 && entryCount > 0 && categoryCount > 0)
    }
    val label: String get() = category ?: "其余（${categoryCount}类）"
}

/** Uses already validated/sorted TypeAnalysis. The remainder is exact subtraction, not another ledger scan. */
fun categoryChartSlices(analysis: TypeAnalysis, maxNamedCategories: Int = 5): List<CategoryChartSlice> {
    require(maxNamedCategories > 0)
    require(analysis.totalCents >= 0 && analysis.entryCount >= 0)
    if (analysis.categories.isEmpty()) {
        require(analysis.totalCents == 0L && analysis.entryCount == 0)
        return emptyList()
    }
    require(analysis.totalCents > 0 && analysis.entryCount > 0)
    val named = analysis.categories.take(maxNamedCategories).map {
        CategoryChartSlice(it.category, it.amountCents, it.entryCount)
    }
    var namedAmount = 0L
    var namedCount = 0
    named.forEach {
        namedAmount = Math.addExact(namedAmount, it.amountCents)
        namedCount = Math.addExact(namedCount, it.entryCount)
    }
    val remainingAmount = Math.subtractExact(analysis.totalCents, namedAmount)
    val remainingCount = Math.subtractExact(analysis.entryCount, namedCount)
    val remainingCategories = analysis.categories.size - named.size
    return if (remainingCategories == 0) {
        require(remainingAmount == 0L && remainingCount == 0) { "分类与类型合计不一致" }
        named
    } else {
        require(remainingAmount > 0 && remainingCount >= remainingCategories) { "分类与类型合计不一致" }
        named + CategoryChartSlice(null, remainingAmount, remainingCount, remainingCategories)
    }
}

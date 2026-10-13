package com.xiaoliumiaomiao.anjizhang.domain

import java.time.YearMonth

data class MonthTotals(val month: YearMonth, val incomeCents: Long, val expenseCents: Long, val entryCount: Int) {
    val balanceCents: Long get() = Math.subtractExact(incomeCents, expenseCents)
}

data class AnnualSummary(val year: Int, val months: List<MonthTotals>) {
    val incomeCents: Long get() = months.fold(0L) { total, month -> Math.addExact(total, month.incomeCents) }
    val expenseCents: Long get() = months.fold(0L) { total, month -> Math.addExact(total, month.expenseCents) }
    val balanceCents: Long get() = Math.subtractExact(incomeCents, expenseCents)

    companion object {
        fun calculate(entries: List<Entry>, year: Int): AnnualSummary {
            val period = StatisticsPeriod.annual(year)
            val buckets = AnnualBuckets(year)
            entries.forEach { entry ->
                if (period.contains(entry.date)) {
                    LedgerRules.validate(entry)
                    buckets.add(entry)
                }
            }
            return buckets.result()
        }
    }
}

/** Shared with period analysis so annual mode still traverses the ledger only once. */
internal class AnnualBuckets(private val year: Int) {
    private val income = LongArray(12)
    private val expense = LongArray(12)
    private val count = IntArray(12)

    fun add(entry: Entry) {
        val index = entry.date.monthValue - 1
        when (entry.kind) {
            EntryKind.INCOME -> income[index] = Math.addExact(income[index], entry.amountCents)
            EntryKind.EXPENSE -> expense[index] = Math.addExact(expense[index], entry.amountCents)
        }
        count[index] = Math.addExact(count[index], 1)
    }

    fun result() = AnnualSummary(year, (1..12).map { month ->
        MonthTotals(YearMonth.of(year, month), income[month - 1], expense[month - 1], count[month - 1])
    })
}

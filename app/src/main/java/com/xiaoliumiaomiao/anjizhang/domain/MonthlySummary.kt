package com.xiaoliumiaomiao.anjizhang.domain

import java.time.YearMonth

data class MonthlySummary(val incomeCents: Long, val expenseCents: Long, val expensesByCategory: Map<String, Long>) {
    val balanceCents: Long get() = Math.subtractExact(incomeCents, expenseCents)

    companion object {
        fun calculate(entries: List<Entry>, month: YearMonth): MonthlySummary {
            var income = 0L
            var expense = 0L
            val categories = sortedMapOf<String, Long>()
            entries.filter { YearMonth.from(it.date) == month }.forEach {
                LedgerRules.validate(it)
                when (it.kind) {
                    EntryKind.INCOME -> income = Math.addExact(income, it.amountCents)
                    EntryKind.EXPENSE -> {
                        expense = Math.addExact(expense, it.amountCents)
                        val category = it.category.trim()
                        categories[category] = Math.addExact(categories[category] ?: 0, it.amountCents)
                    }
                }
            }
            return MonthlySummary(income, expense, categories.toMap())
        }
    }
}

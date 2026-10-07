package com.xiaoliumiaomiao.anjizhang

import com.xiaoliumiaomiao.anjizhang.domain.*
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class PeriodAnalysisTest {
    private fun entry(id: Int, amount: Long, category: String = "餐饮", date: String = "2026-10-02", kind: EntryKind = EntryKind.EXPENSE) =
        Entry("00000000-0000-0000-0000-${id.toString().padStart(12, '0')}", kind, amount, category, LocalDate.parse(date), "备注$id")
    private val october = StatisticsPeriod(2026, 10)
    private val november = LocalDate.of(2026, 11, 1)

    @Test fun sampleConstructorAndIndependentExpectedTotalsMatchFullPeriod() {
        val rows = listOf(entry(1, 10000, "工资", "2026-10-01", EntryKind.INCOME),
            entry(2, 1234, " 餐饮 "), entry(3, 1, "交通"), entry(4, 100, "餐饮外卖", "2026-10-03"),
            entry(5, 2000, "餐饮", "2026-09-30"))
        assertEquals(EntryKind.INCOME, rows[0].kind)
        assertEquals(10000L, rows[0].amountCents)
        assertEquals("工资", rows[0].category)
        assertEquals(LocalDate.of(2026, 10, 1), rows[0].date)
        val result = PeriodAnalysis.calculate(rows, october, november)
        assertEquals(10000L, result.income.totalCents)
        assertEquals(1335L, result.expense.totalCents)
        assertEquals(8665L, result.balanceCents)
        assertEquals(listOf(rows[1]), PeriodAnalysis.drillDown(rows, october, EntryKind.EXPENSE, "餐饮"))
        val annual = AnnualSummary.calculate(rows, 2026)
        assertEquals(12, annual.months.size)
        assertEquals(3335L, annual.expenseCents)
        assertEquals(6665L, annual.balanceCents)
        assertEquals(2000L, annual.months[8].expenseCents)
        assertEquals(1335L, annual.months[9].expenseCents)
    }

    @Test fun categoriesAreExactTrimmedCaseSensitiveAndRankedPerKind() {
        val rows = listOf(entry(1, 10, "\u2003Food\u3000"), entry(2, 20, "Food"), entry(3, 30, "food"),
            entry(4, 30, "%. _"), entry(5, 30, "餐饮外卖"), entry(6, 900, "Food", kind = EntryKind.INCOME),
            entry(7, 100, "收入", kind = EntryKind.INCOME))
        val result = PeriodAnalysis.calculate(rows, october, november)
        assertEquals(listOf("%. _", "Food", "food", "餐饮外卖"), result.expense.categories.map { it.category })
        assertEquals(listOf(30L, 30L, 30L, 30L), result.expense.categories.map { it.amountCents })
        assertEquals(2, result.expense.categories.first { it.category == "Food" }.entryCount)
        assertEquals(120L, result.expense.categories.sumOf { it.amountCents })
        assertEquals(1000L, result.income.categories.sumOf { it.amountCents })
        assertEquals(listOf("Food", "收入"), result.income.categories.map { it.category })
        assertEquals(listOf(rows[0], rows[1]), PeriodAnalysis.drillDown(rows.reversed(), october, EntryKind.EXPENSE, "\u3000Food\u2003"))
        assertEquals(listOf(rows[3]), PeriodAnalysis.drillDown(rows, october, EntryKind.EXPENSE, "%. _"))
        assertEquals(listOf(rows[2]), PeriodAnalysis.drillDown(rows, october, EntryKind.EXPENSE, "food"))
    }

    @Test fun largestThreeUseAmountThenNewestDateThenAscendingId() {
        val rows = listOf(entry(4, 100, date = "2026-10-02"), entry(3, 100, date = "2026-10-03"),
            entry(2, 100, date = "2026-10-03"), entry(1, 101, date = "2026-10-01"), entry(5, 99),
            entry(6, 200, kind = EntryKind.INCOME))
        val result = PeriodAnalysis.calculate(rows, october, november)
        assertEquals(listOf(rows[3], rows[2], rows[1]), result.expense.largestEntries)
        assertEquals(listOf(rows[5]), result.income.largestEntries)
        assertEquals(5, result.expense.entryCount)
        assertEquals(500L, result.expense.totalCents)
    }

    @Test fun drillDownUsesExactPeriodKindAndCategoryWithStableOrder() {
        val rows = listOf(entry(4, 10, "餐饮外卖"), entry(3, 10, "餐饮", "2026-10-03"),
            entry(2, 10, " 餐饮 ", "2026-10-03"), entry(1, 10, "餐饮", "2026-10-01"),
            entry(5, 10, "餐饮", "2026-09-30"), entry(6, 10, "餐饮", kind = EntryKind.INCOME))
        assertEquals(listOf(rows[2], rows[1], rows[3]), PeriodAnalysis.drillDown(rows, october, EntryKind.EXPENSE, "餐饮"))
        assertEquals(listOf(rows[5]), PeriodAnalysis.drillDown(rows, october, EntryKind.INCOME, "餐饮"))
        assertEquals(4, PeriodAnalysis.drillDown(rows, StatisticsPeriod.annual(2026), EntryKind.EXPENSE, "餐饮").size)
        assertTrue(PeriodAnalysis.drillDown(rows, october, EntryKind.EXPENSE, "不存在").isEmpty())
    }

    @Test fun calculationsAndDrillDownPreserveOriginalObjectsOrderAndFields() {
        val rows = mutableListOf(entry(2, 20, " 餐饮 "), entry(1, 10, "餐饮", kind = EntryKind.INCOME))
        val before = rows.map { it.copy() }
        val result = PeriodAnalysis.calculate(rows, october, november)
        val drill = PeriodAnalysis.drillDown(rows, october, EntryKind.EXPENSE, "餐饮")
        AnnualSummary.calculate(rows, 2026)
        assertEquals(before, rows)
        assertEquals(1, result.expense.largestEntries.size)
        assertEquals(1, drill.size)
        assertSame(rows[0], result.expense.largestEntries.single())
        assertSame(rows[0], drill.single())
        rows.clear()
        assertEquals(20L, result.expense.totalCents)
        assertEquals(1, drill.size)
    }

    @Test fun emptyAnnualSummaryHasAllTwelveZeroMonths() {
        val result = PeriodAnalysis.calculate(emptyList(), StatisticsPeriod.annual(2024), LocalDate.of(2025, 1, 1))
        val annual = result.annual!!
        assertEquals((1..12).map { YearMonth.of(2024, it) }, annual.months.map { it.month })
        assertTrue(annual.months.all { it.incomeCents == 0L && it.expenseCents == 0L && it.balanceCents == 0L && it.entryCount == 0 })
        assertEquals(0L, annual.incomeCents)
        assertEquals(0L, annual.expenseCents)
        assertEquals(0L, annual.balanceCents)
        assertEquals(0, result.income.entryCount)
        assertTrue(result.expense.categories.isEmpty())
        assertTrue(result.expense.largestEntries.isEmpty())
        assertFalse(result.hasFutureEntries)
        assertNull(PeriodAnalysis.calculate(emptyList(), october, november).annual)
    }

    @Test fun annualSummaryIncludesYearEdgesLeapDayAndBothTypesOnce() {
        val rows = listOf(entry(1, 10, date = "2023-12-31"), entry(2, 20, date = "2024-01-01"),
            entry(3, 30, date = "2024-02-29", kind = EntryKind.INCOME), entry(4, 40, date = "2024-12-31"),
            entry(5, 50, date = "2025-01-01"))
        val annual = AnnualSummary.calculate(rows, 2024)
        assertEquals(30L, annual.incomeCents)
        assertEquals(60L, annual.expenseCents)
        assertEquals(-30L, annual.balanceCents)
        assertEquals(3, annual.months.sumOf { it.entryCount })
        assertEquals(30L, annual.months[1].incomeCents)
        assertEquals(40L, annual.months[11].expenseCents)
    }

    @Test fun currentMarchComparisonUsesTwentyEightDaysAndKeepsFullOverview() {
        val rows = listOf(entry(1, 10, date = "2026-03-01"), entry(2, 20, date = "2026-03-28"),
            entry(3, 300, date = "2026-03-29"), entry(4, 400, date = "2026-03-31"),
            entry(5, 50, date = "2026-02-01"), entry(6, 60, date = "2026-02-28"))
        val result = PeriodAnalysis.calculate(rows, StatisticsPeriod(2026, 3), LocalDate.of(2026, 3, 31))
        val comparison = result.comparison!!
        assertEquals(LocalDate.of(2026, 3, 1), comparison.currentStart)
        assertEquals(LocalDate.of(2026, 3, 28), comparison.currentEnd)
        assertEquals(LocalDate.of(2026, 2, 1), comparison.previousStart)
        assertEquals(LocalDate.of(2026, 2, 28), comparison.previousEnd)
        assertEquals(730L, result.expense.totalCents)
        assertEquals(30L, comparison.expense.currentCents)
        assertEquals(110L, comparison.expense.previousCents)
        assertEquals(-80L, comparison.expense.deltaCents)
        assertEquals(2, comparison.expense.currentCount)
        assertEquals(2, comparison.expense.previousCount)
    }

    @Test fun currentLeapYearComparisonUsesSameElapsedDays() {
        val rows = listOf(entry(1, 10, date = "2024-02-29"), entry(2, 900, date = "2024-03-01"),
            entry(3, 20, date = "2023-03-01"), entry(4, 800, date = "2023-03-02"))
        val result = PeriodAnalysis.calculate(rows, StatisticsPeriod.annual(2024), LocalDate.of(2024, 2, 29))
        val comparison = result.comparison!!
        assertEquals(LocalDate.of(2024, 1, 1), comparison.currentStart)
        assertEquals(LocalDate.of(2024, 2, 29), comparison.currentEnd)
        assertEquals(LocalDate.of(2023, 1, 1), comparison.previousStart)
        assertEquals(LocalDate.of(2023, 3, 1), comparison.previousEnd)
        assertEquals(910L, result.expense.totalCents)
        assertEquals(10L, comparison.expense.currentCents)
        assertEquals(20L, comparison.expense.previousCents)
        assertTrue(result.hasFutureEntries)
    }

    @Test fun leapYearLastDayClampsBothComparisonWindowsTo365Days() {
        val rows = listOf(entry(1, 10, date = "2024-12-30"), entry(2, 100, date = "2024-12-31"),
            entry(3, 20, date = "2023-12-31"))
        val result = PeriodAnalysis.calculate(rows, StatisticsPeriod.annual(2024), LocalDate.of(2024, 12, 31))
        assertEquals(LocalDate.of(2024, 12, 30), result.comparison!!.currentEnd)
        assertEquals(LocalDate.of(2023, 12, 31), result.comparison!!.previousEnd)
        assertEquals(10L, result.comparison!!.expense.currentCents)
        assertEquals(110L, result.expense.totalCents)
    }

    @Test fun historicalMonthComparesFullUnequalMonthLengths() {
        val rows = listOf(entry(1, 10, date = "2026-03-31"), entry(2, 20, date = "2026-02-28"))
        val comparison = PeriodAnalysis.calculate(rows, StatisticsPeriod(2026, 3), LocalDate.of(2026, 4, 1)).comparison!!
        assertEquals(LocalDate.of(2026, 3, 31), comparison.currentEnd)
        assertEquals(LocalDate.of(2026, 2, 28), comparison.previousEnd)
        assertEquals(10L, comparison.expense.currentCents)
        assertEquals(20L, comparison.expense.previousCents)
    }

    @Test fun historicalYearComparesBothCompleteYears() {
        val rows = listOf(entry(1, 10, date = "2024-12-31"), entry(2, 20, date = "2023-12-31"))
        val comparison = PeriodAnalysis.calculate(rows, StatisticsPeriod.annual(2024), LocalDate.of(2025, 1, 1)).comparison!!
        assertEquals(LocalDate.of(2024, 12, 31), comparison.currentEnd)
        assertEquals(LocalDate.of(2023, 12, 31), comparison.previousEnd)
        assertEquals(10L, comparison.expense.currentCents)
        assertEquals(20L, comparison.expense.previousCents)
    }

    @Test fun futurePeriodAndFirstYearBoundaryHaveNoComparison() {
        assertNull(PeriodAnalysis.calculate(emptyList(), StatisticsPeriod(2026, 11), LocalDate.of(2026, 10, 7)).comparison)
        assertNull(PeriodAnalysis.calculate(emptyList(), StatisticsPeriod.annual(2027), november).comparison)
        assertNull(PeriodAnalysis.calculate(emptyList(), StatisticsPeriod(1900, 1), november).comparison)
        assertNull(PeriodAnalysis.calculate(emptyList(), StatisticsPeriod.annual(1900), november).comparison)
    }

    @Test fun february1900CanCompareJanuaryAnd9999DoesNotOverflowDates() {
        val first = PeriodAnalysis.calculate(emptyList(), StatisticsPeriod(1900, 2), LocalDate.of(1900, 2, 28)).comparison!!
        assertEquals(LocalDate.of(1900, 1, 1), first.previousStart)
        assertEquals(LocalDate.of(1900, 1, 28), first.previousEnd)
        val last = PeriodAnalysis.calculate(listOf(entry(1, 1, date = "9999-12-31")), StatisticsPeriod.annual(9999), LocalDate.of(9999, 12, 31))
        assertEquals(LocalDate.of(9999, 12, 31), last.comparison!!.currentEnd)
        assertEquals(LocalDate.of(9998, 12, 31), last.comparison!!.previousEnd)
        assertEquals(1L, last.annual!!.expenseCents)
    }

    @Test fun futureEntriesAreFlaggedOnlyInsideSelectedPeriodAndExcludedFromElapsedComparison() {
        val rows = listOf(entry(1, 10, date = "2026-10-07"), entry(2, 20, date = "2026-10-08"),
            entry(3, 30, date = "2026-11-01"), entry(4, 40, date = "2026-09-08"))
        val result = PeriodAnalysis.calculate(rows, october, LocalDate.of(2026, 10, 7))
        assertTrue(result.hasFutureEntries)
        assertEquals(30L, result.expense.totalCents)
        assertEquals(10L, result.comparison!!.expense.currentCents)
        assertEquals(0L, result.comparison!!.expense.previousCents)
        assertFalse(PeriodAnalysis.calculate(listOf(rows[0], rows[2]), october, LocalDate.of(2026, 10, 7)).hasFutureEntries)
    }

    @Test fun comparisonCountsDistinguishEmptyTypeFromMissingPreviousType() {
        val empty = PeriodAnalysis.calculate(emptyList(), october, november).comparison!!
        assertEquals(TypeComparison(0, 0, 0, 0), empty.income)
        assertEquals(TypeComparison(0, 0, 0, 0), empty.expense)
        val result = PeriodAnalysis.calculate(listOf(entry(1, 1)), october, november).comparison!!
        assertEquals(TypeComparison(1, 0, 1, 0), result.expense)
        assertNull(StatisticsNumbers.percentage(result.expense.deltaCents, result.expense.previousCents))
    }

    @Test fun validationChecksAnalysisAndComparisonParticipantsOnly() {
        val invalid = entry(1, 0)
        assertThrows(IllegalArgumentException::class.java) { PeriodAnalysis.calculate(listOf(invalid), october, november) }
        assertThrows(IllegalArgumentException::class.java) { PeriodAnalysis.calculate(listOf(invalid.copy(date = LocalDate.of(2026, 9, 1))), october, november) }
        val outside = invalid.copy(id = "bad", date = LocalDate.of(2025, 1, 1))
        assertEquals(0L, PeriodAnalysis.calculate(listOf(outside), october, november).expense.totalCents)
        assertEquals(0L, AnnualSummary.calculate(listOf(outside), 2026).expenseCents)
        assertThrows(IllegalArgumentException::class.java) { AnnualSummary.calculate(listOf(invalid), 2026) }
        assertThrows(IllegalArgumentException::class.java) { AnnualSummary.calculate(emptyList(), 10000) }
    }

    @Test fun drillDownValidatesMatchingEntriesOnly() {
        val matching = entry(1, 0)
        assertThrows(IllegalArgumentException::class.java) { PeriodAnalysis.drillDown(listOf(matching), october, EntryKind.EXPENSE, "餐饮") }
        val irrelevant = listOf(matching.copy(category = "其他"), matching.copy(kind = EntryKind.INCOME),
            matching.copy(date = LocalDate.of(2026, 9, 1)))
        assertTrue(PeriodAnalysis.drillDown(irrelevant, october, EntryKind.EXPENSE, "餐饮").isEmpty())
    }

    @Test fun allEntryValidationFieldsAreEnforcedWithoutMutation() {
        val good = entry(1, 1)
        val invalid = listOf(good.copy(id = "bad"), good.copy(amountCents = Money.MAX_CENTS + 1),
            good.copy(category = " "), good.copy(category = "长".repeat(21)), good.copy(note = "x".repeat(201)))
        invalid.forEach { row ->
            assertThrows(IllegalArgumentException::class.java) { PeriodAnalysis.calculate(listOf(row), october, november) }
            assertThrows(IllegalArgumentException::class.java) { AnnualSummary.calculate(listOf(row), 2026) }
            assertThrows(IllegalArgumentException::class.java) { PeriodAnalysis.drillDown(listOf(row), october, EntryKind.EXPENSE, row.category) }
        }
        assertEquals(1L, PeriodAnalysis.calculate(listOf(good.copy(amountCents = Money.MAX_CENTS)), october, november).expense.entryCount.toLong())
    }
}

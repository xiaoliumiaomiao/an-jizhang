package com.xiaoliumiaomiao.anjizhang

import com.xiaoliumiaomiao.anjizhang.domain.*
import com.xiaoliumiaomiao.anjizhang.ui.chartAmountFraction
import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.*
import org.junit.Test

class ChartSeriesTest {
    private fun entry(id: Int, kind: EntryKind, cents: Long, date: String) = Entry(
        "01700000-0000-0000-0000-${id.toString().padStart(12, '0')}",
        kind, cents, "图表合成", LocalDate.parse(date))
    private val may = StatisticsPeriod.monthly(YearMonth.of(2025, 5))

    @Test fun monthlySeriesKeepsEveryCalendarDayAndExactIndependentTotals() {
        val records = listOf(
            entry(1, EntryKind.INCOME, 5000, "2025-05-02"),
            entry(2, EntryKind.INCOME, 25, "2025-05-02"),
            entry(3, EntryKind.EXPENSE, 125, "2025-05-02"),
            entry(4, EntryKind.EXPENSE, 400, "2025-05-31"),
            entry(5, EntryKind.EXPENSE, 700, "2025-04-30"),
            entry(6, EntryKind.INCOME, 700, "2024-05-02"),
            entry(7, EntryKind.EXPENSE, 5, "2025-06-01"))
        val series = ChartSeries.calculate(records, may)
        assertEquals((1..31).map { LocalDate.of(2025, 5, it) }, series.points.map { it.date })
        assertEquals(ChartPoint(LocalDate.of(2025, 5, 2), 5025, 125, 3), series.points[1])
        assertEquals(4900L, series.points[1].balanceCents)
        assertEquals(ChartPoint(LocalDate.of(2025, 5, 31), 0, 400, 1), series.points.last())
        assertEquals(-400L, series.points.last().balanceCents)
        assertEquals(5025L, series.totalIncomeCents)
        assertEquals(525L, series.totalExpenseCents)
        assertEquals(4500L, series.totalBalanceCents)
        assertEquals(4, series.entryCount)
        assertTrue(series.points.filterIndexed { index, _ -> index != 1 && index != 30 }
            .all { it.incomeCents == 0L && it.expenseCents == 0L && it.entryCount == 0 })
    }

    @Test fun annualSeriesContainsTwelveChronologicalBucketsAndYearEdges() {
        val records = listOf(
            entry(1, EntryKind.INCOME, 15, "2025-01-01"),
            entry(2, EntryKind.INCOME, 10, "2025-01-31"),
            entry(3, EntryKind.EXPENSE, 1, "2025-01-01"),
            entry(4, EntryKind.EXPENSE, 30, "2025-12-31"),
            entry(5, EntryKind.INCOME, 999, "2024-12-31"),
            entry(6, EntryKind.EXPENSE, 999, "2026-01-01"))
        val series = ChartSeries.calculate(records.reversed(), StatisticsPeriod.annual(2025))
        assertEquals((1..12).map { LocalDate.of(2025, it, 1) }, series.points.map { it.date })
        assertEquals(ChartPoint(LocalDate.of(2025, 1, 1), 25, 1, 3), series.points.first())
        assertEquals(ChartPoint(LocalDate.of(2025, 12, 1), 0, 30, 1), series.points.last())
        assertEquals(25L, series.totalIncomeCents)
        assertEquals(31L, series.totalExpenseCents)
        assertEquals(-6L, series.totalBalanceCents)
        assertEquals(4, series.entryCount)
    }

    @Test fun leapDaysSingleActiveDayAndSupportedDateEdgesRemainExact() {
        val leap = ChartSeries.calculate(listOf(entry(1, EntryKind.EXPENSE, 1, "2024-02-29")),
            StatisticsPeriod(2024, 2))
        assertEquals(29, leap.points.size)
        assertEquals(ChartPoint(LocalDate.of(2024, 2, 29), 0, 1, 1), leap.points.last())
        assertEquals(-1L, leap.totalBalanceCents)
        assertEquals(1, leap.points.count { it.entryCount != 0 })
        assertEquals(28, ChartSeries.calculate(emptyList(), StatisticsPeriod(1900, 2)).points.size)
        val last = ChartSeries.calculate(listOf(entry(2, EntryKind.INCOME, 1, "9999-12-31")),
            StatisticsPeriod.annual(9999))
        assertEquals(LocalDate.of(9999, 12, 1), last.points.last().date)
        assertEquals(1L, last.points.last().incomeCents)
    }

    @Test fun emptyPeriodsHaveHonestZeroBucketsAndNoRecords() {
        for (period in listOf(may, StatisticsPeriod.annual(2025))) {
            val series = ChartSeries.calculate(emptyList(), period)
            assertEquals(if (period.month == null) 12 else 31, series.points.size)
            assertEquals(0L, series.totalIncomeCents)
            assertEquals(0L, series.totalExpenseCents)
            assertEquals(0L, series.totalBalanceCents)
            assertEquals(0, series.entryCount)
            assertTrue(series.points.all { it.incomeCents == 0L && it.expenseCents == 0L && it.entryCount == 0 })
        }
    }

    @Test fun seriesMatchesCompletePeriodAnalysisRatherThanElapsedComparisonWindow() {
        val records = mutableListOf(
            entry(1, EntryKind.INCOME, 2000, "2026-10-01"),
            entry(2, EntryKind.EXPENSE, 5000, "2026-10-31"),
            entry(3, EntryKind.EXPENSE, 100, "2026-09-30"))
        val snapshot = records.toList()
        val period = StatisticsPeriod(2026, 10)
        val analysis = PeriodAnalysis.calculate(records, period, LocalDate.of(2026, 10, 1))
        val series = ChartSeries.calculate(records, period)
        assertEquals(2000L, series.totalIncomeCents)
        assertEquals(5000L, series.totalExpenseCents)
        assertEquals(-3000L, series.totalBalanceCents)
        assertEquals(analysis.income.totalCents, series.totalIncomeCents)
        assertEquals(analysis.expense.totalCents, series.totalExpenseCents)
        assertEquals(2, series.entryCount)
        assertEquals(5000L, series.points.last().expenseCents)
        assertEquals(snapshot, records)
        records.clear()
        assertEquals(31, series.points.size)
        assertEquals(5000L, series.totalExpenseCents)
    }

    @Test fun invalidSelectedRecordsAreRejectedAndIrrelevantRecordsAreIgnored() {
        val good = entry(1, EntryKind.EXPENSE, 1, "2025-05-01")
        for (invalid in listOf(good.copy(amountCents = 0), good.copy(amountCents = -1),
            good.copy(amountCents = Long.MAX_VALUE), good.copy(id = "bad"))) {
            assertThrows(IllegalArgumentException::class.java) { ChartSeries.calculate(listOf(invalid), may) }
        }
        val outside = good.copy(id = "bad", amountCents = -1, date = LocalDate.of(2024, 1, 1))
        val empty = ChartSeries.calculate(listOf(outside), may)
        assertEquals(31, empty.points.size)
        assertEquals(0, empty.entryCount)
    }

    @Test fun pointAccumulationUsesExactCentsAndRejectsArithmeticOverflow() {
        val date = LocalDate.of(2025, 5, 1)
        val point = ChartPoint(date, 0, 0, 0)
            .withEntry(entry(1, EntryKind.INCOME, 25, "2025-05-01"))
            .withEntry(entry(2, EntryKind.EXPENSE, 50, "2025-05-01"))
        assertEquals(ChartPoint(date, 25, 50, 2), point)
        assertEquals(-25L, point.balanceCents)
        assertThrows(ArithmeticException::class.java) {
            ChartPoint(date, Long.MAX_VALUE, 0, 1).withEntry(entry(3, EntryKind.INCOME, 1, "2025-05-01"))
        }
        assertThrows(ArithmeticException::class.java) {
            ChartPoint(date, 0, Long.MAX_VALUE, 1).withEntry(entry(4, EntryKind.EXPENSE, 1, "2025-05-01"))
        }
        assertThrows(ArithmeticException::class.java) {
            ChartPoint(date, 0, 0, Int.MAX_VALUE).withEntry(entry(5, EntryKind.INCOME, 1, "2025-05-01"))
        }
    }

    @Test fun aggregatePointsAllowLongMaximumAndSignedBalancesButRejectNegativeTypeAmounts() {
        val date = LocalDate.of(2025, 5, 1)
        assertEquals(Long.MAX_VALUE, ChartPoint(date, Long.MAX_VALUE, 0, 1).balanceCents)
        assertEquals(-Long.MAX_VALUE, ChartPoint(date, 0, Long.MAX_VALUE, 1).balanceCents)
        assertEquals(0L, ChartPoint(date, Long.MAX_VALUE, Long.MAX_VALUE, 2).balanceCents)
        assertEquals("¥92233720368547758.07", Money.format(Long.MAX_VALUE))
        assertThrows(IllegalArgumentException::class.java) { ChartPoint(date, -1, 0, 1) }
        assertThrows(IllegalArgumentException::class.java) { ChartPoint(date, 0, -1, 1) }
        assertThrows(IllegalArgumentException::class.java) { ChartPoint(date, 0, 0, -1) }
    }

    @Test fun visualRatiosAreFiniteForZeroSinglePointAndLongMaximum() {
        assertEquals(0f, chartAmountFraction(0, 0), 0f)
        assertEquals(0f, chartAmountFraction(1, 0), 0f)
        assertEquals(0f, chartAmountFraction(-1, 100), 0f)
        assertEquals(1f, chartAmountFraction(1, 1), 0f)
        assertEquals(.75f, chartAmountFraction(75, 100), 0f)
        assertEquals(1f, chartAmountFraction(Long.MAX_VALUE, Long.MAX_VALUE), 0f)
        assertEquals(.5f, chartAmountFraction(Long.MAX_VALUE / 2, Long.MAX_VALUE), .000001f)
        assertTrue(chartAmountFraction(1, Long.MAX_VALUE).isFinite())
        assertTrue(chartAmountFraction(1, Long.MAX_VALUE) > 0f)
        assertEquals(1f, chartAmountFraction(200, 100), 0f)
    }

    private fun type(categories: List<CategoryStatistics>, total: Long, count: Int) =
        TypeAnalysis(total, count, categories, emptyList())

    @Test fun donutSlicesKeepTopCategoriesAndExactRemainderWithoutInventingDrillCategory() {
        val categories = listOf(40L, 30L, 20L, 10L, 9L, 8L, 7L)
            .mapIndexed { index, amount -> CategoryStatistics("类别${index + 1}", amount, 1) }
        val slices = categoryChartSlices(type(categories, 124, 7))
        assertEquals(6, slices.size)
        assertEquals(categories.take(5).map { it.category }, slices.take(5).map { it.category })
        assertEquals(listOf(40L, 30L, 20L, 10L, 9L, 15L), slices.map { it.amountCents })
        assertEquals(124L, slices.fold(0L) { total, slice -> Math.addExact(total, slice.amountCents) })
        assertNull(slices.last().category)
        assertEquals("其余（2类）", slices.last().label)
        assertEquals(2, slices.last().categoryCount)
        assertEquals(2, slices.last().entryCount)
    }

    @Test fun emptySingleAndRealOtherCategoryRemainHonest() {
        assertTrue(categoryChartSlices(type(emptyList(), 0, 0)).isEmpty())
        val single = categoryChartSlices(type(listOf(CategoryStatistics("其他", 1, 1)), 1, 1))
        assertEquals(1, single.size)
        assertEquals("其他", single.single().category)
        assertEquals(1L, single.single().amountCents)
        assertEquals(1, single.single().categoryCount)
        assertThrows(IllegalArgumentException::class.java) {
            categoryChartSlices(type(listOf(CategoryStatistics("坏数据", 5, 1)), 1, 1))
        }
    }

    @Test fun donutRemainderPreservesLongMaximumWithoutFloatingMoney() {
        val categories = listOf(CategoryStatistics("大类", Long.MAX_VALUE - 6, 1)) +
            (1..6).map { CategoryStatistics("小类$it", 1, 1) }
        val slices = categoryChartSlices(type(categories, Long.MAX_VALUE, 7))
        assertEquals(6, slices.size)
        assertEquals(Long.MAX_VALUE - 6, slices.first().amountCents)
        assertEquals(2L, slices.last().amountCents)
        assertEquals(Long.MAX_VALUE, slices.fold(0L) { total, slice -> Math.addExact(total, slice.amountCents) })
    }
}

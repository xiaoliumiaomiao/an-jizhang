package com.xiaoliumiaomiao.anjizhang

import com.xiaoliumiaomiao.anjizhang.domain.*
import org.junit.Assert.*
import org.junit.Test
import java.time.YearMonth

class StatisticsNumbersTest {
    @Test fun percentagesHaveOneDecimalAndHalfUpRounding() {
        assertEquals("37.0%", StatisticsNumbers.percentage(37, 100))
        assertEquals("16.7%", StatisticsNumbers.percentage(1, 6))
        assertEquals("12.5%", StatisticsNumbers.percentage(1, 8))
        assertEquals("0.0%", StatisticsNumbers.percentage(0, 8))
        assertEquals("-16.7%", StatisticsNumbers.percentage(-1, 6))
    }

    @Test fun percentagesDoNotMultiplyLongBeforeDecimalConversion() {
        assertEquals("100.0%", StatisticsNumbers.percentage(Long.MAX_VALUE, Long.MAX_VALUE))
        assertEquals("922337203685477580700.0%", StatisticsNumbers.percentage(Long.MAX_VALUE, 1))
        assertEquals("-922337203685477580800.0%", StatisticsNumbers.percentage(Long.MIN_VALUE, 1))
    }

    @Test fun zeroAndNegativeComparisonBaseHaveNoPercentage() {
        assertNull(StatisticsNumbers.percentage(10, 0))
        assertNull(StatisticsNumbers.percentage(10, -1))
    }

    @Test fun averagesRoundIntegerCentsHalfUpIncludingOneCent() {
        assertEquals(1L, StatisticsNumbers.averageCents(1, 2))
        assertEquals(0L, StatisticsNumbers.averageCents(1, 3))
        assertEquals(2L, StatisticsNumbers.averageCents(5, 3))
        assertEquals(Long.MAX_VALUE, StatisticsNumbers.averageCents(Long.MAX_VALUE, 1))
        assertEquals(4611686018427387904L, StatisticsNumbers.averageCents(Long.MAX_VALUE, 2))
        assertNull(StatisticsNumbers.averageCents(0, 0))
    }

    @Test fun negativeAverageCountIsRejected() {
        assertThrows(IllegalArgumentException::class.java) { StatisticsNumbers.averageCents(1, -1) }
    }

    @Test fun comparisonAndBalanceUseExactSubtraction() {
        assertEquals(-20L, TypeComparison(10, 30, 1, 1).deltaCents)
        assertEquals(-30L, MonthTotals(YearMonth.of(2026, 10), 0, 30, 1).balanceCents)
        assertThrows(ArithmeticException::class.java) { TypeComparison(Long.MAX_VALUE, -1, 0, 0).deltaCents }
        assertThrows(ArithmeticException::class.java) { MonthTotals(YearMonth.of(2026, 10), Long.MIN_VALUE, 1, 0).balanceCents }
    }

    @Test fun annualAggregateRejectsOverflowRatherThanWrapping() {
        val months = listOf(
            MonthTotals(YearMonth.of(2026, 1), Long.MAX_VALUE, 0, 1),
            MonthTotals(YearMonth.of(2026, 2), 1, 0, 1))
        assertThrows(ArithmeticException::class.java) { AnnualSummary(2026, months).incomeCents }
        assertThrows(ArithmeticException::class.java) { AnnualSummary(2026, months.map { it.copy(incomeCents = 0, expenseCents = it.incomeCents) }).expenseCents }
        assertThrows(ArithmeticException::class.java) {
            AnnualSummary(2026, listOf(MonthTotals(YearMonth.of(2026, 1), Long.MIN_VALUE, 1, 0))).balanceCents
        }
    }
}

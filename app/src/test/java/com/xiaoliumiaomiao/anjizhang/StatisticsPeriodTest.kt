package com.xiaoliumiaomiao.anjizhang

import com.xiaoliumiaomiao.anjizhang.domain.StatisticsPeriod
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class StatisticsPeriodTest {
    @Test fun monthlyPeriodIncludesBothEdgesOnly() {
        val period = StatisticsPeriod.monthly(YearMonth.of(2026, 10))
        assertEquals(LocalDate.of(2026, 10, 1), period.startDate)
        assertEquals(LocalDate.of(2026, 10, 31), period.endDate)
        assertTrue(period.contains(period.startDate))
        assertTrue(period.contains(period.endDate))
        assertFalse(period.contains(period.startDate.minusDays(1)))
        assertFalse(period.contains(period.endDate.plusDays(1)))
    }

    @Test fun leapFebruaryAndCenturyFebruaryUseCalendarRules() {
        assertEquals(LocalDate.of(2024, 2, 29), StatisticsPeriod(2024, 2).endDate)
        assertEquals(LocalDate.of(1900, 2, 28), StatisticsPeriod(1900, 2).endDate)
    }

    @Test fun annualPeriodSupportsBothYearLimitsWithoutNextYear() {
        val first = StatisticsPeriod.annual(1900)
        val last = StatisticsPeriod.annual(9999)
        assertEquals(LocalDate.of(1900, 1, 1), first.startDate)
        assertEquals(LocalDate.of(1900, 12, 31), first.endDate)
        assertEquals(LocalDate.of(9999, 1, 1), last.startDate)
        assertEquals(LocalDate.of(9999, 12, 31), last.endDate)
        assertEquals(LocalDate.of(9999, 12, 31), StatisticsPeriod(9999, 12).endDate)
    }

    @Test fun invalidYearAndMonthAreRejectedAtConstruction() {
        listOf(1899, 10000, Int.MIN_VALUE, Int.MAX_VALUE).forEach {
            assertThrows(IllegalArgumentException::class.java) { StatisticsPeriod(it) }
        }
        listOf(0, 13, -1, Int.MAX_VALUE).forEach {
            assertThrows(IllegalArgumentException::class.java) { StatisticsPeriod(2026, it) }
        }
    }
}

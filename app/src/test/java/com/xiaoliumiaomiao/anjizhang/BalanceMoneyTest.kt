package com.xiaoliumiaomiao.anjizhang

import com.xiaoliumiaomiao.anjizhang.domain.BalanceMoney
import com.xiaoliumiaomiao.anjizhang.domain.Money
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BalanceMoneyTest {
    @Test fun acceptsZeroAndNegativeBalances() {
        assertEquals(0L, BalanceMoney.parse("0"))
        assertEquals(0L, BalanceMoney.parse("-0.00"))
        assertEquals(-1234L, BalanceMoney.parse("-12.34"))
        assertEquals(-1L, BalanceMoney.parse("-0.01"))
    }

    @Test fun acceptsPositiveBalancesAboveSingleEntryCap() {
        assertEquals(Money.MAX_CENTS + 1, BalanceMoney.parse("1000000000"))
        assertEquals(-Money.MAX_CENTS - 1, BalanceMoney.parse("-1000000000.00"))
    }

    @Test fun parsesLongBoundariesExactlyWithoutFloatingPoint() {
        assertEquals(Long.MAX_VALUE, BalanceMoney.parse("92233720368547758.07"))
        assertEquals(-Long.MAX_VALUE, BalanceMoney.parse("-92233720368547758.07"))
        assertEquals(100L, BalanceMoney.parse("1.00"))
        assertEquals(120L, BalanceMoney.parse("1.2"))
    }

    @Test fun rejectsLongMinAndOutOfRangeBalances() {
        for (raw in listOf("-92233720368547758.08", "92233720368547758.08", "92233720368547759", "-92233720368547759")) {
            assertNull(raw, BalanceMoney.parse(raw))
        }
    }

    @Test fun rejectsInvalidOrOverPreciseInput() {
        for (raw in listOf("", "-", "+1", "--1", "1.234", "-0.001", "1.", ".01", "1e2", "1,00", "NaN", "∞", "1 0", "−1", "１２")) {
            assertNull(raw, BalanceMoney.parse(raw))
        }
    }

    @Test fun acceptsWhitespaceAndLeadingZeros() {
        assertEquals(-1L, BalanceMoney.parse("  -" + "0".repeat(40) + "0.01  "))
        assertEquals(0L, BalanceMoney.parse("000.00"))
    }

    @Test fun keepsExpenseIncomeParserContract() {
        assertNull(Money.parse("0"))
        assertNull(Money.parse("-1"))
        assertNull(Money.parse("1000000000"))
        assertEquals(1L, Money.parse("0.01"))
    }
}

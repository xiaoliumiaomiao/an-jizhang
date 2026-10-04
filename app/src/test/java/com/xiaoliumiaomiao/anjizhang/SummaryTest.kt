package com.xiaoliumiaomiao.anjizhang
import com.xiaoliumiaomiao.anjizhang.domain.*
import org.junit.Assert.*
import org.junit.Test
import java.time.*
class SummaryTest {
    @Test fun monthIsolatedAndCategorized() {
        val date=LocalDate.of(2026,10,1)
        val x=listOf(Entry("00000000-0000-0000-0000-000000000001",EntryKind.INCOME,10000,"工资",date),Entry("00000000-0000-0000-0000-000000000002",EntryKind.EXPENSE,1234,"餐饮",date),Entry("00000000-0000-0000-0000-000000000003",EntryKind.EXPENSE,1,"交通",date),Entry("00000000-0000-0000-0000-000000000004",EntryKind.EXPENSE,500,"餐饮",date.minusMonths(1)))
        val s=MonthlySummary.calculate(x,YearMonth.of(2026,10))
        assertEquals(10000,s.incomeCents);assertEquals(1235,s.expenseCents);assertEquals(8765,s.balanceCents);assertEquals(mapOf("餐饮" to 1234L,"交通" to 1L),s.expensesByCategory)
        assertEquals(0,MonthlySummary.calculate(x,YearMonth.of(2026,9)).incomeCents)
    }
    @Test fun balanceCanBeNegative() { assertEquals(-50,MonthlySummary(0,50,emptyMap()).balanceCents) }
}

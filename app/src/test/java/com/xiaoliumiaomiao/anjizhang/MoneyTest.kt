package com.xiaoliumiaomiao.anjizhang
import com.xiaoliumiaomiao.anjizhang.domain.*
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
class MoneyTest {
    @Test fun preciseCents() { assertEquals(1234L,Money.parse("12.34")); assertEquals(1L,Money.parse(" 0.01 ")); assertEquals(Money.MAX_CENTS,Money.parse("999999999.99")) }
    @Test fun acceptsLeadingZeros() { assertEquals(1L,Money.parse("0".repeat(40)+"0.01")) }
    @Test fun invalidAmount() { for(x in listOf("0","-1","1.234","1e2","1000000000","1,00","NaN","", ".01")) assertNull(x,Money.parse(x)) }
    @Test fun exactFormatting() { assertEquals("999999999.99",Money.plain(Money.MAX_CENTS)); assertEquals("-0.01",Money.plain(-1)); assertEquals("¥12.34",Money.format(1234)); assertEquals("-92233720368547758.08",Money.plain(Long.MIN_VALUE)) }
    @Test fun validatesRecords() {
        val valid = Entry("00000000-0000-0000-0000-000000000001",EntryKind.EXPENSE,1,"餐饮",LocalDate.of(2026,10,1))
        LedgerRules.validate(valid)
        for(x in listOf(valid.copy(id="1-1-1-1-1"),valid.copy(id="AAAAAAAA-AAAA-AAAA-AAAA-AAAAAAAAAAAA"),valid.copy(amountCents=0),valid.copy(amountCents=Money.MAX_CENTS+1),valid.copy(category=" "),valid.copy(category="x".repeat(21)),valid.copy(note="x".repeat(201)),valid.copy(date=LocalDate.of(1899,1,1)))) assertThrows(IllegalArgumentException::class.java) { LedgerRules.validate(x) }
    }
}

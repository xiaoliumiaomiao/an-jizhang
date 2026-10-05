package com.xiaoliumiaomiao.anjizhang

import com.xiaoliumiaomiao.anjizhang.domain.Money
import com.xiaoliumiaomiao.anjizhang.ui.AmountKeypadInput
import org.junit.Assert.*
import org.junit.Test

class AmountKeypadInputTest {
    private fun enter(value: String): String = value.fold("") { text, key -> AmountKeypadInput.append(text, key) }
    @Test fun decimalDraftAndOneCentKeepExactMoney() {
        assertEquals("0.", AmountKeypadInput.append("", '.'))
        assertEquals("0", enter("000"))
        assertEquals("12", enter("012"))
        assertNull(Money.parse(enter("0")))
        assertEquals("0.01", enter("0.01"))
        assertEquals(1L, Money.parse(enter("0.01")))
        assertEquals("12.34", enter("12.34"))
    }
    @Test fun repeatedDecimalAndExcessPrecisionCannotChangeValidAmount() {
        assertEquals("12.34", enter("12..345"))
        assertEquals("12.34", AmountKeypadInput.append("12.34", '-'))
        assertEquals("12.34", AmountKeypadInput.append("12.34", 'a'))
        assertEquals("999999999.99", enter("999999999.99"))
        assertEquals(Money.MAX_CENTS, Money.parse(enter("999999999.99")))
        assertEquals("999999999", enter("9999999999"))
    }
    @Test fun backspaceEditsTailWithoutChangingOtherDigits() {
        assertEquals("12.3", AmountKeypadInput.backspace("12.34"))
        assertEquals("12.35", AmountKeypadInput.append(AmountKeypadInput.backspace("12.34"), '5'))
        assertEquals("0", AmountKeypadInput.backspace("0."))
        assertEquals("", AmountKeypadInput.backspace("0"))
        assertEquals("", AmountKeypadInput.backspace(""))
    }
}

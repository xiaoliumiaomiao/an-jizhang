package com.xiaoliumiaomiao.anjizhang

import com.xiaoliumiaomiao.anjizhang.domain.*
import com.xiaoliumiaomiao.anjizhang.ui.*
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class AccountInputTest {
    @Test fun debtMagnitudeBecomesNegativeNetAssets() {
        assertEquals(-12345L, parseBalanceInput("123.45", AccountKind.LIABILITY, LiabilityBalanceMode.DEBT))
    }
    @Test fun overpaymentMagnitudeStaysPositiveNetAssets() {
        assertEquals(12345L, parseBalanceInput("123.45", AccountKind.LIABILITY, LiabilityBalanceMode.OVERPAYMENT))
    }
    @Test fun bothLiabilityModesAcceptZero() {
        LiabilityBalanceMode.entries.forEach { assertEquals(0L, parseBalanceInput("0.00", AccountKind.LIABILITY, it)) }
    }
    @Test fun liabilityRejectsNegativeMagnitudeIncludingNegativeZero() {
        LiabilityBalanceMode.entries.forEach { mode ->
            assertNull(parseBalanceInput("-100", AccountKind.LIABILITY, mode))
            assertNull(parseBalanceInput(" -0.00 ", AccountKind.LIABILITY, mode))
        }
    }
    @Test fun assetAllowsOverdraftAndZero() {
        assertEquals(-10000L, parseBalanceInput("-100", AccountKind.ASSET, LiabilityBalanceMode.DEBT))
        assertEquals(0L, parseBalanceInput("0", AccountKind.ASSET, LiabilityBalanceMode.DEBT))
    }
    @Test fun balancesUseLongRangeInsteadOfEntryAmountCap() {
        assertEquals(Long.MAX_VALUE, parseBalanceInput("92233720368547758.07", AccountKind.ASSET, LiabilityBalanceMode.DEBT))
        assertEquals(-Long.MAX_VALUE, parseBalanceInput("92233720368547758.07", AccountKind.LIABILITY, LiabilityBalanceMode.DEBT))
        assertEquals(Long.MAX_VALUE, parseBalanceInput("92233720368547758.07", AccountKind.LIABILITY, LiabilityBalanceMode.OVERPAYMENT))
        assertEquals(-Long.MAX_VALUE, parseBalanceInput("-92233720368547758.07", AccountKind.ASSET, LiabilityBalanceMode.DEBT))
    }
    @Test fun overflowAndMoreThanTwoDecimalsAreRejected() {
        listOf("92233720368547758.08", "-92233720368547758.08", "1.001", "", " ", "+1", "1e2").forEach { raw ->
            assertNull(parseBalanceInput(raw, AccountKind.ASSET, LiabilityBalanceMode.DEBT))
            assertNull(parseBalanceInput(raw, AccountKind.LIABILITY, LiabilityBalanceMode.DEBT))
        }
    }
    @Test fun untouchedHistoricCategoryAndAccountLinkSurviveFinalConversion() {
        val date = LocalDate.of(2020, 2, 29)
        val source = Entry("entry-id", EntryKind.EXPENSE, 101L, " 旧分类 ", date, "旧备注", "archived-account")
        val candidate = entryFromEditorDraft(EntryEditorState.from(source, date).copy(note = "更新备注"))!!
        assertEquals(" 旧分类 ", candidate.category)
        assertEquals("archived-account", candidate.accountId)
        assertEquals("entry-id", candidate.id)
        assertEquals("更新备注", candidate.note)
    }
    @Test fun explicitlySelectedCategoryUsesItsNameAndUnassignedIsNull() {
        val draft = EntryEditorState.from(null, LocalDate.of(2026, 10, 12)).copy(amount = "0.01", category = "目录类别")
        val entry = entryFromEditorDraft(draft)!!
        assertEquals("目录类别", entry.category)
        assertNull(entry.accountId)
        assertEquals(1L, entry.amountCents)
    }
}

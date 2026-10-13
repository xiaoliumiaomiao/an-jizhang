package com.xiaoliumiaomiao.anjizhang

import com.xiaoliumiaomiao.anjizhang.domain.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.LocalDate

class AccountBalancesTest {
    private val today = LocalDate.of(2026, 10, 12)
    private val bank = account(1, "银行卡")
    private val cash = account(2, "现金")
    private val debt = account(3, "信用卡", AccountKind.LIABILITY, AccountSubtype.CREDIT_CARD)

    @Test fun incomeAddsAndExpenseSubtractsAssetBalance() {
        val snapshot = LedgerSnapshot(
            accounts = listOf(bank, cash),
            entries = listOf(entry(100, EntryKind.INCOME, 15000, bank.id), entry(101, EntryKind.EXPENSE, 2300, bank.id)),
        )
        assertEquals(mapOf(bank.id to 12700L, cash.id to 0L), AccountBalances.balances(snapshot, today))
    }

    @Test fun legacyUnassignedEntriesDoNotInventAccountBalances() {
        val legacy = entry(100, EntryKind.INCOME, 15000, null)
        assertEquals(null, legacy.accountId)
        assertEquals(mapOf(bank.id to 0L), AccountBalances.balances(LedgerSnapshot(entries = listOf(legacy), accounts = listOf(bank)), today))
    }

    @Test fun liabilityPurchaseAndRefundUseNetAssetSign() {
        val snapshot = LedgerSnapshot(accounts = listOf(debt), entries = listOf(
            entry(100, EntryKind.EXPENSE, 10000, debt.id), entry(101, EntryKind.INCOME, 2500, debt.id),
        ))
        assertEquals(-7500L, AccountBalances.balances(snapshot, today).getValue(debt.id))
    }

    @Test fun loanBorrowingMovesFromLiabilityToAssetWithoutIncome() {
        val snapshot = LedgerSnapshot(accounts = listOf(bank, debt), transfers = listOf(transfer(200, debt.id, bank.id, 50000)))
        assertEquals(mapOf(bank.id to 50000L, debt.id to -50000L), AccountBalances.balances(snapshot, today))
        assertEquals(AccountTotals(50000, 50000, 0), AccountBalances.totals(snapshot, today))
    }

    @Test fun repayingLoanMovesAssetToLiabilityWithoutExpense() {
        val snapshot = LedgerSnapshot(accounts = listOf(bank, debt), transfers = listOf(
            transfer(200, debt.id, bank.id, 50000), transfer(201, bank.id, debt.id, 12000),
        ))
        assertEquals(mapOf(bank.id to 38000L, debt.id to -38000L), AccountBalances.balances(snapshot, today))
    }

    @Test fun assetTransferChangesBothAccountsAndKeepsNetAssets() {
        val snapshot = LedgerSnapshot(accounts = listOf(bank, cash), adjustments = listOf(adjustment(300, bank.id, 10000)),
            transfers = listOf(transfer(200, bank.id, cash.id, 3500)))
        assertEquals(mapOf(bank.id to 6500L, cash.id to 3500L), AccountBalances.balances(snapshot, today))
        assertEquals(AccountTotals(10000, 0, 10000), AccountBalances.totals(snapshot, today))
    }

    @Test fun totalsUseActualSignsAndIncludeArchivedAccounts() {
        val snapshot = LedgerSnapshot(accounts = listOf(bank.copy(archived = true), debt, cash), adjustments = listOf(
            adjustment(300, bank.id, -10000), adjustment(301, debt.id, 5000), adjustment(302, cash.id, 0),
        ))
        assertEquals(AccountTotals(5000, 10000, -5000), AccountBalances.totals(snapshot, today))
    }

    @Test fun futureEventsAreExcludedUntilTheirDate() {
        val snapshot = LedgerSnapshot(accounts = listOf(bank, cash),
            entries = listOf(entry(100, EntryKind.INCOME, 700, bank.id).copy(date = today.plusDays(1))),
            transfers = listOf(transfer(200, bank.id, cash.id, 200).copy(date = today.plusDays(2))),
            adjustments = listOf(adjustment(300, cash.id, 50).copy(date = today.plusDays(3))))
        assertEquals(mapOf(bank.id to 0L, cash.id to 0L), AccountBalances.balances(snapshot, today))
        assertEquals(mapOf(bank.id to 700L, cash.id to 0L), AccountBalances.balances(snapshot, today.plusDays(1)))
        assertEquals(mapOf(bank.id to 500L, cash.id to 200L), AccountBalances.balances(snapshot, today.plusDays(2)))
        assertEquals(mapOf(bank.id to 500L, cash.id to 250L), AccountBalances.balances(snapshot, today.plusDays(3)))
    }

    @Test fun adjustmentUsesOriginalDeltaWhenHistoryChanges() {
        val original = LedgerSnapshot(accounts = listOf(bank), entries = listOf(entry(100, EntryKind.INCOME, 1000, bank.id)),
            adjustments = listOf(adjustment(300, bank.id, 400).copy(targetBalanceCents = 1400)))
        assertEquals(1400L, AccountBalances.balances(original, today).getValue(bank.id))
        assertEquals(2400L, AccountBalances.balances(original.copy(entries = listOf(original.entries.single().copy(amountCents = 2000))), today).getValue(bank.id))
        assertEquals(400L, AccountBalances.balances(original.copy(entries = emptyList()), today).getValue(bank.id))
    }

    @Test fun adjustmentCanExceedSingleEntryCap() {
        val delta = Money.MAX_CENTS + 1
        val snapshot = LedgerSnapshot(accounts = listOf(bank), adjustments = listOf(adjustment(300, bank.id, delta)))
        AccountBalances.validateSnapshot(snapshot)
        assertEquals(delta, AccountBalances.balances(snapshot, today).getValue(bank.id))
    }

    @Test fun sameDateEventsCancelExactlyBeforeRangeValidation() {
        val snapshot = LedgerSnapshot(accounts = listOf(bank), adjustments = listOf(
            adjustment(300, bank.id, Long.MAX_VALUE), adjustment(301, bank.id, Long.MAX_VALUE), adjustment(302, bank.id, -Long.MAX_VALUE),
        ), entries = listOf(entry(100, EntryKind.INCOME, 1, bank.id), entry(101, EntryKind.EXPENSE, 1, bank.id)))
        AccountBalances.validateSnapshot(snapshot)
        assertEquals(Long.MAX_VALUE, AccountBalances.balances(snapshot, today).getValue(bank.id))
    }

    @Test fun sameDateTransferLegsDoNotCauseIntermediateTotalsOverflow() {
        val snapshot = LedgerSnapshot(accounts = listOf(bank, cash), adjustments = listOf(adjustment(300, bank.id, Long.MAX_VALUE)),
            transfers = listOf(transfer(200, bank.id, cash.id, 1)))
        AccountBalances.validateSnapshot(snapshot)
        assertEquals(AccountTotals(Long.MAX_VALUE, 0, Long.MAX_VALUE), AccountBalances.totals(snapshot, today))
    }

    @Test fun oppositeSignedLongBoundariesSumExactly() {
        val snapshot = LedgerSnapshot(accounts = listOf(bank, debt), adjustments = listOf(
            adjustment(300, bank.id, Long.MAX_VALUE), adjustment(301, debt.id, -Long.MAX_VALUE),
        ))
        assertEquals(AccountTotals(Long.MAX_VALUE, Long.MAX_VALUE, 0), AccountBalances.totals(snapshot, today))
    }

    @Test fun rejectsAccountOverflowAtEarlierDateEvenIfLaterCancelled() {
        val snapshot = LedgerSnapshot(accounts = listOf(bank), adjustments = listOf(
            adjustment(300, bank.id, Long.MAX_VALUE).copy(date = today.minusDays(2)),
            adjustment(301, bank.id, 1).copy(date = today.minusDays(1)), adjustment(302, bank.id, -1),
        ))
        rejects { AccountBalances.validateSnapshot(snapshot) }
    }

    @Test fun rejectsAssetTotalOverflowWithIndividuallyValidAccounts() {
        rejects { AccountBalances.validateSnapshot(LedgerSnapshot(accounts = listOf(bank, cash), adjustments = listOf(
            adjustment(300, bank.id, Long.MAX_VALUE), adjustment(301, cash.id, 1),
        ))) }
    }

    @Test fun rejectsDebtTotalOverflowWithIndividuallyValidAccounts() {
        rejects { AccountBalances.validateSnapshot(LedgerSnapshot(accounts = listOf(bank, cash), adjustments = listOf(
            adjustment(300, bank.id, -Long.MAX_VALUE), adjustment(301, cash.id, -1),
        ))) }
    }

    @Test fun rejectsLongMinAccountBalance() {
        rejects { AccountBalances.validateSnapshot(LedgerSnapshot(accounts = listOf(bank), adjustments = listOf(
            adjustment(300, bank.id, -Long.MAX_VALUE), adjustment(301, bank.id, -1),
        ))) }
    }

    @Test fun rejectsLongMinAdjustmentDeltaAndTarget() {
        rejects { LedgerRules.validate(adjustment(300, bank.id, Long.MIN_VALUE)) }
        rejects { LedgerRules.validate(adjustment(300, bank.id, 0).copy(targetBalanceCents = Long.MIN_VALUE)) }
        rejects { LedgerRules.validate(BalanceAdjustmentRequest(id(300), bank.id, Long.MIN_VALUE, today)) }
    }

    @Test fun archivedAccountsRemainValidHistoricalReferences() {
        val snapshot = LedgerSnapshot(accounts = listOf(bank.copy(archived = true), cash.copy(archived = true)),
            entries = listOf(entry(100, EntryKind.INCOME, 500, bank.id)),
            transfers = listOf(transfer(200, bank.id, cash.id, 100)), adjustments = listOf(adjustment(300, bank.id, 50)))
        AccountBalances.validateSnapshot(snapshot)
        assertEquals(mapOf(bank.id to 450L, cash.id to 100L), AccountBalances.balances(snapshot, today))
    }

    @Test fun rejectsOrphanAccountReferences() {
        val base = LedgerSnapshot(accounts = listOf(bank))
        for (invalid in listOf(
            base.copy(entries = listOf(entry(100, EntryKind.INCOME, 1, cash.id))),
            base.copy(transfers = listOf(transfer(200, bank.id, cash.id, 1))),
            base.copy(transfers = listOf(transfer(200, cash.id, bank.id, 1))),
            base.copy(adjustments = listOf(adjustment(300, cash.id, 0))),
        )) rejects { AccountBalances.validateSnapshot(invalid) }
    }

    @Test fun rejectsDuplicateIdsInsideEachEntityArray() {
        val oneEntry = entry(100, EntryKind.EXPENSE, 1, bank.id)
        val oneTransfer = transfer(200, bank.id, cash.id, 1)
        val oneAdjustment = adjustment(300, bank.id, 0)
        val oneCategory = Category(id(400), EntryKind.EXPENSE, "餐饮")
        val base = LedgerSnapshot(accounts = listOf(bank, cash))
        for (invalid in listOf(
            base.copy(accounts = listOf(bank, bank)), base.copy(entries = listOf(oneEntry, oneEntry)),
            base.copy(transfers = listOf(oneTransfer, oneTransfer)), base.copy(adjustments = listOf(oneAdjustment, oneAdjustment)),
            base.copy(categories = listOf(oneCategory, oneCategory)),
        )) rejects { AccountBalances.validateSnapshot(invalid) }
    }

    @Test fun sameUuidAcrossDifferentEntityArraysIsAllowed() {
        val snapshot = LedgerSnapshot(accounts = listOf(bank, cash), entries = listOf(entry(1, EntryKind.INCOME, 10, bank.id)),
            transfers = listOf(transfer(1, bank.id, cash.id, 1)), adjustments = listOf(adjustment(1, bank.id, 0)),
            categories = listOf(Category(bank.id, EntryKind.INCOME, "工资")))
        AccountBalances.validateSnapshot(snapshot)
        assertEquals(mapOf(bank.id to 9L, cash.id to 1L), AccountBalances.balances(snapshot, today))
    }

    @Test fun categoryNamesAreUniqueWithinKindAfterTrimming() {
        val category = Category(id(400), EntryKind.EXPENSE, "餐饮")
        rejects { AccountBalances.validateSnapshot(LedgerSnapshot(categories = listOf(category, category.copy(id = id(401), name = " 餐饮 ", archived = true)))) }
        AccountBalances.validateSnapshot(LedgerSnapshot(categories = listOf(category, category.copy(id = id(401), kind = EntryKind.INCOME))))
    }

    @Test fun validatesCanonicalUuidsForEveryEntityAndReference() {
        for (bad in listOf("1-1-1-1-1", "AAAAAAAA-AAAA-AAAA-AAAA-AAAAAAAAAAAA", "", " " + bank.id)) {
            rejects { LedgerRules.validate(bank.copy(id = bad)) }
            rejects { LedgerRules.validate(Category(bad, EntryKind.EXPENSE, "餐饮")) }
            rejects { LedgerRules.validate(transfer(200, bank.id, cash.id, 1).copy(id = bad)) }
            rejects { LedgerRules.validate(transfer(200, bank.id, cash.id, 1).copy(fromAccountId = bad)) }
            rejects { LedgerRules.validate(transfer(200, bank.id, cash.id, 1).copy(toAccountId = bad)) }
            rejects { LedgerRules.validate(adjustment(300, bank.id, 0).copy(id = bad)) }
            rejects { LedgerRules.validate(adjustment(300, bank.id, 0).copy(accountId = bad)) }
            rejects { LedgerRules.validate(entry(100, EntryKind.INCOME, 1, bad)) }
            rejects { LedgerRules.validate(BalanceAdjustmentRequest(bad, bank.id, 0, today)) }
            rejects { LedgerRules.validate(BalanceAdjustmentRequest(id(300), bad, 0, today)) }
        }
    }

    @Test fun validatesAccountNameAndNonNegativeSortOrder() {
        LedgerRules.validate(bank.copy(name = "x".repeat(30), subtype = AccountSubtype.OTHER))
        LedgerRules.validate(debt.copy(subtype = AccountSubtype.OTHER))
        for (invalid in listOf(bank.copy(name = " "), bank.copy(name = "x".repeat(31)), bank.copy(sortOrder = -1))) {
            rejects { LedgerRules.validate(invalid) }
        }
    }

    @Test fun validatesCategoryNameWithLegacyTwentyCharacterLimit() {
        LedgerRules.validate(Category(id(400), EntryKind.EXPENSE, "x".repeat(20)))
        for (invalid in listOf(Category(id(400), EntryKind.EXPENSE, " "), Category(id(400), EntryKind.EXPENSE, "x".repeat(21)),
            Category(id(400), EntryKind.EXPENSE, "餐饮", sortOrder = -1))) rejects { LedgerRules.validate(invalid) }
    }

    @Test fun transfersRequireDistinctAccountsAndPositiveCappedAmount() {
        val valid = transfer(200, bank.id, cash.id, Money.MAX_CENTS)
        LedgerRules.validate(valid)
        for (invalid in listOf(valid.copy(toAccountId = bank.id), valid.copy(amountCents = 0), valid.copy(amountCents = -1),
            valid.copy(amountCents = Money.MAX_CENTS + 1))) rejects { LedgerRules.validate(invalid) }
    }

    @Test fun validatesNotesAndSupportedDatesForTransfersAndAdjustments() {
        val validTransfer = transfer(200, bank.id, cash.id, 1)
        val validAdjustment = adjustment(300, bank.id, 0)
        val validRequest = BalanceAdjustmentRequest(id(300), bank.id, 0, today)
        for (date in listOf(LocalDate.of(1900, 1, 1), LocalDate.of(9999, 12, 31))) {
            LedgerRules.validate(validTransfer.copy(date = date, note = "x".repeat(200)))
            LedgerRules.validate(validAdjustment.copy(date = date, note = "x".repeat(200)))
            LedgerRules.validate(validRequest.copy(date = date, note = "x".repeat(200)))
        }
        for (date in listOf(LocalDate.of(1899, 12, 31), LocalDate.of(10000, 1, 1))) {
            rejects { LedgerRules.validate(validTransfer.copy(date = date)) }
            rejects { LedgerRules.validate(validAdjustment.copy(date = date)) }
            rejects { LedgerRules.validate(validRequest.copy(date = date)) }
        }
        rejects { LedgerRules.validate(validTransfer.copy(note = "x".repeat(201))) }
        rejects { LedgerRules.validate(validAdjustment.copy(note = "x".repeat(201))) }
        rejects { LedgerRules.validate(validRequest.copy(note = "x".repeat(201))) }
    }

    @Test fun snapshotValidatesEveryEntityEvenWithoutFinancialEvents() {
        rejects { AccountBalances.validateSnapshot(LedgerSnapshot(accounts = listOf(bank.copy(name = "")))) }
        rejects { AccountBalances.validateSnapshot(LedgerSnapshot(categories = listOf(Category(id(400), EntryKind.EXPENSE, "")))) }
        rejects { AccountBalances.validateSnapshot(LedgerSnapshot(entries = listOf(entry(100, EntryKind.INCOME, 0, null)))) }
    }

    @Test fun balanceAndTotalsRejectMalformedSnapshotsAndDates() {
        val invalid = LedgerSnapshot(accounts = listOf(bank), entries = listOf(entry(100, EntryKind.INCOME, 1, cash.id)))
        rejects { AccountBalances.balances(invalid, today) }
        rejects { AccountBalances.totals(invalid, today) }
        rejects { AccountBalances.balances(LedgerSnapshot(), LocalDate.of(1899, 12, 31)) }
        rejects { AccountBalances.totals(LedgerSnapshot(), LocalDate.of(10000, 1, 1)) }
    }

    private fun id(value: Int): String = "00000000-0000-0000-0000-" + value.toString().padStart(12, '0')
    private fun account(value: Int, name: String, kind: AccountKind = AccountKind.ASSET, subtype: AccountSubtype = AccountSubtype.BANK) =
        Account(id(value), name, kind, subtype)
    private fun entry(value: Int, kind: EntryKind, cents: Long, accountId: String?) = Entry(id(value), kind, cents, "餐饮", today, accountId = accountId)
    private fun transfer(value: Int, from: String, to: String, cents: Long) = AccountTransfer(id(value), from, to, cents, today)
    private fun adjustment(value: Int, accountId: String, delta: Long) = AccountAdjustment(id(value), accountId, delta, today, delta)
    private fun rejects(action: () -> Unit) { assertThrows(IllegalArgumentException::class.java, action) }
}

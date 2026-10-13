package com.xiaoliumiaomiao.anjizhang

import android.content.Context
import android.content.ContextWrapper
import android.database.DatabaseErrorHandler
import android.database.sqlite.SQLiteDatabase
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import com.xiaoliumiaomiao.anjizhang.data.BackupCodec
import com.xiaoliumiaomiao.anjizhang.data.LedgerDatabase
import com.xiaoliumiaomiao.anjizhang.domain.*
import com.xiaoliumiaomiao.anjizhang.ui.LedgerViewModel
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import java.io.File
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

/** Only the new app runs this class. No fixture reset, legacy reseed or destructive rebuild. */
class LedgerSchema2UpgradeTest {
    private val compose = createAndroidComposeRule<MainActivity>()
    private val originalDatabase = object : ExternalResource() {
        override fun before() {
            LedgerRawUpgradeSeedTest.assertSyntheticDevice()
            val (baseline, code) = LedgerRawUpgradeSeedTest.baselineIdentity()
            val context = LedgerRawUpgradeSeedTest.installedVersion("0.1.8", 10)
            // This gate precedes the Activity rule and every SQLiteOpenHelper open.
            assertTrue("The old installed ledger must exist before launching the new Activity",
                context.getDatabasePath("ledger.db").isFile)
            val schema = SQLiteDatabase.openDatabase(context.getDatabasePath("ledger.db").absolutePath, null, SQLiteDatabase.OPEN_READONLY).use { it.version }
            assertTrue("Only the expected schema1 or already migrated schema2 is permitted", schema == 1 || schema == 2)
            LedgerRawUpgradeSeedTest.readLegacyProof(context, "pre-activity", baseline, code, schema, false)
        }
    }
    @get:Rule val rules: RuleChain = RuleChain.outerRule(originalDatabase).around(UiPreferencesIsolation()).around(compose)

    private fun snapshot(): LedgerSnapshot = LedgerDatabase(compose.activity).use { it.snapshot() }
    private fun newId() = UUID.randomUUID().toString()
    private fun mutate(action: (LedgerDatabase) -> Unit) = LedgerDatabase(compose.activity).use(action)
    private fun balances(day: LocalDate) = AccountBalances.balances(snapshot(), day)
    private fun sameSnapshot(expected: LedgerSnapshot, actual: LedgerSnapshot) {
        assertEquals(expected.entries.associateBy { it.id }, actual.entries.associateBy { it.id })
        assertEquals(expected.accounts.associateBy { it.id }, actual.accounts.associateBy { it.id })
        assertEquals(expected.transfers.associateBy { it.id }, actual.transfers.associateBy { it.id })
        assertEquals(expected.adjustments.associateBy { it.id }, actual.adjustments.associateBy { it.id })
        assertEquals(expected.categories.associateBy { it.id }, actual.categories.associateBy { it.id })
    }

    private fun assertMigration(): JSONObject {
        val (baseline, code) = LedgerRawUpgradeSeedTest.baselineIdentity()
        val proof = LedgerRawUpgradeSeedTest.readLegacyProof(compose.activity, "opened", baseline, code, 2, false)
        val initial = snapshot()
        assertTrue(initial.accounts.isEmpty()); assertTrue(initial.transfers.isEmpty()); assertTrue(initial.adjustments.isEmpty())
        val day = LocalDate.now()
        val amounts = AccountBalances.balances(initial, day)
        val totals = AccountBalances.totals(initial, day)
        assertTrue("Migration must not fabricate account balances", amounts.isEmpty())
        assertEquals(AccountTotals(0, 0, 0), totals)
        // Independent literal canonical catalogue; do not read the implementation's default object.
        val canonical = listOf(
            EntryKind.EXPENSE to listOf("餐饮", "交通", "购物", "居家", "娱乐", "医疗", "教育", "其他支出"),
            EntryKind.INCOME to listOf("工资", "奖金", "理财", "其他收入"),
        ).flatMap { (kind, names) -> names.mapIndexed { order, name ->
            Category(UUID.nameUUIDFromBytes("an-jizhang:category:v1:${kind.name}:$name".toByteArray(Charsets.UTF_8)).toString(), kind, name, order)
        } }
        val extra = Category(UUID.nameUUIDFromBytes("an-jizhang:category:v1:EXPENSE:自定义分类".toByteArray(Charsets.UTF_8)).toString(), EntryKind.EXPENSE, "自定义分类", 8)
        assertEquals("The persisted default and legacy catalogue must match every field, with no extras",
            (canonical + extra).associateBy { it.id }, initial.categories.associateBy { it.id })
        for (entry in initial.entries) {
            assertNull("Legacy entries must keep null account links", entry.accountId)
            assertEquals("Every legacy (kind,trimmed name) has exactly one active category", 1,
                initial.categories.count { it.kind == entry.kind && it.name == entry.category.trim() && !it.archived })
        }
        val persistedCategories = JSONArray()
        SQLiteDatabase.openDatabase(compose.activity.getDatabasePath("ledger.db").absolutePath, null, SQLiteDatabase.OPEN_READONLY).use { db ->
            for (table in listOf("accounts", "transfers", "adjustments")) assertEquals("No fabricated $table", 0L, LedgerRawUpgradeSeedTest.count(db, table))
            db.rawQuery("SELECT id,kind,name,sort_order,archived FROM categories ORDER BY kind,sort_order,id", null).use { cursor ->
                while (cursor.moveToNext()) persistedCategories.put(JSONObject().put("id", cursor.getString(0))
                    .put("kind", cursor.getString(1)).put("name", cursor.getString(2))
                    .put("sortOrder", cursor.getInt(3)).put("archived", cursor.getInt(4) == 1))
            }
            assertEquals(initial.categories.size.toLong(), LedgerRawUpgradeSeedTest.count(db, "categories"))
        }
        return proof.put("canonicalDefaultsMatch", true).put("distinctLegacyPairsMatch", true).put("categoryRows", persistedCategories)
            .put("metadata", JSONObject().put("accountsCount", initial.accounts.size).put("transfersCount", initial.transfers.size)
                .put("adjustmentsCount", initial.adjustments.size).put("accountBalances", JSONObject(amounts))
                .put("assetCents", totals.assetCents).put("debtCents", totals.debtCents).put("netCents", totals.netCents))
    }

    private fun assertCapabilities(): JSONObject {
        val checks = JSONObject()
        val today = LocalDate.now()
        val cash = Account(newId(), "升级合成现金", AccountKind.ASSET, AccountSubtype.CASH)
        val bank = Account(newId(), "升级合成银行", AccountKind.ASSET, AccountSubtype.BANK, sortOrder = 1)
        val debt = Account(newId(), "升级合成信用卡", AccountKind.LIABILITY, AccountSubtype.CREDIT_CARD, sortOrder = 2)
        val zeroRequest = BalanceAdjustmentRequest(newId(), cash.id, 0, today, "合成零初始余额", AdjustmentReason.INITIAL)
        val bankRequest = BalanceAdjustmentRequest(newId(), bank.id, 10000, today, "合成银行初始余额", AdjustmentReason.INITIAL)
        val debtRequest = BalanceAdjustmentRequest(newId(), debt.id, -2000, today, "合成欠款初始余额", AdjustmentReason.INITIAL)
        mutate { database ->
            val before = database.snapshot().entries
            database.createAccount(cash, zeroRequest)
            database.createAccount(bank, bankRequest)
            database.createAccount(debt, debtRequest)
            assertEquals(before, database.snapshot().entries)
        }
        val created = snapshot() // New helper/connection proves actual persistence.
        assertEquals(listOf(cash, bank, debt).associateBy { it.id }, created.accounts.associateBy { it.id })
        checks.put("createAccount", true)
        val zero = created.adjustments.single { it.id == zeroRequest.id }
        assertEquals(AccountAdjustment(zeroRequest.id, cash.id, 0, today, 0, zeroRequest.note, AdjustmentReason.INITIAL), zero)
        assertEquals(mapOf(cash.id to 0L, bank.id to 10000L, debt.id to -2000L), AccountBalances.balances(created, today))
        assertEquals(AccountTotals(10000, 2000, 8000), AccountBalances.totals(created, today))
        assertEquals(3, created.entries.size); assertTrue(created.transfers.isEmpty())
        checks.put("initialBalanceZero", true)

        val income = Entry(newId(), EntryKind.INCOME, 1000, "工资", today, "合成关联收入", cash.id)
        val expense = Entry(newId(), EntryKind.EXPENSE, 200, "餐饮", today, "合成关联支出", cash.id)
        mutate { it.save(income); it.save(expense) }
        val linked = snapshot()
        assertEquals(income, linked.entries.single { it.id == income.id }); assertEquals(expense, linked.entries.single { it.id == expense.id })
        assertEquals(800L, AccountBalances.balances(linked, today).getValue(cash.id))
        assertEquals(AccountTotals(10800, 2000, 8800), AccountBalances.totals(linked, today))
        assertLegacyStillUnchanged()
        checks.put("accountLinkedIncomeExpense", true)

        val monthBefore = MonthlySummary.calculate(linked.entries, YearMonth.from(today))
        val yearBefore = AnnualSummary.calculate(linked.entries, today.year)
        val transfer = AccountTransfer(newId(), cash.id, bank.id, 300, today, "合成升级转账")
        mutate { it.saveTransfer(transfer) }
        val transferred = snapshot()
        assertEquals(transfer, transferred.transfers.single())
        assertEquals(500L, AccountBalances.balances(transferred, today).getValue(cash.id))
        assertEquals(10300L, AccountBalances.balances(transferred, today).getValue(bank.id))
        assertEquals(linked.entries, transferred.entries)
        assertEquals(monthBefore, MonthlySummary.calculate(transferred.entries, YearMonth.from(today)))
        assertEquals(yearBefore, AnnualSummary.calculate(transferred.entries, today.year))
        checks.put("transferDoesNotChangeIncomeExpense", true)

        val calibrationRequest = BalanceAdjustmentRequest(newId(), cash.id, 700, today, "合成升级校准")
        val calibration = LedgerDatabase(compose.activity).use { it.adjustBalance(calibrationRequest) }
        assertEquals(200L, calibration.deltaCents); assertEquals(700L, calibration.targetBalanceCents)
        assertEquals(700L, balances(today).getValue(cash.id))
        val historical = Entry(newId(), EntryKind.INCOME, 400, "工资", today.minusDays(1), "合成历史补录", cash.id)
        mutate { it.save(historical) }; assertEquals(1100L, balances(today).getValue(cash.id))
        mutate { it.save(historical.copy(amountCents = 250)) }; assertEquals(950L, balances(today).getValue(cash.id))
        mutate { it.delete(historical.id) }; assertEquals(700L, balances(today).getValue(cash.id))
        assertEquals(calibration, snapshot().adjustments.single { it.id == calibration.id })
        assertEquals(monthBefore, MonthlySummary.calculate(snapshot().entries, YearMonth.from(today)))
        assertEquals(yearBefore, AnnualSummary.calculate(snapshot().entries, today.year))
        checks.put("calibrationDeltaRecalculated", true)

        val imported = Entry(newId(), EntryKind.EXPENSE, 7, "升级旧备份新类", today, "旧备份真实LF\n与引号\"")
        val legacyBackup = BackupCodec.decodeBackup(BackupCodec.encode(listOf(imported)))
        assertEquals(1, legacyBackup.sourceVersion)
        val importResult = LedgerDatabase(compose.activity).use { it.merge(legacyBackup) }
        assertEquals(1, importResult.imported); assertEquals(0, importResult.skipped)
        val restoredLegacy = snapshot()
        assertEquals(imported, restoredLegacy.entries.single { it.id == imported.id })
        assertNull(restoredLegacy.entries.single { it.id == imported.id }.accountId)
        assertEquals(1, restoredLegacy.categories.count { it.kind == imported.kind && it.name == imported.category && !it.archived })
        assertLegacyStillUnchanged()
        checks.put("format1NewIdMerge", true)

        val beforeDuplicate = snapshot()
        val duplicate = BackupCodec.decodeBackup(BackupCodec.encode(listOf(income.copy(accountId = null))))
        val duplicateResult = LedgerDatabase(compose.activity).use { it.merge(duplicate) }
        assertEquals(0, duplicateResult.imported); assertEquals(1, duplicateResult.skipped)
        sameSnapshot(beforeDuplicate, snapshot())
        assertEquals(income, snapshot().entries.single { it.id == income.id })
        checks.put("format1DuplicatePreservesLocalLink", true)

        assertLinkedEntryThroughUi(cash, today)
        checks.put("linkedEntrySavedThroughUi", true)
        val complete = snapshot()
        val encoded = BackupCodec.encodeSnapshot(complete)
        assertEquals(2, JSONObject(encoded).getInt("version"))
        val decoded = BackupCodec.decodeBackup(encoded)
        assertEquals(2, decoded.sourceVersion); sameSnapshot(complete, decoded.snapshot)
        val scratchName = "upgrade-scratch-${newId()}.db"
        val scratch = object : ContextWrapper(compose.activity.applicationContext) {
            override fun getApplicationContext(): Context = this
            override fun getDatabasePath(name: String): File = baseContext.getDatabasePath(scratchName)
            override fun openOrCreateDatabase(name: String, mode: Int, factory: SQLiteDatabase.CursorFactory?): SQLiteDatabase =
                baseContext.openOrCreateDatabase(scratchName, mode, factory)
            override fun openOrCreateDatabase(name: String, mode: Int, factory: SQLiteDatabase.CursorFactory?, errorHandler: DatabaseErrorHandler?): SQLiteDatabase =
                baseContext.openOrCreateDatabase(scratchName, mode, factory, errorHandler)
        }
        assertNotEquals("Scratch database must never address the upgraded ledger", compose.activity.getDatabasePath("ledger.db"), scratch.getDatabasePath("ledger.db"))
        assertFalse("Use a new independent scratch database", scratch.getDatabasePath("ledger.db").exists())
        LedgerDatabase(scratch).use { database ->
            val result = database.merge(decoded)
            assertEquals(complete.entries.size, result.imported)
            assertEquals(complete.accounts.size, result.accountsImported)
            assertEquals(complete.transfers.size, result.transfersImported)
            assertEquals(complete.adjustments.size, result.adjustmentsImported)
            sameSnapshot(complete, database.snapshot())
        }
        LedgerDatabase(scratch).use { database ->
            val actual = database.snapshot()
            sameSnapshot(complete, actual)
            assertEquals(AccountBalances.balances(complete, today), AccountBalances.balances(actual, today))
            assertEquals(AccountBalances.totals(complete, today), AccountBalances.totals(actual, today))
        }
        sameSnapshot(complete, snapshot())
        checks.put("format2ScratchRoundTrip", true)
        val (baseline, code) = LedgerRawUpgradeSeedTest.baselineIdentity()
        return JSONObject().put("phase", "capabilities").put("baselineName", baseline).put("baselineCode", code).put("assertedChecks", checks)
    }

    private fun assertLegacyStillUnchanged() {
        val (baseline, code) = LedgerRawUpgradeSeedTest.baselineIdentity()
        LedgerRawUpgradeSeedTest.readLegacyProof(compose.activity, "internal-readback", baseline, code, 2, true)
    }

    private fun assertLinkedEntryThroughUi(account: Account, day: LocalDate) {
        val vm = compose.runOnIdle { ViewModelProvider(compose.activity)[LedgerViewModel::class.java] }
        compose.waitUntil(10000) { !vm.state.value.busy }
        val previousRevision = vm.state.value.snapshotRevision
        compose.runOnIdle { vm.load() }
        compose.waitUntil(10000) { !vm.state.value.busy && vm.state.value.snapshotRevision > previousRevision }
        assertNull(vm.state.value.error)
        val before = snapshot()
        compose.onNodeWithTag("nav-accounts").assertIsDisplayed().performClick().assertIsSelected()
        compose.onNodeWithTag("accounts-list").assertIsDisplayed()
        compose.onNodeWithTag("add-entry").assertIsDisplayed().assertIsEnabled().performClick()
        compose.onNodeWithTag("editor-shell").assertIsDisplayed()
        compose.setAmountWithKeypad("1.23")
        compose.selectCategory("餐饮")
        compose.onNodeWithTag("editor-account-select").performScrollTo().assertIsDisplayed().performClick()
        compose.onNodeWithTag("editor-account-${account.id}").performScrollTo().assertIsDisplayed().performClick()
        compose.assertEditorDate(day.toString())
        compose.onNodeWithText("保存").performScrollTo().assertIsDisplayed().assertIsEnabled().performClick()
        compose.waitUntil(10000) { !vm.state.value.busy && vm.state.value.entries.size == before.entries.size + 1 &&
            compose.onAllNodesWithTag("editor-shell").fetchSemanticsNodes().isEmpty() }
        assertNull(vm.state.value.error)
        compose.onNodeWithTag("nav-accounts").assertIsSelected()
        compose.onNodeWithTag("accounts-list").assertIsDisplayed()
        val after = snapshot()
        val saved = after.entries.single { candidate -> before.entries.none { it.id == candidate.id } }
        assertEquals(EntryKind.EXPENSE, saved.kind); assertEquals(123L, saved.amountCents)
        assertEquals("餐饮", saved.category); assertEquals(account.id, saved.accountId); assertEquals(day, saved.date)
        assertEquals("", saved.note)
        before.entries.forEach { original -> assertEquals(original, after.entries.single { it.id == original.id }) }
        assertEquals(AccountBalances.balances(before, day).getValue(account.id) - 123L,
            AccountBalances.balances(after, day).getValue(account.id))
        assertLegacyStillUnchanged()
        captureLedgerScreen("v018-upgrade-linked-entry-saved")
    }

    @Test fun verifyUpgradedLedger() {
        // Instrumentation may restart the earlier cold-launched process. Let the real Activity/VM finish its initial migration/load.
        val vm = compose.runOnIdle { ViewModelProvider(compose.activity)[LedgerViewModel::class.java] }
        compose.waitUntil(10000) { !vm.state.value.busy && (vm.state.value.snapshotRevision > 0 || vm.state.value.readError) }
        assertNull("The actual initial load must succeed", vm.state.value.error)
        assertFalse("The actual initial load must not leave a read error", vm.state.value.readError)
        assertTrue("A real successful initial snapshot is required", vm.state.value.snapshotRevision > 0)
        LedgerRawUpgradeSeedTest.emitProof(assertMigration())
        LedgerRawUpgradeSeedTest.emitProof(assertCapabilities())
        val (baseline, code) = LedgerRawUpgradeSeedTest.baselineIdentity()
        // All helper connections are closed; the final raw connection is a fresh read-only reopen.
        val reopened = LedgerRawUpgradeSeedTest.readLegacyProof(compose.activity, "reopened", baseline, code, 2, true)
        LedgerRawUpgradeSeedTest.emitProof(reopened.put("rawReopened", true))
    }
}

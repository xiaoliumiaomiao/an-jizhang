package com.xiaoliumiaomiao.anjizhang

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.xiaoliumiaomiao.anjizhang.data.*
import com.xiaoliumiaomiao.anjizhang.domain.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class AccountDatabaseTest {
    private lateinit var context: Context
    private lateinit var db: LedgerDatabase
    private var day = LocalDate.of(2026, 10, 12)
    private fun id(n: Int) = UUID(0, n.toLong()).toString()
    private fun account(n: Int, kind: AccountKind = AccountKind.ASSET) =
        Account(id(n), "账户$n", kind, if (kind == AccountKind.ASSET) AccountSubtype.BANK else AccountSubtype.LOAN)
    private fun request(n: Int, a: Account, target: Long, reason: AdjustmentReason = AdjustmentReason.CALIBRATION) =
        BalanceAdjustmentRequest(id(n), a.id, target, day, "校准", reason)
    private fun entry(n: Int, a: Account?, cents: Long = 100, date: LocalDate = day,
        kind: EntryKind = EntryKind.INCOME) = Entry(id(n), kind, cents, "原分类", date, "原备注", a?.id)
    private fun balance(a: Account, at: LocalDate = day) = AccountBalances.balances(db.snapshot(), at).getValue(a.id)

    @Before fun setup() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase("ledger.db")
        db = LedgerDatabase(context) { day }
    }

    @After fun teardown() { db.close(); context.deleteDatabase("ledger.db") }

    @Test fun rawSchema1MigratesWithoutChangingAnyLegacyFieldAndReopens() {
        val legacy = Entry("00000000-0000-0000-0000-000000000001", EntryKind.EXPENSE,
            1234, " 旧类别 ", LocalDate.of(2026, 9, 30), "中文\"备注\n第二行")
        context.openOrCreateDatabase("ledger.db", Context.MODE_PRIVATE, null).use { raw ->
            raw.execSQL("""CREATE TABLE entries (id TEXT PRIMARY KEY NOT NULL,
                kind TEXT NOT NULL CHECK(kind IN ('EXPENSE','INCOME')),
                amount_cents INTEGER NOT NULL CHECK(amount_cents BETWEEN 1 AND 99999999999),
                category TEXT NOT NULL, date TEXT NOT NULL, note TEXT NOT NULL)""")
            raw.execSQL("INSERT INTO entries VALUES(?,?,?,?,?,?)", arrayOf<Any>(legacy.id, legacy.kind.name,
                legacy.amountCents, legacy.category, legacy.date.toString(), legacy.note))
            raw.version = 1
        }
        assertEquals(2, db.readableDatabase.version)
        assertEquals(listOf(legacy), db.listEntries())
        db.close(); db = LedgerDatabase(context)
        assertEquals(2, db.readableDatabase.version)
        assertEquals(listOf(legacy), db.listEntries())
        db.readableDatabase.rawQuery("PRAGMA foreign_keys", null).use { c ->
            assertTrue(c.moveToFirst()); assertEquals(1, c.getInt(0))
        }
        assertTrue(db.snapshot().categories.any { it.name == "旧类别" })
        assertNull(db.listEntries().single().accountId)
        val names = db.readableDatabase.rawQuery("PRAGMA table_info(entries)", null).use { c ->
            buildList { while (c.moveToNext()) add(c.getString(c.getColumnIndexOrThrow("name"))) }
        }
        assertEquals(listOf("id", "kind", "amount_cents", "category", "date", "note", "account_id"), names)
    }

    @Test fun defaultsAreStableAcrossFreshInstallsAndIncludeBothKinds() {
        val first = db.snapshot().categories
        assertEquals(12, first.size)
        assertEquals(8, first.count { it.kind == EntryKind.EXPENSE })
        assertEquals(4, first.count { it.kind == EntryKind.INCOME })
        db.close(); context.deleteDatabase("ledger.db"); db = LedgerDatabase(context) { day }
        assertEquals(first, db.snapshot().categories)
    }

    @Test fun atomicInitialCreatePersistsAndIsIdempotentAcrossDayChange() {
        val a = account(10).copy(name = "  银行卡  ")
        val r = request(20, a, 12345, AdjustmentReason.INITIAL)
        val saved = db.createAccount(a, r)!!
        assertEquals(12345, saved.deltaCents)
        assertEquals("银行卡", db.snapshot().accounts.single().name)
        val before = db.snapshot(); day = day.plusDays(1)
        assertEquals(saved, db.createAccount(a, r))
        assertEquals(before, db.snapshot())
        db.close(); db = LedgerDatabase(context) { day }
        assertEquals(12345, balance(a))
        assertTrue(db.listEntries().isEmpty())
    }

    @Test fun zeroBalanceCreateAndSaveAccountEditTrimWithoutReplacingReferences() {
        val a = account(10)
        assertNull(db.createAccount(a))
        assertNull(db.createAccount(a))
        db.save(entry(20, a))
        db.saveAccount(a.copy(name = "  新名字  "))
        assertEquals("新名字", db.snapshot().accounts.single().name)
        assertEquals(a.id, db.listEntries().single().accountId)
        assertEquals(100, balance(a))
    }

    @Test fun initialCreateRejectsWrongAccountReasonDateAndRequestCollisionWithoutWrites() {
        val a = account(10)
        for (r in listOf(request(20, account(11), 100, AdjustmentReason.INITIAL),
            request(20, a, 100), request(20, a, 100, AdjustmentReason.INITIAL).copy(date = day.minusDays(1)))) {
            val before = db.snapshot()
            assertThrows(IllegalArgumentException::class.java) { db.createAccount(a, r) }
            assertEquals(before, db.snapshot())
        }
        val r = request(20, a, 100, AdjustmentReason.INITIAL)
        db.createAccount(a, r)
        for (bad in listOf(r.copy(targetBalanceCents = 101), r.copy(note = "变更"), r.copy(date = day.plusDays(1)))) {
            val before = db.snapshot()
            assertThrows(IllegalArgumentException::class.java) { db.createAccount(a, bad) }
            assertEquals(before, db.snapshot())
        }
        assertThrows(IllegalArgumentException::class.java) { db.createAccount(a.copy(name = "不同账户"), r) }
    }

    @Test fun sqlTriggerBetweenInitialEntitiesRollsBackAccountAndAdjustment() {
        val before = db.snapshot()
        db.writableDatabase.execSQL("""CREATE TRIGGER reject_initial BEFORE INSERT ON adjustments
            BEGIN SELECT RAISE(ABORT, 'injected initial write failure'); END""")
        val a = account(10)
        assertThrows(Exception::class.java) { db.createAccount(a, request(20, a, 123, AdjustmentReason.INITIAL)) }
        assertEquals(before, db.snapshot())
    }

    @Test fun consecutiveCalibrationUsesCurrentBalanceAndStableRetryReturnsOriginalDelta() {
        val a = account(10); db.saveAccount(a); db.save(entry(20, a, 100))
        val r = request(30, a, 500)
        val first = db.adjustBalance(r)
        assertEquals(400, first.deltaCents)
        db.save(entry(21, a, 40, kind = EntryKind.EXPENSE))
        assertEquals(460, balance(a))
        assertEquals(first, db.adjustBalance(r))
        assertEquals(2, db.listEntries().size)
        val second = db.adjustBalance(request(31, a, -20))
        assertEquals(-480, second.deltaCents); assertEquals(-20, balance(a))
        day = day.plusDays(1)
        assertEquals(first, db.adjustBalance(r))
        assertEquals(2, db.snapshot().adjustments.size)
    }

    @Test fun calibrationRejectsDateReasonChangedRequestAndOrphan() {
        val a = account(10); db.saveAccount(a)
        for (bad in listOf(request(20, a, 1).copy(date = day.minusDays(1)),
            request(20, a, 1).copy(date = day.plusDays(1)), request(20, a, 1, AdjustmentReason.INITIAL),
            request(20, account(11), 1), request(20, a, Long.MIN_VALUE))) {
            val before = db.snapshot()
            assertThrows(IllegalArgumentException::class.java) { db.adjustBalance(bad) }
            assertEquals(before, db.snapshot())
        }
        val r = request(20, a, 1); db.adjustBalance(r)
        for (bad in listOf(r.copy(targetBalanceCents = 2), r.copy(note = "冲突"),
            r.copy(accountId = account(11).id), r.copy(reason = AdjustmentReason.INITIAL), r.copy(date = day.plusDays(1)))) {
            val before = db.snapshot()
            assertThrows(IllegalArgumentException::class.java) { db.adjustBalance(bad) }
            assertEquals(before, db.snapshot())
        }
    }

    @Test fun historicalEntryEditDeleteAndAdjustmentDeleteRecomputeOriginalDelta() {
        val a = account(10); db.saveAccount(a)
        val e = entry(20, a, 100, day.minusDays(2)); db.save(e)
        val adjustment = db.adjustBalance(request(30, a, 500))
        db.save(e.copy(amountCents = 200)); assertEquals(600, balance(a))
        assertEquals(adjustment, db.snapshot().adjustments.single())
        db.delete(e.id); assertEquals(400, balance(a))
        db.deleteAdjustment(adjustment.id); assertEquals(0, balance(a))
    }

    @Test fun futureEntryTransferAndImportedAdjustmentArriveOnTheirDate() {
        val a = account(10); val b = account(11); db.saveAccount(a); db.saveAccount(b)
        val future = day.plusDays(1)
        db.save(entry(20, a, 100, future))
        db.saveTransfer(AccountTransfer(id(30), a.id, b.id, 30, future))
        db.merge(DecodedBackup(LedgerSnapshot(accounts = listOf(a, b),
            adjustments = listOf(AccountAdjustment(id(40), b.id, -10, future, -10))), 2))
        assertEquals(0, balance(a)); assertEquals(0, balance(b))
        assertEquals(70, balance(a, future)); assertEquals(20, balance(b, future))
    }

    @Test fun repeatedBorrowRepayAndLiabilityOverpaymentUseSignedBalances() {
        val a = account(10); val b = account(11, AccountKind.LIABILITY); db.saveAccount(a); db.saveAccount(b)
        val directions = listOf(b to a, a to b, b to a, a to b)
        directions.forEachIndexed { index, (from, to) ->
            db.saveTransfer(AccountTransfer(id(20 + index), from.id, to.id, 100, day, "借还"))
            assertEquals(if (index % 2 == 0) 100L else 0L, balance(a))
            assertEquals(if (index % 2 == 0) -100L else 0L, balance(b))
        }
        db.save(entry(30, b, 20)); assertEquals(20, balance(b))
        assertEquals(AccountTotals(20, 0, 20), AccountBalances.totals(db.snapshot(), day))
        db.deleteTransfer(id(23)); assertEquals(100, balance(a)); assertEquals(-80, balance(b))
        assertEquals(1, db.listEntries().size)
    }

    @Test fun allFourAccountKindTransferCombinationsPersistOneRowAndExactBalancesAndTotals() {
        val accounts = listOf(account(10), account(11),
            account(12, AccountKind.LIABILITY), account(13, AccountKind.LIABILITY))
        val initialBalances = listOf(1000L, 2000L, -3000L, -4000L)
        accounts.forEachIndexed { index, account ->
            db.createAccount(account, request(20 + index, account, initialBalances[index], AdjustmentReason.INITIAL))
        }
        val initial = db.snapshot()
        assertTrue(initial.accounts.all { !it.archived })
        assertEquals(initialBalances, accounts.map { AccountBalances.balances(initial, day).getValue(it.id) })
        assertEquals(AccountTotals(3000, 7000, -4000), AccountBalances.totals(initial, day))
        val transfers = listOf(
            AccountTransfer(id(30), accounts[0].id, accounts[1].id, 100, day, "资产到资产"),
            AccountTransfer(id(31), accounts[0].id, accounts[2].id, 200, day, "资产到负债"),
            AccountTransfer(id(32), accounts[2].id, accounts[1].id, 300, day, "负债到资产"),
            AccountTransfer(id(33), accounts[2].id, accounts[3].id, 400, day, "负债到负债"),
        )
        val accountById = accounts.associateBy { it.id }
        assertEquals(setOf(AccountKind.ASSET to AccountKind.ASSET, AccountKind.ASSET to AccountKind.LIABILITY,
            AccountKind.LIABILITY to AccountKind.ASSET, AccountKind.LIABILITY to AccountKind.LIABILITY),
            transfers.map { accountById.getValue(it.fromAccountId).kind to accountById.getValue(it.toAccountId).kind }.toSet())
        val expectedBalances = listOf(
            listOf(900L, 2100L, -3000L, -4000L), listOf(700L, 2100L, -2800L, -4000L),
            listOf(700L, 2400L, -3100L, -4000L), listOf(700L, 2400L, -3500L, -3600L),
        )
        val expectedTotals = listOf(AccountTotals(3000, 7000, -4000), AccountTotals(2800, 6800, -4000),
            AccountTotals(3100, 7100, -4000), AccountTotals(3100, 7100, -4000))
        transfers.forEachIndexed { index, transfer ->
            val before = db.snapshot()
            db.saveTransfer(transfer)
            val after = db.snapshot()
            assertEquals(expectedBalances[index], accounts.map { AccountBalances.balances(after, day).getValue(it.id) })
            assertEquals(expectedTotals[index], AccountBalances.totals(after, day))
            assertEquals(before.transfers.size + 1, after.transfers.size)
            assertEquals(transfers.take(index + 1).toSet(), after.transfers.toSet())
            assertEquals(initial.entries, after.entries)
            assertTrue(after.entries.isEmpty())
            assertEquals(initial.adjustments, after.adjustments)
            assertEquals(4, after.adjustments.size)
            db.readableDatabase.rawQuery("SELECT COUNT(*) FROM transfers WHERE id = ?", arrayOf(transfer.id)).use { cursor ->
                assertTrue(cursor.moveToFirst()); assertEquals(1, cursor.getInt(0))
            }
        }
        val final = db.snapshot()
        db.close(); db = LedgerDatabase(context) { day }
        assertEquals(final, db.snapshot())
    }

    @Test fun transferFailureAndOrphanAreAtomicAndForeignKeysEnabled() {
        val a = account(10); val b = account(11); db.saveAccount(a); db.saveAccount(b)
        val before = db.snapshot()
        assertThrows(IllegalArgumentException::class.java) {
            db.saveTransfer(AccountTransfer(id(20), a.id, id(999), 100, day))
        }
        db.writableDatabase.execSQL("""CREATE TRIGGER reject_transfer BEFORE INSERT ON transfers
            BEGIN SELECT RAISE(ABORT, 'injected transfer failure'); END""")
        assertThrows(Exception::class.java) { db.saveTransfer(AccountTransfer(id(20), a.id, b.id, 100, day)) }
        assertEquals(before, db.snapshot())
        assertThrows(Exception::class.java) {
            db.writableDatabase.execSQL("INSERT INTO entries VALUES(?,?,?,?,?,?,?)",
                arrayOf<Any>(id(25), "INCOME", 1, "测试", day.toString(), "", id(999)))
        }
    }

    @Test fun archivedAccountKeepsHistoryAndTotalsButRejectsNewUsesAndKindChanges() {
        val a = account(10); val b = account(11); db.saveAccount(a); db.saveAccount(b)
        val e = entry(20, a); db.save(e); db.archiveAccount(a.id)
        db.save(e.copy(note = "历史编辑")); assertEquals(100, balance(a))
        assertEquals(AccountTotals(100, 0, 100), AccountBalances.totals(db.snapshot(), day))
        val before = db.snapshot()
        assertThrows(IllegalArgumentException::class.java) { db.save(entry(21, a)) }
        assertThrows(IllegalArgumentException::class.java) { db.saveTransfer(AccountTransfer(id(30), a.id, b.id, 1, day)) }
        assertThrows(IllegalArgumentException::class.java) { db.saveAccount(db.snapshot().accounts.first { it.id == a.id }.copy(kind = AccountKind.LIABILITY)) }
        assertEquals(before, db.snapshot())
        db.archiveAccount(a.id, false); db.save(entry(21, a)); assertEquals(200, balance(a))
    }

    @Test fun candidateOverflowRejectsSaveDeleteTransferCalibrationAndFutureStates() {
        val a = account(10); val b = account(11); db.saveAccount(a); db.saveAccount(b)
        db.adjustBalance(request(20, a, Long.MAX_VALUE))
        var before = db.snapshot()
        assertThrows(IllegalArgumentException::class.java) { db.save(entry(30, a, 1)) }
        assertThrows(IllegalArgumentException::class.java) { db.save(entry(30, b, 1, day.plusDays(1))) }
        assertThrows(IllegalArgumentException::class.java) { db.saveTransfer(AccountTransfer(id(40), b.id, a.id, 1, day)) }
        assertEquals(before, db.snapshot())
        assertThrows(IllegalArgumentException::class.java) { db.adjustBalance(request(21, a, -Long.MAX_VALUE)) }
        assertEquals(before, db.snapshot())
        // Same-day huge cancellation is valid, but removing the cancelling event is not.
        db = db.also { it.close() }.let { context.deleteDatabase("ledger.db"); LedgerDatabase(context) { day } }
        val neg = AccountAdjustment(id(51), a.id, -1, day, 0)
        db.merge(DecodedBackup(LedgerSnapshot(accounts = listOf(a),
            adjustments = listOf(AccountAdjustment(id(50), a.id, Long.MAX_VALUE, day, Long.MAX_VALUE), neg),
            entries = listOf(entry(52, a, 1))), 2))
        before = db.snapshot()
        assertThrows(IllegalArgumentException::class.java) { db.deleteAdjustment(neg.id) }
        assertEquals(before, db.snapshot())
    }

    @Test fun categoryCrudTrimUniquenessRenameHistoryAndCompleteReorder() {
        val original = db.snapshot().categories.first { it.kind == EntryKind.EXPENSE }
        val e = entry(20, null, kind = EntryKind.EXPENSE).copy(category = original.name); db.save(e)
        db.saveCategory(original.copy(name = "  已改名  ", archived = true))
        assertEquals(original.name, db.listEntries().single().category)
        assertEquals("已改名", db.snapshot().categories.first { it.id == original.id }.name)
        val c = Category(id(30), EntryKind.EXPENSE, " 自定义 "); db.saveCategory(c)
        assertThrows(IllegalArgumentException::class.java) { db.saveCategory(c.copy(id = id(31), name = "自定义")) }
        val active = db.snapshot().categories.filter { it.kind == EntryKind.EXPENSE && !it.archived }.map { it.id }.reversed()
        for (ids in listOf(active.dropLast(1), active + active.first(), active + original.id)) {
            val before = db.snapshot()
            assertThrows(IllegalArgumentException::class.java) { db.reorderCategories(EntryKind.EXPENSE, ids) }
            assertEquals(before, db.snapshot())
        }
        db.reorderCategories(EntryKind.EXPENSE, active)
        assertEquals(active, db.snapshot().categories.filter { it.kind == EntryKind.EXPENSE && !it.archived }.sortedBy { it.sortOrder }.map { it.id })
    }

    @Test fun pristineCatalogAdoptsExactIncomingIncludingEmptyAndNeverReseedsOnReopen() {
        val changed = db.snapshot().categories.mapIndexed { index, c -> if (index == 0) c.copy(name = "恢复分类", archived = true) else c }
        val result = db.merge(DecodedBackup(LedgerSnapshot(categories = changed), 2))
        assertTrue(result.catalogAdopted); assertEquals(changed.toSet(), db.snapshot().categories.toSet())
        db.close(); db = LedgerDatabase(context) { day }
        assertEquals(changed.toSet(), db.snapshot().categories.toSet())
        db.close(); context.deleteDatabase("ledger.db"); db = LedgerDatabase(context) { day }
        assertTrue(db.merge(DecodedBackup(LedgerSnapshot(), 2)).catalogAdopted)
        repeat(2) { db.close(); db = LedgerDatabase(context) { day }; assertTrue(db.snapshot().categories.isEmpty()) }
    }

    @Test fun authorCatalogChangesAndZeroAccountPreventPristineAdoption() {
        for (change in 0..4) {
            db.close(); context.deleteDatabase("ledger.db"); db = LedgerDatabase(context) { day }
            val defaults = db.snapshot().categories
            val first = defaults.first()
            when (change) {
                0 -> db.saveCategory(first.copy(name = "作者分类"))
                1 -> db.saveCategory(first.copy(sortOrder = 99))
                2 -> db.saveCategory(first.copy(archived = true))
                3 -> db.saveCategory(Category(id(50), EntryKind.INCOME, "作者新增"))
                4 -> db.saveAccount(account(10))
            }
            val before = db.snapshot()
            val changed = defaults.map { if (it.id == first.id) it.copy(name = "备份改变") else it }
            assertThrows(IllegalArgumentException::class.java) { db.merge(DecodedBackup(LedgerSnapshot(categories = changed), 2)) }
            assertEquals(before, db.snapshot())
        }
    }

    @Test fun adoptingCatalogWriteFaultRestoresDeletedDefaultsAndOtherEntities() {
        val before = db.snapshot()
        val changed = before.categories.map { it.copy(name = it.name + "新") }
        db.writableDatabase.execSQL("""CREATE TRIGGER reject_category BEFORE INSERT ON categories
            WHEN NEW.name = '${changed.last().name}' BEGIN SELECT RAISE(ABORT, 'injected catalog failure'); END""")
        assertThrows(Exception::class.java) { db.merge(DecodedBackup(LedgerSnapshot(accounts = listOf(account(10)), categories = changed), 2)) }
        assertEquals(before, db.snapshot())
    }

    @Test fun v1SkipsSixFieldsPreservesLocalLinkAndSupplementsByTrimmedNameWithoutOverwritingCatalog() {
        val a = account(10); db.saveAccount(a)
        val e = entry(20, a, kind = EntryKind.EXPENSE).copy(category = "餐饮"); db.save(e)
        val c = db.snapshot().categories.first { it.kind == EntryKind.EXPENSE && it.name == "餐饮" }
        db.saveCategory(c.copy(name = "作者更名", archived = true, sortOrder = 99))
        val sameName = Category(id(60), EntryKind.INCOME, "原分类"); db.saveCategory(sameName)
        val before = db.snapshot()
        val result = db.merge(DecodedBackup(LedgerSnapshot(entries = listOf(e.copy(accountId = null), entry(21, null))), 1))
        assertEquals(ImportResult(1, 1), result)
        assertEquals(a.id, db.listEntries().first { it.id == e.id }.accountId)
        assertTrue(db.snapshot().categories.containsAll(before.categories))
        assertEquals(1, db.snapshot().categories.count { it.kind == sameName.kind && it.name.trim() == sameName.name })
        val derived = db.snapshot().categories.first { it.kind == e.kind && it.name == "餐饮" }
        assertNotEquals(c.id, derived.id)
    }

    @Test fun v1ManufacturedMetadataReferencesUnsupportedVersionAndLateConflictRejectWholeBatch() {
        val a = account(10); val e = entry(20, null); db.save(e)
        val variants = listOf(DecodedBackup(LedgerSnapshot(accounts = listOf(a)), 1),
            DecodedBackup(LedgerSnapshot(categories = db.snapshot().categories), 1),
            DecodedBackup(LedgerSnapshot(entries = listOf(e.copy(accountId = a.id))), 1),
            DecodedBackup(LedgerSnapshot(), 3),
            DecodedBackup(LedgerSnapshot(entries = listOf(entry(21, null), e.copy(note = "真冲突"))), 1))
        for (backup in variants) {
            val before = db.snapshot()
            assertThrows(IllegalArgumentException::class.java) { db.merge(backup) }
            assertEquals(before, db.snapshot())
        }
    }

    @Test fun v2CountsEachEntitySkipsFullEqualityAndTreatsExplicitNullAsConflict() {
        val a = account(10); val b = account(11)
        val e = entry(20, a)
        val t = AccountTransfer(id(30), a.id, b.id, 10, day)
        val adj = AccountAdjustment(id(40), a.id, -20, day, 70)
        val c = Category(id(50), EntryKind.INCOME, "  完整原分类  ")
        val incoming = LedgerSnapshot(listOf(e), listOf(a.copy(name = " 原账户 "), b), listOf(t), listOf(adj), listOf(c))
        val result = db.merge(DecodedBackup(incoming, 2))
        assertEquals(1, result.imported); assertEquals(2, result.accountsImported)
        assertEquals(1, result.transfersImported); assertEquals(1, result.adjustmentsImported)
        assertEquals(1, result.categoriesImported); assertTrue(result.catalogAdopted)
        val again = db.merge(DecodedBackup(incoming, 2))
        assertEquals(1, again.skipped); assertEquals(2, again.accountsSkipped)
        assertEquals(1, again.transfersSkipped); assertEquals(1, again.adjustmentsSkipped); assertEquals(1, again.categoriesSkipped)
        assertFalse(again.catalogAdopted)
        assertEquals(" 原账户 ", db.snapshot().accounts.first { it.id == a.id }.name)
        assertEquals(c.name, db.snapshot().categories.single().name)
        val before = db.snapshot()
        assertThrows(IllegalArgumentException::class.java) { db.merge(DecodedBackup(incoming.copy(entries = listOf(e.copy(accountId = null))), 2)) }
        assertEquals(before, db.snapshot())
    }

    @Test fun v2LateEntityConflictAndUnionOverflowRejectAllNewParentsAndChildren() {
        val a = account(10); db.saveAccount(a)
        val old = db.adjustBalance(request(20, a, Long.MAX_VALUE))
        val b = account(11)
        for (snapshot in listOf(LedgerSnapshot(accounts = listOf(b), entries = listOf(entry(30, b, 1))),
            LedgerSnapshot(accounts = listOf(a, b), adjustments = listOf(old.copy(note = "冲突"))),
            LedgerSnapshot(accounts = listOf(b), entries = listOf(entry(30, b, 1, day.plusDays(1)))))) {
            val before = db.snapshot()
            assertThrows(IllegalArgumentException::class.java) { db.merge(DecodedBackup(snapshot, 2)) }
            assertEquals(before, db.snapshot())
        }
    }

    @Test fun legacySupplementDoesNotLetMaximumAuthorSortBlockValidImport() {
        val first = db.snapshot().categories.first { it.kind == EntryKind.INCOME }
        db.saveCategory(first.copy(sortOrder = Int.MAX_VALUE))
        val author = db.snapshot()
        assertEquals(1, db.merge(listOf(entry(70, null).copy(category = "旧新增类别"))).imported)
        assertTrue(db.snapshot().categories.containsAll(author.categories))
        assertTrue(db.snapshot().categories.any { it.name == "旧新增类别" })
    }

    @Test fun archivedCalibrationRetryAndHistoricalTransferEditKeepOriginalReferences() {
        val a = account(10); val b = account(11); val c = account(12)
        db.saveAccount(a); db.saveAccount(b); db.saveAccount(c)
        val req = request(20, a, 100); val original = db.adjustBalance(req)
        val transfer = AccountTransfer(id(30), a.id, b.id, 20, day)
        db.saveTransfer(transfer); db.archiveAccount(a.id); db.archiveAccount(b.id)
        day = day.plusDays(1)
        assertEquals(original, db.adjustBalance(req))
        db.saveTransfer(transfer.copy(amountCents = 10, note = "历史编辑"))
        val before = db.snapshot()
        assertThrows(IllegalArgumentException::class.java) { db.adjustBalance(request(21, a, 200)) }
        assertThrows(IllegalArgumentException::class.java) { db.saveTransfer(transfer.copy(fromAccountId = c.id, toAccountId = a.id)) }
        assertEquals(before, db.snapshot())
    }

    @Test fun validSameDayUnionCancellationSucceedsAndRemovingOrMovingItRollsBack() {
        val a = account(10); db.saveAccount(a)
        db.adjustBalance(request(20, a, Long.MAX_VALUE))
        val cancelling = entry(30, a, 1, kind = EntryKind.EXPENSE)
        val extra = entry(31, a, 1)
        db.merge(DecodedBackup(LedgerSnapshot(accounts = listOf(a), entries = listOf(cancelling, extra)), 2))
        assertEquals(Long.MAX_VALUE, balance(a))
        val before = db.snapshot()
        assertThrows(IllegalArgumentException::class.java) { db.delete(cancelling.id) }
        assertThrows(IllegalArgumentException::class.java) { db.save(cancelling.copy(date = day.plusDays(1))) }
        assertEquals(before, db.snapshot())
    }

    @Test fun openingRawSchema1AboveBackupEntryCapDoesNotApplyExportCapacity() {
        context.openOrCreateDatabase("ledger.db", Context.MODE_PRIVATE, null).use { raw ->
            raw.execSQL("""CREATE TABLE entries (id TEXT PRIMARY KEY NOT NULL,
                kind TEXT NOT NULL, amount_cents INTEGER NOT NULL, category TEXT NOT NULL,
                date TEXT NOT NULL, note TEXT NOT NULL)""")
            raw.beginTransaction()
            try {
                val statement = raw.compileStatement("INSERT INTO entries VALUES(?,?,?,?,?,?)")
                statement.use { sql ->
                    repeat(50001) { n ->
                        sql.bindString(1, id(n)); sql.bindString(2, "INCOME"); sql.bindLong(3, 1)
                        sql.bindString(4, "历史类别$n"); sql.bindString(5, day.toString()); sql.bindString(6, "")
                        sql.executeInsert()
                    }
                }
                raw.setTransactionSuccessful()
            } finally { raw.endTransaction() }
            raw.version = 1
        }
        val migrated = db.snapshot()
        assertEquals(2, db.readableDatabase.version)
        assertEquals(50001, migrated.entries.size); assertEquals(50013, migrated.categories.size)
        assertTrue(migrated.accounts.isEmpty()); assertTrue(migrated.entries.all { it.accountId == null })
        assertThrows(IllegalArgumentException::class.java) { BackupCodec.encodeSnapshot(migrated) }
        db.close(); db = LedgerDatabase(context) { day }
        assertEquals(migrated, db.snapshot())
    }
}

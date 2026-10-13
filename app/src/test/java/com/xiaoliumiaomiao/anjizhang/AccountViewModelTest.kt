package com.xiaoliumiaomiao.anjizhang

import android.app.Application
import android.os.Looper
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ApplicationProvider
import com.xiaoliumiaomiao.anjizhang.data.BackupCodec
import com.xiaoliumiaomiao.anjizhang.data.LedgerDatabase
import com.xiaoliumiaomiao.anjizhang.domain.*
import com.xiaoliumiaomiao.anjizhang.ui.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import java.io.ByteArrayOutputStream
import java.io.ByteArrayInputStream
import java.time.LocalDate
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class AccountViewModelTest {
    private lateinit var application: Application
    private lateinit var model: LedgerViewModel
    private lateinit var database: LedgerDatabase
    private val store = ViewModelStore()
    private var today = LocalDate.now()
    private fun id() = UUID.randomUUID().toString()
    private fun account(name: String = "现金") = Account(id(), name, AccountKind.ASSET, AccountSubtype.CASH)
    private fun await(predicate: () -> Boolean) {
        val deadline = System.nanoTime() + 10_000_000_000L
        do { Shadows.shadowOf(Looper.getMainLooper()).idle(); Thread.sleep(10) }
        while (!predicate() && System.nanoTime() < deadline)
        assertTrue("等待后台操作超时：${model.state.value}", predicate())
    }
    @Before fun setup() {
        application = ApplicationProvider.getApplicationContext()
        application.deleteDatabase("ledger.db")
        database = LedgerDatabase(application) { today }
        model = LedgerViewModel(application) { today }
        store.put("ledger", model)
        await { !model.state.value.busy }
    }
    @After fun teardown() { store.clear(); database.close() }
    private fun finish(operationId: String) = await { !model.state.value.busy &&
        (model.state.value.completedOperationId == operationId || model.state.value.failedOperationId == operationId) }
    private fun receipt(operationId: String): CommittedOperationResult =
        requireNotNull(model.state.value.committedResults.singleOrNull { it.operationId == operationId }) { "提交结果不能丢失" }
    private fun load() {
        val revision = model.state.value.snapshotRevision
        model.load(); await { !model.state.value.busy && model.state.value.error == null && model.state.value.snapshotRevision > revision }
    }
    private fun fixture(): LedgerSnapshot {
        val first = account(); val second = account("银行卡")
        database.createAccount(first, BalanceAdjustmentRequest(id(), first.id, 1000, today, reason = AdjustmentReason.INITIAL))
        database.saveAccount(second)
        database.save(Entry(id(), EntryKind.EXPENSE, 100, "餐饮", today, accountId = first.id))
        database.saveTransfer(AccountTransfer(id(), first.id, second.id, 200, today))
        database.saveCategory(Category(id(), EntryKind.INCOME, "自定义类别", 20))
        return database.snapshot()
    }

    @Test fun awaitedReadIsPendingBehindLockAndCompletesAfterSnapshotPublication() {
        val expected = fixture()
        val revision = model.state.value.snapshotRevision
        val field = LedgerViewModel::class.java.getDeclaredField("lock").also { it.isAccessible = true }
        val lock = field.get(model) as Mutex
        runBlocking { lock.lock() }
        val reading = try {
            model.loadAwaitable().also { assertFalse("Completion must represent the real queued read", it.isCompleted) }
        } finally { lock.unlock() }
        runBlocking { reading.join() }
        assertTrue(reading.isCompleted)
        assertFalse(reading.isCancelled)
        assertFalse(model.state.value.busy)
        assertFalse(model.state.value.readError)
        assertTrue(model.state.value.snapshotRevision > revision)
        assertEquals(expected, model.state.value.snapshot)
    }

    @Test fun awaitedReadFailureCompletesWithPreservedSnapshotAndReadError() {
        val previous = model.state.value.snapshot
        val revision = model.state.value.snapshotRevision
        database.writableDatabase.execSQL("DROP TABLE categories")
        val reading = model.loadAwaitable()
        runBlocking { reading.join() }
        assertTrue("A failed read still has an exact completion", reading.isCompleted)
        assertFalse(model.state.value.busy)
        assertTrue(model.state.value.readError)
        assertSame(previous, model.state.value.snapshot)
        assertEquals(revision, model.state.value.snapshotRevision)
        assertTrue(model.state.value.error!!.contains("读取"))
        assertTrue(model.state.value.committedResults.isEmpty())
    }

    @Test fun awaitedReadCancellationWhileQueuedDoesNotReadOrPublishFailure() {
        val previous = model.state.value
        val field = LedgerViewModel::class.java.getDeclaredField("lock").also { it.isAccessible = true }
        val lock = field.get(model) as Mutex
        runBlocking { lock.lock() }
        try {
            val reading = model.loadAwaitable()
            assertFalse(reading.isCompleted)
            reading.cancel()
            runBlocking { reading.join() }
            assertTrue(reading.isCancelled)
            assertEquals(previous, model.state.value)
        } finally { lock.unlock() }
    }

    @Test fun awaitedExportIsPendingBehindLockAndCompletesAfterOutputAndRefresh() {
        val expected = fixture(); load()
        val revision = model.state.value.snapshotRevision
        val output = ByteArrayOutputStream()
        val field = LedgerViewModel::class.java.getDeclaredField("lock").also { it.isAccessible = true }
        val lock = field.get(model) as Mutex
        runBlocking { lock.lock() }
        val exporting = try {
            model.exportAwaitable { output }.also { assertFalse("Export completion must follow actual output", it.isCompleted) }
        } finally { lock.unlock() }
        runBlocking { exporting.join() }
        assertFalse(exporting.isCancelled)
        assertFalse(model.state.value.busy)
        assertNull(model.state.value.error)
        assertEquals(expected, BackupCodec.decodeBackup(output.toString("UTF-8")).snapshot)
        assertTrue(model.state.value.snapshotRevision > revision)
        assertEquals("备份已导出，请妥善保管", model.state.value.message)
    }

    @Test fun awaitedExportOutputFailureCompletesWithoutLeakingProviderDetails() {
        val previous = model.state.value.snapshot
        var closed = false
        val output = object : java.io.OutputStream() {
            override fun write(value: Int) { throw java.io.IOException("secret-provider-detail") }
            override fun close() { closed = true }
        }
        val exporting = model.exportAwaitable { output }
        runBlocking { exporting.join() }
        assertTrue(exporting.isCompleted)
        assertTrue(closed)
        assertFalse(model.state.value.busy)
        assertSame(previous, model.state.value.snapshot)
        assertTrue(model.state.value.error!!.contains("输出失败"))
        assertFalse(model.state.value.error!!.contains("secret-provider-detail"))
    }

    @Test fun awaitedExportCancellationWhileQueuedNeverOpensTheOutput() {
        val previous = model.state.value
        val field = LedgerViewModel::class.java.getDeclaredField("lock").also { it.isAccessible = true }
        val lock = field.get(model) as Mutex
        var opened = false
        runBlocking { lock.lock() }
        try {
            val exporting = model.exportAwaitable { opened = true; ByteArrayOutputStream() }
            assertFalse(exporting.isCompleted)
            exporting.cancel()
            runBlocking { exporting.join() }
            assertTrue(exporting.isCancelled)
            assertFalse(opened)
            assertEquals(previous, model.state.value)
        } finally { lock.unlock() }
    }

    @Test fun oneLoadPublishesAllFiveImmutableListsFromOneSnapshot() {
        val expected = fixture(); load()
        val state = model.state.value
        assertEquals(expected, state.snapshot)
        assertSame(state.snapshot.entries, state.entries)
        assertSame(state.snapshot.accounts, state.accounts)
        assertSame(state.snapshot.transfers, state.transfers)
        assertSame(state.snapshot.adjustments, state.adjustments)
        assertSame(state.snapshot.categories, state.categories)
        listOf(state.entries, state.accounts, state.transfers, state.adjustments, state.categories).forEach {
            @Suppress("UNCHECKED_CAST") val mutableList = it as MutableList<Any>
            assertThrows(UnsupportedOperationException::class.java) { mutableList.clear() }
        }
    }

    @Test fun atomicInitialCreationPublishesPayloadAndConsumptionOnlyRemovesOwnReadyResult() {
        val a = account(); val request = BalanceAdjustmentRequest(id(), a.id, -500, today, "初始", AdjustmentReason.INITIAL)
        val operation = model.createAccount(a, request); finish(operation)
        assertNull(model.state.value.error)
        assertEquals(listOf(a), model.state.value.accounts)
        val event = database.snapshot().adjustments.single()
        assertEquals(AccountAdjustment(request.id, a.id, -500, today, -500, "初始", AdjustmentReason.INITIAL), event)
        assertEquals(event, receipt(operation).adjustment)
        assertEquals(LedgerOperation.CREATE_ACCOUNT, receipt(operation).operation)
        assertTrue(receipt(operation).committed)
        assertTrue(receipt(operation).snapshotReady)
        val retry = model.createAccount(a, request); finish(retry)
        assertNotEquals(operation, retry)
        assertEquals(event, receipt(retry).adjustment)
        assertEquals(1, database.snapshot().adjustments.size)
        model.consumeCommittedResult(id())
        assertEquals(2, model.state.value.committedResults.size)
        model.consumeCommittedResult(operation)
        assertEquals(listOf(retry), model.state.value.committedResults.map { it.operationId })
        model.consumeCommittedResult(operation)
        model.consumeCommittedResult(retry)
        assertTrue(model.state.value.committedResults.isEmpty())
    }

    @Test fun accountWithoutInitialBalanceStillPublishesExplicitCommittedResultWithNullPayload() {
        val a = account(); val operation = model.createAccount(a); finish(operation)
        assertEquals(listOf(a), model.state.value.accounts)
        assertTrue(receipt(operation).committed)
        assertNull(receipt(operation).adjustment)
        assertTrue(model.state.value.adjustments.isEmpty())
    }

    @Test fun rejectedAccountPreservesFiveListsAndOldCompletionWithoutLeakingInput() {
        val before = fixture(); load()
        val valid = model.saveAccount(before.accounts.first().copy(name = "改名")); finish(valid)
        val loaded = model.state.value.snapshot
        val secret = "不能回显的合成账户名".repeat(4)
        val failed = model.saveAccount(account(secret)); finish(failed)
        assertEquals(failed, model.state.value.failedOperationId)
        assertEquals(valid, model.state.value.completedOperationId)
        assertSame(loaded, model.state.value.snapshot)
        assertEquals(loaded, database.snapshot())
        assertFalse(model.state.value.readError)
        assertTrue(model.state.value.error!!.contains("未改动"))
        assertFalse(model.state.value.error!!.contains(secret))
        assertNull(model.state.value.committedResults.find { it.operationId == failed })
    }

    @Test fun initialWriteWithReadbackFailureKeepsPayloadAndAllOldListsUntilSuccessfulReload() {
        val before = fixture(); load()
        val previous = model.saveAccount(before.accounts.first()); finish(previous)
        val old = model.state.value.snapshot
        database.writableDatabase.execSQL("""CREATE TRIGGER corrupt_initial AFTER INSERT ON adjustments
            BEGIN UPDATE adjustments SET date = 'invalid-date' WHERE id = NEW.id; END""")
        val a = account("新账户"); val request = BalanceAdjustmentRequest(id(), a.id, 70, today, reason = AdjustmentReason.INITIAL)
        val operation = model.createAccount(a, request); finish(operation)
        assertEquals(operation, model.state.value.failedOperationId)
        assertEquals(previous, model.state.value.completedOperationId)
        assertSame(old, model.state.value.snapshot)
        assertTrue(model.state.value.readError)
        assertNull(model.state.value.message)
        assertEquals(request.id, receipt(operation).adjustment!!.id)
        assertEquals(70L, receipt(operation).adjustment!!.deltaCents)
        assertFalse(receipt(operation).snapshotReady)
        assertTrue(model.state.value.error!!.contains("已保存"))
        assertFalse(model.state.value.error!!.contains("未改动"))
        model.consumeCommittedResult(operation)
        assertFalse(receipt(operation).snapshotReady)
        database.writableDatabase.execSQL("DROP TRIGGER corrupt_initial")
        database.writableDatabase.execSQL("UPDATE adjustments SET date = ? WHERE id = ?", arrayOf(today.toString(), request.id))
        load()
        assertFalse(model.state.value.readError)
        assertEquals(database.snapshot(), model.state.value.snapshot)
        assertTrue(receipt(operation).snapshotReady)
        assertEquals(request.id, receipt(operation).adjustment!!.id)
        val retry = model.createAccount(a, request); finish(retry)
        assertEquals(receipt(operation).adjustment, receipt(retry).adjustment)
        assertEquals(2, model.state.value.adjustments.size)
    }

    @Test fun calibrationUsesStableRequestAcrossBalanceChangesMidnightAndArchival() {
        val a = account(); database.saveAccount(a)
        val request = BalanceAdjustmentRequest(id(), a.id, 100, today, "校准")
        val first = model.adjustBalance(request); finish(first)
        val event = receipt(first).adjustment!!
        database.save(Entry(id(), EntryKind.INCOME, 30, "工资", today, accountId = a.id))
        val second = model.adjustBalance(request); finish(second)
        assertEquals(event, receipt(second).adjustment)
        today = today.plusDays(1)
        database.archiveAccount(a.id)
        val third = model.adjustBalance(request); finish(third)
        assertEquals(event, receipt(third).adjustment)
        assertEquals(1, model.state.value.adjustments.size)
        assertEquals(130L, AccountBalances.balances(model.state.value.snapshot, today)[a.id])
        val failed = model.adjustBalance(request.copy(targetBalanceCents = 200)); finish(failed)
        assertEquals(failed, model.state.value.failedOperationId)
        assertEquals(third, model.state.value.completedOperationId)
        assertNull(model.state.value.committedResults.find { it.operationId == failed })
        assertEquals(1, database.snapshot().adjustments.size)
    }

    @Test fun firstStaleDateRequestIsRejectedWithoutSilentlyChangingItsIdentity() {
        val a = account(); database.saveAccount(a)
        val request = BalanceAdjustmentRequest(id(), a.id, 0, today)
        today = today.plusDays(1)
        val operation = model.adjustBalance(request); finish(operation)
        assertEquals(operation, model.state.value.failedOperationId)
        assertTrue(model.state.value.error!!.contains("日期"))
        assertTrue(database.snapshot().adjustments.isEmpty())
        assertEquals(today.minusDays(1), request.date)
    }

    @Test fun accountTransferAdjustmentAndCategoryCrudUseTheSamePipeline() {
        val a = account(); val b = account("银行")
        finish(model.saveAccount(a)); finish(model.saveAccount(b))
        finish(model.archiveAccount(a.id)); assertTrue(model.state.value.accounts.single { it.id == a.id }.archived)
        finish(model.archiveAccount(a.id, false))
        val transfer = AccountTransfer(id(), a.id, b.id, 25, today)
        finish(model.saveTransfer(transfer)); assertEquals(listOf(transfer), model.state.value.transfers)
        finish(model.deleteTransfer(transfer.id)); assertTrue(model.state.value.transfers.isEmpty())
        val adjusted = model.adjustBalance(BalanceAdjustmentRequest(id(), a.id, 40, today)); finish(adjusted)
        finish(model.deleteAdjustment(receipt(adjusted).adjustment!!.id)); assertTrue(model.state.value.adjustments.isEmpty())
        val category = Category(id(), EntryKind.INCOME, "自定义", 99)
        finish(model.saveCategory(category))
        val order = model.state.value.categories.filter { it.kind == EntryKind.INCOME && !it.archived }.map { it.id }.reversed()
        finish(model.reorderCategories(EntryKind.INCOME, order))
        assertEquals(order, model.state.value.categories.filter { it.kind == EntryKind.INCOME }.sortedBy { it.sortOrder }.map { it.id })
        assertEquals(database.snapshot(), model.state.value.snapshot)
    }

    @Test fun busyOperationQueuesNewMutationWithoutConcurrentDatabaseWork() {
        val started = CountDownLatch(1); val release = CountDownLatch(1)
        val a = account()
        try {
            model.restore { started.countDown(); check(release.await(10, TimeUnit.SECONDS)); ByteArrayInputStream("broken".toByteArray()) }
            await { model.state.value.busy && started.count == 0L }
            val queued = model.saveAccount(a)
            assertNotNull(queued)
            assertTrue(database.snapshot().accounts.isEmpty())
            assertTrue(model.state.value.busy)
            release.countDown(); finish(queued)
            assertEquals(listOf(a), model.state.value.accounts)
            assertEquals(1, database.snapshot().accounts.size)
        } finally { release.countDown() }
    }

    @Test fun clearingQueuedModelCancelsMutationAndPreservesCloseLockContract() {
        val field = LedgerViewModel::class.java.getDeclaredField("lock").also { it.isAccessible = true }
        val lock = field.get(model) as Mutex
        val databaseField = LedgerViewModel::class.java.getDeclaredField("database").also { it.isAccessible = true }
        val opened = (databaseField.get(model) as LedgerDatabase).readableDatabase
        runBlocking { lock.lock() }
        try {
            model.saveAccount(account()); store.clear()
            assertTrue("关闭必须等待同一个lock", opened.isOpen)
        } finally { lock.unlock() }
        await { !opened.isOpen }
        assertTrue(database.snapshot().accounts.isEmpty())
        assertTrue(model.state.value.committedResults.isEmpty())
        assertNull(model.state.value.completedOperationId)
    }

    @Test fun legacyImportSkipsSameSixFieldsAndRetainsLocalAccountAssociation() {
        val a = account(); database.saveAccount(a)
        val linked = Entry(id(), EntryKind.EXPENSE, 10, "餐饮", today, "原六字段", a.id)
        database.save(linked); load()
        val operation = model.restore { ByteArrayInputStream(BackupCodec.encode(listOf(linked.copy(accountId = null))).toByteArray()) }
        finish(operation)
        assertEquals(listOf(linked), model.state.value.entries)
        assertEquals(1, receipt(operation).importResult!!.skipped)
        assertEquals("恢复完成：新增 0 条，跳过 1 条", model.state.value.message)
    }

    @Test fun accountOnlyRestoreReportsEntityCountsAndAdoptedCatalog() {
        val a = account()
        val incoming = LedgerSnapshot(accounts = listOf(a), categories = emptyList())
        val operation = model.restore { ByteArrayInputStream(BackupCodec.encodeSnapshot(incoming).toByteArray()) }; finish(operation)
        assertEquals(incoming, model.state.value.snapshot)
        assertEquals(1, receipt(operation).importResult!!.accountsImported)
        assertTrue(receipt(operation).importResult!!.catalogAdopted)
        assertTrue(model.state.value.message!!.contains("账户：新增 1"))
        assertTrue(model.state.value.message!!.contains("已恢复类别设置"))
        val duplicate = model.restore { ByteArrayInputStream(BackupCodec.encodeSnapshot(incoming).toByteArray()) }; finish(duplicate)
        assertEquals(1, receipt(duplicate).importResult!!.accountsSkipped)
        assertFalse(receipt(duplicate).importResult!!.catalogAdopted)
        assertFalse(model.state.value.message!!.contains("已恢复类别设置"))
    }

    @Test fun committedImportWithReadFailurePreservesResultThroughReload() {
        val before = model.state.value.snapshot
        val entry = Entry(id(), EntryKind.INCOME, 5, "工资", today)
        database.writableDatabase.execSQL("""CREATE TRIGGER corrupt_import AFTER INSERT ON entries
            BEGIN UPDATE entries SET date = 'invalid-date' WHERE id = NEW.id; END""")
        val operation = model.restore { ByteArrayInputStream(BackupCodec.encode(listOf(entry)).toByteArray()) }; finish(operation)
        assertEquals(operation, model.state.value.failedOperationId)
        assertSame(before, model.state.value.snapshot)
        assertEquals(1, receipt(operation).importResult!!.imported)
        assertFalse(receipt(operation).snapshotReady)
        assertTrue(model.state.value.error!!.contains("已导入"))
        assertFalse(model.state.value.error!!.contains("未导入"))
        database.writableDatabase.execSQL("DROP TRIGGER corrupt_import")
        database.writableDatabase.execSQL("UPDATE entries SET date = ? WHERE id = ?", arrayOf(today.toString(), entry.id))
        load(); assertTrue(receipt(operation).snapshotReady)
        assertEquals(listOf(entry), model.state.value.entries)
    }

    @Test fun versionOneStillHasIndependentTenMiBByteGateWithoutMerging() {
        val secret = Entry(id(), EntryKind.EXPENSE, 1, "餐饮", today, "敏感合成输入")
        val raw = BackupCodec.encode(listOf(secret)) + " ".repeat(BackupCodec.MAX_BYTES)
        val operation = model.restore { ByteArrayInputStream(raw.toByteArray()) }; finish(operation)
        assertTrue(model.state.value.error!!.contains("10 MiB"))
        assertFalse(model.state.value.error!!.contains(secret.note))
        assertTrue(database.snapshot().entries.isEmpty())
        assertFalse(model.state.value.readError)
    }

    @Test fun validVersionTwoAboveTenMiBIsAcceptedAndInputStreamIsClosed() {
        val a = account()
        val raw = BackupCodec.encodeSnapshot(LedgerSnapshot(accounts = listOf(a))) + " ".repeat(BackupCodec.MAX_BYTES)
        assertTrue(raw.toByteArray().size > BackupCodec.MAX_BYTES)
        var closed = false
        val operation = model.restore { object : ByteArrayInputStream(raw.toByteArray()) { override fun close() { closed = true; super.close() } } }; finish(operation)
        assertNull(model.state.value.error)
        assertTrue(closed)
        assertEquals(listOf(a), model.state.value.accounts)
    }

    @Test fun failedTransactionDoesNotCreateReceiptOrExposeSqlError() {
        database.writableDatabase.execSQL("""CREATE TRIGGER fail_account BEFORE INSERT ON accounts
            BEGIN SELECT RAISE(ABORT, '合成敏感数据库内容'); END""")
        val operation = model.createAccount(account()); finish(operation)
        assertEquals(operation, model.state.value.failedOperationId)
        assertFalse(model.state.value.error!!.contains("合成敏感数据库内容"))
        assertTrue(model.state.value.error!!.contains("未改动"))
        assertNull(model.state.value.committedResults.find { it.operationId == operation })
        assertFalse(model.state.value.readError)
        assertTrue(database.snapshot().accounts.isEmpty())
    }

    @Test fun applicationOnlyJavaConstructorAndAndroidViewModelFactoryStillWork() {
        assertNotNull(LedgerViewModel::class.java.getConstructor(Application::class.java))
        val other = ViewModelProvider.AndroidViewModelFactory.getInstance(application).create(LedgerViewModel::class.java)
        store.put("factory", other)
        await { !other.state.value.busy }
        assertEquals(database.snapshot(), other.state.value.snapshot)
    }

    @Test fun completedOutputWithFailedRefreshIsReportedAsWrittenFile() {
        val expected = fixture(); load()
        val output = object : ByteArrayOutputStream() {
            override fun close() { super.close(); database.writableDatabase.execSQL("DROP TABLE entries") }
        }
        model.export { output }
        await { model.state.value.error != null && !model.state.value.busy }
        assertEquals(expected, BackupCodec.decodeBackup(output.toString("UTF-8")).snapshot)
        assertTrue(model.state.value.readError)
        assertEquals(expected, model.state.value.snapshot)
        assertTrue(model.state.value.error!!.contains("备份已导出"))
        assertFalse(model.state.value.error!!.contains("新文件可能不完整"))
    }

    @Test fun formalExportUsesFormatTwoAndEveryEntityFromDatabase() {
        val first = account(); val second = account("银行卡")
        database.createAccount(first, BalanceAdjustmentRequest(id(), first.id, 1000, today, reason = AdjustmentReason.INITIAL))
        database.saveAccount(second)
        database.save(Entry(id(), EntryKind.EXPENSE, 100, "餐饮", today, accountId = first.id))
        database.saveTransfer(AccountTransfer(id(), first.id, second.id, 200, today))
        database.saveCategory(Category(id(), EntryKind.INCOME, "自定义类别", 20))
        val expected = database.snapshot()
        val output = ByteArrayOutputStream()
        model.export { output }
        await { model.state.value.error != null || model.state.value.message?.startsWith("备份已导出") == true }
        assertNull("正式导出必须支持已有账户关联", model.state.value.error)
        val decoded = BackupCodec.decodeBackup(output.toString("UTF-8"))
        assertEquals(2, decoded.sourceVersion)
        assertEquals(expected, decoded.snapshot)
    }
}

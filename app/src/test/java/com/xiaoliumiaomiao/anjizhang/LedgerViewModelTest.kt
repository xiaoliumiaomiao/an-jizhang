package com.xiaoliumiaomiao.anjizhang

import android.app.Application
import android.os.Looper
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import com.xiaoliumiaomiao.anjizhang.data.BackupCodec
import com.xiaoliumiaomiao.anjizhang.domain.*
import com.xiaoliumiaomiao.anjizhang.ui.LedgerViewModel
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.time.LocalDate
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class LedgerViewModelTest {
    private lateinit var application: Application
    private lateinit var model: LedgerViewModel
    private val store = ViewModelStore()
    @Before fun setup() {
        application = ApplicationProvider.getApplicationContext()
        application.deleteDatabase("ledger.db")
        model = LedgerViewModel(application)
        store.put("ledger", model)
        await { !model.state.value.busy }
    }
    @After fun teardown() { store.clear() }
    private fun await(predicate: () -> Boolean) {
        val deadline = System.nanoTime() + 10_000_000_000L
        do { Shadows.shadowOf(Looper.getMainLooper()).idle(); Thread.sleep(10) } while (!predicate() && System.nanoTime() < deadline)
        assertTrue("等待后台操作超时：${model.state.value}", predicate())
    }
    private fun entry(cents: Long = 1) = Entry(UUID.randomUUID().toString(), EntryKind.EXPENSE, cents, "餐饮", LocalDate.now())
    @Test fun repeatedSuccessHasOwnExpiryAndStaleDismissDoesNotClearNewMessage() {
        val first = entry(); val second = entry(2)
        val saved = model.save(first)
        await { model.state.value.completedOperationId == saved && !model.state.value.busy }
        val savedSecond = model.save(second)
        await { model.state.value.completedOperationId == savedSecond && !model.state.value.busy }
        val deletedFirst = model.delete(first)
        await { model.state.value.completedOperationId == deletedFirst && !model.state.value.busy }
        assertNotNull("成功提示应有自己的身份", model.state.value.messageId)
        val firstMessage = model.state.value.messageId!!
        assertNotNull(model.state.value.messageExpiresAt)
        assertTrue(model.state.value.messageExpiresAt!! > android.os.SystemClock.elapsedRealtime())
        val deletedSecond = model.delete(second)
        await { model.state.value.completedOperationId == deletedSecond && !model.state.value.busy }
        assertEquals("已删除", model.state.value.message)
        val secondMessage = model.state.value.messageId!!
        assertNotEquals(firstMessage, secondMessage)
        model.clearMessage(firstMessage)
        assertEquals("已删除", model.state.value.message)
        assertEquals(secondMessage, model.state.value.messageId)
        model.clearMessage(secondMessage)
        assertNull(model.state.value.message)
        assertNull(model.state.value.messageId)
        assertNull(model.state.value.messageExpiresAt)
        assertEquals(deletedSecond, model.state.value.completedOperationId)
        assertTrue(model.state.value.entries.isEmpty())
    }
    @Test fun exportWithoutOperationIdGetsFreshMessageAndDismissKeepsFailedState() {
        val valid = entry(); val saved = model.save(valid)
        await { model.state.value.completedOperationId == saved && !model.state.value.busy }
        model.export { ByteArrayOutputStream() }
        await { model.state.value.message?.startsWith("备份已导出") == true && !model.state.value.busy }
        assertNotNull(model.state.value.messageId)
        val exported = model.state.value.messageId!!
        model.export { ByteArrayOutputStream() }
        await { model.state.value.messageId != exported && !model.state.value.busy }
        assertNotNull(model.state.value.messageId)
        val latest = model.state.value.messageId!!
        model.clearMessage(exported)
        assertEquals(latest, model.state.value.messageId)
        val failed = model.save(valid.copy(amountCents = 0))
        await { model.state.value.failedOperationId == failed && !model.state.value.busy }
        val before = model.state.value
        model.clearMessage(latest)
        assertEquals(before, model.state.value)
        assertEquals(listOf(valid), model.state.value.entries)
        assertNotNull(model.state.value.error)
        assertEquals(saved, model.state.value.completedOperationId)
    }
    @Test fun databaseReadFailureShowsErrorAndRetryRecovers() {
        val outside = com.xiaoliumiaomiao.anjizhang.data.LedgerDatabase(application)
        try {
            outside.writableDatabase.execSQL("DROP TABLE entries")
            model.load()
            await { model.state.value.error != null && !model.state.value.busy }
            assertTrue(model.state.value.error!!.contains("操作失败"))
            assertTrue(model.state.value.readError)
            recreateEntriesTable(outside.writableDatabase)
            model.load()
            await { model.state.value.error == null && !model.state.value.busy }
            assertTrue(model.state.value.entries.isEmpty())
            assertFalse(model.state.value.readError)
        } finally { outside.close() }
    }
    @Test fun successfulSavePublishesUniqueCompletionForNewObserver() {
        val valid = entry()
        val operation = model.save(valid)
        await { model.state.value.entries.size == 1 && !model.state.value.busy }
        val completion = model.state.value.completedOperationId
        assertEquals("成功操作ID必须保留在可观察状态，不能只通知旧闭包", operation, completion)
    }
    @Test fun deletePublishesDifferentCompletionAndFailedSaveCannotReuseOldSuccess() {
        val valid = entry()
        val saved = model.save(valid)
        await { model.state.value.completedOperationId == saved && !model.state.value.busy }
        val failed = model.save(valid.copy(amountCents = 0))
        await { model.state.value.error != null && !model.state.value.busy }
        assertNotEquals(saved, failed)
        assertEquals(saved, model.state.value.completedOperationId)
        val deleted = model.delete(valid)
        assertNotEquals(saved, deleted)
        await { model.state.value.completedOperationId == deleted && !model.state.value.busy }
        assertTrue(model.state.value.entries.isEmpty())
    }
    @Test fun malformedUtf8DoesNotMerge() {
        model.restore { ByteArrayInputStream(byteArrayOf(0xC3.toByte(), 0x28)) }
        await { model.state.value.error != null && !model.state.value.busy }
        assertTrue(model.state.value.entries.isEmpty())
    }
    @Test fun failedOutputReportsErrorWithoutChangingLedger() {
        val valid = entry().copy(note = "不能回显的合成备注")
        val saved = model.save(valid)
        await { model.state.value.completedOperationId == saved && !model.state.value.busy }
        model.export { object : java.io.OutputStream() { override fun write(value: Int) { throw java.io.IOException("输出失败：${valid.id}/${valid.category}/${valid.note}") } } }
        await { model.state.value.error?.contains("输出失败") == true && !model.state.value.busy }
        assertEquals(listOf(valid), model.state.value.entries)
        assertTrue(model.state.value.error!!.contains("新文件可能不完整"))
        assertTrue(model.state.value.error!!.contains("保留旧备份"))
        assertFalse(model.state.value.error!!.contains(valid.id))
        assertFalse(model.state.value.error!!.contains(valid.category))
        assertFalse(model.state.value.error!!.contains(valid.note))
    }
    @Test fun startsEmptyWithoutDemoData() { assertTrue(model.state.value.entries.isEmpty()); assertNull(model.state.value.error) }
    @Test fun failedSaveKeepsDataAndAllowsRetry() {
        val failedOperation = model.save(entry(0))
        await { model.state.value.error != null && !model.state.value.busy }
        assertNotEquals(failedOperation, model.state.value.completedOperationId); assertTrue(model.state.value.entries.isEmpty())
        val valid = entry()
        val savedOperation = model.save(valid)
        await { model.state.value.completedOperationId == savedOperation && !model.state.value.busy }
        assertEquals(listOf(valid), model.state.value.entries); assertNull(model.state.value.error)
    }
    @Test fun exportAndDuplicateRestoreReportCounts() {
        val valid = entry(); model.save(valid)
        await { model.state.value.entries.size == 1 && !model.state.value.busy }
        val output = ByteArrayOutputStream()
        model.export { output }
        await { model.state.value.message?.startsWith("备份已导出") == true && !model.state.value.busy }
        val decoded = BackupCodec.decodeBackup(output.toString("UTF-8"))
        assertEquals(2, decoded.sourceVersion)
        assertEquals(model.state.value.snapshot, decoded.snapshot)
        assertEquals(listOf(valid), decoded.snapshot.entries)
        model.restore { ByteArrayInputStream(output.toByteArray()) }
        await { model.state.value.message?.startsWith("恢复完成") == true && !model.state.value.busy }
        assertEquals("恢复完成：新增 0 条，跳过 1 条", model.state.value.message)
    }
    @Test fun invalidAndConflictingImportsPreserveExistingData() {
        val valid = entry(); model.save(valid)
        await { model.state.value.entries.size == 1 && !model.state.value.busy }
        model.restore { ByteArrayInputStream("broken".toByteArray()) }
        await { model.state.value.error != null && !model.state.value.busy }
        assertEquals(listOf(valid), model.state.value.entries)
        val conflict = BackupCodec.encode(listOf(valid.copy(amountCents = 2), entry()))
        model.restore { ByteArrayInputStream(conflict.toByteArray()) }
        await { model.state.value.error?.contains("冲突") == true && !model.state.value.busy }
        assertEquals(listOf(valid), model.state.value.entries)
        assertFalse(model.state.value.error!!.contains(valid.id))
    }
    @Test fun failedReadDoesNotMergeAndCanRecover() {
        val secret = "不能回显的合成provider账本片段"
        model.restore { throw java.io.IOException("读取失败：$secret") }
        await { model.state.value.error != null && !model.state.value.busy }
        assertTrue(model.state.value.entries.isEmpty())
        assertTrue(model.state.value.error!!.contains("读取失败"))
        assertFalse(model.state.value.error!!.contains(secret))
        val valid = entry()
        model.restore { ByteArrayInputStream(BackupCodec.encode(listOf(valid)).toByteArray()) }
        await { model.state.value.entries.size == 1 && !model.state.value.busy }
        assertEquals("恢复完成：新增 1 条，跳过 0 条", model.state.value.message)
    }
    private fun invalidBackupErrorMustBePrivate(mutate: (String) -> String, secret: String) {
        val valid = entry().copy(date = LocalDate.of(2026, 1, 1))
        val saved = model.save(valid)
        await { model.state.value.completedOperationId == saved && !model.state.value.busy }
        model.restore { ByteArrayInputStream(mutate(BackupCodec.encode(listOf(valid))).toByteArray()) }
        await { model.state.value.error != null && !model.state.value.busy }
        assertTrue(model.state.value.error!!.contains("备份无效"))
        assertFalse(model.state.value.error!!.contains(secret))
        assertEquals(listOf(valid), model.state.value.entries)
    }
    @Test fun invalidBackupDateDoesNotEchoRawValue() {
        invalidBackupErrorMustBePrivate({ it.replace("2026-01-01", "2026-02-30") }, "2026-02-30")
    }
    @Test fun invalidBackupKindDoesNotEchoRawValue() {
        invalidBackupErrorMustBePrivate({ it.replace("EXPENSE", "敏感错误类型") }, "敏感错误类型")
    }
    @Test fun oversizedStreamReadsOnlyLimitPlusOneBytes() {
        var read = 0
        val stream = object : InputStream() {
            override fun read(): Int { read++; return 32 }
            override fun read(buffer: ByteArray, offset: Int, length: Int): Int { buffer.fill(32, offset, offset + length); read += length; return length }
        }
        model.restore { stream }
        await { model.state.value.error?.contains("32 MiB") == true && !model.state.value.busy }
        assertEquals(BackupCodec.MAX_SNAPSHOT_BYTES + 1, read); assertTrue(model.state.value.entries.isEmpty())
    }
    private fun assertDatabaseMatches(expected: List<Entry>) {
        val outside = com.xiaoliumiaomiao.anjizhang.data.LedgerDatabase(application)
        try {
            assertEquals(expected.toSet(), outside.listEntries().toSet())
        } finally { outside.close() }
    }
    @Test fun damagedRestoreKeepsLoadedStatisticsReadable() {
        val valid = entry(1234).copy(note = "合成恢复测试")
        val saved = model.save(valid)
        await { model.state.value.completedOperationId == saved && !model.state.value.busy }
        model.restore { ByteArrayInputStream("broken".toByteArray()) }
        await { model.state.value.error?.contains("备份无效") == true && !model.state.value.busy }
        assertFalse("损坏备份不应让已读取的账目统计失效", model.state.value.readError)
        assertEquals(listOf(valid), model.state.value.entries)
        assertEquals(saved, model.state.value.completedOperationId)
        assertDatabaseMatches(listOf(valid))
    }
    @Test fun backupInputFailureKeepsLoadedStatisticsReadable() {
        val valid = entry(1234)
        val saved = model.save(valid)
        await { model.state.value.completedOperationId == saved && !model.state.value.busy }
        model.restore { throw java.io.IOException("合成provider读取失败") }
        await { model.state.value.error?.contains("读取失败") == true && !model.state.value.busy }
        assertFalse("备份文件读取失败不是账目读取失败", model.state.value.readError)
        assertEquals(listOf(valid), model.state.value.entries)
        assertDatabaseMatches(listOf(valid))
    }
    @Test fun failedOutputKeepsLoadedStatisticsReadable() {
        val valid = entry(1234)
        val saved = model.save(valid)
        await { model.state.value.completedOperationId == saved && !model.state.value.busy }
        model.export { object : java.io.OutputStream() {
            override fun write(value: Int) { throw java.io.IOException("合成输出失败") }
        } }
        await { model.state.value.error?.contains("输出失败") == true && !model.state.value.busy }
        assertFalse("导出输出失败不应让有效统计失效", model.state.value.readError)
        assertEquals(listOf(valid), model.state.value.entries)
        assertEquals(saved, model.state.value.completedOperationId)
        assertDatabaseMatches(listOf(valid))
    }
    @Test fun rejectedSaveKeepsLoadedStatisticsReadableAndOwnFailureId() {
        val valid = entry(1234)
        val saved = model.save(valid)
        await { model.state.value.completedOperationId == saved && !model.state.value.busy }
        val failed = model.save(valid.copy(amountCents = 0))
        await { model.state.value.failedOperationId == failed && !model.state.value.busy }
        assertFalse("输入校验失败不应让有效统计失效", model.state.value.readError)
        assertNotNull(model.state.value.error)
        assertEquals(saved, model.state.value.completedOperationId)
        assertEquals(listOf(valid), model.state.value.entries)
        assertDatabaseMatches(listOf(valid))
    }
    @Test fun readFailureSurvivesInProgressAndFailedRestoreUntilSuccessfulReload() {
        val valid = entry(1234)
        val saved = model.save(valid)
        await { model.state.value.completedOperationId == saved && !model.state.value.busy }
        val outside = com.xiaoliumiaomiao.anjizhang.data.LedgerDatabase(application)
        try {
            outside.writableDatabase.execSQL("DROP TABLE entries")
            model.load()
            await { model.state.value.error?.contains("读取账目失败") == true && !model.state.value.busy }
            assertTrue(model.state.value.readError)
            val started = java.util.concurrent.CountDownLatch(1)
            val release = java.util.concurrent.CountDownLatch(1)
            try {
                model.restore {
                    started.countDown()
                    check(release.await(10, java.util.concurrent.TimeUnit.SECONDS))
                    ByteArrayInputStream("broken".toByteArray())
                }
                await { model.state.value.busy && started.count == 0L }
                assertTrue("另一操作开始不能解除真实读错", model.state.value.readError)
            } finally { release.countDown() }
            await { model.state.value.error?.contains("备份无效") == true && !model.state.value.busy }
            assertTrue("另一操作失败不能解除真实读错", model.state.value.readError)
            assertEquals(listOf(valid), model.state.value.entries)
            recreateEntriesTable(outside.writableDatabase)
            outside.save(valid)
            model.load()
            await { model.state.value.error == null && !model.state.value.busy }
            assertFalse("成功重读应解除真实读错", model.state.value.readError)
            assertEquals(listOf(valid), model.state.value.entries)
        } finally { outside.close() }
    }
    // Repair only the table this fixture deliberately removed; the schema2 catalog is untouched.
    private fun recreateEntriesTable(db: android.database.sqlite.SQLiteDatabase) {
        db.execSQL("""CREATE TABLE entries (
            id TEXT PRIMARY KEY NOT NULL,
            kind TEXT NOT NULL CHECK(kind IN ('EXPENSE','INCOME')),
            amount_cents INTEGER NOT NULL CHECK(amount_cents BETWEEN 1 AND 99999999999),
            category TEXT NOT NULL, date TEXT NOT NULL, note TEXT NOT NULL,
            account_id TEXT REFERENCES accounts(id))""")
    }

    @Test fun exportDatabaseReadFailureMarksLedgerUnreadable() {
        var openedOutput = false
        val outside = com.xiaoliumiaomiao.anjizhang.data.LedgerDatabase(application)
        try {
            outside.writableDatabase.execSQL("DROP TABLE entries")
            model.export { openedOutput = true; ByteArrayOutputStream() }
            await { model.state.value.error != null && !model.state.value.busy }
            assertTrue("导出中的账目读取失败也是真实读错", model.state.value.readError)
            assertFalse(openedOutput)
        } finally { outside.close() }
    }
    @Test fun successfulSaveWithFailedRefreshMarksLedgerUnreadableAndKeepsOldSnapshot() {
        val valid = entry(1234)
        val saved = model.save(valid)
        await { model.state.value.completedOperationId == saved && !model.state.value.busy }
        val outside = com.xiaoliumiaomiao.anjizhang.data.LedgerDatabase(application)
        try {
            outside.writableDatabase.execSQL("""CREATE TRIGGER corrupt_new_date AFTER INSERT ON entries
                BEGIN UPDATE entries SET date = 'invalid-date' WHERE id = NEW.id; END""")
            val attempted = model.save(entry(5678))
            await { model.state.value.failedOperationId == attempted && !model.state.value.busy }
            assertTrue("保存后的快照刷新失败必须标记真实读错", model.state.value.readError)
            val readFailure = requireNotNull(model.state.value.error)
            assertFalse("保存已经写入，不能承诺原有账目未被替换", readFailure.contains("原有账目未被替换"))
            assertTrue("真实读取失败应提示重新读取", readFailure.contains("无法重新读取账目，请重试读取"))
            assertEquals(saved, model.state.value.completedOperationId)
            assertEquals(listOf(valid), model.state.value.entries)
            assertNull(model.state.value.message)
            outside.readableDatabase.rawQuery("SELECT COUNT(*) FROM entries WHERE date = 'invalid-date'", null).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("实际保存已发生，失败来自最终读取", 1, cursor.getInt(0))
            }
        } finally { outside.close() }
    }
    @Test fun repeatedFailuresPublishOwnIdsWithoutReplacingOldSuccessAndRetryWorks() {
        val valid=entry();val success=model.save(valid)
        await { model.state.value.completedOperationId==success && !model.state.value.busy }
        val firstFailure=model.save(valid.copy(amountCents=0))
        await { model.state.value.error!=null && !model.state.value.busy }
        val firstMessage=model.state.value.error
        assertEquals(firstFailure,model.state.value.failedOperationId)
        assertEquals(success,model.state.value.completedOperationId)
        val secondFailure=model.save(valid.copy(amountCents=0))
        await { model.state.value.failedOperationId==secondFailure && !model.state.value.busy }
        assertNotEquals(firstFailure,secondFailure)
        assertEquals(firstMessage,model.state.value.error)
        assertEquals(success,model.state.value.completedOperationId)
        assertEquals(listOf(valid),model.state.value.entries)
        val retry=model.save(valid.copy(amountCents=2))
        await { model.state.value.completedOperationId==retry && !model.state.value.busy }
        assertEquals(listOf(valid.copy(amountCents=2)),model.state.value.entries)
        assertNull(model.state.value.error)
    }
}

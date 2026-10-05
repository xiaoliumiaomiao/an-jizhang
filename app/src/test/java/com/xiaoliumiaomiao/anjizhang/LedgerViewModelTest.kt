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
            outside.onCreate(outside.writableDatabase)
            model.load()
            await { model.state.value.error == null && !model.state.value.busy }
            assertTrue(model.state.value.entries.isEmpty())
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
        val valid = entry()
        val saved = model.save(valid)
        await { model.state.value.completedOperationId == saved && !model.state.value.busy }
        model.export { object : java.io.OutputStream() { override fun write(value: Int) { throw java.io.IOException("输出失败") } } }
        await { model.state.value.error?.contains("输出失败") == true && !model.state.value.busy }
        assertEquals(listOf(valid), model.state.value.entries)
        assertTrue(model.state.value.error!!.contains("新文件可能不完整"))
        assertTrue(model.state.value.error!!.contains("保留旧备份"))
        assertFalse(model.state.value.error!!.contains(valid.id))
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
        assertEquals(listOf(valid), BackupCodec.decode(output.toString("UTF-8")))
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
    }
    @Test fun failedReadDoesNotMergeAndCanRecover() {
        model.restore { throw java.io.IOException("读取失败") }
        await { model.state.value.error != null && !model.state.value.busy }
        assertTrue(model.state.value.entries.isEmpty())
        val valid = entry()
        model.restore { ByteArrayInputStream(BackupCodec.encode(listOf(valid)).toByteArray()) }
        await { model.state.value.entries.size == 1 && !model.state.value.busy }
        assertEquals("恢复完成：新增 1 条，跳过 0 条", model.state.value.message)
    }
    @Test fun oversizedStreamReadsOnlyLimitPlusOneBytes() {
        var read = 0
        val stream = object : InputStream() {
            override fun read(): Int { read++; return 32 }
            override fun read(buffer: ByteArray, offset: Int, length: Int): Int { buffer.fill(32, offset, offset + length); read += length; return length }
        }
        model.restore { stream }
        await { model.state.value.error?.contains("10 MiB") == true && !model.state.value.busy }
        assertEquals(BackupCodec.MAX_BYTES + 1, read); assertTrue(model.state.value.entries.isEmpty())
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

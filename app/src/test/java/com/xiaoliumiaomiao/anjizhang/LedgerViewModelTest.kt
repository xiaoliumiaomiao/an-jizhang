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
    @Test fun startsEmptyWithoutDemoData() { assertTrue(model.state.value.entries.isEmpty()); assertNull(model.state.value.error) }
    @Test fun failedSaveKeepsDataAndAllowsRetry() {
        var success = false
        model.save(entry(0)) { success = true }
        await { model.state.value.error != null && !model.state.value.busy }
        assertFalse(success); assertTrue(model.state.value.entries.isEmpty())
        val valid = entry()
        model.save(valid) { success = true }
        await { success && !model.state.value.busy }
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
}

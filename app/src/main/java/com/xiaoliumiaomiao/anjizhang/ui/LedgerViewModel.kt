package com.xiaoliumiaomiao.anjizhang.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xiaoliumiaomiao.anjizhang.data.BackupCodec
import com.xiaoliumiaomiao.anjizhang.data.LedgerDatabase
import com.xiaoliumiaomiao.anjizhang.domain.Entry
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction

data class LedgerUiState(val entries: List<Entry> = emptyList(), val busy: Boolean = false,
    val error: String? = null, val message: String? = null, val completedOperationId: String? = null,
    val failedOperationId: String? = null)

class LedgerViewModel(application: Application) : AndroidViewModel(application) {
    val instanceId: String = UUID.randomUUID().toString()
    private val mutable = MutableStateFlow(LedgerUiState(busy = true))
    val state: StateFlow<LedgerUiState> = mutable.asStateFlow()
    private val lock = Mutex()
    private var database: LedgerDatabase? = null
    private fun db(): LedgerDatabase = database ?: LedgerDatabase(getApplication()).also { database = it }
    init { load() }
    private fun work(action: suspend () -> String?, operationId: String? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            lock.withLock {
                mutable.update { it.copy(busy = true, error = null, message = null) }
                try {
                    val message = action()
                    val entries = db().listEntries()
                    mutable.update { it.copy(entries = entries, message = message, completedOperationId = operationId ?: it.completedOperationId) }
                } catch (cancelled: CancellationException) { throw cancelled
                } catch (failure: Exception) {
                    mutable.update { it.copy(error = "操作失败：${failure.message ?: "请重试"}。原有账目未被替换。",
    failedOperationId = operationId ?: it.failedOperationId) }
                } finally { mutable.update { it.copy(busy = false) } }
            }
        }
    }
    fun load() = work({ null })
    fun save(entry: Entry): String = UUID.randomUUID().toString().also { operationId ->
        work({ db().save(entry); "已保存" }, operationId)
    }
    fun delete(entry: Entry): String = UUID.randomUUID().toString().also { operationId ->
        work({ db().delete(entry.id); "已删除" }, operationId)
    }
    fun export(open: () -> OutputStream?) = work({
        val bytes = BackupCodec.encode(db().listEntries()).toByteArray(Charsets.UTF_8)
        (open() ?: error("无法打开备份位置")).use { it.write(bytes) }
        "备份已导出，请妥善保管"
    })
    fun restore(open: () -> InputStream?) = work({
        val bytes = (open() ?: error("无法读取备份文件")).use { input ->
            val output = java.io.ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            var remaining = BackupCodec.MAX_BYTES + 1
            while (remaining > 0) {
                val count = input.read(buffer, 0, minOf(buffer.size, remaining))
                if (count < 0) break
                if (count == 0) continue
                output.write(buffer, 0, count); remaining -= count
            }
            output.toByteArray()
        }
        require(bytes.size <= BackupCodec.MAX_BYTES) { "备份超过 10 MiB" }
        val text = Charsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString()
        val validated = BackupCodec.decode(text)
        val result = db().merge(validated)
        "恢复完成：新增 ${result.imported} 条，跳过 ${result.skipped} 条"
    })
    override fun onCleared() {
        super.onCleared()
        CoroutineScope(Dispatchers.IO).launch { lock.withLock { database?.close(); database = null } }
    }
}

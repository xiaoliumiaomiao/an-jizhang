package com.xiaoliumiaomiao.anjizhang.ui

import android.app.Application
import android.os.SystemClock
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
    val failedOperationId: String? = null, val messageId: String? = null, val messageExpiresAt: Long? = null)

class LedgerViewModel(application: Application) : AndroidViewModel(application) {
    val instanceId: String = UUID.randomUUID().toString()
    private val mutable = MutableStateFlow(LedgerUiState(busy = true))
    val state: StateFlow<LedgerUiState> = mutable.asStateFlow()
    fun clearMessage(messageId: String) {
        mutable.update { if (it.messageId == messageId) it.copy(message = null, messageId = null, messageExpiresAt = null) else it }
    }
    private val lock = Mutex()
    private var database: LedgerDatabase? = null
    private fun db(): LedgerDatabase = database ?: LedgerDatabase(getApplication()).also { database = it }
    init { load() }
    private fun work(action: suspend () -> String?, operationId: String? = null,
        failureText: (Exception) -> String = { "无法完成操作，请重试" }) {
        viewModelScope.launch(Dispatchers.IO) {
            lock.withLock {
                mutable.update { it.copy(busy = true, error = null, message = null, messageId = null, messageExpiresAt = null) }
                try {
                    val message = action()
                    val entries = db().listEntries()
                    val messageId = message?.let { UUID.randomUUID().toString() }
                    val expiresAt = message?.let { SystemClock.elapsedRealtime() + 3000L }
                    mutable.update { it.copy(entries = entries, message = message, messageId = messageId, messageExpiresAt = expiresAt,
                        completedOperationId = operationId ?: it.completedOperationId) }
                } catch (cancelled: CancellationException) { throw cancelled
                } catch (failure: Exception) {
                    mutable.update { it.copy(error = "操作失败：${failureText(failure)}。原有账目未被替换。",
                        failedOperationId = operationId ?: it.failedOperationId) }
                } finally { mutable.update { it.copy(busy = false) } }
            }
        }
    }
    fun load() = work({ null }, failureText = { "读取账目失败，请重试" })
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
    }, failureText = { failure ->
        val reason = when (failure.message) {
            "备份超过 10 MiB" -> "备份超过 10 MiB"
            "备份最多 50000 条" -> "备份最多 50000 条"
            else -> "输出失败"
        }
        "$reason。新文件可能不完整，请保留旧备份并重新导出"
    })
    private enum class RestoreStage { READ, SIZE, DECODE, MERGE }
    fun restore(open: () -> InputStream?) {
        var stage = RestoreStage.READ
        work({
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
            stage = RestoreStage.SIZE
            require(bytes.size <= BackupCodec.MAX_BYTES) { "备份超过 10 MiB" }
            stage = RestoreStage.DECODE
            val text = Charsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString()
            val validated = BackupCodec.decode(text)
            stage = RestoreStage.MERGE
            val result = db().merge(validated)
            "恢复完成：新增 ${result.imported} 条，跳过 ${result.skipped} 条"
        }, failureText = { failure ->
            when (stage) {
                RestoreStage.READ -> "读取失败，请确认备份文件可访问"
                RestoreStage.SIZE -> "备份超过 10 MiB"
                RestoreStage.DECODE -> when (failure.message) {
                    "备份无效：不支持的备份版本", "备份无效：不支持的币种", "备份无效：不是安记账备份" -> "备份格式不兼容"
                    "备份无效：备份最多 50000 条" -> "备份最多 50000 条"
                    else -> "备份无效或已损坏，请选择有效备份"
                }
                RestoreStage.MERGE -> if (failure is IllegalArgumentException &&
                    failure.message?.endsWith("内容冲突，整批未导入") == true) "内容冲突，整批未导入" else "恢复失败，请重试"
            }
        })
    }
    override fun onCleared() {
        super.onCleared()
        CoroutineScope(Dispatchers.IO).launch { lock.withLock { database?.close(); database = null } }
    }
}

package com.xiaoliumiaomiao.anjizhang.ui

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xiaoliumiaomiao.anjizhang.data.BackupCodec
import com.xiaoliumiaomiao.anjizhang.data.LedgerDatabase
import com.xiaoliumiaomiao.anjizhang.domain.*
import com.xiaoliumiaomiao.anjizhang.data.ImportResult
import com.xiaoliumiaomiao.anjizhang.data.CategoryDefaults
import java.time.LocalDate
import java.util.Collections
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
    val failedOperationId: String? = null, val messageId: String? = null, val messageExpiresAt: Long? = null,
    val readError: Boolean = false,
    val snapshot: LedgerSnapshot = LedgerSnapshot(),
    val accounts: List<Account> = emptyList(), val transfers: List<AccountTransfer> = emptyList(),
    val adjustments: List<AccountAdjustment> = emptyList(), val categories: List<Category> = emptyList(),
    val committedResults: List<CommittedOperationResult> = emptyList(), val snapshotRevision: Long = 0)

enum class LedgerOperation { SAVE_ENTRY, DELETE_ENTRY, SAVE_ACCOUNT, CREATE_ACCOUNT, ARCHIVE_ACCOUNT,
    SAVE_TRANSFER, DELETE_TRANSFER, ADJUST_BALANCE, DELETE_ADJUSTMENT, SAVE_CATEGORY, REORDER_CATEGORIES, RESTORE }

data class CommittedOperationResult(val operationId: String, val operation: LedgerOperation,
    val adjustment: AccountAdjustment? = null, val importResult: ImportResult? = null,
    val snapshotReady: Boolean = false, val committed: Boolean = true)

class LedgerViewModel(application: Application, private val today: () -> LocalDate) : AndroidViewModel(application) {
    constructor(application: Application) : this(application, LocalDate::now)
    val instanceId: String = UUID.randomUUID().toString()
    private val mutable = MutableStateFlow(LedgerUiState(busy = true))
    val state: StateFlow<LedgerUiState> = mutable.asStateFlow()
    fun clearMessage(messageId: String) {
        mutable.update { if (it.messageId == messageId) it.copy(message = null, messageId = null, messageExpiresAt = null) else it }
    }
    private val lock = Mutex()
    private var database: LedgerDatabase? = null
    private fun db(): LedgerDatabase = database ?: LedgerDatabase(getApplication(), today).also { database = it }
    private class SnapshotReadFailure(cause: Exception) : Exception(cause)
    private fun <T> immutable(items: List<T>): List<T> = Collections.unmodifiableList(ArrayList(items))
    private fun readSnapshot(): LedgerSnapshot = try {
        db().snapshot().let { it.copy(entries = immutable(it.entries), accounts = immutable(it.accounts),
            transfers = immutable(it.transfers), adjustments = immutable(it.adjustments), categories = immutable(it.categories)) }
    } catch (cancelled: CancellationException) { throw cancelled
    } catch (failure: Exception) { throw SnapshotReadFailure(failure) }
    init { load() }
    private data class Outcome(val message: String? = null, val adjustment: AccountAdjustment? = null,
        val importResult: ImportResult? = null)
    private fun work(action: suspend () -> Outcome, operationId: String? = null, operation: LedgerOperation? = null,
        failureText: (Exception) -> String = { "无法完成操作，请重试" }): Job {
        return viewModelScope.launch(Dispatchers.IO) {
            lock.withLock {
                mutable.update { it.copy(busy = true, error = null, message = null, messageId = null, messageExpiresAt = null) }
                var outcome: Outcome? = null
                try {
                    outcome = action()
                    if (operationId != null && operation != null) {
                        val result = CommittedOperationResult(operationId, operation, outcome.adjustment, outcome.importResult)
                        mutable.update { it.copy(committedResults = immutable(it.committedResults + result)) }
                    }
                    val snapshot = readSnapshot()
                    val message = outcome.message
                    val messageId = message?.let { UUID.randomUUID().toString() }
                    val expiresAt = message?.let { SystemClock.elapsedRealtime() + 3000L }
                    mutable.update { it.copy(snapshot = snapshot, entries = snapshot.entries, accounts = snapshot.accounts,
                        transfers = snapshot.transfers, adjustments = snapshot.adjustments, categories = snapshot.categories,
                        committedResults = immutable(it.committedResults.map { result -> result.copy(snapshotReady = true) }),
                        snapshotRevision = it.snapshotRevision + 1, readError = false, message = message, messageId = messageId, messageExpiresAt = expiresAt,
                        completedOperationId = operationId ?: it.completedOperationId) }
                } catch (cancelled: CancellationException) { throw cancelled
                } catch (failure: Exception) {
                    val failedRead = failure is SnapshotReadFailure
                    val error = if (failedRead && outcome != null) {
                        val committed = when (operation) {
                            LedgerOperation.RESTORE -> "已导入备份"
                            LedgerOperation.DELETE_ENTRY, LedgerOperation.DELETE_TRANSFER, LedgerOperation.DELETE_ADJUSTMENT -> "已删除"
                            null -> if (outcome.message != null) "备份已导出" else null
                            else -> "已保存"
                        }
                        if (committed != null) "操作失败：$committed，但无法重新读取账目，请重试读取。"
                        else "操作失败：${failureText(failure)}。无法重新读取账目，请重试读取。"
                    } else {
                        val advice = if (failedRead) "无法重新读取账目，请重试读取。" else "原有账目未被替换。账本未改动。"
                        "操作失败：${failureText(failure)}。$advice"
                    }
                    mutable.update { it.copy(error = error,
                        readError = it.readError || failedRead,
                        failedOperationId = operationId ?: it.failedOperationId) }
                } finally { mutable.update { it.copy(busy = false) } }
            }
        }
    }
    fun load(): Unit { loadAwaitable() }
    fun loadAwaitable(): Job = work({ Outcome() }, failureText = { "读取账目失败，请重试" })
    private fun mutate(operation: LedgerOperation, action: () -> Outcome): String =
        UUID.randomUUID().toString().also { work(action, it, operation, ::mutationFailure) }
    private fun mutationFailure(failure: Exception): String = when (failure.message) {
        "初始余额日期须为今天", "余额校准日期须为今天" -> "请求日期已变化，请保留输入并重新发起"
        else -> "无法完成操作，请检查输入后重试"
    }
    fun saveAccount(account: Account): String = mutate(LedgerOperation.SAVE_ACCOUNT) { db().saveAccount(account); Outcome("已保存") }
    fun createAccount(account: Account, initialRequest: BalanceAdjustmentRequest? = null): String =
        mutate(LedgerOperation.CREATE_ACCOUNT) { Outcome("已保存", adjustment = db().createAccount(account, initialRequest)) }
    fun archiveAccount(id: String, archived: Boolean = true): String = mutate(LedgerOperation.ARCHIVE_ACCOUNT) {
        db().archiveAccount(id, archived); Outcome(if (archived) "已归档" else "已取消归档")
    }
    fun saveTransfer(transfer: AccountTransfer): String = mutate(LedgerOperation.SAVE_TRANSFER) { db().saveTransfer(transfer); Outcome("已保存") }
    fun deleteTransfer(id: String): String = mutate(LedgerOperation.DELETE_TRANSFER) { db().deleteTransfer(id); Outcome("已删除") }
    fun adjustBalance(request: BalanceAdjustmentRequest): String = mutate(LedgerOperation.ADJUST_BALANCE) {
        Outcome("已保存", adjustment = db().adjustBalance(request))
    }
    fun deleteAdjustment(id: String): String = mutate(LedgerOperation.DELETE_ADJUSTMENT) { db().deleteAdjustment(id); Outcome("已删除") }
    fun saveCategory(category: Category): String = mutate(LedgerOperation.SAVE_CATEGORY) { db().saveCategory(category); Outcome("已保存") }
    fun reorderCategories(kind: EntryKind, ids: List<String>): String {
        val requestedIds = ids.toList()
        return mutate(LedgerOperation.REORDER_CATEGORIES) { db().reorderCategories(kind, requestedIds); Outcome("已保存") }
    }
    fun consumeCommittedResult(operationId: String) {
        mutable.update { state ->
            if (state.readError || state.committedResults.none { it.operationId == operationId && it.snapshotReady }) state
            else state.copy(committedResults = immutable(state.committedResults.filterNot { it.operationId == operationId }))
        }
    }
    fun save(entry: Entry): String = mutate(LedgerOperation.SAVE_ENTRY) { db().save(entry); Outcome("已保存") }
    fun delete(entry: Entry): String = mutate(LedgerOperation.DELETE_ENTRY) { db().delete(entry.id); Outcome("已删除") }
    fun export(open: () -> OutputStream?): Unit { exportAwaitable(open) }
    fun exportAwaitable(open: () -> OutputStream?): Job = work({
        val bytes = BackupCodec.encodeSnapshot(readSnapshot()).toByteArray(Charsets.UTF_8)
        (open() ?: error("无法打开备份位置")).use { it.write(bytes) }
        Outcome("备份已导出，请妥善保管")
    }, failureText = { failure ->
        val reason = when (failure.message) {
            "备份超过 32 MiB" -> "备份超过 32 MiB"
            "备份最多 50000 条" -> "备份最多 50000 条"
            "每种金融记录最多 50000 条" -> "每种金融记录最多 50000 条"
            "账户最多 1000 个" -> "账户最多 1000 个"
            "类别最多 100000 个" -> "类别最多 100000 个"
            else -> "输出失败"
        }
        "$reason。新文件可能不完整，请保留旧备份并重新导出"
    })
    private enum class RestoreStage { READ, SIZE, DECODE, MERGE }
    fun restore(open: () -> InputStream?): String {
        val operationId = UUID.randomUUID().toString()
        var stage = RestoreStage.READ
        work({
            val bytes = (open() ?: error("无法读取备份文件")).use { input ->
                val output = java.io.ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                var remaining = BackupCodec.MAX_SNAPSHOT_BYTES + 1
                while (remaining > 0) {
                    val count = input.read(buffer, 0, minOf(buffer.size, remaining))
                    if (count < 0) break
                    if (count == 0) continue
                    output.write(buffer, 0, count); remaining -= count
                }
                output.toByteArray()
            }
            stage = RestoreStage.SIZE
            require(bytes.size <= BackupCodec.MAX_SNAPSHOT_BYTES) { "备份超过 32 MiB" }
            stage = RestoreStage.DECODE
            val text = Charsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString()
            val validated = BackupCodec.decodeBackup(text)
            stage = RestoreStage.MERGE
            val result = db().merge(validated)
            Outcome(importMessage(result, validated.snapshot), importResult = result)
        }, operationId = operationId, operation = LedgerOperation.RESTORE, failureText = { failure ->
            when (stage) {
                RestoreStage.READ -> "读取失败，请确认备份文件可访问"
                RestoreStage.SIZE -> "备份超过 32 MiB"
                RestoreStage.DECODE -> when (failure.message) {
                    "备份无效：不支持的备份版本", "备份无效：不支持的币种", "备份无效：不是安记账备份" -> "备份格式不兼容"
                    "备份无效：备份最多 50000 条" -> "备份最多 50000 条"
                    "备份无效：备份超过 10 MiB" -> "旧版备份超过 10 MiB"
                    "备份无效：备份超过 32 MiB" -> "备份超过 32 MiB"
                    else -> "备份无效或已损坏，请选择有效备份"
                }
                RestoreStage.MERGE -> if (failure is IllegalArgumentException &&
                    failure.message?.endsWith("内容冲突，整批未导入") == true) "内容冲突，整批未导入" else "恢复失败，请重试"
            }
        })
        return operationId
    }
    private fun importMessage(result: ImportResult, incoming: LedgerSnapshot): String = buildString {
        append("恢复完成：新增 ${result.imported} 条，跳过 ${result.skipped} 条")
        fun entity(label: String, imported: Int, skipped: Int) {
            if (imported + skipped > 0) append("；$label：新增 $imported，跳过 $skipped")
        }
        entity("账户", result.accountsImported, result.accountsSkipped)
        entity("转账", result.transfersImported, result.transfersSkipped)
        entity("余额调整", result.adjustmentsImported, result.adjustmentsSkipped)
        // Automatic canonical metadata alone keeps the existing entry-only feedback contract.
        if (result.categoriesImported > 0 || result.catalogAdopted ||
            incoming.categories.any { it !in CategoryDefaults.canonical } ||
            (incoming.entries.isEmpty() && incoming.accounts.isEmpty() && incoming.transfers.isEmpty() && incoming.adjustments.isEmpty())) {
            entity("类别", result.categoriesImported, result.categoriesSkipped)
        }
        if (result.catalogAdopted) append("；已恢复类别设置")
    }
    override fun onCleared() {
        super.onCleared()
        CoroutineScope(Dispatchers.IO).launch { lock.withLock { database?.close(); database = null } }
    }
}

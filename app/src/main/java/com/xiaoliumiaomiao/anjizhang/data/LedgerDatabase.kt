package com.xiaoliumiaomiao.anjizhang.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.xiaoliumiaomiao.anjizhang.domain.*
import java.math.BigInteger
import java.time.LocalDate

data class DecodedBackup(val snapshot: LedgerSnapshot, val sourceVersion: Int)
data class ImportResult(
    val imported: Int, val skipped: Int,
    val accountsImported: Int = 0, val accountsSkipped: Int = 0,
    val transfersImported: Int = 0, val transfersSkipped: Int = 0,
    val adjustmentsImported: Int = 0, val adjustmentsSkipped: Int = 0,
    val categoriesImported: Int = 0, val categoriesSkipped: Int = 0,
    val catalogAdopted: Boolean = false,
)

class LedgerDatabase(context: Context, private val today: () -> LocalDate = LocalDate::now) :
    SQLiteOpenHelper(context.applicationContext, "ledger.db", null, 2) {
    override fun onConfigure(db: SQLiteDatabase) { db.setForeignKeyConstraintsEnabled(true) }

    override fun onCreate(db: SQLiteDatabase) {
        LedgerSnapshotStorage.createParentAndEventTables(db)
        LedgerSnapshotStorage.createEntries(db)
        seedCategories(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        require(oldVersion == 1 && newVersion == 2) { "数据库从 $oldVersion 到 $newVersion 需要显式迁移，现有数据未删除" }
        LedgerSnapshotStorage.createParentAndEventTables(db)
        db.execSQL("ALTER TABLE entries ADD COLUMN account_id TEXT REFERENCES accounts(id)")
        seedCategories(db)
    }

    private fun seedCategories(db: SQLiteDatabase) {
        CategoryDefaults.supplement(CategoryDefaults.canonical, LedgerSnapshotStorage.entries(db))
            .forEach { LedgerSnapshotStorage.put(db, it) }
    }

    fun snapshot(): LedgerSnapshot = transaction(readableDatabase) { db ->
        LedgerSnapshotStorage.read(db).also(AccountBalances::validateSnapshot)
    }
    fun listEntries(): List<Entry> = transaction(readableDatabase) { db ->
        LedgerSnapshotStorage.entries(db).also { it.forEach(LedgerRules::validate) }
    }

    fun save(entry: Entry) = transaction(writableDatabase) { db ->
        val old = LedgerSnapshotStorage.read(db)
        val previous = old.entries.find { it.id == entry.id }
        requireActiveReference(old, entry.accountId, previous?.accountId)
        AccountBalances.validateSnapshot(old.copy(entries = replace(old.entries, entry) { it.id }))
        LedgerSnapshotStorage.put(db, entry, previous != null)
    }

    fun delete(id: String) = transaction(writableDatabase) { db ->
        val old = LedgerSnapshotStorage.read(db)
        AccountBalances.validateSnapshot(old.copy(entries = old.entries.filterNot { it.id == id }))
        db.delete("entries", "id = ?", arrayOf(id))
        Unit
    }

    fun saveAccount(account: Account) = transaction(writableDatabase) { db ->
        val normalized = account.copy(name = account.name.trim())
        val old = LedgerSnapshotStorage.read(db)
        val previous = old.accounts.find { it.id == normalized.id }
        if (previous != null && previous.kind != normalized.kind) {
            require(!hasReferences(old, normalized.id)) { "已有记录关联，不能改变账户类型" }
        }
        AccountBalances.validateSnapshot(old.copy(accounts = replace(old.accounts, normalized) { it.id }))
        LedgerSnapshotStorage.put(db, normalized, previous != null)
    }

    /** Account and optional INITIAL event are validated and committed as one unit. */
    fun createAccount(account: Account, initialRequest: BalanceAdjustmentRequest? = null): AccountAdjustment? =
        transaction(writableDatabase) { db ->
            val normalized = account.copy(name = account.name.trim())
            LedgerRules.validate(normalized)
            val old = LedgerSnapshotStorage.read(db)
            val previous = old.accounts.find { it.id == normalized.id }
            if (previous != null) {
                require(previous == normalized) { "账户 ID 内容冲突" }
                if (initialRequest == null) return@transaction null
                val event = old.adjustments.find { it.id == initialRequest.id }
                require(event != null && event.matches(initialRequest) && event.reason == AdjustmentReason.INITIAL &&
                    initialRequest.accountId == normalized.id) { "初始余额请求 ID 内容冲突" }
                return@transaction event
            }
            val event = initialRequest?.let { request ->
                LedgerRules.validate(request)
                require(request.accountId == normalized.id && request.reason == AdjustmentReason.INITIAL) { "初始余额请求与账户不匹配" }
                require(request.date == today()) { "初始余额日期须为今天" }
                require(!normalized.archived) { "归档账户不能新增初始余额" }
                require(old.adjustments.none { it.id == request.id }) { "初始余额请求 ID 内容冲突" }
                AccountAdjustment(request.id, request.accountId, request.targetBalanceCents, request.date,
                    request.targetBalanceCents, request.note, request.reason)
            }
            val candidate = old.copy(accounts = old.accounts + normalized,
                adjustments = old.adjustments + listOfNotNull(event))
            AccountBalances.validateSnapshot(candidate)
            LedgerSnapshotStorage.put(db, normalized)
            event?.let { LedgerSnapshotStorage.put(db, it) }
            event
        }

    fun archiveAccount(id: String, archived: Boolean = true) = transaction(writableDatabase) { db ->
        val old = LedgerSnapshotStorage.read(db)
        val account = old.accounts.find { it.id == id } ?: throw IllegalArgumentException("账户不存在")
        val updated = account.copy(archived = archived)
        AccountBalances.validateSnapshot(old.copy(accounts = replace(old.accounts, updated) { it.id }))
        LedgerSnapshotStorage.put(db, updated, true)
    }

    fun saveTransfer(transfer: AccountTransfer) = transaction(writableDatabase) { db ->
        val old = LedgerSnapshotStorage.read(db)
        val previous = old.transfers.find { it.id == transfer.id }
        requireActiveReference(old, transfer.fromAccountId, previous?.fromAccountId)
        requireActiveReference(old, transfer.toAccountId, previous?.toAccountId)
        AccountBalances.validateSnapshot(old.copy(transfers = replace(old.transfers, transfer) { it.id }))
        LedgerSnapshotStorage.put(db, transfer, previous != null)
    }

    fun deleteTransfer(id: String) = transaction(writableDatabase) { db ->
        val old = LedgerSnapshotStorage.read(db)
        AccountBalances.validateSnapshot(old.copy(transfers = old.transfers.filterNot { it.id == id }))
        db.delete("transfers", "id = ?", arrayOf(id)); Unit
    }

    fun adjustBalance(request: BalanceAdjustmentRequest): AccountAdjustment = transaction(writableDatabase) { db ->
        val old = LedgerSnapshotStorage.read(db)
        // A committed request keeps its old delta even across midnight, edits, or archival.
        old.adjustments.find { it.id == request.id }?.let { event ->
            require(event.matches(request)) { "余额请求 ID 内容冲突" }
            return@transaction event
        }
        LedgerRules.validate(request)
        require(request.reason == AdjustmentReason.CALIBRATION) { "初始余额须随账户原子创建" }
        require(request.date == today()) { "余额校准日期须为今天" }
        requireActiveReference(old, request.accountId, null)
        val current = AccountBalances.balances(old, request.date).getValue(request.accountId)
        val delta = BigInteger.valueOf(request.targetBalanceCents) - BigInteger.valueOf(current)
        val max = BigInteger.valueOf(Long.MAX_VALUE)
        require(delta >= max.negate() && delta <= max) { "余额调整金额超出范围" }
        val event = AccountAdjustment(request.id, request.accountId, delta.toLong(), request.date,
            request.targetBalanceCents, request.note, request.reason)
        AccountBalances.validateSnapshot(old.copy(adjustments = old.adjustments + event))
        LedgerSnapshotStorage.put(db, event)
        event
    }

    fun deleteAdjustment(id: String) = transaction(writableDatabase) { db ->
        val old = LedgerSnapshotStorage.read(db)
        AccountBalances.validateSnapshot(old.copy(adjustments = old.adjustments.filterNot { it.id == id }))
        db.delete("adjustments", "id = ?", arrayOf(id)); Unit
    }

    fun saveCategory(category: Category) = transaction(writableDatabase) { db ->
        val old = LedgerSnapshotStorage.read(db)
        val normalized = category.copy(name = category.name.trim())
        AccountBalances.validateSnapshot(old.copy(categories = replace(old.categories, normalized) { it.id }))
        LedgerSnapshotStorage.put(db, normalized, old.categories.any { it.id == normalized.id })
    }

    fun reorderCategories(kind: EntryKind, ids: List<String>) = transaction(writableDatabase) { db ->
        val old = LedgerSnapshotStorage.read(db)
        val current = old.categories.filter { it.kind == kind && !it.archived }
        require(ids.size == current.size && ids.toSet().size == ids.size && ids.toSet() == current.map { it.id }.toSet()) {
            "排序须包含该收支类型全部未归档类别且各一次"
        }
        val order = ids.withIndex().associate { it.value to it.index }
        val categories = old.categories.map { c -> order[c.id]?.let { c.copy(sortOrder = it) } ?: c }
        AccountBalances.validateSnapshot(old.copy(categories = categories))
        categories.filter { it.id in order }.forEach { LedgerSnapshotStorage.put(db, it, true) }
    }

    fun merge(entries: List<Entry>): ImportResult = merge(DecodedBackup(LedgerSnapshot(entries = entries), 1))

    fun merge(backup: DecodedBackup): ImportResult {
        require(backup.sourceVersion in 1..2) { "不支持的备份版本" }
        val incoming = backup.snapshot
        if (backup.sourceVersion == 1) {
            require(incoming.accounts.isEmpty() && incoming.transfers.isEmpty() && incoming.adjustments.isEmpty() &&
                incoming.categories.isEmpty() && incoming.entries.all { it.accountId == null }) { "旧版备份不能包含账户或目录元数据" }
            require(incoming.entries.size <= 50000) { "备份最多 50000 条" }
        } else BackupCodec.validateSnapshotCapacity(incoming)
        AccountBalances.validateSnapshot(incoming)
        return transaction(writableDatabase) { db ->
            val local = LedgerSnapshotStorage.read(db)
            val entries = combine(local.entries, incoming.entries, { it.id }) { old, new ->
                if (backup.sourceVersion == 1) old.copy(accountId = null) == new else old == new
            }
            val accounts = combine(local.accounts, incoming.accounts, { it.id })
            val transfers = combine(local.transfers, incoming.transfers, { it.id })
            val adjustments = combine(local.adjustments, incoming.adjustments, { it.id })
            val pristine = backup.sourceVersion == 2 && local.entries.isEmpty() && local.accounts.isEmpty() &&
                local.transfers.isEmpty() && local.adjustments.isEmpty() &&
                local.categories.associateBy { it.id } == CategoryDefaults.canonical.associateBy { it.id }
            val adopted = pristine && local.categories.associateBy { it.id } != incoming.categories.associateBy { it.id }
            val categories = when {
                backup.sourceVersion == 1 -> Combined(CategoryDefaults.supplement(local.categories, incoming.entries), emptyList(), 0)
                adopted -> Combined(incoming.categories, incoming.categories, 0)
                else -> combine(local.categories, incoming.categories, { it.id })
            }
            val candidate = LedgerSnapshot(entries.all, accounts.all, transfers.all, adjustments.all, categories.all)
            AccountBalances.validateSnapshot(candidate)
            if (adopted) db.delete("categories", null, null)
            accounts.added.forEach { LedgerSnapshotStorage.put(db, it) }
            val newCategories = if (backup.sourceVersion == 1) {
                val existing = local.categories.map { it.id }.toSet()
                categories.all.filter { it.id !in existing }
            } else categories.added
            newCategories.forEach { LedgerSnapshotStorage.put(db, it) }
            entries.added.forEach { LedgerSnapshotStorage.put(db, it) }
            transfers.added.forEach { LedgerSnapshotStorage.put(db, it) }
            adjustments.added.forEach { LedgerSnapshotStorage.put(db, it) }
            ImportResult(entries.added.size, entries.skipped, accounts.added.size, accounts.skipped,
                transfers.added.size, transfers.skipped, adjustments.added.size, adjustments.skipped,
                categories.added.size, categories.skipped, adopted)
        }
    }

    private data class Combined<T>(val all: List<T>, val added: List<T>, val skipped: Int)
    private fun <T> combine(local: List<T>, incoming: List<T>, id: (T) -> String,
        equal: (T, T) -> Boolean = { a, b -> a == b }): Combined<T> {
        val index = local.associateBy(id)
        val added = mutableListOf<T>()
        var skipped = 0
        incoming.forEach { item ->
            val old = index[id(item)]
            if (old == null) added.add(item)
            else { require(equal(old, item)) { "记录 ${id(item)} 内容冲突，整批未导入" }; skipped++ }
        }
        return Combined(local + added, added, skipped)
    }
    private fun <T> replace(old: List<T>, item: T, id: (T) -> String) = old.filterNot { id(it) == id(item) } + item
    private fun AccountAdjustment.matches(r: BalanceAdjustmentRequest) = id == r.id && accountId == r.accountId &&
        targetBalanceCents == r.targetBalanceCents && date == r.date && note == r.note && reason == r.reason
    private fun hasReferences(s: LedgerSnapshot, id: String) = s.entries.any { it.accountId == id } ||
        s.transfers.any { it.fromAccountId == id || it.toAccountId == id } || s.adjustments.any { it.accountId == id }
    private fun requireActiveReference(s: LedgerSnapshot, id: String?, originalId: String?) {
        if (id == null) return
        val account = s.accounts.find { it.id == id } ?: throw IllegalArgumentException("关联账户不存在")
        require(!account.archived || id == originalId) { "归档账户不能用于新记录" }
    }
    private inline fun <T> transaction(db: SQLiteDatabase, action: (SQLiteDatabase) -> T): T {
        db.beginTransaction()
        try { val result = action(db); db.setTransactionSuccessful(); return result }
        finally { db.endTransaction() }
    }
}

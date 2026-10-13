package com.xiaoliumiaomiao.anjizhang.ui

import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.xiaoliumiaomiao.anjizhang.data.UiPreferencesStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import com.xiaoliumiaomiao.anjizhang.domain.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.YearMonth
import java.util.UUID

internal data class UiSessionState(
    val owner: String = "", val kind: String = "", val seed: List<String> = emptyList(),
    val child: String = "", val childKind: String = "", val childSeed: List<String> = emptyList(),
    val sending: Boolean = false, val attempt: String = "", val operation: String = "",
    val payload: List<String> = emptyList(), val vmOwner: String = "", val frozen: Boolean = false,
    val unknown: Boolean = false, val failedAttempt: String = "", val error: String? = null,
    val readToken: String = "",
) { val actor: String get() = child.ifEmpty { owner } }

/** The actual root uses this small synchronous owner/attempt gate; no frame can open a second session. */
internal class UiOperationSession(initial: UiSessionState = UiSessionState()) : AutoCloseable {
    private var closed = false
    var restoreReadConfirmed by mutableStateOf(false)
        private set
    override fun close() { if (!closed) { closed = true; value = value.copy(readToken = "", sending = false) } }
    fun trackRead(job: kotlinx.coroutines.Job, scope: kotlinx.coroutines.CoroutineScope, owner: String, token: String) {
        job.invokeOnCompletion { scope.launch { if (!closed && value.actor == owner) endRead(token) } }
    }
    fun trackRead(job: kotlinx.coroutines.Job, vm: LedgerViewModel, owner: String, token: String, previousRevision: Long) {
        job.invokeOnCompletion { cause -> vm.viewModelScope.launch {
            if (!closed && value.actor == owner && value.readToken == token) {
                val latest = vm.state.value
                endRead(token)
                confirmRestoreRead(owner, cause == null && !latest.busy && !latest.readError && latest.snapshotRevision > previousRevision)
            }
        } }
    }
    internal fun confirmRestoreRead(owner: String, successful: Boolean) {
        if (!closed && value.actor == owner && value.unknown && value.operation == "RESTORE" && value.readToken.isEmpty()) restoreReadConfirmed = successful
    }
    fun acknowledgeUnknownRestore(owner: String, busy: Boolean, readError: Boolean): Boolean {
        if (closed || busy || readError || !restoreReadConfirmed || !value.unknown || value.operation != "RESTORE" ||
            owner != value.actor || value.child.isNotEmpty() || value.readToken.isNotEmpty() || value.sending) return false
        value = UiSessionState(); restoreReadConfirmed = false
        return true
    }
    fun trackExport(job: kotlinx.coroutines.Job, vm: LedgerViewModel, owner: String) {
        job.invokeOnCompletion { cause -> vm.viewModelScope.launch {
            if (!closed && value.owner == owner && value.operation == "EXPORT") {
                val latest = vm.state.value
                finishTransient(owner, latest.readError, latest.error)
                if (cause == null && latest.error == null) dismiss(owner, latest.busy)
            }
        } }
    }
    var value by mutableStateOf(initial)
        private set
    fun openBlocked(busy: Boolean) = closed || busy || value.owner.isNotEmpty() || value.readToken.isNotEmpty()
    fun submitBlocked(actor: String, busy: Boolean) = closed || busy || value.sending || value.attempt.isNotEmpty() ||
        value.frozen || value.readToken.isNotEmpty() || actor.isEmpty() || actor != value.actor
    fun acquire(owner: String, kind: String, seed: List<String>, vmOwner: String, busy: Boolean): Boolean {
        if (owner.isEmpty() || openBlocked(busy)) return false
        value = UiSessionState(owner = owner, kind = kind, seed = seed.toList(), vmOwner = vmOwner)
        return true
    }
    fun acquireChild(parent: String, child: String, kind: String, seed: List<String>, busy: Boolean): Boolean {
        if (parent != value.owner || value.child.isNotEmpty() || submitBlocked(parent, busy) || child.isEmpty()) return false
        value = value.copy(child = child, childKind = kind, childSeed = seed.toList())
        return true
    }
    fun dismiss(actor: String, busy: Boolean): Boolean {
        if (submitBlocked(actor, busy)) return false
        value = if (value.child.isNotEmpty()) value.copy(child = "", childKind = "", childSeed = emptyList())
        else UiSessionState(readToken = value.readToken)
        return true
    }
    fun latchSubmit(actor: String, operation: String, payload: List<String>, busy: Boolean): Boolean {
        if (submitBlocked(actor, busy)) return false
        value = value.copy(sending = true, operation = operation, payload = payload.toList(), failedAttempt = "", error = null)
        return true
    }
    fun bindAttempt(actor: String, attempt: String, vmOwner: String) {
        if (actor == value.actor && value.sending && value.attempt.isEmpty())
            value = value.copy(attempt = attempt, vmOwner = vmOwner)
    }
    fun observe(state: LedgerUiState, vmOwner: String): CommittedOperationResult? {
        val attempt = value.attempt.takeIf { it.isNotEmpty() } ?: return null
        if (value.vmOwner != vmOwner) {
            value = value.copy(frozen = true, unknown = true, sending = false,
                error = "无法确认上次提交结果，请重试读取；输入与原请求已保留。")
            return null
        }
        val own = state.committedResults.firstOrNull { it.operationId == attempt && it.committed }
        if (own != null) {
            value = value.copy(frozen = true, sending = false, error = if (state.readError) state.error else null)
            return own.takeIf { it.snapshotReady && !state.readError && !state.busy }
        }
        if (!state.busy && state.failedOperationId == attempt && !value.unknown) {
            value = value.copy(attempt = "", sending = false, frozen = false, failedAttempt = attempt, error = state.error)
        }
        return null
    }
    fun complete(actor: String, attempt: String): Boolean {
        if (actor != value.actor || attempt != value.attempt || attempt.isEmpty() || value.readToken.isNotEmpty()) return false
        value = if (value.child.isNotEmpty()) value.copy(child = "", childKind = "", childSeed = emptyList(),
            sending = false, attempt = "", operation = "", payload = emptyList(), frozen = false, unknown = false,
            failedAttempt = "", error = null)
        else UiSessionState(readToken = value.readToken)
        return true
    }
    fun beginRead(actor: String, token: String, busy: Boolean): Boolean {
        if (closed || busy || token.isEmpty() || value.readToken.isNotEmpty() || (value.sending && !value.frozen) ||
            (value.owner.isNotEmpty() && actor != value.actor) || (value.owner.isEmpty() && actor.isNotEmpty())) return false
        restoreReadConfirmed = false
        value = value.copy(readToken = token)
        return true
    }
    fun endRead(token: String): Boolean {
        if (token.isEmpty() || value.readToken != token) return false
        value = value.copy(readToken = "")
        return true
    }
    fun resolveUnknown(committed: Boolean?, vmOwner: String): Boolean {
        if (!value.unknown || value.readToken.isNotEmpty() || committed == null) return false
        if (committed) return complete(value.actor, value.attempt)
        value = value.copy(sending = false, failedAttempt = value.attempt, attempt = "", frozen = false,
            unknown = false, vmOwner = vmOwner, error = "未找到已提交的原请求，输入已保留，可重试。")
        return true
    }
    fun clearFailed(actor: String, busy: Boolean): Boolean {
        if (submitBlocked(actor, busy) || value.failedAttempt.isEmpty()) return false
        value = value.copy(failedAttempt = "", error = null, payload = emptyList(), operation = "")
        return true
    }
    fun canRestartFailed(actor: String, ui: LedgerUiState, today: LocalDate): Boolean =
        !submitBlocked(actor, ui.busy) && value.failedAttempt.isNotEmpty() && !ui.readError && !value.unknown &&
        ui.failedOperationId == value.failedAttempt && ui.committedResults.none { it.operationId == value.failedAttempt } && when(value.operation) {
            "CREATE_ACCOUNT" -> value.payload.size == 12 && value.payload[9] != today.toString()
            "ADJUST_BALANCE" -> value.payload.getOrNull(3)?.let { it != today.toString() } == true
            else -> false
        }
    fun finishTransient(actor: String, frozen: Boolean, error: String?) {
        if (!closed && actor == value.actor && value.attempt.isEmpty()) value = value.copy(sending = false, frozen = frozen, error = error)
    }
    fun saved(): List<String> = with(value) {
        listOf(owner, kind, child, childKind, sending.toString(), attempt, operation, vmOwner,
            frozen.toString(), unknown.toString(), failedAttempt, error.orEmpty(), readToken) +
            listOf(seed.size.toString()) + seed + listOf(childSeed.size.toString()) + childSeed +
            listOf(payload.size.toString()) + payload
    }
    companion object {
        private const val KEY = "ledger-ui-operation-session-v018"
        fun forViewModel(vm: androidx.lifecycle.ViewModel, vmOwner: String, restored: UiOperationSession? = null): UiOperationSession {
            vm.getCloseable<UiOperationSession>(KEY)?.let { return it }
            val session = restored ?: UiOperationSession()
            if (session.value.vmOwner != vmOwner) {
                val cancelledExport = session.value.attempt.isEmpty() && session.value.operation == "EXPORT"
                session.value = session.value.copy(readToken = "", vmOwner = vmOwner,
                    frozen = session.value.attempt.isNotEmpty(), unknown = session.value.attempt.isNotEmpty(),
                    sending = if (session.value.attempt.isNotEmpty() || cancelledExport) false else session.value.sending,
                    error = if (cancelledExport) "无法确认上次导出结果，请检查备份文件；可返回后重新导出。" else session.value.error)
            }
            vm.addCloseable(KEY, session)
            return session
        }
        fun restored(fields: List<String>): UiOperationSession {
            if (fields.isEmpty()) return UiOperationSession()
            var index = 13
            fun section(): List<String> { val size = fields[index++].toInt(); return fields.subList(index, index + size).toList().also { index += size } }
            val seed = section(); val childSeed = section(); val payload = section()
            return UiOperationSession(UiSessionState(fields[0], fields[1], seed, fields[2], fields[3], childSeed,
                fields[4].toBooleanStrict(), fields[5], fields[6], payload, fields[7], fields[8].toBooleanStrict(),
                fields[9].toBooleanStrict(), fields[10], fields[11].takeIf { it.isNotEmpty() }, fields[12]))
        }
    }
}

data class LedgerDocumentResult(val owner: String, val uri: String?, val export: Boolean)

private fun accountFields(a: Account) = listOf(a.id, a.name, a.kind.name, a.subtype.name, a.archived.toString(), a.sortOrder.toString())
private fun accountValue(f: List<String>) = Account(f[0], f[1], AccountKind.valueOf(f[2]), AccountSubtype.valueOf(f[3]), f[4].toBooleanStrict(), f[5].toInt())
private fun transferFields(t: AccountTransfer) = listOf(t.id, t.fromAccountId, t.toAccountId, t.amountCents.toString(), t.date.toString(), t.note)
private fun transferValue(f: List<String>) = AccountTransfer(f[0], f[1], f[2], f[3].toLong(), LocalDate.parse(f[4]), f[5])
private fun requestFields(r: BalanceAdjustmentRequest) = listOf(r.id, r.accountId, r.targetBalanceCents.toString(), r.date.toString(), r.note, r.reason.name)
private fun adjustmentFields(a: AccountAdjustment) = listOf(a.id, a.accountId, a.deltaCents.toString(), a.date.toString(), a.targetBalanceCents.toString(), a.note, a.reason.name)
private fun adjustmentValue(f: List<String>) = AccountAdjustment(f[0], f[1], f[2].toLong(), LocalDate.parse(f[3]), f[4].toLong(), f[5], AdjustmentReason.valueOf(f[6]))
private fun categoryFields(c: Category) = listOf(c.id, c.kind.name, c.name, c.sortOrder.toString(), c.archived.toString())
private fun categoryValue(f: List<String>) = Category(f[0], EntryKind.valueOf(f[1]), f[2], f[3].toInt(), f[4].toBooleanStrict())

/** Uses the complete published snapshot, never just an ID or a previous completion flag. */
private fun payloadDisposition(s: UiSessionState, snapshot: LedgerSnapshot): Boolean? {
    val p = s.payload
    fun <T> changed(actual: T?, wanted: T, before: T?): Boolean? = when {
        actual == wanted -> true
        actual == before -> false
        else -> null
    }
    fun adjustmentMatches(f: List<String>): Boolean? {
        val actual = snapshot.adjustments.firstOrNull { it.id == f[0] } ?: return false
        return if (listOf(actual.id, actual.accountId, actual.targetBalanceCents.toString(), actual.date.toString(), actual.note, actual.reason.name) == f) true else null
    }
    return when (s.operation) {
        "SAVE_ENTRY" -> changed(snapshot.entries.firstOrNull { it.id == p[0] }, entryFromSnapshot(p)!!, entryFromSnapshot(s.seed.drop(3)))
        "DELETE_ENTRY" -> snapshot.entries.firstOrNull { it.id == p[0] }?.let { if (entrySnapshot(it) == p) false else null } ?: true
        "SAVE_ACCOUNT", "ARCHIVE_ACCOUNT" -> changed(snapshot.accounts.firstOrNull { it.id == p[0] }, accountValue(p), s.seed.takeIf { it.size == 6 }?.let(::accountValue))
        "CREATE_ACCOUNT" -> {
            val wanted = accountValue(p.take(6)); val actual = snapshot.accounts.firstOrNull { it.id == wanted.id }
            when { actual == null -> false; actual != wanted -> null; p.size == 6 -> true; else -> adjustmentMatches(p.drop(6)) }
        }
        "SAVE_TRANSFER" -> changed(snapshot.transfers.firstOrNull { it.id == p[0] }, transferValue(p), s.seed.takeIf { it.size == 6 }?.let(::transferValue))
        "ADJUST_BALANCE" -> adjustmentMatches(p)
        "DELETE_TRANSFER" -> snapshot.transfers.firstOrNull { it.id == p[0] }?.let { if (transferFields(it) == p) false else null } ?: true
        "DELETE_ADJUSTMENT" -> snapshot.adjustments.firstOrNull { it.id == p[0] }?.let { if (adjustmentFields(it) == p) false else null } ?: true
        "SAVE_CATEGORY" -> changed(snapshot.categories.firstOrNull { it.id == p[0] }, categoryValue(p),
            (if (s.child.isNotEmpty()) s.childSeed.drop(2) else s.seed.drop(2)).takeIf { it.size == 5 }?.let(::categoryValue))
        "REORDER_CATEGORIES" -> {
            val actual = snapshot.categories.filter { it.kind.name == p[0] && !it.archived }.sortedWith(compareBy({ it.sortOrder }, { it.id })).map { it.id }
            when (actual) { p.drop(1) -> true; s.seed.drop(1) -> false; else -> null }
        }
        // Restore's own ready receipt confirms its atomic merge; a fresh VM cannot infer arbitrary backup content from a URI.
        "RESTORE" -> null
        else -> null
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable fun LedgerScreen(viewModel: LedgerViewModel, onExport: () -> Unit, onImport: () -> Unit,
    onLicenses: () -> Unit, modalVisible: Boolean = false,
    preferences: UiPreferences = UiPreferences(), preferenceStore: UiPreferencesStore? = null,
    onSelectExport: (String) -> Unit = { onExport() }, onSelectImport: (String) -> Unit = { onImport() },
    documentResult: LedgerDocumentResult? = null, onDocumentConsumed: () -> Unit = {}) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current.applicationContext
    val scope = rememberCoroutineScope()
    val session = rememberSaveable(viewModel, saver = listSaver(
        save = { it.saved() }, restore = { UiOperationSession.forViewModel(viewModel, viewModel.instanceId, UiOperationSession.restored(it)) }
    )) { UiOperationSession.forViewModel(viewModel, viewModel.instanceId) }
    val spec = session.value
    val forms = rememberSaveableStateHolder()
    var page by rememberSaveable { mutableStateOf("ledger") }
    var settingsDestination by rememberSaveable { mutableStateOf(SettingsDestination.ROOT) }
    var accountDetailId by rememberSaveable { mutableStateOf<String?>(null) }
    var monthText by rememberSaveable { mutableStateOf(YearMonth.now().toString()) }
    val month = YearMonth.parse(monthText)
    var query by rememberSaveable { mutableStateOf("") }
    var kindName by rememberSaveable { mutableStateOf("") }
    val kind = EntryKind.entries.firstOrNull { it.name == kindName }
    var detailId by rememberSaveable { mutableStateOf<String?>(null) }
    var detailOrigin by rememberSaveable { mutableStateOf("ledger") }
    var statsAnnual by rememberSaveable { mutableStateOf(false) }
    var statsYear by rememberSaveable { mutableStateOf<Int?>(null) }
    var statsKindName by rememberSaveable { mutableStateOf(EntryKind.EXPENSE.name) }
    val statsKind = EntryKind.valueOf(statsKindName)
    var drillFields by rememberSaveable { mutableStateOf<List<String>>(emptyList()) }
    val drillKey = remember(drillFields) { drillFields.takeIf { it.size == 4 }?.let {
        CategoryKey(StatisticsPeriod(it[0].toInt(), it[1].toIntOrNull()), EntryKind.valueOf(it[2]), it[3])
    } }
    var selectedCategoryFields by rememberSaveable { mutableStateOf<List<String>>(emptyList()) }
    var selectedCategoryToken by rememberSaveable { mutableStateOf<String?>(null) }
    var statisticsRetry by remember { mutableIntStateOf(0) }
    var preferenceSending by remember { mutableStateOf(false) }
    var preferenceError by remember { mutableStateOf<String?>(null) }
    var preferenceMessage by remember { mutableStateOf<String?>(null) }
    var recoveryNotice by rememberSaveable { mutableStateOf<String?>(null) }
    val ledgerScroll = rememberLazyListState(); val statsScroll = rememberLazyListState()
    val drillScroll = rememberLazyListState(); val settingsScroll = rememberLazyListState(); val accountsScroll = rememberLazyListState()
    val today = rememberStatisticsToday(page)
    val balances = rememberAccountBalances(state.snapshot, state.snapshotRevision, today)
    val period = if (statsAnnual) StatisticsPeriod.annual(statsYear ?: month.year) else StatisticsPeriod.monthly(month)
    val statistics = rememberStatistics(StatisticsRequest(EntriesIdentity(state.entries), period, today, statisticsRetry))
    val drill = rememberDrill(drillKey?.let { DrillRequest(EntriesIdentity(state.entries), it, today, statisticsRetry) })
    val detail = state.entries.firstOrNull { it.id == detailId }
    fun globalBlocked() = modalVisible || preferenceSending || session.openBlocked(viewModel.state.value.busy)
    fun ownBlocked(actor: String) = modalVisible || preferenceSending || session.submitBlocked(actor, viewModel.state.value.busy)
    fun open(kind: String, seed: List<String> = emptyList()): Boolean = !globalBlocked() &&
        session.acquire(UUID.randomUUID().toString(), kind, seed, viewModel.instanceId, viewModel.state.value.busy)
    fun openEntry(entry: Entry?, fromDetail: Boolean) {
        if (open("entry", listOf(fromDetail.toString(), page, monthText) + entrySnapshot(entry))) {
            selectedCategoryFields = emptyList(); selectedCategoryToken = null
        }
    }
    fun dismiss(actor: String) { if (!ownBlocked(actor) && session.dismiss(actor, viewModel.state.value.busy)) forms.removeState(actor) }
    fun submit(actor: String, operation: String, payload: List<String>, action: () -> String) {
        if (!modalVisible && !preferenceSending && session.latchSubmit(actor, operation, payload, viewModel.state.value.busy))
            session.bindAttempt(actor, action(), viewModel.instanceId)
    }
    fun retryRead() {
        val actor = session.value.actor; val token = UUID.randomUUID().toString()
        if (!modalVisible && session.beginRead(actor, token, viewModel.state.value.busy)) {
            val revision = viewModel.state.value.snapshotRevision
            val job = viewModel.loadAwaitable()
            session.trackRead(job, viewModel, actor, token, revision)
        }
    }
    fun retryStatistics() { statisticsRetry++; retryRead() }
    fun resetStats() { scope.launch { statsScroll.scrollToItem(0) } }
    fun changeMonth(value: YearMonth) { if (!globalBlocked()) { monthText = value.toString(); if (page == "stats") { statsAnnual = false; resetStats() } } }
    fun returnDetail() { detailId = null; page = when (detailOrigin) { "account" -> "accounts"; "ledger" -> "ledger"; else -> "stats" } }
    fun addCategory(kind: EntryKind, order: Int, parent: String? = null) {
        val seed = listOf(kind.name, order.toString())
        if (parent == null) open("category", seed)
        else if (!ownBlocked(parent)) session.acquireChild(parent, UUID.randomUUID().toString(), "category", seed, viewModel.state.value.busy)
    }
    fun immediate(kind: String, seed: List<String>, op: String, payload: List<String>, action: () -> String) {
        if (open(kind, seed)) submit(session.value.owner, op, payload, action)
    }
    fun savePreference(change: suspend (UiPreferencesStore) -> Result<UiPreferences>) {
        val store = preferenceStore ?: return
        if (globalBlocked()) return
        preferenceSending = true; preferenceError = null; preferenceMessage = null
        scope.launch {
            try { val result = change(store); if (result.isSuccess) preferenceMessage = "已保存外观设置" else preferenceError = "外观设置保存失败，请重试" }
            catch (cancelled: CancellationException) { throw cancelled }
            finally { preferenceSending = false }
        }
    }
    fun finishCommitted(before: UiSessionState) {
        if (before.operation == "SAVE_CATEGORY" && before.child.isNotEmpty()) {
            selectedCategoryFields = before.payload; selectedCategoryToken = before.attempt
        }
        if (before.operation == "SAVE_ENTRY") {
            if (before.seed.firstOrNull() == "true") returnDetail()
            else if (before.seed.getOrNull(1) == "ledger" && detailId == null) monthText = YearMonth.from(LocalDate.parse(before.payload[4])).toString()
        }
        if (before.operation == "DELETE_ENTRY") returnDetail()
        forms.removeState(before.actor)
    }
    LaunchedEffect(viewModel.instanceId, state, spec.attempt, spec.readToken, spec.unknown) {
        val before = session.value
        val receipt = session.observe(state, viewModel.instanceId)
        if (receipt != null && (before.operation == "RESTORE" || payloadDisposition(before, state.snapshot) == true)) {
            if (session.complete(before.actor, before.attempt)) { finishCommitted(before); viewModel.consumeCommittedResult(receipt.operationId) }
        } else if (session.value.unknown && !state.busy && !state.readError && state.snapshotRevision > 0 && session.value.readToken.isEmpty()) {
            val resolved = payloadDisposition(before, state.snapshot)
            if (session.resolveUnknown(resolved, viewModel.instanceId) && resolved == true) finishCommitted(before)
        }
    }
    LaunchedEffect(viewModel.instanceId, state.messageId) {
        val id = state.messageId ?: return@LaunchedEffect
        val remaining = (state.messageExpiresAt ?: 0L) - SystemClock.elapsedRealtime()
        if (remaining > 0) delay(remaining)
        viewModel.clearMessage(id)
    }
    LaunchedEffect(documentResult) {
        val result = documentResult ?: return@LaunchedEffect
        val current = session.value
        val expected = if (result.export) "EXPORT_SELECT" else "RESTORE_SELECT"
        if (current.owner == result.owner && current.operation == expected && current.sending) {
            session.finishTransient(result.owner, false, null)
            if (result.uri == null) dismiss(result.owner)
            else if (result.export) {
                if (session.latchSubmit(result.owner, "EXPORT", listOf(result.uri), viewModel.state.value.busy)) {
                    val job = viewModel.exportAwaitable { context.contentResolver.openOutputStream(android.net.Uri.parse(result.uri), "wt") }
                    session.trackExport(job, viewModel, result.owner)
                }
            } else submit(result.owner, "RESTORE", listOf(result.uri)) {
                viewModel.restore { context.contentResolver.openInputStream(android.net.Uri.parse(result.uri)) }
            }
        }
        onDocumentConsumed()
    }
    // An export has no mutation receipt; a successful explicit read resolves only its already-completed output session.
    LaunchedEffect(spec.kind, spec.operation, spec.frozen, spec.readToken, state.snapshotRevision) {
        if (spec.operation == "EXPORT" && spec.frozen && !state.busy && !state.readError && spec.readToken.isEmpty()) {
            session.finishTransient(spec.actor, false, null); dismiss(spec.actor)
        }
    }
    BackHandler(detailId != null && spec.owner.isEmpty()) { if (!globalBlocked()) returnDetail() }
    BackHandler(detailId == null && page == "stats" && drillKey != null && spec.owner.isEmpty()) { if (!globalBlocked()) drillFields = emptyList() }
    BackHandler(page == "accounts" && accountDetailId != null && detailId == null && spec.owner.isEmpty()) { if (!globalBlocked()) accountDetailId = null }
    BackHandler(page == "settings" && settingsDestination != SettingsDestination.ROOT && spec.owner.isEmpty()) { if (!globalBlocked()) settingsDestination = SettingsDestination.ROOT }
    val busy = state.busy || spec.owner.isNotEmpty() || spec.readToken.isNotEmpty() || preferenceSending || modalVisible
    var fabHeight by remember { mutableIntStateOf(0) }
    val fabTail = with(LocalDensity.current) { fabHeight.toDp() } + 32.dp
    val tailPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = fabTail)
    Scaffold(topBar = {
        if (detailId == null && page == "stats" && drillKey == null) Box(Modifier.fillMaxWidth().statusBarsPadding()) else
        Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (detailId != null) TextButton(enabled = !busy, onClick = { if (!globalBlocked()) returnDetail() }, modifier = Modifier.heightIn(min = 48.dp).testTag("detail-back")) { Text("返回") }
            else if (page == "stats" && drillKey != null) TextButton(enabled = !busy, onClick = { if (!globalBlocked()) drillFields = emptyList() }, modifier = Modifier.heightIn(min = 48.dp).testTag("stats-drill-back")) { Text("返回") }
            else LedgerGlyph(page, Modifier.size(28.dp), MaterialTheme.colorScheme.primary)
            Text(if (detailId != null) "账目详情" else when(page) { "stats" -> if (drillKey != null) "分类记录" else if (statsAnnual) "年统计" else "月统计"; "accounts" -> "账户"; "settings" -> "设置"; else -> "安记账" }, style = MaterialTheme.typography.headlineSmall)
        }
    }, bottomBar = {
        if (detailId == null && !(page == "stats" && drillKey != null)) NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
            listOf("ledger" to "明细", "stats" to "统计", "accounts" to "账户", "settings" to "设置").forEach { (key, label) ->
                NavigationBarItem(selected = page == key, enabled = !busy, onClick = { if (!globalBlocked()) page = key },
                    icon = { DestinationIcon(key) }, label = { Text(label) }, modifier = Modifier.testTag("nav-$key"))
            }
        }
    }, floatingActionButtonPosition = if (LocalHandedness.current == Handedness.LEFT) FabPosition.Start else FabPosition.End,
        floatingActionButton = {
            if (!busy) ExtendedFloatingActionButton(onClick = { openEntry(null, false) },
                icon = { LedgerGlyph("plus") }, text = { Text("记一笔") },
                modifier = Modifier.testTag("add-entry").heightIn(min = 48.dp).onSizeChanged { fabHeight = it.height })
        }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding).imePadding()) {
            if (state.busy || spec.readToken.isNotEmpty()) LinearProgressIndicator(Modifier.fillMaxWidth())
            state.error?.let { error ->
                Column(Modifier.fillMaxWidth().heightIn(max = 160.dp).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
                    Text(error, color = MaterialTheme.colorScheme.error)
                    if (state.readError && spec.owner.isEmpty()) TextButton(onClick = ::retryRead, enabled = !state.busy && spec.readToken.isEmpty()) { Text("重试读取") }
                }
            }
            state.message?.let { Text(it, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 16.dp)) }
            recoveryNotice?.let { Text(it, modifier = Modifier.padding(horizontal = 16.dp).testTag("restore-result-unconfirmed")) }
            if (detailId != null) {
                if (detail != null) EntryDetail(detail, busy, { openEntry(detail, true) }, { open("delete-entry", entrySnapshot(detail)) }, tailPadding,
                    detail.accountId?.let { id -> state.accounts.firstOrNull { it.id == id }?.name } ?: "未分配账户")
                else Column(Modifier.fillMaxSize().padding(tailPadding), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(when { state.busy -> "正在读取账目…"; state.readError -> "无法读取账目，请重试"; else -> "这笔账目已不存在" })
                    TextButton(enabled = !busy, onClick = { if (!globalBlocked()) returnDetail() }) { Text("返回") }
                }
            } else when (page) {
                "stats" -> if (drillKey != null) CategoryDrillPage(drillKey, drill, state.busy, state.readError, busy, drillScroll, ::retryStatistics,
                    { if (!globalBlocked()) { detailOrigin = "drill"; detailId = it.id } }, tailPadding)
                else StatisticsPage(month, statsAnnual, statsYear ?: month.year, statsKind, statistics, today, state.busy, state.readError, busy,
                    statsScroll, ::changeMonth, { annual -> if (!globalBlocked()) { if (annual && statsYear == null) statsYear = month.year; statsAnnual = annual; resetStats() } },
                    { year -> if (!globalBlocked()) { statsYear = year; resetStats() } }, { if (!globalBlocked()) statsKindName = it.name },
                    { category -> if (!globalBlocked()) { drillFields = listOf(period.year.toString(), period.month?.toString().orEmpty(), statsKind.name, category); scope.launch { drillScroll.scrollToItem(0) } } },
                    { if (!globalBlocked()) { detailOrigin = "stats"; detailId = it.id } }, ::retryStatistics, tailPadding,
                    { open("month", listOf(monthText)) }, { open("year", listOf((statsYear ?: month.year).toString())) })
                "accounts" -> AccountsPage(state.snapshot, balances?.balances, balances?.totals, today, busy, state.readError, tailPadding, accountDetailId,
                    { if (!globalBlocked()) accountDetailId = it }, { open("account") }, { open("account", accountFields(it)) },
                    { a, archived -> immediate("action", accountFields(a), "ARCHIVE_ACCOUNT", accountFields(a.copy(archived = archived))) { viewModel.archiveAccount(a.id, archived) } },
                    { open("transfer") }, { a -> if (balances != null && balances.balances.containsKey(a.id)) open("balance", accountFields(a) + listOf(today.toString(), balances.balances.getValue(a.id).toString())) },
                    { if (!globalBlocked()) { detailOrigin = "account"; detailId = it.id } }, { open("transfer", transferFields(it)) },
                    { open("delete-transfer", transferFields(it)) }, { open("delete-adjustment", adjustmentFields(it)) }, ::retryRead, ::globalBlocked, accountsScroll)
                "settings" -> SettingsPages(settingsDestination, preferences, state.categories, busy, preferenceSending, preferenceError, preferenceMessage, tailPadding,
                    { if (!globalBlocked()) settingsDestination = it }, { savePreference { store -> store.setTheme(it) } }, { savePreference { store -> store.setHand(it) } },
                    { k, order -> addCategory(k, order) }, { c -> open("category", listOf(c.kind.name, c.sortOrder.toString()) + categoryFields(c)) },
                    { c, archived -> immediate("action", listOf(c.kind.name, c.sortOrder.toString()) + categoryFields(c), "SAVE_CATEGORY", categoryFields(c.copy(archived = archived))) { viewModel.saveCategory(c.copy(archived = archived)) } },
                    { k, ids -> val before = state.categories.filter { it.kind == k && !it.archived }.sortedWith(compareBy({ it.sortOrder }, { it.id })).map { it.id }; immediate("action", listOf(k.name) + before, "REORDER_CATEGORIES", listOf(k.name) + ids) { viewModel.reorderCategories(k, ids) } },
                    { open("export") }, { open("restore") }, { open("licenses") }, ::globalBlocked, settingsScroll)
                else -> {
                    val monthEntries = remember(state.entries, month) { EntryFilter.apply(state.entries, month) }
                    val visible = remember(state.entries, month, query, kindName) { EntryFilter.apply(state.entries, month, query, kind) }
                    val summary = remember(state.entries, month) { MonthlySummary.calculate(state.entries, month) }
                    LedgerPage(month, summary, visible, monthEntries.size, query, kind, busy, ledgerScroll, ::changeMonth,
                        { if (!globalBlocked()) query = it }, { if (!globalBlocked()) kindName = it?.name.orEmpty() }, { if (!globalBlocked()) { query = ""; kindName = "" } },
                        { openEntry(null, false) }, { if (!globalBlocked()) { detailOrigin = "ledger"; detailId = it.id } }, tailPadding, { open("month", listOf(monthText)) })
                }
            }
        }
    }
    val actor = spec.actor
    val opState = ComponentOperationState(state.busy || spec.sending || spec.attempt.isNotEmpty() && !spec.frozen || spec.readToken.isNotEmpty(),
        spec.frozen || spec.unknown, spec.error, state.readError || spec.unknown)
    val ownGate = { ownBlocked(actor) }
    val close = { dismiss(actor) }
    val restartAllowed = !ownBlocked(actor) && session.canRestartFailed(actor, state, LocalDate.now())
    val restartRequest = {
        val latest = viewModel.state.value
        !ownBlocked(actor) && session.canRestartFailed(actor, latest, LocalDate.now()) && session.clearFailed(actor, latest.busy)
    }
    if (spec.kind == "entry") forms.SaveableStateProvider(spec.owner) { key(spec.owner) {
        val original = entryFromSnapshot(spec.seed.drop(3))
        val unavailable = original != null && !state.busy && !state.readError && state.entries.none { it.id == original.id } && spec.attempt.isEmpty()
        if (!unavailable) EntryEditor(original, opState.busy, opState.error, { dismiss(spec.owner) },
            { candidate -> if (original == null || viewModel.state.value.entries.any { it.id == original.id }) submit(spec.owner, "SAVE_ENTRY", entrySnapshot(candidate)) { viewModel.save(candidate) } },
            state.entries, { ownBlocked(spec.owner) }, state.accounts, state.categories,
            { k -> addCategory(k, ((state.categories.filter { it.kind == k }.maxOfOrNull { it.sortOrder } ?: -1).toLong() + 1).coerceAtMost(Int.MAX_VALUE.toLong()).toInt(), spec.owner) },
            spec.child.isNotEmpty(), spec.frozen || spec.unknown, state.readError || spec.unknown, ::retryRead,
            when(spec.seed.getOrNull(1)) { "accounts" -> "账户"; "settings" -> "设置"; "stats" -> "统计"; else -> "明细" },
            selectedCategoryFields.takeIf { it.size == 5 }?.let(::categoryValue), selectedCategoryToken)
        else AlertDialog(onDismissRequest = { dismiss(spec.owner) }, title = { Text("这笔账目已不存在") }, text = { SyncDialogWindowAppearance(DialogWindowAppearance.FLOATING); Text("请返回并重新读取账目。") }, confirmButton = { TextButton(onClick = { dismiss(spec.owner) }) { Text("返回") } })
    } }
    val formKind = if (spec.child.isNotEmpty()) spec.childKind else spec.kind
    val seed = if (spec.child.isNotEmpty()) spec.childSeed else spec.seed
    if (actor.isNotEmpty() && formKind != "entry") forms.SaveableStateProvider(actor) { key(actor) {
        when(formKind) {
            "account" -> {
                val account = seed.takeIf { it.size == 6 }?.let(::accountValue)
                val locked = account != null && (state.entries.any { it.accountId == account.id } || state.transfers.any { it.fromAccountId == account.id || it.toAccountId == account.id } || state.adjustments.any { it.accountId == account.id })
                AccountEditorDialog(account, locked, today, opState, close,
                    { a, r -> submit(actor, "CREATE_ACCOUNT", accountFields(a) + r?.let(::requestFields).orEmpty()) { viewModel.createAccount(a, r) } },
                    { a -> submit(actor, "SAVE_ACCOUNT", accountFields(a)) { viewModel.saveAccount(a) } }, ::retryRead, ownGate,
                    restartAllowed, restartRequest)
            }
            "balance" -> BalanceEditorDialog(accountValue(seed.take(6)), LocalDate.parse(seed[6]), opState, close,
                { r -> submit(actor, "ADJUST_BALANCE", requestFields(r)) { viewModel.adjustBalance(r) } }, ::retryRead, ownGate,
                restartAllowed, restartRequest, currentBalanceCents = seed[7].toLong())
            "transfer" -> TransferEditorDialog(seed.takeIf { it.size == 6 }?.let(::transferValue), state.accounts, today, opState, close,
                { t -> submit(actor, "SAVE_TRANSFER", transferFields(t)) { viewModel.saveTransfer(t) } }, ::retryRead, ownGate)
            "category" -> CategoryEditorDialog(seed.drop(2).takeIf { it.size == 5 }?.let(::categoryValue), EntryKind.valueOf(seed[0]), seed[1].toInt(), opState, close,
                { c -> submit(actor, "SAVE_CATEGORY", categoryFields(c)) { viewModel.saveCategory(c) } }, ::retryRead, ownGate)
            "delete-transfer", "delete-adjustment" -> AccountEventDeleteDialog(if (formKind == "delete-transfer") transferValue(seed) else null,
                if (formKind == "delete-adjustment") adjustmentValue(seed) else null, opState, close,
                { id -> if (formKind == "delete-transfer") submit(actor, "DELETE_TRANSFER", seed) { viewModel.deleteTransfer(id) }
                    else submit(actor, "DELETE_ADJUSTMENT", seed) { viewModel.deleteAdjustment(id) } }, ::retryRead, ownGate)
            "delete-entry" -> AlertDialog(onDismissRequest = { if (!ownGate()) close() }, title = { Text("删除账目？") }, text = {
                SyncDialogWindowAppearance(DialogWindowAppearance.FLOATING)
                Column { val entry = entryFromSnapshot(seed)!!; Text("${entry.date} ${entry.category} ${Money.format(entry.amountCents)}，删除后无法撤销。"); OperationFeedback(opState, ::retryRead) }
            }, confirmButton = { HandedFormActions(!ownGate(), close, { submit(actor, "DELETE_ENTRY", seed) { viewModel.delete(entryFromSnapshot(seed)!!) } }, "确认删除", "entry-delete-confirm") })
            "month" -> MonthPicker(YearMonth.parse(seed[0]), close, { if (!ownGate()) { monthText = it.toString(); if (page == "stats") { statsAnnual = false; resetStats() }; close() } }, ownGate)
            "year" -> YearPicker(seed[0].toInt(), close, { if (!ownGate()) { statsYear = it; resetStats(); close() } }, ownGate)
            "export", "restore" -> AlertDialog(onDismissRequest = { if (!ownGate()) close() }, title = { Text(if (formKind == "export") "导出明文备份" else "合并恢复账目") }, text = {
                SyncDialogWindowAppearance(DialogWindowAppearance.FLOATING)
                Column(Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState())) {
                    Text(if (formKind == "export") "备份包含明文账目、账户和备注，请保存在可信位置。完整备份（格式2）最多32 MiB；账目、转账和余额调整各最多50,000条，账户最多1,000个，类别最多100,000个。旧备份（格式1）最多50,000条账目且不超过10 MiB。目前不支持分批导出。系统提供者可能包含云盘，请自行选择可信位置，并保留第二份有效备份。"
                        else "恢复会合并账目、账户、转账、余额调整和类别，相同记录跳过；任何内容冲突时整批拒绝，现有记录不被替换。请选择可信的安记账备份。")
                    OperationFeedback(opState, ::retryRead)
                    if (spec.unknown && spec.operation == "RESTORE" && session.restoreReadConfirmed)
                        Text("上次恢复结果无法确认。账本已重新读取；重新选择相同备份时，相同记录会跳过，冲突会拒绝。")
                }
            }, confirmButton = { if (spec.unknown && spec.operation == "RESTORE" && session.restoreReadConfirmed)
                TextButton(enabled = !state.busy && !state.readError && spec.readToken.isEmpty(), onClick = {
                    if (session.acknowledgeUnknownRestore(actor, viewModel.state.value.busy, viewModel.state.value.readError)) {
                        recoveryNotice = "上次恢复结果无法确认。账本已重新读取；重新选择相同备份时，相同记录会跳过，冲突会拒绝。"
                        forms.removeState(actor)
                    }
                }, modifier = Modifier.testTag("restore-acknowledge-unknown")) { Text("返回账本") }
            else HandedFormActions(!ownGate(), close, {
                if (session.latchSubmit(actor, if (formKind == "export") "EXPORT_SELECT" else "RESTORE_SELECT", emptyList(), viewModel.state.value.busy)) {
                    if (formKind == "export") onSelectExport(actor) else onSelectImport(actor)
                }
            }, if (formKind == "export") "选择保存位置" else "选择备份文件", "document-select") })
            "licenses" -> {
                val licenses by produceState<String?>(null) { value = withContext(Dispatchers.IO) {
                    try { context.assets.list("licenses").orEmpty().sorted().joinToString("\n\n") { "$it\n" + context.assets.open("licenses/$it").bufferedReader().use { reader -> reader.readText() } } }
                    catch (cancelled: CancellationException) { throw cancelled } catch (_: Exception) { "许可读取失败，请重新打开。" }
                } }
                AlertDialog(onDismissRequest = close, title = { Text("第三方许可") }, text = { SyncDialogWindowAppearance(DialogWindowAppearance.FLOATING); Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) { Text(licenses ?: "正在读取许可…") } }, confirmButton = { TextButton(onClick = close, enabled = !ownGate()) { Text("关闭") } })
            }
            "action" -> AlertDialog(onDismissRequest = { if (!ownGate()) close() }, title = { Text("正在确认操作") }, text = { SyncDialogWindowAppearance(DialogWindowAppearance.FLOATING); OperationFeedback(opState, ::retryRead) }, confirmButton = { TextButton(enabled = !ownGate(), onClick = close) { Text("关闭") } })
        }
    } }
}

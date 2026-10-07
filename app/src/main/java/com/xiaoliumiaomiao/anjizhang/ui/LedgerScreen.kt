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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xiaoliumiaomiao.anjizhang.domain.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.YearMonth
import java.util.UUID

@Composable fun LedgerScreen(viewModel: LedgerViewModel, onExport: () -> Unit, onImport: () -> Unit,
    onLicenses: () -> Unit, modalVisible: Boolean = false) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var page by rememberSaveable { mutableStateOf("ledger") }
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
    // The drill key is a small saveable value, never an Entry list or complete ledger.
    var drillFields by rememberSaveable { mutableStateOf<List<String>>(emptyList()) }
    val drillKey = remember(drillFields) { drillFields.takeIf { it.size == 4 }?.let {
        CategoryKey(StatisticsPeriod(it[0].toInt(), it[1].toIntOrNull()), EntryKind.valueOf(it[2]), it[3])
    } }
    var statisticsRetry by remember { mutableIntStateOf(0) }
    var editing by rememberSaveable { mutableStateOf(false) }
    var editingFields by rememberSaveable { mutableStateOf<List<String>>(emptyList()) }
    var editorSession by rememberSaveable { mutableStateOf("") }
    var editorReturnToDetail by rememberSaveable { mutableStateOf(false) }
    var deletingId by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingSaveId by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingDeleteId by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingOwner by rememberSaveable { mutableStateOf(viewModel.instanceId) }
    val editorStates = rememberSaveableStateHolder()
    val effectiveBusy = state.busy || pendingSaveId != null || pendingDeleteId != null
    val detail = state.entries.firstOrNull { it.id == detailId }
    val editingEntry = remember(editingFields) { entryFromSnapshot(editingFields) }
    val deletingEntry = state.entries.firstOrNull { it.id == deletingId }
    val unavailableEdit = editing && editingEntry != null && !state.busy && state.entries.none { it.id == editingEntry.id }
    val ledgerScroll = rememberLazyListState()
    val statsScroll = rememberLazyListState()
    val drillScroll = rememberLazyListState()
    val settingsScroll = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val today = rememberStatisticsToday(page)
    val statisticsPeriod = if (statsAnnual) StatisticsPeriod.annual(statsYear ?: month.year) else StatisticsPeriod.monthly(month)
    val sourceIdentity = EntriesIdentity(state.entries)
    // Producers live above destination/detail conditionals so valid results survive returns.
    val statistics = rememberStatistics(StatisticsRequest(sourceIdentity, statisticsPeriod, today, statisticsRetry))
    val drill = rememberDrill(drillKey?.let { DrillRequest(sourceIdentity, it, today, statisticsRetry) })
    fun retryStatistics() { statisticsRetry++; viewModel.load() }
    fun resetStatisticsScroll() { scope.launch { statsScroll.scrollToItem(0) } }
    fun changeStatisticsMonth(value: YearMonth) { monthText = value.toString(); statsAnnual = false; resetStatisticsScroll() }
    fun returnFromDetail() { detailId = null; page = if (detailOrigin == "ledger") "ledger" else "stats" }
    // Callbacks can be invoked again before recomposition updates effectiveBusy.
    fun operationBlocked() = viewModel.state.value.busy || pendingSaveId != null || pendingDeleteId != null
    fun closeEditor() { editing = false; editorStates.removeState(editorSession) }
    fun openEditor(entry: Entry?, fromDetail: Boolean) {
        if (operationBlocked()) return
        editorStates.removeState(editorSession)
        pendingSaveId = null; editingFields = entrySnapshot(entry)
        editorSession = UUID.randomUUID().toString(); editorReturnToDetail = fromDetail; editing = true
    }
    LaunchedEffect(viewModel.instanceId) {
        if (pendingOwner != viewModel.instanceId) {
            pendingSaveId = null; pendingDeleteId = null; pendingOwner = viewModel.instanceId
        }
    }
    LaunchedEffect(viewModel.instanceId, state.messageId) {
        val messageId = state.messageId ?: return@LaunchedEffect
        val remaining = (state.messageExpiresAt ?: 0L) - SystemClock.elapsedRealtime()
        if (remaining > 0) delay(remaining)
        viewModel.clearMessage(messageId)
    }
    LaunchedEffect(state.completedOperationId, state.failedOperationId, pendingSaveId, pendingDeleteId) {
        state.completedOperationId?.let { completed ->
            if (completed == pendingSaveId) {
                closeEditor(); pendingSaveId = null
                if (editorReturnToDetail) returnFromDetail()
            }
            if (completed == pendingDeleteId) { deletingId = null; pendingDeleteId = null; returnFromDetail() }
        }
        state.failedOperationId?.let { failed ->
            if (failed == pendingSaveId) pendingSaveId = null
            if (failed == pendingDeleteId) pendingDeleteId = null
        }
    }
    BackHandler(enabled = page == "stats" && drillKey != null && detailId == null && !editing) { if (!operationBlocked()) drillFields = emptyList() }
    BackHandler(enabled = detailId != null && !editing && deletingId == null) { if (!operationBlocked()) returnFromDetail() }
    Scaffold(topBar = {
        Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (detailId != null) TextButton(onClick = { if (!operationBlocked()) returnFromDetail() }, enabled = !effectiveBusy,
                modifier = Modifier.heightIn(min = 48.dp).testTag("detail-back")) { Text("返回") }
            else if (page == "stats" && drillKey != null) TextButton(onClick = { if (!operationBlocked()) drillFields = emptyList() }, enabled = !effectiveBusy,
                modifier = Modifier.heightIn(min = 48.dp).testTag("stats-drill-back")) { Text("返回") }
            Text(if (detailId != null) "账目详情" else when (page) { "stats" -> if (drillKey != null) "分类记录" else if (statsAnnual) "年统计" else "月统计"; "settings" -> "设置"; else -> "安记账" },
                style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(vertical = 8.dp))
        }
    }, bottomBar = {
        if (detailId == null && !(page == "stats" && drillKey != null)) Column {
            // The action's measured height is part of Scaffold's content padding, even on the first frame.
            if (!editing && deletingId == null && !modalVisible) Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.End) {
                ExtendedFloatingActionButton(onClick = { openEditor(null, false) },
                    modifier = Modifier.testTag("add-entry").semantics { if (effectiveBusy) disabled() }) { Text("记一笔") }
            }
            NavigationBar {
                listOf("ledger" to "明细", "stats" to "统计", "settings" to "设置").forEach { (key, label) ->
                    NavigationBarItem(selected = page == key, onClick = { page = key }, icon = { DestinationIcon(key) },
                        label = { Text(label) }, modifier = Modifier.testTag("nav-$key"))
                }
            }
        }
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding).imePadding()) {
            if (effectiveBusy) LinearProgressIndicator(Modifier.fillMaxWidth())
            state.error?.let { error ->
                Column(Modifier.fillMaxWidth().heightIn(max = 160.dp).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
                    Text(error, color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = viewModel::load, enabled = !effectiveBusy) { Text("重试读取") }
                }
            }
            state.message?.let { Text(it, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 16.dp)) }
            if (detailId != null) {
                if (detail != null) EntryDetail(detail, effectiveBusy,
                    onEdit = { openEditor(detail, true) }, onDelete = { if (!operationBlocked()) deletingId = detail.id })
                else Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(when { state.busy -> "正在读取账目…"; state.error != null -> "无法读取账目，请重试"; else -> "这笔账目已不存在" })
                    TextButton(onClick = { if (!operationBlocked()) returnFromDetail() }, enabled = !effectiveBusy) { Text(if (detailOrigin == "ledger") "返回明细" else "返回统计") }
                }
            } else when (page) {
                "stats" -> if (drillKey != null) CategoryDrillPage(drillKey, drill, state.busy, state.error != null,
                    effectiveBusy, drillScroll, ::retryStatistics) { entry ->
                    if (!operationBlocked()) { detailOrigin = "drill"; detailId = entry.id }
                } else StatisticsPage(month, statsAnnual, statsYear ?: month.year, statsKind, statistics, today,
                    state.busy, state.error != null, effectiveBusy, statsScroll, ::changeStatisticsMonth,
                    { annual ->
                        if (annual && statsYear == null) statsYear = month.year
                        statsAnnual = annual; resetStatisticsScroll()
                    }, { year -> statsYear = year; resetStatisticsScroll() }, { statsKindName = it.name },
                    { category ->
                        if (!operationBlocked()) {
                            drillFields = listOf(statisticsPeriod.year.toString(), statisticsPeriod.month?.toString().orEmpty(), statsKind.name, category)
                            scope.launch { drillScroll.scrollToItem(0) }
                        }
                    }, { entry -> if (!operationBlocked()) { detailOrigin = "stats"; detailId = entry.id } }, ::retryStatistics)
                "settings" -> SettingsPage(effectiveBusy, settingsScroll, onExport, onImport, onLicenses)
                else -> {
                    val monthEntries = remember(state.entries, month) { EntryFilter.apply(state.entries, month) }
                    val visible = remember(state.entries, month, query, kindName) { EntryFilter.apply(state.entries, month, query, kind) }
                    val summary = remember(state.entries, month) { MonthlySummary.calculate(state.entries, month) }
                    LedgerPage(month, summary, visible, monthEntries.size, query, kind, effectiveBusy, ledgerScroll,
                        { monthText = it.toString() }, { query = it }, { kindName = it?.name.orEmpty() }, { query = ""; kindName = "" },
                        { openEditor(null, false) }, { if (!operationBlocked()) { detailOrigin = "ledger"; detailId = it.id } })
                }
            }
        }
    }
    if (editing && !unavailableEdit) editorStates.SaveableStateProvider(editorSession) {
        key(editorSession) {
            EntryEditor(editingEntry, effectiveBusy, state.error,
                onDismiss = { if (!operationBlocked()) closeEditor() },
                onSave = { candidate ->
                    if (!operationBlocked() && (editingEntry == null || viewModel.state.value.entries.any { it.id == editingEntry.id }))
                        pendingSaveId = viewModel.save(candidate)
                }, recentEntries = state.entries, isOperationBlocked = ::operationBlocked)
        }
    }
    if (unavailableEdit) AlertDialog(onDismissRequest = { if (!operationBlocked()) closeEditor() },
        title = { Text(if (state.error == null) "这笔账目已不存在" else "无法读取账目，请重试") },
        text = { Text("请返回并重新读取账目。") },
        confirmButton = { TextButton(onClick = { if (!operationBlocked()) closeEditor() }) { Text("返回") } },
        dismissButton = { if (state.error != null) TextButton(onClick = viewModel::load) { Text("重试读取") } })
    if (deletingId != null) AlertDialog(onDismissRequest = { if (!operationBlocked()) deletingId = null },
        title = { Text("删除账目？") }, text = {
            Column {
                if (deletingEntry != null) Text("${deletingEntry.date} ${deletingEntry.category} ${Money.format(deletingEntry.amountCents)}，删除后无法撤销。")
                else Text(if (state.busy) "正在读取账目…" else if (state.error != null) "无法读取账目，请重试" else "这笔账目已不存在")
                state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        }, confirmButton = {
            if (deletingEntry != null) TextButton(enabled = !effectiveBusy, onClick = {
                if (!operationBlocked()) pendingDeleteId = viewModel.delete(deletingEntry)
            }) { Text("确认删除") }
        }, dismissButton = { TextButton(enabled = !effectiveBusy, onClick = { if (!operationBlocked()) deletingId = null }) { Text("取消") } })
}

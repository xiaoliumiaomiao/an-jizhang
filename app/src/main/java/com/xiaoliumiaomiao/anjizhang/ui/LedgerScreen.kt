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
    val monthEntries = remember(state.entries, month) { EntryFilter.apply(state.entries, month) }
    val visible = remember(state.entries, month, query, kindName) { EntryFilter.apply(state.entries, month, query, kind) }
    val summary = remember(state.entries, month) { MonthlySummary.calculate(state.entries, month) }
    var detailId by rememberSaveable { mutableStateOf<String?>(null) }
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
    val settingsScroll = rememberLazyListState()
    fun closeEditor() { editing = false; editorStates.removeState(editorSession) }
    fun openEditor(entry: Entry?, fromDetail: Boolean) {
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
                if (editorReturnToDetail) { detailId = null; page = "ledger" }
            }
            if (completed == pendingDeleteId) { deletingId = null; pendingDeleteId = null; detailId = null; page = "ledger" }
        }
        state.failedOperationId?.let { failed ->
            if (failed == pendingSaveId) pendingSaveId = null
            if (failed == pendingDeleteId) pendingDeleteId = null
        }
    }
    BackHandler(enabled = detailId != null && !editing && deletingId == null) { if (!effectiveBusy) detailId = null }
    Scaffold(topBar = {
        Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (detailId != null) TextButton(onClick = { detailId = null }, enabled = !effectiveBusy,
                modifier = Modifier.testTag("detail-back")) { Text("返回") }
            Text(if (detailId != null) "账目详情" else when (page) { "stats" -> "月统计"; "settings" -> "设置"; else -> "安记账" },
                style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(vertical = 8.dp))
        }
    }, bottomBar = {
        if (detailId == null) Column {
            // The action's measured height is part of Scaffold's content padding, even on the first frame.
            if (!editing && deletingId == null && !modalVisible) Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.End) {
                ExtendedFloatingActionButton(onClick = { if (!effectiveBusy) openEditor(null, false) },
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
                    onEdit = { openEditor(detail, true) }, onDelete = { pendingDeleteId = null; deletingId = detail.id })
                else Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(when { state.busy -> "正在读取账目…"; state.error != null -> "无法读取账目，请重试"; else -> "这笔账目已不存在" })
                    TextButton(onClick = { detailId = null }, enabled = !effectiveBusy) { Text("返回明细") }
                }
            } else when (page) {
                "stats" -> StatisticsPage(month, summary, statsScroll) { monthText = it.toString() }
                "settings" -> SettingsPage(effectiveBusy, settingsScroll, onExport, onImport, onLicenses)
                else -> LedgerPage(month, summary, visible, monthEntries.size, query, kind, effectiveBusy, ledgerScroll,
                    { monthText = it.toString() }, { query = it }, { kindName = it?.name.orEmpty() }, { query = ""; kindName = "" },
                    { openEditor(null, false) }, { detailId = it.id })
            }
        }
    }
    if (editing && !unavailableEdit) editorStates.SaveableStateProvider(editorSession) {
        key(editorSession) {
            EntryEditor(editingEntry, effectiveBusy, state.error,
                onDismiss = { if (!effectiveBusy) closeEditor() },
                onSave = { candidate ->
                    if (!effectiveBusy && (editingEntry == null || state.entries.any { it.id == editingEntry.id }))
                        pendingSaveId = viewModel.save(candidate)
                }, recentEntries = state.entries)
        }
    }
    if (unavailableEdit) AlertDialog(onDismissRequest = { closeEditor() },
        title = { Text(if (state.error == null) "这笔账目已不存在" else "无法读取账目，请重试") },
        text = { Text("请返回并重新读取账目。") },
        confirmButton = { TextButton(onClick = { closeEditor() }) { Text("返回") } },
        dismissButton = { if (state.error != null) TextButton(onClick = viewModel::load) { Text("重试读取") } })
    if (deletingId != null) AlertDialog(onDismissRequest = { if (!effectiveBusy) deletingId = null },
        title = { Text("删除账目？") }, text = {
            Column {
                if (deletingEntry != null) Text("${deletingEntry.date} ${deletingEntry.category} ${Money.format(deletingEntry.amountCents)}，删除后无法撤销。")
                else Text(if (state.busy) "正在读取账目…" else if (state.error != null) "无法读取账目，请重试" else "这笔账目已不存在")
                state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        }, confirmButton = {
            if (deletingEntry != null) TextButton(enabled = !effectiveBusy, onClick = {
                if (!effectiveBusy) pendingDeleteId = viewModel.delete(deletingEntry)
            }) { Text("确认删除") }
        }, dismissButton = { TextButton(enabled = !effectiveBusy, onClick = { if (!effectiveBusy) deletingId = null }) { Text("取消") } })
}

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xiaoliumiaomiao.anjizhang.data.BackupCodec
import com.xiaoliumiaomiao.anjizhang.domain.*
import kotlinx.coroutines.delay
import java.time.YearMonth
import java.util.UUID

@Composable fun LedgerScreen(viewModel:LedgerViewModel,onExport:()->Unit,onImport:()->Unit,onLicenses:()->Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var page by rememberSaveable { mutableStateOf("ledger") }
    var monthText by rememberSaveable { mutableStateOf(YearMonth.now().toString()) }
    val month=YearMonth.parse(monthText)
    var query by rememberSaveable { mutableStateOf("") }
    var kindName by rememberSaveable { mutableStateOf("") }
    val kind=EntryKind.entries.firstOrNull{it.name==kindName}
    val monthEntries=remember(state.entries,month){EntryFilter.apply(state.entries,month)}
    val visible=remember(state.entries,month,query,kindName){EntryFilter.apply(state.entries,month,query,kind)}
    val summary=remember(state.entries,month){MonthlySummary.calculate(state.entries,month)}
    var detailJson by rememberSaveable { mutableStateOf<String?>(null) }
    var editing by rememberSaveable { mutableStateOf(false) }
    var editingJson by rememberSaveable { mutableStateOf<String?>(null) }
    var editorSession by rememberSaveable { mutableStateOf("") }
    var editorReturnToDetail by rememberSaveable { mutableStateOf(false) }
    var deletingJson by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingSaveId by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingDeleteId by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingOwner by rememberSaveable { mutableStateOf(viewModel.instanceId) }
    val effectiveBusy=state.busy || pendingSaveId!=null || pendingDeleteId!=null
    val detail=remember(detailJson){detailJson?.let{BackupCodec.decode(it).single()}}
    val editingEntry=remember(editingJson){editingJson?.let{BackupCodec.decode(it).single()}}
    val deletingEntry=remember(deletingJson){deletingJson?.let{BackupCodec.decode(it).single()}}
    val ledgerScroll=rememberLazyListState();val statsScroll=rememberLazyListState();val settingsScroll=rememberLazyListState()
    fun openEditor(entry:Entry?,fromDetail:Boolean) {
        pendingSaveId=null;editingJson=entry?.let{BackupCodec.encode(listOf(it))}
        editorSession=UUID.randomUUID().toString();editorReturnToDetail=fromDetail;editing=true
    }
    LaunchedEffect(viewModel.instanceId) {
        if(pendingOwner!=viewModel.instanceId) {
            pendingSaveId=null;pendingDeleteId=null;pendingOwner=viewModel.instanceId
        }
    }
    LaunchedEffect(viewModel.instanceId, state.messageId) {
        val messageId = state.messageId ?: return@LaunchedEffect
        val remaining = (state.messageExpiresAt ?: 0L) - SystemClock.elapsedRealtime()
        if (remaining > 0) delay(remaining)
        viewModel.clearMessage(messageId)
    }
    LaunchedEffect(state.completedOperationId,state.failedOperationId,pendingSaveId,pendingDeleteId) {
        state.completedOperationId?.let { completed->
            if(completed==pendingSaveId) {
                editing=false;pendingSaveId=null
                if(editorReturnToDetail) { detailJson=null;page="ledger" }
            }
            if(completed==pendingDeleteId) { deletingJson=null;pendingDeleteId=null;detailJson=null;page="ledger" }
        }
        state.failedOperationId?.let { failed->
            if(failed==pendingSaveId) pendingSaveId=null
            if(failed==pendingDeleteId) pendingDeleteId=null
        }
    }
    BackHandler(enabled=detail!=null && !editing && deletingEntry==null) { if(!effectiveBusy) detailJson=null }
    Scaffold(topBar={
        Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal=16.dp,vertical=8.dp),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            if(detail!=null) TextButton(onClick={detailJson=null},enabled=!effectiveBusy,modifier=Modifier.testTag("detail-back")){Text("返回")}
            Text(if(detail!=null)"账目详情" else when(page){"stats"->"月统计";"settings"->"设置";else->"安记账"},style=MaterialTheme.typography.headlineSmall,modifier=Modifier.padding(vertical=8.dp))
        }
    },bottomBar={
        if(detail==null) NavigationBar {
            listOf("ledger" to "明细","stats" to "统计","settings" to "设置").forEach { (key,label)->
                NavigationBarItem(selected=page==key,onClick={page=key},icon={DestinationIcon(key)},label={Text(label)},modifier=Modifier.testTag("nav-$key"))
            }
        }
    },floatingActionButton={
        if(detail==null) ExtendedFloatingActionButton(onClick={if(!effectiveBusy)openEditor(null,false)},modifier=Modifier.imePadding().testTag("add-entry").semantics { if(effectiveBusy) disabled() }) { Text("记一笔") }
    }) { padding->
        Column(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding).imePadding()) {
            if(effectiveBusy) LinearProgressIndicator(Modifier.fillMaxWidth())
            state.error?.let{error->Column(Modifier.fillMaxWidth().heightIn(max=160.dp).verticalScroll(rememberScrollState()).padding(horizontal=16.dp)){Text(error,color=MaterialTheme.colorScheme.error);TextButton(onClick=viewModel::load,enabled=!effectiveBusy){Text("重试读取")}}}
            state.message?.let{Text(it,color=MaterialTheme.colorScheme.primary,modifier=Modifier.padding(horizontal=16.dp))}
            if(detail!=null) EntryDetail(detail,effectiveBusy,onEdit={openEditor(detail,true)},onDelete={pendingDeleteId=null;deletingJson=BackupCodec.encode(listOf(detail))})
            else when(page) {
                "stats" -> StatisticsPage(month,summary,statsScroll){monthText=it.toString()}
                "settings" -> SettingsPage(effectiveBusy,settingsScroll,onExport,onImport,onLicenses)
                else -> LedgerPage(month,summary,visible,monthEntries.size,query,kind,effectiveBusy,ledgerScroll,{monthText=it.toString()},{query=it},{kindName=it?.name.orEmpty()},{query="";kindName=""},{openEditor(null,false)},{detailJson=BackupCodec.encode(listOf(it))})
            }
        }
    }
    if(editing) key(editorSession) {
        EntryEditor(editingEntry,effectiveBusy,state.error,onDismiss={if(!state.busy && pendingSaveId==null && pendingDeleteId==null)editing=false},onSave={if(!state.busy && pendingSaveId==null && pendingDeleteId==null)pendingSaveId=viewModel.save(it)})
    }
    deletingEntry?.let { entry->
        AlertDialog(onDismissRequest={if(!state.busy && pendingSaveId==null && pendingDeleteId==null)deletingJson=null},title={Text("删除账目？")},text={Column{Text("${entry.date} ${entry.category} ${Money.format(entry.amountCents)}，删除后无法撤销。");state.error?.let{Text(it,color=MaterialTheme.colorScheme.error)}}},confirmButton={TextButton(enabled=!effectiveBusy,onClick={if(!state.busy && pendingSaveId==null && pendingDeleteId==null)pendingDeleteId=viewModel.delete(entry)}){Text("确认删除")}},dismissButton={TextButton(enabled=!effectiveBusy,onClick={if(!state.busy && pendingSaveId==null && pendingDeleteId==null)deletingJson=null}){Text("取消")}})
    }
}

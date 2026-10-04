package com.xiaoliumiaomiao.anjizhang.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xiaoliumiaomiao.anjizhang.data.BackupCodec
import com.xiaoliumiaomiao.anjizhang.domain.*
import java.time.YearMonth

@OptIn(ExperimentalLayoutApi::class)
@Composable fun LedgerScreen(viewModel: LedgerViewModel, onExport: () -> Unit, onImport: () -> Unit, onLicenses: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var page by rememberSaveable { mutableStateOf("账目") }
    var monthText by rememberSaveable { mutableStateOf(YearMonth.now().toString()) }
    val month = YearMonth.parse(monthText)
    var editing by rememberSaveable { mutableStateOf(false) }
    var editingJson by rememberSaveable { mutableStateOf<String?>(null) }
    var deletingJson by rememberSaveable { mutableStateOf<String?>(null) }
    val editingEntry = remember(editingJson) { editingJson?.let { BackupCodec.decode(it).single() } }
    val deletingEntry = remember(deletingJson) { deletingJson?.let { BackupCodec.decode(it).single() } }
    val summary = remember(state.entries, month) { MonthlySummary.calculate(state.entries, month) }
    Scaffold(topBar = {
        Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("安记账", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(top = 8.dp))
            Button(onClick = { editingJson = null; editing = true }, enabled = !state.busy) { Text("记一笔") }
        }
    }, bottomBar = {
        NavigationBar {
            listOf("账目", "统计", "设置").forEach { destination ->
                NavigationBarItem(selected = page == destination, onClick = { page = destination }, icon = { Text(when(destination) { "账目" -> "账"; "统计" -> "计"; else -> "设" }) }, label = { Text(destination) })
            }
        }
    }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
            if (state.busy) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            state.error?.let { error -> item { Text(error, color = MaterialTheme.colorScheme.error); TextButton(onClick = viewModel::load, enabled = !state.busy) { Text("重试读取") } } }
            state.message?.let { message -> item { Text(message, color = MaterialTheme.colorScheme.primary) } }
            if (page != "设置") {
                item {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(onClick = { if (month.year > 1900 || month.monthValue > 1) monthText = month.minusMonths(1).toString() }) { Text("上月") }
                        Text("${month.year}年${month.monthValue}月", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(vertical = 12.dp))
                        TextButton(onClick = { if (month.year < 9999 || month.monthValue < 12) monthText = month.plusMonths(1).toString() }) { Text("下月") }
                        TextButton(onClick = { monthText = YearMonth.now().toString() }) { Text("本月") }
                    }
                }
                item {
                    Surface(shape = MaterialTheme.shapes.medium) {
                        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("收入 ${Money.format(summary.incomeCents)}", color = MaterialTheme.colorScheme.primary)
                            Text("支出 ${Money.format(summary.expenseCents)}", color = MaterialTheme.colorScheme.error)
                            Text("本月结余 ${Money.format(summary.balanceCents)}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            when (page) {
                "账目" -> {
                    val entries = state.entries.filter { YearMonth.from(it.date) == month }
                    if (entries.isEmpty()) item {
                        Column(Modifier.fillMaxWidth().padding(vertical = 32.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("这个月还没有账目", style = MaterialTheme.typography.titleMedium)
                            Button(onClick = { editingJson = null; editing = true }, enabled = !state.busy) { Text("记一笔") }
                        }
                    }
                    items(entries, key = { it.id }) { entry ->
                        Column(Modifier.fillMaxWidth().clickable(enabled = !state.busy) { editingJson = BackupCodec.encode(listOf(entry)); editing = true }.padding(vertical = 8.dp)) {
                            Text(entry.date.toString(), style = MaterialTheme.typography.labelLarge)
                            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(entry.category, style = MaterialTheme.typography.titleMedium)
                                Text((if (entry.kind == EntryKind.EXPENSE) "-" else "+") + Money.format(entry.amountCents), style = MaterialTheme.typography.titleLarge, color = if (entry.kind == EntryKind.EXPENSE) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                            }
                            Text(if (entry.kind == EntryKind.EXPENSE) "支出" else "收入", style = MaterialTheme.typography.labelLarge)
                            if (entry.note.isNotEmpty()) Text(entry.note)
                            Row {
                                TextButton(onClick = { editingJson = BackupCodec.encode(listOf(entry)); editing = true }, enabled = !state.busy) { Text("编辑") }
                                TextButton(onClick = { deletingJson = BackupCodec.encode(listOf(entry)) }, enabled = !state.busy) { Text("删除") }
                            }
                            HorizontalDivider()
                        }
                    }
                }
                "统计" -> {
                    item { Text("支出分类", style = MaterialTheme.typography.titleLarge) }
                    if (summary.expensesByCategory.isEmpty()) item { Text("这个月还没有支出") }
                    items(summary.expensesByCategory.toList().sortedByDescending { it.second }) { (category, cents) ->
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("$category  ${Money.format(cents)}")
                            LinearProgressIndicator(progress = { cents.toFloat() / summary.expenseCents.toFloat() }, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
                "设置" -> {
                    item { Text("备份与恢复", style = MaterialTheme.typography.titleLarge)
                        Button(onClick = onExport, enabled = !state.busy) { Text("导出备份") }
                        OutlinedButton(onClick = onImport, enabled = !state.busy) { Text("恢复备份") }
                        Text("备份文件包含明文账目，请保存在可信位置。恢复会合并账目，相同记录跳过；内容冲突时整批拒绝。") }
                    item { HorizontalDivider(); Text("本地隐私", style = MaterialTheme.typography.titleLarge); Text("账目仅保存在此设备，无需登录，不联网，无广告或分析服务。") }
                    item { Text("卸载提醒", style = MaterialTheme.typography.titleLarge); Text("卸载应用会删除本地账目；更换设备或卸载前，请先导出备份。系统云备份已关闭。") }
                    item { Text("版本 0.1.0"); TextButton(onClick = onLicenses) { Text("第三方许可") } }
                }
            }
        }
    }
    if (editing) EntryEditor(editingEntry, state.busy, state.error, onDismiss = { editing = false }, onSave = { entry -> viewModel.save(entry) { editing = false } })
    deletingEntry?.let { entry ->
        AlertDialog(onDismissRequest = { if (!state.busy) deletingJson = null }, title = { Text("删除账目？") }, text = { Column { Text("${entry.date} ${entry.category} ${Money.format(entry.amountCents)}，删除后无法撤销。") ; state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) } } }, confirmButton = { TextButton(enabled = !state.busy, onClick = { viewModel.delete(entry) { deletingJson = null } }) { Text("确认删除") } }, dismissButton = { TextButton(enabled = !state.busy, onClick = { deletingJson = null }) { Text("取消") } })
    }
}

package com.xiaoliumiaomiao.anjizhang.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.platform.testTag
import com.xiaoliumiaomiao.anjizhang.domain.*
import java.time.*
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable fun EntryEditor(entry: Entry?, busy: Boolean, saveError: String?, onDismiss: () -> Unit,
    onSave: (Entry) -> Unit) {
    var kindName by rememberSaveable { mutableStateOf(entry?.kind?.name ?: "EXPENSE") }
    var amount by rememberSaveable { mutableStateOf(entry?.let { Money.plain(it.amountCents) } ?: "") }
    var date by rememberSaveable { mutableStateOf((entry?.date ?: LocalDate.now()).toString()) }
    var category by rememberSaveable { mutableStateOf(entry?.category ?: "餐饮") }
    var note by rememberSaveable { mutableStateOf(entry?.note ?: "") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var datePicker by rememberSaveable { mutableStateOf(false) }
    val id = rememberSaveable { entry?.id ?: UUID.randomUUID().toString() }
    Dialog(onDismissRequest = { if (!busy) onDismiss() }, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize().systemBarsPadding().imePadding().testTag("editor-form").verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(if (entry == null) "记一笔" else "编辑账目", style = MaterialTheme.typography.headlineSmall)
                Row {
                    listOf(EntryKind.EXPENSE to "支出", EntryKind.INCOME to "收入").forEach { (kind, label) ->
                        FilterChip(selected = kindName == kind.name, onClick = { if (kindName != kind.name) { kindName = kind.name; category = if (kind == EntryKind.EXPENSE) "餐饮" else "工资" } }, label = { Text(label) }, modifier = Modifier.padding(end = 8.dp).heightIn(min = 48.dp))
                    }
                }
                OutlinedTextField(amount, { amount = it }, label = { Text("金额（元）") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(date, { date = it }, label = { Text("日期（yyyy-MM-dd）") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                TextButton(onClick = { datePicker = true }) { Text("选择日期") }
                OutlinedTextField(category, { category = it }, label = { Text("分类（可自定义）") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                val suggestions = if (kindName == "EXPENSE") listOf("餐饮", "交通", "购物", "住房", "其他") else listOf("工资", "奖金", "其他")
                // Wrap chips for narrow screens and enlarged system fonts.
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    suggestions.forEach { suggestion -> SuggestionChip(onClick = { category = suggestion }, label = { Text(suggestion) }, modifier = Modifier.heightIn(min = 48.dp)) }
                }
                OutlinedTextField(note, { note = it }, label = { Text("备注（可选，最多200字）") }, modifier = Modifier.fillMaxWidth())
                (error ?: saveError)?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss, enabled = !busy) { Text("取消") }
                    Button(enabled = !busy, onClick = {
                        val cents = Money.parse(amount)
                        if (cents == null) { error = "金额请输入0.01至999999999.99，最多两位小数"; return@Button }
                        val parsedDate = runCatching { LocalDate.parse(date) }.getOrNull()
                        if (parsedDate == null) { error = "日期请输入有效的yyyy-MM-dd"; return@Button }
                        val candidate = Entry(id, EntryKind.valueOf(kindName), cents, category.trim(), parsedDate, note)
                        val invalid = runCatching { LedgerRules.validate(candidate) }.exceptionOrNull()
                        if (invalid != null) error = invalid.message else { error = null; onSave(candidate) }
                    }) { Text(if (busy) "保存中…" else "保存") }
                }
            }
        }
    }
    if (datePicker) {
        val selected = runCatching { LocalDate.parse(date).takeIf { it.year in 1900..9999 }?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli() }.getOrNull()
        val picker = rememberDatePickerState(initialSelectedDateMillis = selected, yearRange = 1900..9999)
        DatePickerDialog(onDismissRequest = { datePicker = false }, confirmButton = {
            TextButton(onClick = { picker.selectedDateMillis?.let { date = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toString() }; datePicker = false }) { Text("确定") }
        }, dismissButton = { TextButton(onClick = { datePicker = false }) { Text("取消") } }) { DatePicker(state = picker) }
    }
}

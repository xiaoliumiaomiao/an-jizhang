package com.xiaoliumiaomiao.anjizhang.ui

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.xiaoliumiaomiao.anjizhang.domain.*
import java.time.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable fun EntryEditor(entry: Entry?, busy: Boolean, saveError: String?, onDismiss: () -> Unit,
    onSave: (Entry) -> Unit, recentEntries: List<Entry> = emptyList(), isOperationBlocked: () -> Boolean = { busy }) {
    val initial = rememberSaveable(saver = EntryEditorStateSaver) { EntryEditorState.from(entry, LocalDate.now()) }
    var draft by rememberSaveable(stateSaver = EntryEditorStateSaver) { mutableStateOf(initial) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var datePicker by rememberSaveable { mutableStateOf(false) }
    var noteExpanded by rememberSaveable { mutableStateOf(initial.note.isNotEmpty()) }
    var discard by rememberSaveable { mutableStateOf(false) }
    var keypadOpen by rememberSaveable { mutableStateOf(false) }
    val currentOperationBlocked by rememberUpdatedState(isOperationBlocked)
    fun operationBlocked() = busy || currentOperationBlocked()
    fun requestDismiss() {
        if (!operationBlocked()) {
            keypadOpen = false
            if (draft.hasChangesFrom(initial)) discard = true else onDismiss()
        }
    }
    Dialog(onDismissRequest = {
        if (!operationBlocked()) { if (keypadOpen) keypadOpen = false else requestDismiss() }
    }, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        val focusManager = LocalFocusManager.current
        val keyboardController = LocalSoftwareKeyboardController.current
        val amountInteractionSource = remember { MutableInteractionSource() }
        fun dismissIme() { focusManager.clearFocus(force = true); keyboardController?.hide() }
        fun openAmountPanel(): Boolean {
            if (operationBlocked()) return false
            dismissIme(); keypadOpen = true
            return true
        }
        LaunchedEffect(amountInteractionSource, busy, focusManager, keyboardController) {
            amountInteractionSource.interactions.collect { interaction ->
                if (interaction is PressInteraction.Release) openAmountPanel()
            }
        }
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            BoxWithConstraints(Modifier.fillMaxSize().systemBarsPadding().imePadding()) {
                val panelMaxHeight = maxHeight * .48f
                Column(Modifier.fillMaxSize().testTag("editor-shell")) {
                    Column(Modifier.weight(1f).fillMaxWidth().testTag("editor-form")
                        .verticalScroll(rememberScrollState()).padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(if (entry == null) "记一笔" else "编辑账目", style = MaterialTheme.typography.headlineSmall)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(EntryKind.EXPENSE to "支出", EntryKind.INCOME to "收入").forEach { (kind, label) ->
                                FilterChip(selected = draft.kind == kind, enabled = !busy,
                                    onClick = { draft = draft.copy(kind = kind) }, label = { Text(label) },
                                    modifier = Modifier.heightIn(min = 48.dp))
                            }
                        }
                        OutlinedTextField(draft.amount, {}, label = { Text("金额（元）") }, singleLine = true,
                            textStyle = MaterialTheme.typography.headlineMedium, readOnly = true, enabled = !busy,
                            interactionSource = amountInteractionSource,
                            modifier = Modifier.fillMaxWidth().semantics { onClick(label = "输入金额") { openAmountPanel() } }
                                .onFocusChanged { if (it.isFocused) keyboardController?.hide() })
                        OutlinedTextField(draft.date, { draft = draft.copy(date = it) }, label = { Text("日期（yyyy-MM-dd）") },
                            singleLine = true, enabled = !busy,
                            modifier = Modifier.fillMaxWidth().onFocusChanged { if (it.isFocused) keypadOpen = false })
                        TextButton(enabled = !busy, onClick = { dismissIme(); keypadOpen = false; datePicker = true }) { Text("选择日期") }
                        OutlinedTextField(draft.category, { draft = draft.copy(category = it) }, label = { Text("分类（可自定义）") },
                            singleLine = true, enabled = !busy,
                            modifier = Modifier.fillMaxWidth().onFocusChanged { if (it.isFocused) keypadOpen = false })
                        val recent = remember(recentEntries, draft.kind) { RecentCategories.forKind(recentEntries, draft.kind) }
                        if (recent.isNotEmpty()) Column(Modifier.fillMaxWidth().testTag("recent-categories")) {
                            Text("最近使用", style = MaterialTheme.typography.bodySmall)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                recent.forEach { category -> SuggestionChip(enabled = !busy,
                                    onClick = { draft = draft.copy(category = category) }, label = { Text(category) },
                                    modifier = Modifier.heightIn(min = 48.dp)) }
                            }
                        }
                        val suggestions = if (draft.kind == EntryKind.EXPENSE) listOf("餐饮", "交通", "购物", "住房", "其他")
                            else listOf("工资", "奖金", "其他")
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            suggestions.forEach { category -> SuggestionChip(enabled = !busy,
                                onClick = { draft = draft.copy(category = category) }, label = { Text(category) },
                                modifier = Modifier.heightIn(min = 48.dp)) }
                        }
                        TextButton(enabled = !busy, onClick = {
                            if (noteExpanded) dismissIme()
                            noteExpanded = !noteExpanded
                        }, modifier = Modifier.heightIn(min = 48.dp).testTag("note-toggle")) {
                            Text(if (noteExpanded) "收起备注" else "添加备注")
                        }
                        if (noteExpanded) OutlinedTextField(draft.note, { draft = draft.copy(note = it) },
                            label = { Text("备注（可选，最多200字）") }, enabled = !busy,
                            modifier = Modifier.fillMaxWidth().onFocusChanged { if (it.isFocused) keypadOpen = false })
                        (error ?: saveError)?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(enabled = !busy, onClick = { dismissIme(); requestDismiss() }) { Text("取消") }
                            Button(enabled = !busy, onClick = {
                                if (operationBlocked()) return@Button
                                dismissIme(); keypadOpen = false
                                val cents = Money.parse(draft.amount)
                                if (cents == null) { error = "金额请输入0.01至999999999.99，最多两位小数"; return@Button }
                                if (draft.category.isBlank()) { error = "请选择或填写分类"; return@Button }
                                val parsedDate = runCatching { LocalDate.parse(draft.date) }.getOrNull()
                                if (parsedDate == null) { error = "日期请输入有效的yyyy-MM-dd"; return@Button }
                                val candidate = Entry(draft.id, draft.kind, cents, draft.category.trim(), parsedDate, draft.note)
                                val invalid = runCatching { LedgerRules.validate(candidate) }.exceptionOrNull()
                                if (invalid != null) error = invalid.message else { error = null; onSave(candidate) }
                            }) { Text(if (busy) "保存中…" else "保存") }
                        }
                    }
                    if (keypadOpen) Surface(Modifier.fillMaxWidth().heightIn(max = panelMaxHeight).testTag("amount-keypad-panel"),
                        color = MaterialTheme.colorScheme.surfaceContainer, tonalElevation = 3.dp) {
                        Column(Modifier.verticalScroll(rememberScrollState()).padding(8.dp)) {
                            AmountKeypad(enabled = !busy,
                                onKey = { dismissIme(); draft = draft.copy(amount = AmountKeypadInput.append(draft.amount, it)) },
                                onBackspace = { dismissIme(); draft = draft.copy(amount = AmountKeypadInput.backspace(draft.amount)) },
                                onClear = { dismissIme(); draft = draft.copy(amount = "") },
                                onClose = { if (!operationBlocked()) { dismissIme(); keypadOpen = false } })
                        }
                    }
                }
            }
        }
    }
    if (discard) AlertDialog(modifier = Modifier.testTag("discard-draft"),
        onDismissRequest = { if (!operationBlocked()) discard = false }, title = { Text("放弃未保存的修改？") },
        text = { Text("放弃后本次输入不会保存。") },
        confirmButton = { TextButton(enabled = !busy, onClick = { if (!operationBlocked()) { discard = false; onDismiss() } }) { Text("放弃修改") } },
        dismissButton = { TextButton(enabled = !busy, onClick = { if (!operationBlocked()) discard = false }) { Text("继续编辑") } })
    if (datePicker) {
        val selected = runCatching { LocalDate.parse(draft.date).takeIf { it.year in 1900..9999 }
            ?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli() }.getOrNull()
        val picker = rememberDatePickerState(initialSelectedDateMillis = selected, yearRange = 1900..9999)
        DatePickerDialog(onDismissRequest = { datePicker = false }, confirmButton = {
            TextButton(onClick = { picker.selectedDateMillis?.let {
                draft = draft.copy(date = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toString())
            }; datePicker = false }) { Text("确定") }
        }, dismissButton = { TextButton(onClick = { datePicker = false }) { Text("取消") } }) { DatePicker(state = picker) }
    }
}

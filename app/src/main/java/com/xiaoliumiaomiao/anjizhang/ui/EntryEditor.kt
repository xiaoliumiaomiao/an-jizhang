package com.xiaoliumiaomiao.anjizhang.ui

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.xiaoliumiaomiao.anjizhang.domain.*
import java.time.*

internal fun entryFromEditorDraft(draft: EntryEditorState): Entry? {
    val cents = Money.parse(draft.amount) ?: return null
    val date = runCatching { LocalDate.parse(draft.date) }.getOrNull() ?: return null
    return Entry(draft.id, draft.kind, cents, draft.category, date, draft.note, draft.accountId)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable fun EntryEditor(entry: Entry?, busy: Boolean, saveError: String?, onDismiss: () -> Unit,
    onSave: (Entry) -> Unit, recentEntries: List<Entry> = emptyList(), isOperationBlocked: () -> Boolean = { busy },
    accounts: List<Account> = emptyList(), categories: List<Category>? = null,
    onAddCategory: ((EntryKind) -> Unit)? = null, childModalOpen: Boolean = false,
    frozen: Boolean = false, readError: Boolean = false, onRetryRead: () -> Unit = {},
    contextLabel: String = "明细", categorySelection: Category? = null, categorySelectionToken: String? = null) {
    val initial = rememberSaveable(saver = EntryEditorStateSaver) { EntryEditorState.from(entry, LocalDate.now()) }
    var draft by rememberSaveable(stateSaver = EntryEditorStateSaver) { mutableStateOf(initial) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var datePicker by rememberSaveable { mutableStateOf(false) }
    var noteExpanded by rememberSaveable { mutableStateOf(initial.note.isNotEmpty()) }
    var discard by rememberSaveable { mutableStateOf(false) }
    var keypadOpen by rememberSaveable { mutableStateOf(false) }
    var categoryPicker by rememberSaveable { mutableStateOf(false) }
    var accountPicker by rememberSaveable { mutableStateOf(false) }
    var lastCategoryToken by rememberSaveable { mutableStateOf<String?>(null) }
    val directory = remember(categories, recentEntries) {
        categories ?: recentEntries.distinctBy { it.kind to it.category }.mapIndexed { index, old ->
            Category("legacy-${old.id}", old.kind, old.category, index)
        }
    }
    LaunchedEffect(categorySelectionToken, categorySelection) {
        if (categorySelectionToken != null && categorySelectionToken != lastCategoryToken && categorySelection != null) {
            draft = draft.copy(category = categorySelection.name)
            lastCategoryToken = categorySelectionToken
        }
    }
    val left = LocalHandedness.current == Handedness.LEFT
    val currentOperationBlocked by rememberUpdatedState(isOperationBlocked)
    fun formLocked() = busy || frozen || childModalOpen || currentOperationBlocked()
    fun operationBlocked() = formLocked() || datePicker || categoryPicker || accountPicker || discard
    fun requestDismiss() {
        if (!operationBlocked()) {
            keypadOpen = false
            if (draft.hasChangesFrom(initial)) discard = true else onDismiss()
        }
    }
    Dialog(onDismissRequest = {
        if (!operationBlocked()) { if (keypadOpen) keypadOpen = false else requestDismiss() }
    }, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        SyncDialogWindowAppearance(DialogWindowAppearance.FULL_SCREEN)
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
        val kindColor = if (draft.kind == EntryKind.INCOME) MaterialTheme.colorScheme.secondary
            else MaterialTheme.colorScheme.tertiary
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            BoxWithConstraints(Modifier.fillMaxSize().systemBarsPadding().imePadding()) {
                val panelMaxHeight = maxHeight * .48f
                Column(Modifier.fillMaxSize().testTag("editor-shell")) {
                    Column(Modifier.weight(1f).fillMaxWidth().testTag("editor-form")
                        .verticalScroll(rememberScrollState()).padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            if (left) IconButton(enabled = !operationBlocked(), onClick = { dismissIme(); requestDismiss() },
                                modifier = Modifier.size(48.dp).testTag("editor-close").semantics { contentDescription = "关闭编辑器" }) { LedgerGlyph("back") }
                            Text(if (entry == null) "记一笔" else "编辑账目",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                            if (!left) IconButton(enabled = !operationBlocked(), onClick = { dismissIme(); requestDismiss() },
                                modifier = Modifier.size(48.dp).testTag("editor-close").semantics { contentDescription = "关闭编辑器" }) {
                                LedgerGlyph("back")
                            }
                        }
                        Text("返回$contextLabel", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.testTag("editor-context"))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            listOf(EntryKind.EXPENSE to "支出", EntryKind.INCOME to "收入").forEach { (kind, label) ->
                                val selectedColor = if (kind == EntryKind.INCOME) MaterialTheme.colorScheme.secondary
                                    else MaterialTheme.colorScheme.tertiary
                                FilterChip(selected = draft.kind == kind, enabled = !operationBlocked(),
                                    onClick = { if (!operationBlocked()) draft = draft.copy(kind = kind) }, label = { Text(label) },
                                    leadingIcon = { KindMarker(kind) }, shape = RoundedCornerShape(16.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = selectedColor.copy(alpha = .12f),
                                        selectedLabelColor = selectedColor),
                                    modifier = Modifier.weight(1f).heightIn(min = 52.dp))
                            }
                        }
                        LedgerPanel {
                            BoxWithConstraints(Modifier.fillMaxWidth()) {
                                val textMeasurer = rememberTextMeasurer()
                                val density = LocalDensity.current
                                val availableWidth = (constraints.maxWidth - with(density) { 32.dp.roundToPx() }).coerceAtLeast(0)
                                val styles = listOf(MaterialTheme.typography.headlineMedium,
                                    MaterialTheme.typography.headlineSmall, MaterialTheme.typography.titleLarge,
                                    MaterialTheme.typography.bodyLarge).map {
                                    it.copy(fontWeight = FontWeight.SemiBold, color = kindColor)
                                }
                                val amountStyle = if (draft.amount.isEmpty()) styles.first() else styles.firstOrNull {
                                    textMeasurer.measure(draft.amount, style = it, softWrap = false, maxLines = 1)
                                        .size.width <= availableWidth
                                } ?: styles.last()
                                OutlinedTextField(draft.amount, {}, label = { Text("金额（元）") }, singleLine = true,
                                    textStyle = amountStyle, readOnly = true, enabled = !operationBlocked(),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = kindColor),
                                    interactionSource = amountInteractionSource,
                                    modifier = Modifier.fillMaxWidth().semantics { onClick(label = "输入金额") { openAmountPanel() } }
                                        .onFocusChanged { if (it.isFocused) keyboardController?.hide() })
                            }
                            Text("点按金额，使用数字键盘", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        LedgerPanel {
                            LedgerSectionTitle("账目详情")
                            DateSelectionRow(draft.date, !operationBlocked(), "editor-date-select") {
                                if (!operationBlocked()) { dismissIme(); keypadOpen = false; datePicker = true }
                            }
                            val accountName = accounts.firstOrNull { it.id == draft.accountId }?.let {
                                it.name + if (it.archived) "（已归档）" else ""
                            } ?: if (draft.accountId == null) "未分配账户" else "原账户（保留关联）"
                            OutlinedButton(enabled = !operationBlocked(), onClick = {
                                if (!operationBlocked()) { dismissIme(); keypadOpen = false; accountPicker = true }
                            }, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("editor-account-select")) {
                                Text("账户：$accountName")
                            }
                            EditorCategoryPicker(directory, draft.kind, draft.category, !operationBlocked(),
                                onSelect = { if (!operationBlocked()) draft = draft.copy(category = it.name) },
                                onAll = { if (!operationBlocked()) { dismissIme(); keypadOpen = false; categoryPicker = true } },
                                onAdd = onAddCategory?.let { add -> { if (!operationBlocked()) { dismissIme(); keypadOpen = false; add(draft.kind) } } })
                        }
                        LedgerPanel {
                            TextButton(enabled = !operationBlocked(), onClick = {
                                if (operationBlocked()) return@TextButton
                                if (noteExpanded) dismissIme()
                                noteExpanded = !noteExpanded
                            }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("note-toggle")) {
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    LedgerGlyph(if (noteExpanded) "chevron" else "plus")
                                    Text(if (noteExpanded) "收起备注" else "添加备注")
                                }
                            }
                            if (noteExpanded) OutlinedTextField(draft.note, { if (!operationBlocked()) draft = draft.copy(note = it) },
                                label = { Text("备注（可选，最多200字）") }, enabled = !operationBlocked(), shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth().onFocusChanged { if (it.isFocused) keypadOpen = false })
                        }
                        (error ?: saveError)?.let {
                            Surface(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(16.dp)) {
                                Text(it, modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                        if (readError || frozen) OperationFeedback(ComponentOperationState(busy, frozen, null, readError), onRetryRead)
                        HandedFormActions(!operationBlocked(), { dismissIme(); requestDismiss() }, {
                                if (!operationBlocked()) {
                                dismissIme(); keypadOpen = false
                                val candidate = entryFromEditorDraft(draft)
                                when {
                                    Money.parse(draft.amount) == null -> error = "金额请输入0.01至999999999.99，最多两位小数"
                                    draft.category.isBlank() -> error = "请选择类别"
                                    candidate == null -> error = "请选择有效日期"
                                    else -> {
                                        val invalid = runCatching { LedgerRules.validate(candidate) }.exceptionOrNull()
                                        if (invalid != null) error = invalid.message else { error = null; onSave(candidate) }
                                    }
                                }
                                }
                            }, if (busy) "保存中…" else "保存", "editor-save")
                    }
                    if (keypadOpen) Surface(Modifier.fillMaxWidth().heightIn(max = panelMaxHeight).testTag("amount-keypad-panel"),
                        color = MaterialTheme.colorScheme.surfaceContainer, shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)) {
                        Column(Modifier.verticalScroll(rememberScrollState()).padding(8.dp)) {
                            AmountKeypad(enabled = !operationBlocked(),
                                onKey = { if (!operationBlocked()) { dismissIme(); draft = draft.copy(amount = AmountKeypadInput.append(draft.amount, it)) } },
                                onBackspace = { if (!operationBlocked()) { dismissIme(); draft = draft.copy(amount = AmountKeypadInput.backspace(draft.amount)) } },
                                onClear = { if (!operationBlocked()) { dismissIme(); draft = draft.copy(amount = "") } },
                                onClose = { if (!operationBlocked()) { dismissIme(); keypadOpen = false } })
                        }
                    }
                }
            }
        }
    }
    if (discard) AlertDialog(modifier = Modifier.testTag("discard-draft"),
        onDismissRequest = { if (!formLocked()) discard = false }, title = { Text("放弃未保存的修改？") },
        text = { SyncDialogWindowAppearance(DialogWindowAppearance.FLOATING); Text("放弃后本次输入不会保存。") },
        confirmButton = { HandedFormActions(!formLocked(), { if (!formLocked()) discard = false },
            { if (!formLocked()) { discard = false; onDismiss() } }, "放弃修改", "discard-confirm", "继续编辑") })
    if (datePicker) CalendarDateDialog(draft.date, { if (!formLocked()) draft = draft.copy(date = it) }, { datePicker = false }, ::formLocked)
    if (categoryPicker) CategorySelectionDialog(directory, draft.kind, draft.category,
        { if (!formLocked()) draft = draft.copy(category = it.name) }, { categoryPicker = false }, ::formLocked)
    if (accountPicker) EditorAccountSelectionDialog(accounts, draft.accountId, entry?.accountId,
        { if (!formLocked()) draft = draft.copy(accountId = it) }, { accountPicker = false }, ::formLocked)
}

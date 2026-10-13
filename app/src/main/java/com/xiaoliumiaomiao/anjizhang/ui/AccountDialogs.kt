package com.xiaoliumiaomiao.anjizhang.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.xiaoliumiaomiao.anjizhang.domain.*
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID

enum class LiabilityBalanceMode { DEBT, OVERPAYMENT }

internal fun parseBalanceInput(raw: String, kind: AccountKind, mode: LiabilityBalanceMode): Long? {
    if (kind == AccountKind.LIABILITY && raw.trim().startsWith("-")) return null
    val cents = BalanceMoney.parse(raw) ?: return null
    return if (kind == AccountKind.LIABILITY && mode == LiabilityBalanceMode.DEBT) -cents else cents
}

/** Presentation only. The root owns attempts, receipts and the synchronous sending latch. */
data class ComponentOperationState(
    val busy: Boolean = false,
    val frozen: Boolean = false,
    val error: String? = null,
    val readError: Boolean = false,
)

@OptIn(ExperimentalLayoutApi::class)
@Composable internal fun HandedFormActions(enabled: Boolean, onDismiss: () -> Unit, onSave: () -> Unit,
    saveLabel: String = "保存", saveTag: String = "form-save", dismissLabel: String = "取消") {
    val left = LocalHandedness.current == Handedness.LEFT
    FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, if (left) Alignment.Start else Alignment.End),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val save: @Composable () -> Unit = {
            Button(enabled = enabled, onClick = onSave, modifier = Modifier.heightIn(min = 48.dp).testTag(saveTag)) { Text(saveLabel) }
        }
        val cancel: @Composable () -> Unit = {
            OutlinedButton(enabled = enabled, onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp).testTag("$saveTag-cancel")) { Text(dismissLabel) }
        }
        if (left) { save(); cancel() } else { cancel(); save() }
    }
}

@Composable internal fun OperationFeedback(state: ComponentOperationState, onRetryRead: () -> Unit) {
    state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    if (state.frozen) Text(if (state.readError) "无法读取提交结果。请重试读取，当前输入暂时锁定。" else "正在确认提交结果，当前输入暂时锁定。",
        style = MaterialTheme.typography.bodySmall)
    if (state.readError || (state.frozen && !state.busy)) TextButton(onClick = onRetryRead, enabled = !state.busy,
        modifier = Modifier.heightIn(min = 48.dp).testTag("operation-retry-read")) { Text("重试读取") }
    if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
}

@Composable private fun AccountFormDialog(title: String, tag: String, state: ComponentOperationState,
    blocked: () -> Boolean, onDismiss: () -> Unit, onSubmit: () -> Unit, onRetryRead: () -> Unit,
    saveLabel: String = "保存", content: @Composable ColumnScope.() -> Unit) {
    AlertDialog(onDismissRequest = { if (!blocked()) onDismiss() },
        modifier = Modifier.testTag(tag), title = { Text(title) }, text = {
            SyncDialogWindowAppearance(DialogWindowAppearance.FLOATING)
            Column(Modifier.fillMaxWidth().heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                content()
                OperationFeedback(state, onRetryRead)
            }
        }, confirmButton = {
            HandedFormActions(!state.busy && !state.frozen && !blocked(),
                { if (!blocked()) onDismiss() }, { if (!blocked()) onSubmit() }, saveLabel, "$tag-save")
        })
}

@OptIn(ExperimentalLayoutApi::class)
@Composable internal fun BalanceAmountField(raw: String, onRaw: (String) -> Unit, kind: AccountKind,
    mode: LiabilityBalanceMode, onMode: (LiabilityBalanceMode) -> Unit, enabled: Boolean, tag: String) {
    if (kind == AccountKind.LIABILITY) FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        LiabilityBalanceMode.entries.forEach { value ->
            FilterChip(selected = mode == value, enabled = enabled, onClick = { if (enabled) onMode(value) },
                label = { Text(if (value == LiabilityBalanceMode.DEBT) "欠款" else "溢缴") },
                modifier = Modifier.heightIn(min = 48.dp).testTag("$tag-${value.name.lowercase()}"))
        }
    }
    OutlinedTextField(raw, { if (enabled) onRaw(it) }, enabled = enabled,
        label = { Text(if (kind == AccountKind.ASSET) "余额（元，可为0或负数）" else "金额（元，输入非负金额）") },
        singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth().testTag(tag))
    if (kind == AccountKind.ASSET) TextButton(enabled = enabled,
        onClick = { if (enabled) onRaw(if (raw.startsWith("-")) raw.removePrefix("-") else "-$raw") },
        modifier = Modifier.heightIn(min = 48.dp).testTag("$tag-minus")) { Text("切换正负号（负数表示透支）") }
    else Text("欠款减少净资产，溢缴增加净资产；零表示无欠款。", style = MaterialTheme.typography.bodySmall)
}

internal fun subtypeLabel(value: AccountSubtype): String = when (value) {
    AccountSubtype.CASH -> "现金"; AccountSubtype.BANK -> "银行"; AccountSubtype.FUND -> "基金"
    AccountSubtype.CREDIT_CARD -> "信用卡"; AccountSubtype.LOAN -> "借款"; AccountSubtype.OTHER -> "其他"
}

@OptIn(ExperimentalLayoutApi::class)
@Composable fun AccountEditorDialog(account: Account?, kindLocked: Boolean, today: LocalDate,
    operationState: ComponentOperationState, onDismiss: () -> Unit,
    onCreate: (Account, BalanceAdjustmentRequest?) -> Unit, onSave: (Account) -> Unit,
    onRetryRead: () -> Unit, isOperationBlocked: () -> Boolean,
    canRestartRequest: Boolean = false, onRequestRestart: () -> Boolean = { false }, currentToday: () -> LocalDate = LocalDate::now) {
    var draft by rememberSaveable(stateSaver = AccountEditorStateSaver) { mutableStateOf(AccountEditorState.from(account, today)) }
    var mode by rememberSaveable { mutableStateOf(LiabilityBalanceMode.DEBT) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    val gate by rememberUpdatedState(isOperationBlocked)
    fun blocked() = operationState.busy || operationState.frozen || gate()
    AccountFormDialog(if (account == null) "新增账户" else "编辑账户", "account-editor", operationState, ::blocked,
        onDismiss, {
            val name = draft.name.trim()
            if (name.length !in 1..30) { error = "账户名称请输入1至30字"; return@AccountFormDialog }
            if (draft.note.length > 200) { error = "备注最多200字"; return@AccountFormDialog }
            val candidate = draft.copy(name = name).toAccount()
            if (account != null) { error = null; onSave(candidate); return@AccountFormDialog }
            val initial = if (draft.initialBalance.isBlank()) null else {
                val cents = parseBalanceInput(draft.initialBalance, draft.kind, mode)
                if (cents == null) { error = "余额请输入有效金额，最多两位小数，负债金额不能带负号"; return@AccountFormDialog }
                draft.copy(initialBalance = Money.plain(cents)).initialRequest()
                    ?: run { error = "请求日期无效，请重新发起"; return@AccountFormDialog }
            }
            error = null; onCreate(candidate, initial)
        }, onRetryRead) {
        OutlinedTextField(draft.name, { if (!blocked()) draft = draft.copy(name = it) }, enabled = !blocked(),
            label = { Text("账户名称") }, singleLine = true, modifier = Modifier.fillMaxWidth().testTag("account-name"))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AccountKind.entries.forEach { kind -> FilterChip(selected = draft.kind == kind, enabled = !blocked() && !kindLocked,
                onClick = { if (!blocked() && !kindLocked) draft = draft.copy(kind = kind,
                    subtype = if (kind == AccountKind.ASSET) AccountSubtype.CASH else AccountSubtype.CREDIT_CARD) },
                label = { Text(if (kind == AccountKind.ASSET) "资产账户" else "负债账户") },
                modifier = Modifier.heightIn(min = 48.dp).testTag("account-kind-${kind.name.lowercase()}")) }
        }
        if (kindLocked) Text("账户已有记录，资产或负债类型保持不变。", style = MaterialTheme.typography.bodySmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AccountSubtype.entries.forEach { subtype -> FilterChip(selected = draft.subtype == subtype, enabled = !blocked(),
                onClick = { if (!blocked()) draft = draft.copy(subtype = subtype) }, label = { Text(subtypeLabel(subtype)) },
                modifier = Modifier.heightIn(min = 48.dp).testTag("account-subtype-${subtype.name.lowercase()}")) }
        }
        if (account == null) {
            Text("初始余额（可选，留空不写余额调整）", style = MaterialTheme.typography.titleSmall)
            BalanceAmountField(draft.initialBalance, { if (!blocked()) draft = draft.copy(initialBalance = it) }, draft.kind, mode,
                { if (!blocked()) mode = it }, !blocked(), "account-initial-balance")
            Text("初始余额日期：${draft.date}。与账户一起保存，不计入收支。", style = MaterialTheme.typography.bodySmall)
            OutlinedTextField(draft.note, { if (!blocked()) draft = draft.copy(note = it) }, enabled = !blocked(),
                label = { Text("备注（可选，最多200字）") }, modifier = Modifier.fillMaxWidth().testTag("account-initial-note"))
            if (canRestartRequest) TextButton(enabled = !blocked(), onClick = {
                if (!blocked()) {
                    val restarted = draft.restartForToday(onRequestRestart, currentToday)
                    if (restarted != draft) { draft = restarted; error = null }
                }
            }, modifier = Modifier.heightIn(min = 48.dp).testTag("account-restart-request")) { Text("以今天重新发起请求（保留输入）") }
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
}

@Composable fun BalanceEditorDialog(account: Account, today: LocalDate, operationState: ComponentOperationState,
    onDismiss: () -> Unit, onSave: (BalanceAdjustmentRequest) -> Unit,
    onRetryRead: () -> Unit, isOperationBlocked: () -> Boolean,
    canRestartRequest: Boolean = false, onRequestRestart: () -> Boolean = { false }, currentToday: () -> LocalDate = LocalDate::now,
    currentBalanceCents: Long? = null) {
    var draft by rememberSaveable(stateSaver = BalanceEditorStateSaver) {
        mutableStateOf(BalanceEditorState.create(account.id, today).copy(targetBalance = currentBalanceCents?.let {
            Money.plain(if (account.kind == AccountKind.LIABILITY && it < 0) -it else it)
        }.orEmpty()))
    }
    var mode by rememberSaveable { mutableStateOf(if (account.kind == AccountKind.LIABILITY && (currentBalanceCents ?: 0L) > 0)
        LiabilityBalanceMode.OVERPAYMENT else LiabilityBalanceMode.DEBT) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    val gate by rememberUpdatedState(isOperationBlocked)
    fun blocked() = operationState.busy || operationState.frozen || gate()
    AccountFormDialog("调整 ${account.name} 的余额", "balance-editor", operationState, ::blocked, onDismiss, {
        val cents = parseBalanceInput(draft.targetBalance, account.kind, mode)
        if (cents == null) { error = "余额请输入有效金额，最多两位小数，负债金额不能带负号"; return@AccountFormDialog }
        if (draft.note.length > 200) { error = "备注最多200字"; return@AccountFormDialog }
        val request = draft.copy(targetBalance = Money.plain(cents)).request()
        if (request == null) error = "请求日期无效，请重新发起" else { error = null; onSave(request) }
    }, onRetryRead, saveLabel = "确认调整") {
        Text("将截至今天的余额调整为此金额，不计入收入和支出。之后补录、修改或删除历史记录，余额会重新计算。")
        Text("请求日期：${draft.date}", style = MaterialTheme.typography.bodySmall)
        BalanceAmountField(draft.targetBalance, { if (!blocked()) draft = draft.copy(targetBalance = it) }, account.kind, mode,
            { if (!blocked()) mode = it }, !blocked(), "balance-target")
        OutlinedTextField(draft.note, { if (!blocked()) draft = draft.copy(note = it) }, enabled = !blocked(),
            label = { Text("备注（可选，最多200字）") }, modifier = Modifier.fillMaxWidth().testTag("balance-note"))
        if (canRestartRequest) TextButton(enabled = !blocked(), onClick = {
            if (!blocked()) {
                val restarted = draft.restartForToday(onRequestRestart, currentToday)
                if (restarted != draft) { draft = restarted; error = null }
            }
        }, modifier = Modifier.heightIn(min = 48.dp).testTag("balance-restart-request")) { Text("以今天重新发起请求（保留输入）") }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable internal fun CalendarDateDialog(rawDate: String, onSelect: (String) -> Unit, onDismiss: () -> Unit,
    blocked: () -> Boolean = { false }) {
    val selected = runCatching { LocalDate.parse(rawDate).takeIf { it.year in 1900..9999 }
        ?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli() }.getOrNull()
    val picker = rememberDatePickerState(initialSelectedDateMillis = selected, yearRange = 1900..9999)
    DatePickerDialog(onDismissRequest = { if (!blocked()) onDismiss() }, confirmButton = {
        TextButton(enabled = !blocked() && picker.selectedDateMillis != null, onClick = {
            if (!blocked()) picker.selectedDateMillis?.let {
                onSelect(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toString())
                onDismiss()
            }
        }, modifier = Modifier.heightIn(min = 48.dp).testTag("calendar-confirm")) { Text("确定") }
    }, dismissButton = { TextButton(enabled = !blocked(), onClick = { if (!blocked()) onDismiss() },
        modifier = Modifier.heightIn(min = 48.dp).testTag("calendar-cancel")) { Text("取消") } }) {
        SyncDialogWindowAppearance(DialogWindowAppearance.FLOATING)
        DatePicker(state = picker, showModeToggle = false)
    }
}

@Composable internal fun DateSelectionRow(rawDate: String, enabled: Boolean, tag: String, onClick: () -> Unit) {
    val label = runCatching { LocalDate.parse(rawDate).let { "${it.year}年${it.monthValue}月${it.dayOfMonth}日" } }
        .getOrDefault("请选择有效日期")
    Surface(onClick = { if (enabled) onClick() }, enabled = enabled, color = MaterialTheme.colorScheme.surfaceContainer,
        shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp).testTag(tag)
            .semantics { contentDescription = "选择日期，$label" }) {
        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            LedgerGlyph("calendar")
            Column(Modifier.weight(1f)) { Text("日期", style = MaterialTheme.typography.labelMedium); Text(label) }
            Text("选择日期", style = MaterialTheme.typography.labelLarge)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable fun TransferEditorDialog(transfer: AccountTransfer?, accounts: List<Account>, today: LocalDate,
    operationState: ComponentOperationState, onDismiss: () -> Unit, onSave: (AccountTransfer) -> Unit,
    onRetryRead: () -> Unit, isOperationBlocked: () -> Boolean) {
    var draft by rememberSaveable(stateSaver = TransferEditorStateSaver) { mutableStateOf(TransferEditorState.from(transfer, today)) }
    var datePicker by rememberSaveable { mutableStateOf(false) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    val gate by rememberUpdatedState(isOperationBlocked)
    fun locked() = operationState.busy || operationState.frozen || gate()
    fun blocked() = locked() || datePicker
    val active = remember(accounts) { accounts.filterNot { it.archived }.sortedWith(compareBy({ it.sortOrder }, { it.id })) }
    AccountFormDialog(if (transfer == null) "账户转账" else "编辑转账", "transfer-editor", operationState, ::blocked, onDismiss, {
        if (draft.fromAccountId == draft.toAccountId || active.none { it.id == draft.fromAccountId } || active.none { it.id == draft.toAccountId }) {
            error = "请选择两个不同的活动账户"; return@AccountFormDialog
        }
        val candidate = draft.transfer()
        if (candidate == null) { error = "转账金额请输入0.01至999999999.99，最多两位小数"; return@AccountFormDialog }
        if (candidate.date.year !in 1900..9999) { error = "请选择有效日期"; return@AccountFormDialog }
        if (candidate.note.length > 200) { error = "备注最多200字"; return@AccountFormDialog }
        error = null; onSave(candidate)
    }, onRetryRead) {
        val focus = LocalFocusManager.current
        val keyboard = LocalSoftwareKeyboardController.current
        Text("转账只改变账户余额，不计入收入和支出。", style = MaterialTheme.typography.bodySmall)
        listOf(true, false).forEach { from ->
            Text(if (from) "转出账户" else "转入账户", style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                active.forEach { item -> FilterChip(selected = item.id == if (from) draft.fromAccountId else draft.toAccountId,
                    enabled = !blocked(), onClick = { if (!blocked()) draft = if (from) draft.copy(fromAccountId = item.id) else draft.copy(toAccountId = item.id) },
                    label = { Text(item.name) }, modifier = Modifier.heightIn(min = 48.dp).testTag("transfer-${if (from) "from" else "to"}-${item.id}")) }
            }
        }
        OutlinedTextField(draft.amount, { if (!blocked()) draft = draft.copy(amount = it) }, enabled = !blocked(),
            label = { Text("转账金额（元）") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth().testTag("transfer-amount"))
        DateSelectionRow(draft.date, !blocked(), "transfer-date-select") {
            if (!blocked()) { focus.clearFocus(force = true); keyboard?.hide(); datePicker = true }
        }
        OutlinedTextField(draft.note, { if (!blocked()) draft = draft.copy(note = it) }, enabled = !blocked(),
            label = { Text("备注（可选，最多200字）") }, modifier = Modifier.fillMaxWidth().testTag("transfer-note"))
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
    if (datePicker) CalendarDateDialog(draft.date, { if (!locked()) draft = draft.copy(date = it) }, { datePicker = false }, ::locked)
}

@Composable fun CategoryEditorDialog(category: Category?, kind: EntryKind, sortOrder: Int,
    operationState: ComponentOperationState, onDismiss: () -> Unit, onSave: (Category) -> Unit,
    onRetryRead: () -> Unit, isOperationBlocked: () -> Boolean) {
    val id = rememberSaveable { category?.id ?: UUID.randomUUID().toString() }
    val capturedKind = rememberSaveable { category?.kind ?: kind }
    val capturedOrder = rememberSaveable { category?.sortOrder ?: sortOrder }
    val capturedArchived = rememberSaveable { category?.archived ?: false }
    var name by rememberSaveable { mutableStateOf(category?.name.orEmpty()) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    val gate by rememberUpdatedState(isOperationBlocked)
    fun blocked() = operationState.busy || operationState.frozen || gate()
    AccountFormDialog(if (category == null) "新增类别" else "重命名类别", "category-editor", operationState, ::blocked,
        onDismiss, {
            val trimmed = name.trim()
            if (trimmed.length !in 1..20) error = "类别名称请输入1至20字"
            else { error = null; onSave(Category(id, capturedKind, trimmed, capturedOrder, capturedArchived)) }
        }, onRetryRead) {
        Text(if (capturedKind == EntryKind.INCOME) "收入类别" else "支出类别")
        OutlinedTextField(name, { if (!blocked()) name = it }, enabled = !blocked(), label = { Text("类别名称") },
            singleLine = true, modifier = Modifier.fillMaxWidth().testTag("category-name"))
        Text("已有账目保留当时的类别名称；重命名不会改写历史记录。", style = MaterialTheme.typography.bodySmall)
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
}

@Composable fun AccountEventDeleteDialog(transfer: AccountTransfer?, adjustment: AccountAdjustment?,
    operationState: ComponentOperationState, onDismiss: () -> Unit, onDelete: (String) -> Unit,
    onRetryRead: () -> Unit, isOperationBlocked: () -> Boolean) {
    require((transfer == null) != (adjustment == null))
    val gate by rememberUpdatedState(isOperationBlocked)
    fun blocked() = operationState.busy || operationState.frozen || gate()
    AccountFormDialog(if (transfer != null) "删除转账？" else "删除余额调整？", "account-event-delete", operationState,
        ::blocked, onDismiss, { onDelete(transfer?.id ?: adjustment!!.id) }, onRetryRead, "确认删除") {
        Text("删除后无法撤销，只影响账户余额，不计入收入和支出。")
        Text(if (transfer != null) "${transfer.date} ${Money.format(transfer.amountCents)} ${transfer.note}"
            else "${adjustment!!.date} 调整 ${Money.format(adjustment.deltaCents)} ${adjustment.note}")
    }
}

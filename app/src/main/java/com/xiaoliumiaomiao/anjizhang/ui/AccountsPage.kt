package com.xiaoliumiaomiao.anjizhang.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.xiaoliumiaomiao.anjizhang.domain.*
import java.time.LocalDate

internal fun accountBalanceLabel(account: Account, cents: Long): String = when {
    account.kind == AccountKind.LIABILITY && cents < 0 -> "欠款 ${Money.format(-cents)}"
    account.kind == AccountKind.LIABILITY && cents > 0 -> "溢缴 ${Money.format(cents)}"
    account.kind == AccountKind.LIABILITY -> "无欠款 ${Money.format(0)}"
    cents < 0 -> "透支 ${Money.format(-cents)}"
    else -> "余额 ${Money.format(cents)}"
}

/** Balances and totals are supplied by the root's background cache; this page never calculates them. */
@OptIn(ExperimentalLayoutApi::class)
@Composable fun AccountsPage(snapshot: LedgerSnapshot, balances: Map<String, Long>?, totals: AccountTotals?, today: LocalDate,
    busy: Boolean, readError: Boolean, contentPadding: PaddingValues, detailAccountId: String?,
    onDetail: (String?) -> Unit, onCreate: () -> Unit, onEdit: (Account) -> Unit,
    onArchive: (Account, Boolean) -> Unit, onTransfer: () -> Unit, onAdjust: (Account) -> Unit,
    onEntry: (Entry) -> Unit, onEditTransfer: (AccountTransfer) -> Unit,
    onDeleteTransfer: (AccountTransfer) -> Unit, onDeleteAdjustment: (AccountAdjustment) -> Unit,
    onRetryRead: () -> Unit, isOperationBlocked: () -> Boolean,
    listState: LazyListState = rememberLazyListState()) {
    var showArchived by rememberSaveable { mutableStateOf(false) }
    val gate by rememberUpdatedState(isOperationBlocked)
    fun blocked() = busy || readError || gate()
    val accounts = remember(snapshot.accounts, showArchived) {
        snapshot.accounts.filter { showArchived || !it.archived }.sortedWith(compareBy({ it.sortOrder }, { it.id }))
    }
    val detail = snapshot.accounts.firstOrNull { it.id == detailAccountId }
    val entries = remember(snapshot.entries, detailAccountId) {
        if (detailAccountId == null) emptyList() else snapshot.entries.filter { it.accountId == detailAccountId }.sortedWith(compareByDescending<Entry> { it.date }.thenBy { it.id })
    }
    val transfers = remember(snapshot.transfers, detailAccountId) {
        if (detailAccountId == null) emptyList() else snapshot.transfers.filter { it.fromAccountId == detailAccountId || it.toAccountId == detailAccountId }
            .sortedWith(compareByDescending<AccountTransfer> { it.date }.thenBy { it.id })
    }
    val adjustments = remember(snapshot.adjustments, detailAccountId) {
        if (detailAccountId == null) emptyList() else snapshot.adjustments.filter { it.accountId == detailAccountId }
            .sortedWith(compareByDescending<AccountAdjustment> { it.date }.thenBy { it.id })
    }
    val names = remember(snapshot.accounts) { snapshot.accounts.associate { it.id to it.name } }
    val left = LocalHandedness.current == Handedness.LEFT
    LazyColumn(Modifier.fillMaxSize().testTag(if (detailAccountId == null) "accounts-list" else "account-detail-list"),
        state = listState, contentPadding = contentPadding, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("截至今天 ${today.year}年${today.monthValue}月${today.dayOfMonth}日", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (readError) {
                Text("无法读取账户，请重试读取。", color = MaterialTheme.colorScheme.error)
                TextButton(onClick = onRetryRead, enabled = !busy, modifier = Modifier.heightIn(min = 48.dp).testTag("accounts-retry")) { Text("重试读取") }
            }
        }
        if (detailAccountId == null) {
            item {
                LedgerPanel {
                    LedgerSectionTitle("净资产")
                    Text(totals?.netCents?.let(Money::format) ?: "正在计算…", style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary, modifier = Modifier.testTag("accounts-net"))
                    Text("资产 ${totals?.assetCents?.let(Money::format) ?: "—"}", modifier = Modifier.testTag("accounts-assets"))
                    Text("负债 ${totals?.debtCents?.let(Money::format) ?: "—"}", modifier = Modifier.testTag("accounts-debt"))
                    Text("包括已归档账户。正余额计入资产，负余额计入负债；未来记录到期后才计入。", style = MaterialTheme.typography.bodySmall)
                }
            }
            item {
                FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, if (left) Alignment.Start else Alignment.End),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val add: @Composable () -> Unit = { Button(onClick = { if (!blocked()) onCreate() }, enabled = !blocked(),
                        modifier = Modifier.heightIn(min = 48.dp).testTag("account-add")) { Text("新增账户") } }
                    val transfer: @Composable () -> Unit = { OutlinedButton(onClick = { if (!blocked()) onTransfer() }, enabled = !blocked(),
                        modifier = Modifier.heightIn(min = 48.dp).testTag("account-transfer")) { Text("账户转账") } }
                    if (left) { add(); transfer() } else { transfer(); add() }
                }
                FilterChip(selected = showArchived, onClick = { if (!blocked()) showArchived = !showArchived }, enabled = !blocked(),
                    label = { Text("显示已归档账户") }, modifier = Modifier.heightIn(min = 48.dp).testTag("account-show-archived"))
            }
            if (accounts.isEmpty()) item { Text("暂无账户。新增现金、银行或信用卡账户，开始记录余额。") }
            items(accounts, key = { "account-${it.id}" }) { account ->
                Surface(onClick = { if (!blocked()) onDetail(account.id) }, enabled = !blocked(), shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.fillMaxWidth().heightIn(min = 72.dp).testTag("account-row-${account.id}")) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(account.name + if (account.archived) "（已归档）" else "", style = MaterialTheme.typography.titleMedium)
                        Text(subtypeLabel(account.subtype), style = MaterialTheme.typography.bodySmall)
                        Text(balances?.get(account.id)?.let { accountBalanceLabel(account, it) } ?: "正在计算余额…")
                    }
                }
            }
        } else if (detail == null) item {
            Text(if (busy) "正在读取账户…" else if (readError) "账户暂不可读取" else "此账户已不存在")
            TextButton(onClick = { if (!blocked()) onDetail(null) }, enabled = !blocked(), modifier = Modifier.heightIn(min = 48.dp)) { Text("返回账户") }
        } else {
            item {
                TextButton(onClick = { if (!blocked()) onDetail(null) }, enabled = !blocked(),
                    modifier = Modifier.heightIn(min = 48.dp).testTag("account-detail-back")) { Text("返回账户") }
                LedgerPanel {
                    LedgerSectionTitle(detail.name + if (detail.archived) "（已归档）" else "")
                    Text(balances?.get(detail.id)?.let { accountBalanceLabel(detail, it) } ?: "正在计算余额…",
                        style = MaterialTheme.typography.headlineSmall, modifier = Modifier.testTag("account-detail-balance"))
                    Text("${if (detail.kind == AccountKind.ASSET) "资产" else "负债"} · ${subtypeLabel(detail.subtype)}")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(enabled = !blocked(), onClick = { if (!blocked()) onEdit(detail) }, modifier = Modifier.heightIn(min = 48.dp).testTag("account-edit")) { Text("编辑账户") }
                        TextButton(enabled = !blocked() && !detail.archived, onClick = { if (!blocked() && !detail.archived) onAdjust(detail) },
                            modifier = Modifier.heightIn(min = 48.dp).testTag("account-adjust")) { Text("调整余额") }
                        TextButton(enabled = !blocked(), onClick = { if (!blocked()) onArchive(detail, !detail.archived) },
                            modifier = Modifier.heightIn(min = 48.dp).testTag("account-archive")) { Text(if (detail.archived) "取消归档" else "归档账户") }
                    }
                    if (detail.archived) Text("已归档账户仍保留历史记录及余额。取消归档后可新增记录。", style = MaterialTheme.typography.bodySmall)
                }
            }
            item { LedgerSectionTitle("收支记录"); if (entries.isEmpty()) Text("暂无关联收支") }
            items(entries, key = { "entry-${it.id}" }) { entry ->
                Surface(onClick = { if (!blocked()) onEntry(entry) }, enabled = !blocked(), shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("account-entry-${entry.id}")) {
                    Column(Modifier.padding(12.dp)) { Text("${entry.date} ${entry.category}"); Text("${if (entry.kind == EntryKind.INCOME) "收入" else "支出"} ${Money.format(entry.amountCents)}") }
                }
            }
            item { LedgerSectionTitle("账户转账"); if (transfers.isEmpty()) Text("暂无转账") }
            items(transfers, key = { "transfer-${it.id}" }) { transfer ->
                LedgerPanel {
                    Text("${names[transfer.fromAccountId] ?: "原账户"} → ${names[transfer.toAccountId] ?: "原账户"}")
                    Text("${transfer.date} ${Money.format(transfer.amountCents)}")
                    if (transfer.note.isNotEmpty()) Text(transfer.note)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(enabled = !blocked(), onClick = { if (!blocked()) onEditTransfer(transfer) },
                            modifier = Modifier.heightIn(min = 48.dp).testTag("account-transfer-edit-${transfer.id}")) { Text("编辑转账") }
                        TextButton(enabled = !blocked(), onClick = { if (!blocked()) onDeleteTransfer(transfer) },
                            modifier = Modifier.heightIn(min = 48.dp).testTag("account-transfer-delete-${transfer.id}")) { Text("删除转账") }
                    }
                }
            }
            item { LedgerSectionTitle("余额调整"); if (adjustments.isEmpty()) Text("暂无余额调整") }
            items(adjustments, key = { "adjustment-${it.id}" }) { adjustment ->
                LedgerPanel {
                    Text("${adjustment.date} ${if (adjustment.reason == AdjustmentReason.INITIAL) "初始余额" else "手动校准"}")
                    Text("调整 ${Money.format(adjustment.deltaCents)}；目标 ${accountBalanceLabel(detail, adjustment.targetBalanceCents)}")
                    if (adjustment.note.isNotEmpty()) Text(adjustment.note)
                    TextButton(enabled = !blocked(), onClick = { if (!blocked()) onDeleteAdjustment(adjustment) },
                        modifier = Modifier.heightIn(min = 48.dp).testTag("account-adjustment-delete-${adjustment.id}")) { Text("删除余额调整") }
                }
            }
        }
    }
}

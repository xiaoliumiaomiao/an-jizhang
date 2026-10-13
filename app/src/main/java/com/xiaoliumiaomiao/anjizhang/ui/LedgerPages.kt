package com.xiaoliumiaomiao.anjizhang.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.xiaoliumiaomiao.anjizhang.BuildConfig
import com.xiaoliumiaomiao.anjizhang.domain.*
import java.time.YearMonth

@Composable
internal fun DestinationIcon(key: String) {
    LedgerGlyph(key)
}

@Composable
internal fun MonthNavigator(month: YearMonth, onChange: (YearMonth) -> Unit, onPick: () -> Unit) {
    PeriodNavigator(month, { onChange(month.minusMonths(1)) }, { onChange(month.plusMonths(1)) }, onPick, { onChange(YearMonth.now()) })
}
@Composable
internal fun MonthlyOverview(summary: MonthlySummary) {
    LedgerPanel {
        Text("整月概览", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        val balanceLabelStyle = MaterialTheme.typography.bodySmall
        val balanceLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
        val balance = buildAnnotatedString {
            withStyle(SpanStyle(
                fontSize = balanceLabelStyle.fontSize,
                fontWeight = balanceLabelStyle.fontWeight,
                color = balanceLabelColor
            )) { append("本月结余 ") }
            append(Money.format(summary.balanceCents))
        }
        Text(balance, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.fillMaxWidth())
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            if (LocalDensity.current.fontScale >= 1.5f || maxWidth < 280.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("收入 ${Money.format(summary.incomeCents)}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.secondary)
                    Text("支出 ${Money.format(summary.expenseCents)}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.tertiary)
                }
            } else {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("收入 ${Money.format(summary.incomeCents)}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.weight(1f))
                    Text("支出 ${Money.format(summary.expenseCents)}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.tertiary, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
internal fun LedgerPage(month: YearMonth, summary: MonthlySummary, visible: List<Entry>, total: Int, query: String, kind: EntryKind?, busy: Boolean, listState: LazyListState, onMonth: (YearMonth) -> Unit, onQuery: (String) -> Unit, onKind: (EntryKind?) -> Unit, onClear: () -> Unit, onAdd: () -> Unit, onDetail: (Entry) -> Unit, contentPadding: PaddingValues, onPickMonth: () -> Unit) {
    LazyColumn(
        Modifier.fillMaxSize().testTag("ledger-list"),
        state = listState,
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { MonthNavigator(month, onMonth, onPickMonth) }
        item { MonthlyOverview(summary) }
        item { EntryFilters(query, kind, onQuery, onKind, onClear) }
        item { Text("显示 ${visible.size} / 本月 $total 条", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        if (total == 0) item {
            LedgerPanel {
                LedgerGlyph("ledger", color = MaterialTheme.colorScheme.primary)
                Text("这个月还没有账目", style = MaterialTheme.typography.titleMedium)
                Button(onClick = onAdd, enabled = !busy, modifier = Modifier.heightIn(min = 48.dp)) {
                    LedgerGlyph("plus")
                    Spacer(Modifier.width(8.dp))
                    Text("记一笔")
                }
            }
        } else if (visible.isEmpty()) item {
            LedgerPanel { Text("没有符合条件的账目", style = MaterialTheme.typography.titleMedium) }
        }
        EntryGroups.forDisplay(visible).forEach { day ->
            item(key = "day-${day.date}") {
                Row(
                    Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    Text("${day.date} · ${day.entries.size}条", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    HorizontalDivider(Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
            items(day.entries, key = { it.id }) { entry -> EntryRow(entry, busy) { onDetail(entry) } }
        }
    }
}

@Composable
internal fun EntryRow(entry: Entry, busy: Boolean, verticalLayout: Boolean = false, onClick: () -> Unit) {
    val amount = (if (entry.kind == EntryKind.EXPENSE) "-" else "+") + Money.format(entry.amountCents)
    val amountColor = if (entry.kind == EntryKind.EXPENSE) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.secondary
    val textMeasurer = rememberTextMeasurer()
    val amountStyle = MaterialTheme.typography.headlineMedium
    Surface(
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("entry-${entry.id}")
            .clickable(enabled = !busy, onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val naturalAmountWidth = textMeasurer.measure(amount, style = amountStyle, softWrap = false, maxLines = 1).size.width
                val amountWidth = with(LocalDensity.current) { naturalAmountWidth.toDp() }
                // Reserve 112dp for the category, a 40dp marker and two 12dp gaps.
                val inline = !verticalLayout && LocalDensity.current.fontScale < 1.5f &&
                    maxWidth >= 320.dp && amountWidth + 176.dp <= maxWidth
                if (inline) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        KindMarker(entry.kind)
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(entry.category, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text(if (entry.kind == EntryKind.EXPENSE) "支出" else "收入", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(amount, style = amountStyle, color = amountColor, textAlign = TextAlign.End)
                    }
                } else {
                    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            KindMarker(entry.kind)
                            Text(entry.category, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text(if (entry.kind == EntryKind.EXPENSE) "支出" else "收入", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        LedgerAmount(amount, Modifier.fillMaxWidth(), amountColor, TextAlign.End)
                    }
                }
            }
            if (entry.note.isNotEmpty()) {
                Text(entry.note, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** Keep every character; choose the largest heading that fits the actual scaled amount. */
@Composable
internal fun LedgerAmount(value: String, modifier: Modifier = Modifier, color: Color = LocalContentColor.current, textAlign: TextAlign = TextAlign.Start) {
    val textMeasurer = rememberTextMeasurer()
    val styles = listOf(
        MaterialTheme.typography.headlineMedium,
        MaterialTheme.typography.headlineSmall,
        MaterialTheme.typography.titleLarge,
        MaterialTheme.typography.bodyLarge
    )
    BoxWithConstraints(modifier) {
        val style = styles.firstOrNull {
            textMeasurer.measure(value, style = it, softWrap = false, maxLines = 1).size.width <= constraints.maxWidth
        } ?: styles.last()
        Text(value, style = style, color = color, textAlign = textAlign, modifier = Modifier.fillMaxWidth())
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun EntryDetail(entry: Entry, busy: Boolean, onEdit: () -> Unit, onDelete: () -> Unit, contentPadding: PaddingValues, accountName: String) {
    val amountColor = if (entry.kind == EntryKind.EXPENSE) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.secondary
    Column(
        Modifier.fillMaxSize().testTag("entry-detail").verticalScroll(rememberScrollState()).padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        LedgerPanel {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                KindMarker(entry.kind)
                Text(if (entry.kind == EntryKind.EXPENSE) "支出" else "收入", style = MaterialTheme.typography.titleMedium, color = amountColor)
            }
            LedgerAmount((if (entry.kind == EntryKind.EXPENSE) "-" else "+") + Money.format(entry.amountCents), Modifier.fillMaxWidth(), amountColor)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Text("分类：${entry.category}", style = MaterialTheme.typography.bodyLarge)
            Text("日期：${entry.date}", style = MaterialTheme.typography.bodyLarge)
            Text("账户：$accountName", style = MaterialTheme.typography.bodyLarge)
        }
        LedgerPanel {
            Text("备注：${entry.note.ifEmpty { "无" }}", style = MaterialTheme.typography.bodyLarge)
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onEdit, enabled = !busy, modifier = Modifier.heightIn(min = 48.dp)) { Text("编辑") }
            OutlinedButton(
                onClick = onDelete,
                enabled = !busy,
                modifier = Modifier.heightIn(min = 48.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) { Text("删除") }
        }
    }
}

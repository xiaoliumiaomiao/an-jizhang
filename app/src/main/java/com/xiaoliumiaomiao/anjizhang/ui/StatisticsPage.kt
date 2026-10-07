package com.xiaoliumiaomiao.anjizhang.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.xiaoliumiaomiao.anjizhang.domain.*
import java.time.LocalDate
import java.time.YearMonth

@Composable internal fun StatisticsPage(month: YearMonth, annual: Boolean, year: Int, kind: EntryKind,
    result: StatisticsResult?, today: LocalDate, reading: Boolean, readError: Boolean, busy: Boolean,
    listState: LazyListState, onMonth: (YearMonth) -> Unit, onMode: (Boolean) -> Unit, onYear: (Int) -> Unit,
    onKind: (EntryKind) -> Unit, onCategory: (String) -> Unit, onDetail: (Entry) -> Unit, onRetry: () -> Unit) {
    val analysis = result?.analysis
    val ready = !reading && !readError && analysis != null
    // Keep the data list state out of layout while loading; a short placeholder must not clamp its saved position.
    val pendingScroll = rememberLazyListState()
    LazyColumn(Modifier.fillMaxSize().testTag("stats-list"), state = if (ready) listState else pendingScroll,
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item(key = "mode") { StatisticsModes(annual, onMode) }
        item(key = "period") {
            if (annual) YearNavigator(year, onYear) else MonthNavigator(month, onMonth)
        }
        if (!ready) {
            item(key = "status") {
                when {
                    reading -> StatisticsStatus("正在读取账目…")
                    readError -> StatisticsStatus("无法读取账目，请重试", onRetry)
                    result?.failed == true -> StatisticsStatus("无法整理统计，请重试", onRetry)
                    else -> StatisticsStatus("正在整理统计…", loading = true)
                }
            }
        } else {
            val data = requireNotNull(analysis)
            val selected = if (kind == EntryKind.INCOME) data.income else data.expense
            val typeLabel = if (kind == EntryKind.INCOME) "收入" else "支出"
            item(key = "overview-title") { Text(if (annual) "整年概览" else "整月概览", style = MaterialTheme.typography.labelLarge) }
            item(key = "income") { StatisticsLine("stats-overview", "收入 ${Money.format(data.income.totalCents)}", income = true) }
            item(key = "expense") { StatisticsLine("stats-overview", "支出 ${Money.format(data.expense.totalCents)}", expense = true) }
            item(key = "balance") {
                Box(Modifier.testTag("stats-overview")) {
                    Text("${if (annual) "本年" else "本月"}结余 ${Money.format(data.balanceCents)}",
                        style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                }
            }
            item(key = "balance-note") { Text("结余为记录收入减记录支出", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            if (data.hasFutureEntries) item(key = "future") { Text("包含未来日期账目", color = MaterialTheme.colorScheme.primary) }
            item(key = "kind") { StatisticsKinds(kind, onKind) }
            item(key = "comparison-title") { SectionHeading(if (annual) "与上年同期比较" else "与上月同期比较") }
            val comparison = data.comparison
            if (comparison == null) {
                item(key = "comparison-none") {
                    StatisticsLine("stats-comparison", when {
                        data.period.startDate > today -> "所选期间尚未开始，暂无同期对比"
                        annual -> "已到最早年份，暂无上年对比"
                        else -> "已到最早月份，暂无上月对比"
                    })
                }
            } else {
                val numbers = if (kind == EntryKind.INCOME) comparison.income else comparison.expense
                if (data.period.contains(today)) item(key = "comparison-days") { StatisticsLine("stats-comparison", "按相同天数比较") }
                item(key = "comparison-current-range") { StatisticsLine("stats-comparison", "本期：${comparison.currentStart} 至 ${comparison.currentEnd}") }
                item(key = "comparison-previous-range") { StatisticsLine("stats-comparison", "对比：${comparison.previousStart} 至 ${comparison.previousEnd}") }
                item(key = "comparison-current-amount") { StatisticsLine("stats-comparison", "本期$typeLabel ${Money.format(numbers.currentCents)}") }
                item(key = "comparison-previous-amount") { StatisticsLine("stats-comparison", "对比期$typeLabel ${Money.format(numbers.previousCents)}") }
                if (numbers.previousCount == 0 && numbers.currentCount > 0) item(key = "comparison-no-base-delta") {
                    StatisticsLine("stats-comparison", "差额 ${Money.format(numbers.deltaCents)}")
                }
                item(key = "comparison-delta") { StatisticsLine("stats-comparison", comparisonText(numbers)) }
            }
            item(key = "metrics") {
                Column(Modifier.fillMaxWidth().testTag("stats-metrics").semantics(mergeDescendants = true) {}, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("记录 ${selected.entryCount} 笔", style = MaterialTheme.typography.titleMedium)
                    val average = StatisticsNumbers.averageCents(selected.totalCents, selected.entryCount)?.let(Money::format) ?: "—"
                    Text("平均每笔 $average")
                }
            }
            item(key = "category-title") { SectionHeading("${typeLabel}分类") }
            if (selected.categories.isEmpty()) item(key = "category-empty") { Text("${if (annual) "这一年" else "这个月"}还没有$typeLabel") }
            items(selected.categories, key = { "category-${kind.name}-${it.category}" }) { category ->
                Column(Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("stats-category-${kind.name}-${category.category}")
                    .clickable(enabled = !busy) { onCategory(category.category) }.padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Preserve the original exact monthly category Text contract.
                    Text("${category.category}  ${Money.format(category.amountCents)}", style = MaterialTheme.typography.bodyLarge)
                    Text("${StatisticsNumbers.percentage(category.amountCents, selected.totalCents)} · ${category.entryCount}笔",
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
            item(key = "largest-title") { SectionHeading("较大单笔 · 前3笔") }
            if (selected.largestEntries.isEmpty()) item(key = "largest-empty") { Text("暂无较大记录") }
            itemsIndexed(selected.largestEntries, key = { _, entry -> "largest-${entry.id}" }) { index, entry ->
                Column(Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("stats-big-entry-${entry.id}")
                    .clickable(enabled = !busy) { onDetail(entry) }.padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("${index + 1}.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${entry.category.trim()}  ${Money.format(entry.amountCents)}", style = MaterialTheme.typography.titleMedium)
                    Text(entry.date.toString())
                    if (entry.note.isNotEmpty()) Text(entry.note, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium)
                    HorizontalDivider()
                }
            }
            data.annual?.let { yearly ->
                item(key = "annual-title") { SectionHeading("每月收支") }
                item(key = "annual-chart") { AnnualBars(yearly.months) }
                items(yearly.months, key = { "annual-month-${it.month.monthValue}" }) { totals ->
                    Column(Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("stats-month-${totals.month.monthValue}")
                        .clickable { onMonth(totals.month) }.padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("${totals.month.monthValue}月", style = MaterialTheme.typography.titleMedium)
                        Text("收入 ${Money.format(totals.incomeCents)}", color = MaterialTheme.colorScheme.secondary)
                        Text("支出 ${Money.format(totals.expenseCents)}", color = MaterialTheme.colorScheme.error)
                        Text("结余 ${Money.format(totals.balanceCents)}")
                        if (totals.entryCount == 0) Text("无记录", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (totals.month > YearMonth.from(today)) Text("未到", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        HorizontalDivider()
                    }
                }
            }
            item(key = "guide-title") { SectionHeading("如何看这份统计") }
            item(key = "guide-steps") { Text("先看同期变化，再看分类金额与记录笔数，最后打开较大记录结合备注回顾。") }
            item(key = "guide-scope") { Text("仅根据已记录账目，无记录不代表无消费") }
            item(key = "guide-rounding") { Text("分类占比按本类型总额独立四舍五入到1位小数，合计可能不恰好100%；平均每笔四舍五入到分。", style = MaterialTheme.typography.bodySmall) }
        }
    }
}

@Composable private fun SectionHeading(text: String) {
    Column(Modifier.fillMaxWidth().padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        HorizontalDivider(); Text(text, style = MaterialTheme.typography.titleLarge)
    }
}
@Composable private fun StatisticsLine(section: String, text: String, income: Boolean = false, expense: Boolean = false) {
    Box(Modifier.fillMaxWidth().testTag(section)) {
        Text(text, color = when { income -> MaterialTheme.colorScheme.secondary; expense -> MaterialTheme.colorScheme.error; else -> MaterialTheme.colorScheme.onSurface })
    }
}
private fun comparisonText(numbers: TypeComparison): String {
    if (numbers.previousCount == 0) return if (numbers.currentCount == 0) "暂无可比较记录" else "对比期无此类型记录"
    val delta = numbers.deltaCents
    val amount = if (delta < 0) "-" + Money.format(Math.negateExact(delta)) else Money.format(delta)
    return "差额 $amount（${StatisticsNumbers.percentage(delta, numbers.previousCents)}）"
}
@OptIn(ExperimentalLayoutApi::class)
@Composable private fun StatisticsModes(annual: Boolean, onMode: (Boolean) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        FilterChip(!annual, { onMode(false) }, label = { Text("月度") }, modifier = Modifier.heightIn(min = 48.dp).testTag("stats-mode-month"))
        FilterChip(annual, { onMode(true) }, label = { Text("年度") }, modifier = Modifier.heightIn(min = 48.dp).testTag("stats-mode-year"))
    }
}
@OptIn(ExperimentalLayoutApi::class)
@Composable private fun StatisticsKinds(kind: EntryKind, onKind: (EntryKind) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        listOf(EntryKind.EXPENSE to "支出", EntryKind.INCOME to "收入").forEach { (value, label) ->
            FilterChip(kind == value, { onKind(value) }, label = { Text(label) },
                modifier = Modifier.heightIn(min = 48.dp).testTag("stats-kind-${value.name.lowercase(java.util.Locale.ROOT)}"))
        }
    }
}
@OptIn(ExperimentalLayoutApi::class)
@Composable private fun YearNavigator(year: Int, onYear: (Int) -> Unit) {
    var choosing by rememberSaveable { mutableStateOf(false) }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        TextButton({ onYear(year - 1) }, enabled = year > 1900, modifier = Modifier.heightIn(min = 48.dp).testTag("stats-year-prev")) { Text("上年") }
        TextButton({ choosing = true }, modifier = Modifier.heightIn(min = 48.dp).testTag("stats-year-select")) { Text("${year}年", style = MaterialTheme.typography.titleLarge) }
        TextButton({ onYear(year + 1) }, enabled = year < 9999, modifier = Modifier.heightIn(min = 48.dp).testTag("stats-year-next")) { Text("下年") }
        TextButton({ onYear(LocalDate.now().year) }, modifier = Modifier.heightIn(min = 48.dp).testTag("stats-year-current")) { Text("本年") }
    }
    if (choosing) {
        var input by rememberSaveable { mutableStateOf(year.toString()) }
        var invalid by rememberSaveable { mutableStateOf(false) }
        AlertDialog(onDismissRequest = { choosing = false }, title = { Text("选择年份") }, text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(input, { input = it }, label = { Text("年份（1900–9999）") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth().testTag("stats-year-input"))
                if (invalid) Text("年份请输入1900至9999", color = MaterialTheme.colorScheme.error)
            }
        }, confirmButton = { TextButton({
            val parsed = input.toIntOrNull()
            if (parsed == null || parsed !in 1900..9999) invalid = true else { onYear(parsed); choosing = false }
        }) { Text("确定") } }, dismissButton = { TextButton({ choosing = false }) { Text("取消") } })
    }
}

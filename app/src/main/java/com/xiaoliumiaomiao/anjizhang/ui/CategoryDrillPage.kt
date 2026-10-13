package com.xiaoliumiaomiao.anjizhang.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.xiaoliumiaomiao.anjizhang.domain.Entry
import com.xiaoliumiaomiao.anjizhang.domain.EntryKind

@Composable internal fun CategoryDrillPage(key: CategoryKey, result: DrillResult?, reading: Boolean, readError: Boolean,
    busy: Boolean, listState: LazyListState, onRetry: () -> Unit, onDetail: (Entry) -> Unit, contentPadding: PaddingValues) {
    when {
        reading -> StatisticsStatus("正在读取账目…")
        readError -> StatisticsStatus("无法读取账目，请重试", onRetry)
        result == null -> StatisticsStatus("正在整理分类记录…", loading = true)
        result.failed -> StatisticsStatus("无法整理统计，请重试", onRetry)
        else -> LazyColumn(Modifier.fillMaxSize().testTag("stats-drill-list"), state = listState,
            contentPadding = contentPadding, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            val entries = requireNotNull(result.entries)
            item(key = "category") {
                LedgerPanel {
                    LedgerSectionTitle(key.category)
                    Text("${key.period.startDate} 至 ${key.period.endDate} · ${if (key.kind == EntryKind.INCOME) "收入" else "支出"}",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("共 ${entries.size} 条记录", style = MaterialTheme.typography.titleMedium)
                }
            }
            if (entries.isEmpty()) item(key = "empty") { Text("此分类在所选期间没有记录") }
            items(entries, key = { it.id }) { entry ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(entry.date.toString(), style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.testTag("stats-drill-date-${entry.id}"))
                    EntryRow(entry, busy, verticalLayout = true) { onDetail(entry) }
                }
            }
        }
    }
}

@Composable internal fun StatisticsStatus(message: String, onRetry: (() -> Unit)? = null, loading: Boolean = false) {
    Column(Modifier.fillMaxWidth().padding(16.dp).testTag(if (loading) "stats-loading" else if (onRetry != null) "stats-error" else "stats-reading"),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
        Text(message)
        if (onRetry != null) TextButton(onClick = onRetry, modifier = Modifier.heightIn(min = 48.dp)) { Text("重试读取") }
    }
}

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
import com.xiaoliumiaomiao.anjizhang.BuildConfig
import com.xiaoliumiaomiao.anjizhang.domain.*

enum class SettingsDestination { ROOT, APPEARANCE, CATEGORIES, BACKUP, PRIVACY, ABOUT }

private fun themeLabel(mode: ThemeMode) = when (mode) { ThemeMode.SYSTEM -> "跟随系统"; ThemeMode.LIGHT -> "浅色"; ThemeMode.DARK -> "深色" }

/** Destination, persistence, SAF and category-operation sessions remain root-owned. */
@OptIn(ExperimentalLayoutApi::class)
@Composable fun SettingsPages(destination: SettingsDestination, preferences: UiPreferences, categories: List<Category>,
    busy: Boolean, preferenceBusy: Boolean, preferenceError: String?, preferenceMessage: String?,
    contentPadding: PaddingValues, onDestination: (SettingsDestination) -> Unit,
    onTheme: (ThemeMode) -> Unit, onHand: (Handedness) -> Unit,
    onAddCategory: (EntryKind, Int) -> Unit, onRenameCategory: (Category) -> Unit,
    onArchiveCategory: (Category, Boolean) -> Unit, onReorderCategories: (EntryKind, List<String>) -> Unit,
    onExport: () -> Unit, onImport: () -> Unit, onLicenses: () -> Unit, isOperationBlocked: () -> Boolean,
    listState: LazyListState = rememberLazyListState()) {
    var kind by rememberSaveable { mutableStateOf(EntryKind.EXPENSE) }
    var query by rememberSaveable { mutableStateOf("") }
    var showArchived by rememberSaveable { mutableStateOf(false) }
    val gate by rememberUpdatedState(isOperationBlocked)
    fun blocked() = busy || preferenceBusy || gate()
    val sorted = remember(categories, kind) { categories.filter { it.kind == kind }.sortedWith(compareBy({ it.sortOrder }, { it.id })) }
    val active = remember(sorted) { sorted.filterNot { it.archived } }
    val activeIndices = remember(active) { active.withIndex().associate { it.value.id to it.index } }
    val visible = remember(sorted, showArchived, query) {
        sorted.filter { (showArchived || !it.archived) && (query.isEmpty() || it.name.contains(query, ignoreCase = true)) }
    }
    fun move(category: Category, direction: Int) {
        if (blocked()) return
        val ids = active.map { it.id }.toMutableList()
        val index = ids.indexOf(category.id)
        val target = index + direction
        if (index < 0 || target !in ids.indices) return
        val swapped = ids[target]; ids[target] = ids[index]; ids[index] = swapped
        // Submit every active ID of this kind, even when the visible list is searched.
        onReorderCategories(kind, ids)
    }
    val left = LocalHandedness.current == Handedness.LEFT
    LazyColumn(Modifier.fillMaxSize().testTag(if (destination == SettingsDestination.ROOT) "settings-list" else "settings-${destination.name.lowercase()}-list"),
        state = listState, contentPadding = contentPadding, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (destination != SettingsDestination.ROOT) item {
            TextButton(enabled = !blocked(), onClick = { if (!blocked()) onDestination(SettingsDestination.ROOT) },
                modifier = Modifier.heightIn(min = 48.dp).testTag("settings-child-back")) { Text("返回设置") }
        }
        when (destination) {
            SettingsDestination.ROOT -> {
                val rows = listOf(SettingsDestination.APPEARANCE to "外观与操作", SettingsDestination.CATEGORIES to "收支类别",
                    SettingsDestination.BACKUP to "备份与恢复", SettingsDestination.PRIVACY to "本地隐私", SettingsDestination.ABOUT to "关于")
                items(rows, key = { it.first.name }) { (target, label) ->
                    Surface(onClick = { if (!blocked()) onDestination(target) }, enabled = !blocked(),
                        shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp).testTag("settings-open-${target.name.lowercase()}")) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Column(Modifier.weight(1f)) {
                                Text(label, style = MaterialTheme.typography.titleMedium)
                                if (target == SettingsDestination.APPEARANCE) Text("${themeLabel(preferences.theme)} · ${if (preferences.hand == Handedness.RIGHT) "右手" else "左手"}",
                                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            LedgerGlyph("chevron")
                        }
                    }
                }
            }
            SettingsDestination.APPEARANCE -> {
                item {
                    LedgerPanel {
                        LedgerSectionTitle("主题")
                        ThemeMode.entries.forEach { value ->
                            FilterChip(selected = preferences.theme == value, enabled = !blocked(),
                                onClick = { if (!blocked()) onTheme(value) }, label = { Text(themeLabel(value)) },
                                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("theme-${value.name.lowercase()}"))
                        }
                    }
                }
                item {
                    LedgerPanel {
                        LedgerSectionTitle("惯用手")
                        Handedness.entries.forEach { value ->
                            FilterChip(selected = preferences.hand == value, enabled = !blocked(),
                                onClick = { if (!blocked()) onHand(value) }, label = { Text(if (value == Handedness.RIGHT) "右手" else "左手") },
                                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("hand-${value.name.lowercase()}"))
                        }
                        Text("移动记账、保存和关闭等主要操作的位置；中文、数字键盘和日期顺序保持不变。", style = MaterialTheme.typography.bodySmall)
                    }
                }
                preferenceError?.let { item { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.testTag("preference-error")) } }
                preferenceMessage?.let { item { Text(it, color = MaterialTheme.colorScheme.primary, modifier = Modifier.testTag("preference-saved")) } }
                if (preferenceBusy) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            }
            SettingsDestination.CATEGORIES -> {
                item {
                    LedgerSectionTitle("收支类别")
                    Text("已有账目保留当时的名称。重命名、归档和排序只改变类别目录；归档类别不再提供给新选择。", style = MaterialTheme.typography.bodySmall)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        EntryKind.entries.forEach { value -> FilterChip(selected = kind == value, enabled = !blocked(),
                            onClick = { if (!blocked()) kind = value }, label = { Text(if (value == EntryKind.INCOME) "收入" else "支出") },
                            modifier = Modifier.heightIn(min = 48.dp).testTag("categories-kind-${value.name.lowercase()}")) }
                    }
                    OutlinedTextField(query, { if (!blocked()) query = it }, enabled = !blocked(), singleLine = true,
                        label = { Text("搜索类别") }, modifier = Modifier.fillMaxWidth().testTag("settings-category-search"))
                    FilterChip(selected = showArchived, enabled = !blocked(), onClick = { if (!blocked()) showArchived = !showArchived },
                        label = { Text("显示已归档类别") }, modifier = Modifier.heightIn(min = 48.dp).testTag("settings-category-show-archived"))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (left) Arrangement.Start else Arrangement.End) {
                        Button(enabled = !blocked(), onClick = {
                            if (!blocked()) onAddCategory(kind, ((sorted.maxOfOrNull { it.sortOrder } ?: -1).toLong() + 1).coerceAtMost(Int.MAX_VALUE.toLong()).toInt())
                        }, modifier = Modifier.heightIn(min = 48.dp).testTag("settings-category-add")) { Text("新增类别") }
                    }
                }
                if (visible.isEmpty()) item { Text("没有匹配的类别") }
                items(visible, key = { it.id }) { category ->
                    LedgerPanel {
                        Text(category.name + if (category.archived) "（已归档）" else "", style = MaterialTheme.typography.titleMedium)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            TextButton(enabled = !blocked(), onClick = { if (!blocked()) onRenameCategory(category) },
                                modifier = Modifier.heightIn(min = 48.dp).testTag("category-rename-${category.id}")) { Text("重命名") }
                            TextButton(enabled = !blocked(), onClick = { if (!blocked()) onArchiveCategory(category, !category.archived) },
                                modifier = Modifier.heightIn(min = 48.dp).testTag("category-archive-${category.id}")) { Text(if (category.archived) "取消归档" else "归档") }
                            if (!category.archived) {
                                val index = activeIndices[category.id] ?: -1
                                TextButton(enabled = !blocked() && index > 0, onClick = { move(category, -1) },
                                    modifier = Modifier.heightIn(min = 48.dp).testTag("category-up-${category.id}")) { Text("上移") }
                                TextButton(enabled = !blocked() && index >= 0 && index < active.lastIndex, onClick = { move(category, 1) },
                                    modifier = Modifier.heightIn(min = 48.dp).testTag("category-down-${category.id}")) { Text("下移") }
                            }
                        }
                    }
                }
            }
            SettingsDestination.BACKUP -> {
                item {
                    LedgerPanel {
                        LedgerSectionTitle("备份与恢复")
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(enabled = !blocked(), onClick = { if (!blocked()) onExport() }, modifier = Modifier.heightIn(min = 48.dp).testTag("backup-export")) { Text("导出备份") }
                            OutlinedButton(enabled = !blocked(), onClick = { if (!blocked()) onImport() }, modifier = Modifier.heightIn(min = 48.dp).testTag("backup-import")) { Text("恢复备份") }
                        }
                        Text("备份包含明文账目、账户和备注，请保存在可信位置。恢复会合并数据，相同记录跳过；任何内容冲突时整批拒绝。")
                    }
                }
                item { Text("当前完整备份（格式2）最多32 MiB；账目、转账和余额调整各最多50,000条，账户最多1,000个，类别最多100,000个。", style = MaterialTheme.typography.bodySmall) }
                item { Text("兼容旧备份（格式1）：最多50,000条账目且不超过10 MiB。旧账未分配账户，不会补造旧余额。目前不支持分批导出。", style = MaterialTheme.typography.bodySmall) }
                item { Text("备注等内容也占容量；接近上限时请先保存备份。系统文件选择器的提供者可能包含云盘，请自行选择可信位置，并在另一可信位置保留第二份备份。", style = MaterialTheme.typography.bodySmall) }
            }
            SettingsDestination.PRIVACY -> {
                item { LedgerPanel { LedgerSectionTitle("本地隐私"); Text("账目仅保存在此设备，无需登录，不联网，无广告或分析服务。系统云备份已关闭。") } }
                item { LedgerPanel { LedgerSectionTitle("卸载提醒"); Text("卸载应用会删除本地账目；更换设备或卸载前，请先导出有效备份。") } }
            }
            SettingsDestination.ABOUT -> {
                item {
                    LedgerPanel {
                        LedgerSectionTitle("安记账")
                        Text("版本 ${BuildConfig.VERSION_NAME}")
                        TextButton(enabled = !blocked(), onClick = { if (!blocked()) onLicenses() },
                            modifier = Modifier.heightIn(min = 48.dp).testTag("settings-licenses")) { Text("第三方许可") }
                    }
                }
            }
        }
    }
}

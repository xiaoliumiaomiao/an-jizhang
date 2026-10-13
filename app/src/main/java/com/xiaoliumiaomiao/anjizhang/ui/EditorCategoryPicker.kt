package com.xiaoliumiaomiao.anjizhang.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.xiaoliumiaomiao.anjizhang.domain.*

@OptIn(ExperimentalLayoutApi::class)
@Composable fun EditorCategoryPicker(categories: List<Category>, kind: EntryKind, selected: String,
    enabled: Boolean, onSelect: (Category) -> Unit, onAll: () -> Unit, onAdd: (() -> Unit)?) {
    val active = remember(categories, kind) {
        categories.filter { it.kind == kind && !it.archived }.sortedWith(compareBy({ it.sortOrder }, { it.id }))
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("类别", style = MaterialTheme.typography.titleSmall)
        if (selected.isNotEmpty()) Text(
            if (active.none { it.name == selected }) "原分类（保留）：$selected" else "当前选择：$selected",
            modifier = Modifier.testTag("editor-category-current"), style = MaterialTheme.typography.bodyMedium)
        if (active.isEmpty()) Text("暂无活动类别，请新增类别。", style = MaterialTheme.typography.bodySmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            active.take(12).forEach { category ->
                FilterChip(selected = category.name == selected, enabled = enabled,
                    onClick = { if (enabled) onSelect(category) }, label = { Text(category.name) },
                    modifier = Modifier.heightIn(min = 48.dp).testTag("editor-category-${category.id}"))
            }
            SuggestionChip(enabled = enabled && onAdd != null, onClick = { if (enabled) onAdd?.invoke() },
                label = { Text("新增类别") }, modifier = Modifier.heightIn(min = 48.dp).testTag("editor-category-add"))
        }
        TextButton(enabled = enabled, onClick = { if (enabled) onAll() },
            modifier = Modifier.heightIn(min = 48.dp).testTag("editor-category-all")) { Text("全部类别") }
    }
}

@Composable internal fun CategorySelectionDialog(categories: List<Category>, kind: EntryKind, selected: String,
    onSelect: (Category) -> Unit, onDismiss: () -> Unit, blocked: () -> Boolean) {
    var query by rememberSaveable { mutableStateOf("") }
    val active = remember(categories, kind) {
        categories.filter { it.kind == kind && !it.archived }.sortedWith(compareBy({ it.sortOrder }, { it.id }))
    }
    val visible = remember(active, query) { if (query.isEmpty()) active else active.filter { it.name.contains(query, ignoreCase = true) } }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = active.indexOfFirst { it.name == selected }.coerceAtLeast(0))
    AlertDialog(onDismissRequest = { if (!blocked()) onDismiss() }, title = { Text("选择类别") }, text = {
        SyncDialogWindowAppearance(DialogWindowAppearance.FLOATING)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (selected.isNotEmpty()) Text("当前选择：$selected", style = MaterialTheme.typography.bodySmall)
            OutlinedTextField(query, { if (!blocked()) query = it }, enabled = !blocked(), singleLine = true,
                label = { Text("搜索类别") }, modifier = Modifier.fillMaxWidth().testTag("editor-category-search"))
            LazyColumn(Modifier.fillMaxWidth().heightIn(max = 320.dp).testTag("editor-category-list"), state = listState) {
                if (visible.isEmpty()) item { Text("没有匹配的活动类别", modifier = Modifier.padding(12.dp)) }
                items(visible, key = { it.id }) { category ->
                    TextButton(onClick = { if (!blocked()) { onSelect(category); onDismiss() } }, enabled = !blocked(),
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("editor-category-row-${category.id}")) {
                        Text(if (category.name == selected) "✓ ${category.name}" else category.name)
                    }
                }
            }
        }
    }, confirmButton = { TextButton(onClick = { if (!blocked()) onDismiss() }, enabled = !blocked(),
        modifier = Modifier.heightIn(min = 48.dp)) { Text("返回") } })
}

@Composable internal fun EditorAccountSelectionDialog(accounts: List<Account>, selectedId: String?, originalId: String?,
    onSelect: (String?) -> Unit, onDismiss: () -> Unit, blocked: () -> Boolean) {
    val available = remember(accounts, originalId) {
        accounts.filter { !it.archived || it.id == originalId }.sortedWith(compareBy({ it.sortOrder }, { it.id }))
    }
    AlertDialog(onDismissRequest = { if (!blocked()) onDismiss() }, title = { Text("选择账户") }, text = {
        SyncDialogWindowAppearance(DialogWindowAppearance.FLOATING)
        LazyColumn(Modifier.heightIn(max = 360.dp).fillMaxWidth().testTag("editor-account-list")) {
            item { TextButton(onClick = { if (!blocked()) { onSelect(null); onDismiss() } }, enabled = !blocked(),
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("editor-account-unassigned")) {
                Text(if (selectedId == null) "✓ 未分配账户" else "未分配账户")
            } }
            items(available, key = { it.id }) { account ->
                TextButton(onClick = { if (!blocked()) { onSelect(account.id); onDismiss() } }, enabled = !blocked(),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("editor-account-${account.id}")) {
                    Text("${if (selectedId == account.id) "✓ " else ""}${account.name}${if (account.archived) "（已归档，原账户）" else ""}")
                }
            }
            if (selectedId != null && available.none { it.id == selectedId }) item { Text("原账户暂不可读取，保留原关联。") }
        }
    }, confirmButton = { TextButton(onClick = { if (!blocked()) onDismiss() }, enabled = !blocked(),
        modifier = Modifier.heightIn(min = 48.dp)) { Text("返回") } })
}

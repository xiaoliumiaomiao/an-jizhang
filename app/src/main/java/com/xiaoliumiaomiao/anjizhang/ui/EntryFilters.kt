package com.xiaoliumiaomiao.anjizhang.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.xiaoliumiaomiao.anjizhang.domain.EntryKind

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EntryFilters(query: String, kind: EntryKind?, onQueryChange: (String) -> Unit,
                 onKindChange: (EntryKind?) -> Unit, onClear: () -> Unit) {
    val focus = LocalFocusManager.current
    LedgerPanel {
        OutlinedTextField(value = query, onValueChange = onQueryChange,
            label = { Text("搜索分类或备注") }, singleLine = true,
            leadingIcon = { LedgerGlyph("search") },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focus.clearFocus() }),
            modifier = Modifier.fillMaxWidth())
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(null to "全部类型", EntryKind.INCOME to "仅收入", EntryKind.EXPENSE to "仅支出").forEach { (value, label) ->
                FilterChip(selected = kind == value, onClick = { onKindChange(value) },
                    label = { Text(label) }, modifier = Modifier.heightIn(min = 48.dp))
            }
            TextButton(onClick = { focus.clearFocus(); onClear() }, enabled = query.isNotEmpty() || kind != null) {
                Text("清除筛选")
            }
        }
        Text("筛选仅影响列表，统计仍为全月", style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

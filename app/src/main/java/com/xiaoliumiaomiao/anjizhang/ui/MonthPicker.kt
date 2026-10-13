package com.xiaoliumiaomiao.anjizhang.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.time.YearMonth

@Composable internal fun MonthPicker(month: YearMonth, onDismiss: () -> Unit, onConfirm: (YearMonth) -> Unit, blocked: () -> Boolean = { false }) {
    var year by rememberSaveable { mutableStateOf(month.year.toString()) }
    var selectedMonth by rememberSaveable { mutableIntStateOf(month.monthValue) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    AlertDialog(onDismissRequest = { if (!blocked()) onDismiss() }, title = { Text("选择月份") }, text = {
        SyncDialogWindowAppearance(DialogWindowAppearance.FLOATING)
        Column(Modifier.fillMaxWidth().heightIn(max = 360.dp).verticalScroll(rememberScrollState()).testTag("month-picker"),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(year, { if (!blocked()) year = it }, enabled = !blocked(), label = { Text("年份（1900–9999）") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
            (1..12).chunked(3).forEach { months ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    months.forEach { value ->
                        FilterChip(selected = selectedMonth == value, enabled = !blocked(), onClick = { if (!blocked()) selectedMonth = value },
                            label = { Text("${value}月") }, modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("month-$value"))
                    }
                }
            }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }, confirmButton = {
        TextButton(enabled = !blocked(), onClick = {
            if (blocked()) return@TextButton
            val parsed = year.toIntOrNull()
            if (parsed == null || parsed !in 1900..9999) error = "年份请输入1900至9999"
            else onConfirm(YearMonth.of(parsed, selectedMonth))
        }) { Text("确定") }
    }, dismissButton = { TextButton(enabled = !blocked(), onClick = { if (!blocked()) onDismiss() }) { Text("取消") } })
}

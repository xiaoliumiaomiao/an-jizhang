package com.xiaoliumiaomiao.anjizhang.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

@Composable
fun AmountKeypad(enabled: Boolean, onKey: (Char) -> Unit, onBackspace: () -> Unit, onClear: () -> Unit) {
    Column(Modifier.fillMaxWidth().testTag("amount-keypad"), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("123", "456", "789").forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { key ->
                    Button(onClick = { onKey(key) }, enabled = enabled,
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("amount-key-$key")) { Text(key.toString()) }
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { onKey('.') }, enabled = enabled,
                modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("amount-key-dot")
                    .semantics { contentDescription = "小数点" }) { Text(".") }
            Button(onClick = { onKey('0') }, enabled = enabled,
                modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("amount-key-0")) { Text("0") }
            Button(onClick = onBackspace, enabled = enabled,
                modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("amount-key-backspace")) { Text("退格") }
        }
        Button(onClick = onClear, enabled = enabled,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("amount-key-clear")) { Text("清空金额") }
    }
}

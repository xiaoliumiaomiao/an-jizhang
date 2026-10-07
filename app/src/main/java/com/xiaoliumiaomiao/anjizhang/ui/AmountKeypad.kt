package com.xiaoliumiaomiao.anjizhang.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.TextButton
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AmountKeypad(enabled: Boolean, onKey: (Char) -> Unit, onBackspace: () -> Unit, onClear: () -> Unit, onClose: () -> Unit) {
    Column(Modifier.fillMaxWidth().testTag("amount-keypad"), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            TextButton(onClick = onClear, enabled = enabled,
                modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("amount-key-clear")) { Text("清空金额") }
            TextButton(onClick = onClose, enabled = enabled,
                modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("amount-keypad-close")) { Text("收起金额键盘") }
        }
        listOf("123", "456", "789").forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                row.forEach { key ->
                    FilledTonalButton(onClick = { onKey(key) }, enabled = enabled, shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("amount-key-$key")) { Text(key.toString(), fontSize = 22.sp, lineHeight = 28.sp) }
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            FilledTonalButton(onClick = { onKey('.') }, enabled = enabled, shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("amount-key-dot")
                    .semantics { contentDescription = "小数点" }) { Text(".", fontSize = 22.sp, lineHeight = 28.sp) }
            FilledTonalButton(onClick = { onKey('0') }, enabled = enabled, shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("amount-key-0")) { Text("0", fontSize = 22.sp, lineHeight = 28.sp) }
            FilledTonalButton(onClick = onBackspace, enabled = enabled, shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("amount-key-backspace")) { Text("退格") }
        }
    }
}

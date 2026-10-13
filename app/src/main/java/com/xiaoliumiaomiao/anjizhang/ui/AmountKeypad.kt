package com.xiaoliumiaomiao.anjizhang.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AmountKeypad(enabled: Boolean, onKey: (Char) -> Unit, onBackspace: () -> Unit, onClear: () -> Unit, onClose: () -> Unit) {
    val keyColors = ButtonDefaults.filledTonalButtonColors(
        containerColor = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.onSurface)
    val keyShape = RoundedCornerShape(14.dp)
    Column(Modifier.fillMaxWidth().testTag("amount-keypad"), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            val close: @Composable RowScope.() -> Unit = {
                FilledTonalButton(onClick = onClose, enabled = enabled, shape = keyShape,
                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer),
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("amount-keypad-close")) { Text("收起金额键盘") }
            }
            if (LocalHandedness.current == Handedness.LEFT) close()
            TextButton(onClick = onClear, enabled = enabled,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
                modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("amount-key-clear")) { Text("清空金额") }
            if (LocalHandedness.current == Handedness.RIGHT) close()
        }
        listOf("123", "456", "789").forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                row.forEach { key ->
                    FilledTonalButton(onClick = { onKey(key) }, enabled = enabled, shape = keyShape, colors = keyColors,
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("amount-key-$key")) {
                        Text(key.toString(), fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            FilledTonalButton(onClick = { onKey('.') }, enabled = enabled, shape = keyShape, colors = keyColors,
                modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("amount-key-dot")
                    .semantics { contentDescription = "小数点" }) {
                Text(".", fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.Medium)
            }
            FilledTonalButton(onClick = { onKey('0') }, enabled = enabled, shape = keyShape, colors = keyColors,
                modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("amount-key-0")) {
                Text("0", fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.Medium)
            }
            FilledTonalButton(onClick = onBackspace, enabled = enabled, shape = keyShape,
                colors = ButtonDefaults.filledTonalButtonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer),
                modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("amount-key-backspace")) { Text("退格") }
        }
    }
}

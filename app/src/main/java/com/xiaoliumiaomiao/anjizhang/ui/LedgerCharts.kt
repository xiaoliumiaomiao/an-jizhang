package com.xiaoliumiaomiao.anjizhang.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.xiaoliumiaomiao.anjizhang.domain.MonthTotals

@Composable internal fun AnnualBars(months: List<MonthTotals>) {
    val income = MaterialTheme.colorScheme.secondary
    val expense = MaterialTheme.colorScheme.error
    val baseline = MaterialTheme.colorScheme.outlineVariant
    val maximum = months.maxOf { maxOf(it.incomeCents, it.expenseCents) }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("收入与支出 · 1月至12月", style = MaterialTheme.typography.titleMedium)
        Canvas(Modifier.fillMaxWidth().height(150.dp).testTag("stats-annual-chart").semantics {
            contentDescription = "收入和支出年度双柱图，从1月到12月；各月准确金额见下方文字"
        }) {
            val step = size.width / 12f
            val width = step * .3f
            drawLine(baseline, Offset(0f, size.height), Offset(size.width, size.height), 1.dp.toPx())
            if (maximum > 0) months.forEachIndexed { index, month ->
                // Floating point is confined to visual proportions, never stored or used as money.
                val incomeHeight = (month.incomeCents.toDouble() / maximum.toDouble() * size.height).toFloat()
                val expenseHeight = (month.expenseCents.toDouble() / maximum.toDouble() * size.height).toFloat()
                drawRect(income, Offset(step * index + step * .15f, size.height - incomeHeight), Size(width, incomeHeight))
                drawRect(expense, Offset(step * index + step * .55f, size.height - expenseHeight), Size(width, expenseHeight))
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            Text("收入", color = income); Text("支出", color = expense)
        }
    }
}

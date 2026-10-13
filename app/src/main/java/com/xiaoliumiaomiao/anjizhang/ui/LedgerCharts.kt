package com.xiaoliumiaomiao.anjizhang.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.xiaoliumiaomiao.anjizhang.domain.CategoryStatistics
import com.xiaoliumiaomiao.anjizhang.domain.EntryKind
import com.xiaoliumiaomiao.anjizhang.domain.Money
import com.xiaoliumiaomiao.anjizhang.domain.MonthTotals

@OptIn(ExperimentalLayoutApi::class)
@Composable internal fun AnnualBars(months: List<MonthTotals>) {
    val income = MaterialTheme.colorScheme.secondary
    val expense = MaterialTheme.colorScheme.tertiary
    val grid = MaterialTheme.colorScheme.outlineVariant
    val maximum = months.maxOfOrNull { maxOf(it.incomeCents, it.expenseCents) } ?: 0L
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("收入与支出 · 1月至12月", style = MaterialTheme.typography.titleMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("左柱 · 收入", color = income, style = MaterialTheme.typography.bodySmall)
            Text("右柱 · 支出", color = expense, style = MaterialTheme.typography.bodySmall)
        }
        Text("纵轴 ¥0.00 — ${Money.format(maximum)}", style = MaterialTheme.typography.bodySmall)
        Canvas(Modifier.fillMaxWidth().height(156.dp).testTag("stats-annual-chart").semantics {
            contentDescription = "收入和支出年度双柱图，从1月到12月；各月准确金额见下方文字"
        }) {
            val margin = 3.dp.toPx()
            val plotHeight = (size.height - margin * 2).coerceAtLeast(0f)
            val bottom = margin + plotHeight
            val step = size.width / 12f
            val width = step * .28f
            (0..4).forEach { index ->
                val y = margin + plotHeight * index / 4f
                drawLine(grid, Offset(0f, y), Offset(size.width, y), 1.dp.toPx())
            }
            if (maximum > 0L) months.forEach { month ->
                val index = month.month.monthValue - 1
                // Coordinates alone use fractions; all displayed amounts use exact cents.
                val incomeHeight = plotHeight * chartAmountFraction(month.incomeCents, maximum)
                val expenseHeight = plotHeight * chartAmountFraction(month.expenseCents, maximum)
                if (incomeHeight > 0f) drawRoundRect(income,
                    Offset(step * index + step * .16f, bottom - incomeHeight), Size(width, incomeHeight),
                    cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx()))
                if (expenseHeight > 0f) drawRect(expense,
                    Offset(step * index + step * .56f, bottom - expenseHeight), Size(width, expenseHeight))
            }
        }
        // Match sparse numeric anchors to the twelve bar groups at narrow and font2 sizes.
        Row(Modifier.fillMaxWidth()) {
            (1..12).forEach { month ->
                if (month in listOf(1, 4, 8, 12)) Text(month.toString(),
                    style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f).testTag("stats-chart-month-$month"))
                else Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable internal fun CategoryShareBar(category: CategoryStatistics, totalCents: Long,
    kind: EntryKind, percentage: String) {
    val fraction = chartAmountFraction(category.amountCents, totalCents)
    val type = if (kind == EntryKind.INCOME) "收入" else "支出"
    val fill = if (kind == EntryKind.INCOME) MaterialTheme.colorScheme.secondary
        else MaterialTheme.colorScheme.tertiary
    val track = MaterialTheme.colorScheme.outlineVariant
    Canvas(Modifier.fillMaxWidth().height(8.dp)
        .testTag("stats-category-bar-${kind.name}-${category.category}").semantics {
            contentDescription = "${category.category}占本期${type}总额"
            stateDescription = percentage
            progressBarRangeInfo = ProgressBarRangeInfo(fraction, 0f..1f)
        }) {
        val corners = CornerRadius(size.height / 2f, size.height / 2f)
        drawRoundRect(track, cornerRadius = corners)
        if (fraction > 0f) drawRoundRect(fill, size = Size(size.width * fraction, size.height),
            cornerRadius = corners)
    }
}

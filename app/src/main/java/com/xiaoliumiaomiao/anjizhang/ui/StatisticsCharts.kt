package com.xiaoliumiaomiao.anjizhang.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.xiaoliumiaomiao.anjizhang.domain.*
import java.time.YearMonth

// Floating point is only a bounded drawing coordinate. Amounts remain exact integer cents.
internal fun chartAmountFraction(amountCents: Long, maximumCents: Long): Float =
    if (amountCents <= 0L || maximumCents <= 0L) 0f
    else (amountCents.toDouble() / maximumCents.toDouble()).toFloat().coerceIn(0f, 1f)

@Composable internal fun PeriodTrendChart(
    series: ChartSeries,
    modifier: Modifier = Modifier,
    onMonth: ((YearMonth) -> Unit)? = null,
) {
    // Restored values do not validate inputs; give each period its own saved-state location.
    var showData by key(series.period) {
        rememberSaveable { mutableStateOf(false) }
    }
    Column(modifier.fillMaxWidth().testTag("stats-period-trend"),
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (series.period.month == null) {
            AnnualBars(series.points.map {
                MonthTotals(YearMonth.from(it.date), it.incomeCents, it.expenseCents, it.entryCount)
            })
            if (series.entryCount == 0) Text("这一年还没有记录，暂无趋势",
                style = MaterialTheme.typography.bodyMedium)
        } else DailyTrendPlot(series)

        TextButton(onClick = { showData = !showData },
            modifier = Modifier.heightIn(min = 48.dp).testTag("stats-chart-data-toggle").semantics {
                stateDescription = if (showData) "已展开" else "已收起"
            }) {
            Text(if (showData) "收起图表数据"
                else if (series.period.month == null) "查看每月金额" else "查看每日金额")
        }
        if (showData) Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            series.points.forEach { point ->
                val month = YearMonth.from(point.date)
                val rowModifier = Modifier.fillMaxWidth()
                    .testTag("stats-chart-point-${point.date}")
                val clickableRow = if (series.period.month == null && onMonth != null)
                    rowModifier.clickable { onMonth(month) }.heightIn(min = 48.dp)
                else rowModifier
                Column(clickableRow.padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(if (series.period.month == null) "${point.date.monthValue}月 · ${point.entryCount}笔"
                        else "${point.date.monthValue}月${point.date.dayOfMonth}日 · ${point.entryCount}笔",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold)
                    Text("收入 ${Money.format(point.incomeCents)}", style = MaterialTheme.typography.bodyMedium)
                    Text("支出 ${Money.format(point.expenseCents)}", style = MaterialTheme.typography.bodyMedium)
                    Text("结余 ${Money.format(point.balanceCents)}", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable private fun DailyTrendPlot(series: ChartSeries) {
    val income = MaterialTheme.colorScheme.secondary
    val expense = MaterialTheme.colorScheme.tertiary
    val grid = MaterialTheme.colorScheme.outlineVariant
    val maximum = series.points.maxOfOrNull { maxOf(it.incomeCents, it.expenseCents) } ?: 0L
    val lastDay = series.period.endDate.dayOfMonth
    Text("每日收入与支出", style = MaterialTheme.typography.titleMedium)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)) {
        TrendKey("实线 · 收入", income, false)
        TrendKey("虚线 · 支出", expense, true)
    }
    Text("纵轴 ¥0.00 — ${Money.format(maximum)}", style = MaterialTheme.typography.bodySmall)
    Canvas(Modifier.fillMaxWidth().height(144.dp).testTag("stats-daily-chart").semantics {
        contentDescription = "收入和支出每日趋势图，从1日到${lastDay}日；各日准确金额可展开图表数据"
    }) {
        val margin = 5.dp.toPx()
        val width = (size.width - margin * 2).coerceAtLeast(0f)
        val height = (size.height - margin * 2).coerceAtLeast(0f)
        val bottom = margin + height
        (0..4).forEach { index ->
            val y = margin + height * index / 4f
            drawLine(grid, Offset(margin, y), Offset(margin + width, y), 1.dp.toPx())
        }
        if (maximum > 0 && series.points.isNotEmpty()) {
            fun position(index: Int, amount: Long): Offset {
                val x = dailyChartX(index, series.points.size, size.width, margin)
                return Offset(x, bottom - height * chartAmountFraction(amount, maximum))
            }
            fun path(expenseSeries: Boolean): Path = Path().apply {
                series.points.forEachIndexed { index, point ->
                    val amount = if (expenseSeries) point.expenseCents else point.incomeCents
                    val location = position(index, amount)
                    if (index == 0) moveTo(location.x, location.y) else lineTo(location.x, location.y)
                }
            }
            // Skip an absent type completely; a zero baseline must not pretend to be a series.
            if (series.points.any { it.incomeCents > 0L }) {
                drawPath(path(false), income, style = Stroke(width = 2.4.dp.toPx()))
                series.points.forEachIndexed { index, point ->
                    if (point.incomeCents > 0L) drawCircle(income, 3.dp.toPx(),
                        position(index, point.incomeCents))
                }
            }
            if (series.points.any { it.expenseCents > 0L }) {
                drawPath(path(true), expense, style = Stroke(width = 2.4.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 4.dp.toPx()))))
                series.points.forEachIndexed { index, point ->
                    if (point.expenseCents > 0L) {
                        val pointPosition = position(index, point.expenseCents)
                        val side = 6.dp.toPx()
                        drawRect(expense, Offset(pointPosition.x - side / 2f, pointPosition.y - side / 2f),
                            Size(side, side))
                    }
                }
            }
        }
    }
    val tickDays = listOf(1, 10, 20, lastDay).distinct()
    Layout(content = {
        tickDays.forEach { day ->
            Text("${day}日", style = MaterialTheme.typography.labelSmall, maxLines = 1)
        }
    }, modifier = Modifier.fillMaxWidth()) { measurables, constraints ->
        val labels = measurables.map {
            it.measure(constraints.copy(minWidth = 0, minHeight = 0))
        }
        val width = if (constraints.hasBoundedWidth) constraints.maxWidth
            else labels.sumOf { it.width }
        val height = (labels.maxOfOrNull { it.height } ?: 0)
            .coerceIn(constraints.minHeight, constraints.maxHeight)
        layout(width, height) {
            labels.forEachIndexed { index, label ->
                val x = dailyChartX(tickDays[index] - 1, lastDay, width.toFloat(), 5.dp.toPx())
                val left = (x - label.width / 2f).toInt().coerceIn(0, (width - label.width).coerceAtLeast(0))
                label.placeRelative(left, 0)
            }
        }
    }
    if (series.entryCount == 0) Text("这个月还没有记录，暂无趋势",
        style = MaterialTheme.typography.bodyMedium)
}

private fun dailyChartX(index: Int, pointCount: Int, width: Float, margin: Float): Float {
    val plotWidth = (width - margin * 2f).coerceAtLeast(0f)
    return if (pointCount <= 1) margin + plotWidth / 2f
        else margin + plotWidth * index / (pointCount - 1)
}

@Composable private fun TrendKey(label: String, color: Color, dashed: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Canvas(Modifier.size(24.dp, 12.dp)) {
            drawLine(color, Offset(0f, size.height / 2f), Offset(size.width, size.height / 2f),
                strokeWidth = 2.dp.toPx(),
                pathEffect = if (dashed) PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 3.dp.toPx())) else null)
            if (dashed) drawRect(color, Offset(size.width / 2f - 3.dp.toPx(), size.height / 2f - 3.dp.toPx()),
                Size(6.dp.toPx(), 6.dp.toPx()))
            else drawCircle(color, 3.dp.toPx(), Offset(size.width / 2f, size.height / 2f))
        }
        Text(label, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable internal fun CategoryDonutChart(
    analysis: TypeAnalysis,
    kind: EntryKind,
    onCategory: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val slices = remember(analysis) { categoryChartSlices(analysis) }
    val type = if (kind == EntryKind.INCOME) "收入" else "支出"
    val scheme = MaterialTheme.colorScheme
    val colors = listOf(
        if (kind == EntryKind.INCOME) scheme.secondary else scheme.tertiary,
        if (scheme.surface.luminance() < .5f) Color(0xFF9EBAD2) else Color(0xFF506F89),
        Color(0xFFBD842C), Color(0xFF657D9A), Color(0xFF97789B), scheme.outline,
    )
    val fontScale = LocalDensity.current.fontScale
    val amountText = Money.format(analysis.totalCents)
    Column(modifier.fillMaxWidth().testTag("stats-category-donut"),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.fillMaxWidth().height(220.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(206.dp).testTag("stats-category-donut-chart").semantics {
                contentDescription = "${type}分类金额环形图；各分类准确金额和占比见图例"
                stateDescription = "${type}总额 ${amountText}"
            }) {
                if (slices.isNotEmpty()) {
                    val stroke = 26.dp.toPx()
                    val diameter = (size.minDimension - stroke).coerceAtLeast(0f)
                    val origin = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
                    var angle = -90f
                    slices.forEachIndexed { index, slice ->
                        val sweep = chartAmountFraction(slice.amountCents, analysis.totalCents) * 360f
                        drawArc(colors[index % colors.size], angle, sweep, false,
                            topLeft = origin, size = Size(diameter, diameter), style = Stroke(stroke))
                        angle += sweep
                    }
                }
            }
            if (slices.isNotEmpty()) Column(Modifier.width(130.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(type, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
                if (fontScale < 1.5f) Text("分类占比", style = MaterialTheme.typography.bodySmall)
            }
        }
        Text("${type}总额", style = MaterialTheme.typography.labelLarge)
        // Exact long amounts live below the ring and can wrap at large accessibility text sizes.
        Text(amountText, modifier = Modifier.fillMaxWidth(),
            style = if (fontScale >= 1.5f || amountText.length > 14)
                MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold)
        if (slices.isEmpty()) Text("暂无${type}分类", style = MaterialTheme.typography.bodyMedium)
        slices.forEachIndexed { index, slice ->
            val percentage = StatisticsNumbers.percentage(slice.amountCents, analysis.totalCents) ?: "—"
            val tag = if (slice.category == null) "stats-donut-other-${kind.name}"
                else "stats-donut-category-${kind.name}-${slice.category}"
            val rowModifier = Modifier.fillMaxWidth().testTag(tag).semantics {
                contentDescription = "${slice.label}，${Money.format(slice.amountCents)}，${percentage}，${slice.entryCount}笔"
                stateDescription = percentage
            }
            val interactive = if (slice.category != null)
                rowModifier.clickable(enabled = enabled) { onCategory(slice.category) }.heightIn(min = 48.dp)
            else rowModifier
            Row(interactive.padding(vertical = 8.dp), verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Canvas(Modifier.padding(top = 5.dp).size(10.dp)) { drawCircle(colors[index % colors.size]) }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("${index + 1}. ${slice.label}", style = MaterialTheme.typography.bodyLarge)
                    Text("${Money.format(slice.amountCents)} · ${percentage}",
                        style = MaterialTheme.typography.bodyMedium)
                    Text("${slice.entryCount}笔", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

package com.xiaoliumiaomiao.anjizhang.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.xiaoliumiaomiao.anjizhang.domain.EntryKind
import kotlin.math.min

@Composable
internal fun LedgerPanel(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content
        )
    }
}

@Composable
internal fun LedgerSectionTitle(title: String, subtitle: String? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        if (subtitle != null) {
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
internal fun KindMarker(kind: EntryKind, modifier: Modifier = Modifier) {
    val color = if (kind == EntryKind.EXPENSE) MaterialTheme.colorScheme.tertiary
        else MaterialTheme.colorScheme.secondary
    Surface(
        modifier = modifier.size(40.dp),
        shape = RoundedCornerShape(18.dp),
        color = color.copy(alpha = 0.10f),
        contentColor = color
    ) {
        Canvas(Modifier.fillMaxSize().padding(12.dp)) {
            val stroke = 2.dp.toPx()
            drawLine(color, Offset(0f, size.height / 2), Offset(size.width, size.height / 2), stroke, StrokeCap.Round)
            if (kind == EntryKind.INCOME) {
                drawLine(color, Offset(size.width / 2, 0f), Offset(size.width / 2, size.height), stroke, StrokeCap.Round)
            }
        }
    }
}

/** Original 24dp line drawings. Visible action labels provide their semantics. */
@Composable
internal fun LedgerGlyph(
    name: String,
    modifier: Modifier = Modifier,
    color: Color = LocalContentColor.current
) {
    Canvas(modifier.size(24.dp)) {
        val unit = min(size.width, size.height) / 24f
        val stroke = Stroke(1.7f * unit, cap = StrokeCap.Round, join = StrokeJoin.Round)
        fun point(x: Float, y: Float) = Offset(x * unit, y * unit)
        fun line(x1: Float, y1: Float, x2: Float, y2: Float) =
            drawLine(color, point(x1, y1), point(x2, y2), stroke.width, StrokeCap.Round)
        fun roundRect(x: Float, y: Float, width: Float, height: Float, radius: Float = 2f) =
            drawRoundRect(
                color, point(x, y), Size(width * unit, height * unit),
                CornerRadius(radius * unit), style = stroke
            )
        when (name) {
            "ledger" -> {
                roundRect(4f, 3f, 16f, 18f)
                line(8f, 3f, 8f, 21f)
                line(11f, 8f, 16f, 8f)
                line(11f, 12f, 17f, 12f)
                line(11f, 16f, 15f, 16f)
            }
            "stats" -> {
                line(3f, 20f, 21f, 20f)
                roundRect(4f, 12f, 3f, 8f, 0.8f)
                roundRect(10.5f, 7f, 3f, 13f, 0.8f)
                roundRect(17f, 3f, 3f, 17f, 0.8f)
            }
            "accounts" -> {
                roundRect(3f, 5f, 18f, 15f)
                roundRect(14f, 10f, 7f, 6f, 1f)
                drawCircle(color, 0.8f * unit, point(17f, 13f))
                line(6f, 8f, 11f, 8f)
            }
            "settings" -> {
                drawCircle(color, 6.2f * unit, point(12f, 12f), style = stroke)
                drawCircle(color, 2.2f * unit, point(12f, 12f), style = stroke)
                line(12f, 2f, 12f, 5f); line(12f, 19f, 12f, 22f)
                line(2f, 12f, 5f, 12f); line(19f, 12f, 22f, 12f)
                line(5f, 5f, 7f, 7f); line(17f, 17f, 19f, 19f)
                line(5f, 19f, 7f, 17f); line(17f, 7f, 19f, 5f)
            }
            "plus" -> { line(12f, 4f, 12f, 20f); line(4f, 12f, 20f, 12f) }
            "back" -> {
                line(19f, 12f, 5f, 12f)
                line(5f, 12f, 11f, 6f); line(5f, 12f, 11f, 18f)
            }
            "search" -> {
                drawCircle(color, 6.5f * unit, point(10.5f, 10.5f), style = stroke)
                line(15.5f, 15.5f, 21f, 21f)
            }
            "calendar" -> {
                roundRect(3f, 5f, 18f, 16f)
                line(3f, 10f, 21f, 10f)
                line(8f, 3f, 8f, 7f); line(16f, 3f, 16f, 7f)
                line(7f, 14f, 9f, 14f); line(15f, 14f, 17f, 14f)
                line(7f, 17f, 9f, 17f)
            }
            "backup", "restore" -> {
                line(4f, 16f, 4f, 21f); line(4f, 21f, 20f, 21f); line(20f, 21f, 20f, 16f)
                if (name == "backup") {
                    line(12f, 17f, 12f, 3f)
                    line(12f, 3f, 7f, 8f); line(12f, 3f, 17f, 8f)
                } else {
                    line(12f, 3f, 12f, 17f)
                    line(12f, 17f, 7f, 12f); line(12f, 17f, 17f, 12f)
                }
            }
            "shield" -> {
                val shield = Path().apply {
                    moveTo(12f * unit, 2f * unit)
                    lineTo(20f * unit, 6f * unit)
                    lineTo(19f * unit, 14f * unit)
                    quadraticBezierTo(18f * unit, 18f * unit, 12f * unit, 22f * unit)
                    quadraticBezierTo(6f * unit, 18f * unit, 5f * unit, 14f * unit)
                    lineTo(4f * unit, 6f * unit)
                    close()
                }
                drawPath(shield, color, style = stroke)
                line(8f, 12f, 11f, 15f); line(11f, 15f, 16f, 9f)
            }
            "info" -> {
                drawCircle(color, 9f * unit, point(12f, 12f), style = stroke)
                drawCircle(color, unit, point(12f, 7f))
                line(12f, 11f, 12f, 17f)
            }
            "chevron" -> { line(9f, 5f, 16f, 12f); line(16f, 12f, 9f, 19f) }
            "category" -> {
                roundRect(3f, 3f, 7f, 7f, 1.5f); roundRect(14f, 3f, 7f, 7f, 1.5f)
                roundRect(3f, 14f, 7f, 7f, 1.5f); roundRect(14f, 14f, 7f, 7f, 1.5f)
            }
        }
    }
}

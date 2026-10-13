package com.xiaoliumiaomiao.anjizhang.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.time.YearMonth

@Composable
fun PeriodNavigator(
    month: YearMonth,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onPick: () -> Unit,
    onToday: () -> Unit,
    modifier: Modifier = Modifier,
) = PeriodNavigationControls(
    label = "${month.year}年${month.monthValue}月",
    selectAction = "选择月份",
    previousLabel = "上月", nextLabel = "下月", currentLabel = "本月",
    canPrevious = month > YearMonth.of(1900, 1),
    canNext = month < YearMonth.of(9999, 12),
    onPrevious = onPrevious, onNext = onNext, onPick = onPick, onToday = onToday,
    selectTag = "month-select", tagPrefix = "month", modifier = modifier,
)

@Composable
fun YearPeriodNavigator(
    year: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onPick: () -> Unit,
    onToday: () -> Unit,
    modifier: Modifier = Modifier,
) = PeriodNavigationControls(
    label = "${year}年",
    selectAction = "选择年份",
    previousLabel = "上年", nextLabel = "下年", currentLabel = "本年",
    canPrevious = year > 1900, canNext = year < 9999,
    onPrevious = onPrevious, onNext = onNext, onPick = onPick, onToday = onToday,
    selectTag = "stats-year-select", tagPrefix = "stats-year", modifier = modifier,
)

/** A controlled presentation: picker state and current-date resolution stay with callers. */
@Composable
internal fun PeriodNavigationControls(
    label: String,
    selectAction: String,
    previousLabel: String,
    nextLabel: String,
    currentLabel: String,
    canPrevious: Boolean,
    canNext: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onPick: () -> Unit,
    onToday: () -> Unit,
    selectTag: String,
    tagPrefix: String,
    modifier: Modifier = Modifier,
) {
    val leftHanded = LocalHandedness.current == Handedness.LEFT
    Surface(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(
                onClick = onPick,
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag(selectTag)
                    .semantics { contentDescription = "$selectAction，$label" },
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                shape = RoundedCornerShape(12.dp),
            ) {
                LedgerGlyph("calendar")
                Spacer(Modifier.width(12.dp))
                Text(label, Modifier.weight(1f), style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center, softWrap = true)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (leftHanded) {
                    CurrentPeriodAction(currentLabel, onToday, "$tagPrefix-current")
                    Spacer(Modifier.weight(1f))
                }
                PeriodArrow(previousLabel, canPrevious, onPrevious, "$tagPrefix-prev", previous = true)
                PeriodArrow(nextLabel, canNext, onNext, "$tagPrefix-next", previous = false)
                if (!leftHanded) {
                    Spacer(Modifier.weight(1f))
                    CurrentPeriodAction(currentLabel, onToday, "$tagPrefix-current")
                }
            }
        }
    }
}

@Composable
private fun PeriodArrow(label: String, enabled: Boolean, action: () -> Unit, tag: String, previous: Boolean) {
    FilledTonalIconButton(
        onClick = { if (enabled) action() }, enabled = enabled,
        modifier = Modifier.size(48.dp).testTag(tag).semantics { contentDescription = label },
    ) {
        LedgerGlyph("chevron", Modifier.rotate(if (previous) 180f else 0f))
    }
}

@Composable
private fun CurrentPeriodAction(label: String, action: () -> Unit, tag: String) {
    AssistChip(onClick = action,
        label = { Text(label, style = MaterialTheme.typography.labelLarge, softWrap = true) },
        modifier = Modifier.heightIn(min = 48.dp).widthIn(min = 48.dp).testTag(tag))
}

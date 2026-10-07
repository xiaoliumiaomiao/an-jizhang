package com.xiaoliumiaomiao.anjizhang.ui

import androidx.compose.runtime.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.xiaoliumiaomiao.anjizhang.domain.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate

/** Compose keys and render checks must use the same source identity rule. */
internal class EntriesIdentity(val entries: List<Entry>) {
    override fun equals(other: Any?) = other is EntriesIdentity && entries === other.entries
    override fun hashCode() = System.identityHashCode(entries)
}
internal data class StatisticsRequest(val source: EntriesIdentity, val period: StatisticsPeriod, val today: LocalDate, val retry: Int)
internal data class StatisticsResult(val request: StatisticsRequest, val analysis: PeriodAnalysis? = null, val failed: Boolean = false)
internal data class CategoryKey(val period: StatisticsPeriod, val kind: EntryKind, val category: String)
internal data class DrillRequest(val source: EntriesIdentity, val key: CategoryKey, val today: LocalDate, val retry: Int)
internal data class DrillResult(val request: DrillRequest, val entries: List<Entry>? = null, val failed: Boolean = false)

@Composable internal fun rememberStatisticsToday(page: String): LocalDate {
    val owner = LocalLifecycleOwner.current
    // Re-entering a destination must use the visit date in its first composition.
    var today by remember(page) { mutableStateOf(LocalDate.now()) }
    DisposableEffect(owner, page) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) today = LocalDate.now() }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    return today
}

@Composable internal fun rememberStatistics(request: StatisticsRequest): StatisticsResult? {
    val result by produceState<StatisticsResult?>(null, request) {
        value = null
        value = try {
            withContext(Dispatchers.Default) {
                StatisticsResult(request, PeriodAnalysis.calculate(request.source.entries.toList(), request.period, request.today))
            }
        } catch (cancelled: CancellationException) { throw cancelled
        } catch (_: Exception) { StatisticsResult(request, failed = true) }
    }
    // produceState keeps its previous value until its new coroutine runs. Gate that first frame too.
    return result?.takeIf { it.request == request }
}

@Composable internal fun rememberDrill(request: DrillRequest?): DrillResult? {
    val result by produceState<DrillResult?>(null, request) {
        value = null
        if (request != null) value = try {
            withContext(Dispatchers.Default) {
                DrillResult(request, PeriodAnalysis.drillDown(request.source.entries.toList(), request.key.period,
                    request.key.kind, request.key.category))
            }
        } catch (cancelled: CancellationException) { throw cancelled
        } catch (_: Exception) { DrillResult(request, failed = true) }
    }
    return result?.takeIf { it.request == request }
}

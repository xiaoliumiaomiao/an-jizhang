package com.xiaoliumiaomiao.anjizhang

import androidx.compose.ui.test.*
import androidx.lifecycle.viewModelScope
import com.xiaoliumiaomiao.anjizhang.domain.*
import kotlinx.coroutines.Job
import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.*
import org.junit.Test

class LedgerStatisticsTest : StatisticsDeviceScenario() {
    @Test fun monthlyCategoriesAndMetricsUseFullLedgerDespiteDetailFilter() {
        ready(); month(2025, 5)
        text("搜索分类或备注").performTextReplacement(" keep "); compose.closeKeyboard()
        text("仅收入").performClick(); text("显示 1 / 本月 10 条")
        compose.onNodeWithTag("nav-stats").performClick(); tag("stats-kind-income").assertExists()
        overview("100.01", "16.42", "83.59"); captureLedgerScreen("s2-month-overview"); metrics(8, "2.05")
        categoryOrder(listOf("餐饮", "交通", "餐饮外卖", "Dining", "dining", "末项"))
        listOf(Triple("餐饮", "12.35", "75.2% · 2笔"), Triple("交通", "2.00", "12.2% · 2笔"),
            Triple("餐饮外卖", "1.00", "6.1% · 1笔"), Triple("Dining", "0.50", "3.0% · 1笔"),
            Triple("dining", "0.50", "3.0% · 1笔"), Triple("末项", "0.07", "0.4% · 1笔")).forEach { (name, amount, share) ->
            category(name).assertTextContains("$name  ¥$amount").assertTextContains(share)
        }
        listOf(3, 6, 7).forEachIndexed { index, id -> tag("stats-big-entry-${statsId(id)}").assertTextContains("${index + 1}.") }
        captureLedgerScreen("s2-month-expense")
        kind(EntryKind.INCOME); overview("100.01", "16.42", "83.59"); metrics(2, "50.01")
        categoryOrder(listOf("工资", "餐饮"), EntryKind.INCOME)
        category("工资", EntryKind.INCOME).assertTextContains("工资  ¥100.00").assertTextContains("100.0% · 1笔")
        category("餐饮", EntryKind.INCOME).assertTextContains("餐饮  ¥0.01").assertTextContains("0.0% · 1笔")
        tag("stats-big-entry-${statsId(1)}").assertTextContains("1."); tag("stats-big-entry-${statsId(2)}").assertTextContains("2.")
        captureLedgerScreen("s2-month-income")
        compose.onNodeWithTag("nav-ledger").performClick()
        text("搜索分类或备注").assertTextContains(" keep "); text("仅收入").assertIsSelected()
        tag("month-select").assertTextContains("2025年5月"); text("显示 1 / 本月 10 条")
    }

    @Test fun annualTwelveMonthsAndMonthEntryUseIndependentYear() {
        ready(); month(2025, 5); compose.onNodeWithTag("nav-stats").performClick()
        modeYear(); tag("stats-year-select").assertTextContains("2025年")
        overview("350.01", "45.42", "304.59", annual = true)
        tag("stats-annual-chart").assertContentDescriptionEquals("收入和支出年度双柱图，从1月到12月；各月准确金额见下方文字")
        captureLedgerScreen("s2-year-overview")
        for (month in 1..12) {
            val row = tag("stats-month-$month").assertTextContains("${month}月")
            when(month) {
                4 -> row.assertTextContains("收入 ¥50.00").assertTextContains("支出 ¥20.00").assertTextContains("结余 ¥30.00")
                5 -> row.assertTextContains("收入 ¥100.01").assertTextContains("支出 ¥16.42").assertTextContains("结余 ¥83.59")
                6 -> row.assertTextContains("收入 ¥200.00").assertTextContains("支出 ¥9.00").assertTextContains("结余 ¥191.00")
                else -> row.assertTextContains("收入 ¥0.00").assertTextContains("支出 ¥0.00").assertTextContains("结余 ¥0.00").assertTextContains("无记录")
            }
        }
        captureLedgerScreen("s2-year-months")
        tag("stats-month-5").performClick(); tag("stats-mode-month").assertIsSelected()
        tag("month-select").assertTextContains("2025年5月"); overview("100.01", "16.42", "83.59")
        modeYear(); year(2024); overview("40.00", "8.88", "31.12", annual = true)
        tag("stats-mode-month").performClick(); month(2023, 7)
        modeYear(); tag("stats-year-select").assertTextContains("2024年")
        compose.activityRule.scenario.recreate(); tag("stats-year-select").assertTextContains("2024年")
    }

    @Test fun yearBoundsRejectInvalidYearsWithoutNavigatingBeyondRange() {
        openStats(); modeYear(); year(1900)
        tag("stats-year-prev").assertIsNotEnabled(); tag("stats-year-next").assertIsEnabled()
        text("已到最早年份，暂无上年对比"); captureLedgerScreen("s2-year-1900")
        year(9999); tag("stats-year-next").assertIsNotEnabled(); tag("stats-year-prev").assertIsEnabled()
        tag("stats-month-12").assertTextContains("12月").assertTextContains("未到")
        captureLedgerScreen("s2-year-9999")
        listOf("1899", "10000").forEach { invalid ->
            tag("stats-year-select").performClick(); compose.onNodeWithTag("stats-year-input").performTextReplacement(invalid)
            compose.closeKeyboard(); compose.onNodeWithText("确定").performClick()
            compose.onNodeWithText("年份请输入1900至9999").assertIsDisplayed()
            compose.onNodeWithText("取消").performClick(); tag("stats-year-select").assertTextContains("9999年")
        }
        tag("stats-year-current").performClick(); tag("stats-year-select").assertTextContains("${LocalDate.now().year}年")
    }

    @Test fun categoryDrillUsesExactTrimKindCaseAndCompletePeriod() {
        openStats(); drill("餐饮"); allDrillIds(listOf(3, 4)); captureLedgerScreen("s2-drill-precise"); drillBack()
        kind(EntryKind.INCOME); drill("餐饮", EntryKind.INCOME); allDrillIds(listOf(2)); drillBack()
        kind(EntryKind.EXPENSE); modeYear(); drill("餐饮"); allDrillIds(listOf(13, 3, 4, 11))
        listOf(13 to "2025-06-01", 3 to "2025-05-02", 4 to "2025-05-02", 11 to "2025-04-30").forEach { (id, date) ->
            tag("stats-drill-date-${statsId(id)}").assertTextEquals(date)
        }
        drillBack()
        tag("stats-mode-month").performClick()
        drill("Dining"); allDrillIds(listOf(8)); drillBack(); drill("dining"); allDrillIds(listOf(9)); drillBack()
        assertEquals(statisticsFixture.toSet(), entries().toSet())
    }

    @Test fun drillRotationAndBothBackPathsRetainOriginAndIndependentScroll() {
        openStats()
        val name = "一二三四五六七八九十一二三四五六七八九十"
        val records = (1..20).map { Entry(statsId(100 + it), EntryKind.EXPENSE, 101, name, LocalDate.of(2025, 5, it), "滚动$it") } +
            Entry(statsId(121), EntryKind.INCOME, 100, name, LocalDate.of(2025, 5, 20), "收入对照")
        replaceFixture(records)
        compose.onNodeWithTag("nav-ledger").performClick()
        text("搜索分类或备注").performTextReplacement("滚动"); compose.closeKeyboard(); text("仅支出").performClick()
        tag("entry-${statsId(105)}"); val ledgerOffset = anchor("entry-${statsId(105)}")
        compose.onNodeWithTag("nav-stats").performClick()
        category(name); val statsOffset = anchor("stats-category-EXPENSE-$name")
        drill(name); tag("entry-${statsId(104)}"); val drillOffset = anchor("entry-${statsId(104)}")
        compose.activityRule.scenario.recreate(); assertAnchor("entry-${statsId(104)}", drillOffset)
        compose.onNodeWithTag("entry-${statsId(104)}").performClick(); compose.onNodeWithTag("detail-back").performClick()
        assertAnchor("entry-${statsId(104)}", drillOffset); captureLedgerScreen("s2-drill-restored-scroll")
        back(); assertAnchor("stats-category-EXPENSE-$name", statsOffset)
        compose.onNodeWithTag("stats-category-EXPENSE-$name").performClick(); drillBack()
        assertAnchor("stats-category-EXPENSE-$name", statsOffset)
        compose.onNodeWithTag("nav-settings").performClick(); compose.onNodeWithTag("nav-stats").performClick()
        assertAnchor("stats-category-EXPENSE-$name", statsOffset)
        compose.activityRule.scenario.recreate(); assertAnchor("stats-category-EXPENSE-$name", statsOffset)
        compose.onNodeWithTag("nav-ledger").performClick(); assertAnchor("entry-${statsId(105)}", ledgerOffset)
        text("搜索分类或备注").assertTextContains("滚动"); text("仅支出").assertIsSelected()
        tag("month-select").assertTextContains("2025年5月")
    }

    @Test fun drillEditCancelLeavesDetailAndOriginalFields() {
        openStats(); drill("餐饮"); detail(3); edit()
        field("分类（可自定义）", "未保存")
        compose.onNodeWithText("取消").performScrollTo().performClick(); compose.onNodeWithText("放弃修改").performClick()
        compose.onNodeWithTag("entry-detail").assertIsDisplayed(); assertEquals(statisticsFixture.toSet(), entries().toSet())
        compose.activityRule.scenario.recreate(); compose.onNodeWithTag("detail-back").performClick(); allDrillIds(listOf(3, 4))
    }
    private fun editOut(fieldName: String, value: String, expected: Entry, expense: String, balance: String) {
        openStats(); drill("餐饮"); detail(3); edit()
        if (fieldName == "kind") compose.onNode(hasText("收入") and isSelectable()).performScrollTo().performClick()
        else field(fieldName, value)
        save(); allDrillIds(listOf(4))
        assertEquals((statisticsFixture.filter { it.id != statsId(3) } + expected).toSet(), entries().toSet())
        drillBack(); overview(if (fieldName == "kind") "112.35" else "100.01", expense, balance)
        category("餐饮").assertTextContains("餐饮  ¥0.01")
    }
    @Test fun editedDateMovesEntryOutAndReturnsToOriginalDrill() = editOut("日期（yyyy-MM-dd）", "2025-06-02",
        statisticsFixture[2].copy(date = LocalDate.of(2025, 6, 2), category = "餐饮"), "4.08", "95.93")
    @Test fun editedKindMovesEntryOutAndReturnsToOriginalDrill() = editOut("kind", "",
        statisticsFixture[2].copy(kind = EntryKind.INCOME, category = "餐饮"), "4.08", "108.27")
    @Test fun editedCategoryMovesEntryOutWithoutChangingMonthTotal() = editOut("分类（可自定义）", "餐饮外卖",
        statisticsFixture[2].copy(category = "餐饮外卖"), "16.42", "83.59")

    @Test fun deleteLastDrillEntryReturnsToPersistentEmptyCategory() {
        openStats(); drill("末项"); detail(10)
        compose.onNodeWithText("删除").performScrollTo().performClick(); compose.onNodeWithText("取消").performClick()
        compose.onNodeWithTag("entry-detail").assertIsDisplayed(); assertEquals(statisticsFixture.toSet(), entries().toSet())
        delete(); text("此分类在所选期间没有记录"); captureLedgerScreen("s2-drill-empty")
        compose.activityRule.scenario.recreate(); text("此分类在所选期间没有记录")
        assertEquals(statisticsFixture.filter { it.id != statsId(10) }.toSet(), entries().toSet())
        drillBack(); overview("100.01", "16.35", "83.66")
    }
    @Test fun topThreeCancelSaveAndDeleteReturnToStatisticsSource() {
        openStats(); val topTag = "stats-big-entry-${statsId(3)}"
        tag(topTag); val offset = anchor(topTag)
        compose.onNodeWithTag(topTag).performClick(); edit(); compose.onNodeWithText("取消").performScrollTo().performClick()
        compose.onNodeWithTag("entry-detail").assertIsDisplayed(); back(); assertAnchor(topTag, offset)
        compose.onNodeWithTag(topTag).performClick(); edit(); field("备注（可选，最多200字）", "复查完成"); save()
        compose.onNodeWithTag("nav-stats").assertIsSelected(); assertAnchor(topTag, offset)
        assertEquals("复查完成", entries().single { it.id == statsId(3) }.note)
        captureLedgerScreen("s2-top3-return")
        tag(topTag).performClick(); delete(); compose.onNodeWithTag("nav-stats").assertIsSelected()
        overview("100.01", "4.08", "95.93")
        assertEquals(statisticsFixture.filter { it.id != statsId(3) }.toSet(), entries().toSet())
    }

    @Test fun comparisonShowsExactHistoricalRangesAndDifferences() {
        openStats()
        sectionText("stats-comparison", "本期：2025-05-01 至 2025-05-31")
        sectionText("stats-comparison", "对比：2025-04-01 至 2025-04-30")
        sectionText("stats-comparison", "本期支出 ¥16.42")
        sectionText("stats-comparison", "对比期支出 ¥20.00")
        sectionText("stats-comparison", "差额 -¥3.58（-17.9%）")
        kind(EntryKind.INCOME); sectionText("stats-comparison", "差额 ¥50.01（100.0%）")
        modeYear(); sectionText("stats-comparison", "本期：2025-01-01 至 2025-12-31")
        sectionText("stats-comparison", "对比：2024-01-01 至 2024-12-31")
        sectionText("stats-comparison", "差额 ¥310.01（775.0%）")
        kind(EntryKind.EXPENSE); sectionText("stats-comparison", "差额 ¥36.54（411.5%）")
        captureLedgerScreen("s2-comparison-full")
    }
    @Test fun comparisonDistinguishesMissingBasesFutureAndEarliestPeriod() {
        openStats(); month(2025, 3)
        for (type in EntryKind.entries) { kind(type); sectionText("stats-comparison", "暂无可比较记录") }
        captureLedgerScreen("s2-comparison-none"); month(2025, 4)
        for (type in EntryKind.entries) { kind(type); sectionText("stats-comparison", "对比期无此类型记录") }
        captureLedgerScreen("s2-comparison-no-base"); month(2025, 7)
        kind(EntryKind.INCOME); sectionText("stats-comparison", "差额 -¥200.00（-100.0%）")
        kind(EntryKind.EXPENSE); sectionText("stats-comparison", "差额 -¥9.00（-100.0%）")
        month(9999, 12); sectionText("stats-comparison", "所选期间尚未开始，暂无同期对比")
        month(1900, 1); sectionText("stats-comparison", "已到最早月份，暂无上月对比")
        month(1900, 2); sectionText("stats-comparison", "对比：1900-01-01 至 1900-01-31")
    }

    private fun currentComparison(annual: Boolean) {
        openStats(); val today = LocalDate.now()
        val start = if (annual) today.withDayOfYear(1) else today.withDayOfMonth(1)
        val previous = if (annual) start.minusYears(1) else start.minusMonths(1)
        val previousLength = if (annual) previous.lengthOfYear() else previous.lengthOfMonth()
        val days = minOf(if (annual) today.dayOfYear else today.dayOfMonth, previousLength)
        val end = start.plusDays(days - 1L); val previousEnd = previous.plusDays(days - 1L)
        val periodEnd = if (annual) today.withMonth(12).withDayOfMonth(31) else YearMonth.from(today).atEndOfMonth()
        fun row(id: Int, type: EntryKind, cents: Long, date: LocalDate) = Entry(statsId(id), type, cents, "同期测试", date, "合成")
        val rows = mutableListOf(row(201, EntryKind.INCOME, 10000, start), row(202, EntryKind.EXPENSE, 1000, start),
            row(203, EntryKind.INCOME, 5000, previous), row(204, EntryKind.EXPENSE, 400, previous),
            row(205, EntryKind.EXPENSE, 1, end), row(206, EntryKind.EXPENSE, 1, previousEnd))
        if (periodEnd > today) rows += row(207, EntryKind.INCOME, 20000, periodEnd)
        val previousPeriodEnd = if (annual) previous.withMonth(12).withDayOfMonth(31) else YearMonth.from(previous).atEndOfMonth()
        if (previousEnd < previousPeriodEnd) rows += row(208, EntryKind.EXPENSE, 9000, previousEnd.plusDays(1))
        replaceFixture(rows); month(today.year, today.monthValue)
        if (annual) { modeYear(); year(today.year) }
        overview(if (periodEnd > today) "300.00" else "100.00", "10.01", if (periodEnd > today) "289.99" else "89.99", annual)
        if (periodEnd > today) text("包含未来日期账目")
        captureLedgerScreen(if (annual) "s2-current-year-overview" else "s2-current-month-overview")
        sectionText("stats-comparison", "按相同天数比较")
        sectionText("stats-comparison", "本期：$start 至 $end")
        sectionText("stats-comparison", "对比：$previous 至 $previousEnd")
        sectionText("stats-comparison", "本期支出 ¥10.01")
        sectionText("stats-comparison", "对比期支出 ¥4.01")
        sectionText("stats-comparison", "差额 ¥6.00（149.6%）")
        kind(EntryKind.INCOME); sectionText("stats-comparison", "差额 ¥50.00（100.0%）")
        sectionText("stats-comparison", "本期收入 ¥100.00"); sectionText("stats-comparison", "对比期收入 ¥50.00")
        captureLedgerScreen(if (annual) "s2-comparison-current-year" else "s2-comparison-current-month")
        if (annual && today.monthValue < 12) tag("stats-month-12").assertTextContains("未到").assertTextContains("收入 ¥200.00")
        android.util.Log.i("LedgerS2", "today=$today timezone=${java.time.ZoneId.systemDefault()} annual=$annual windowDays=$days future=${periodEnd > today} previousAfterWindow=${previousEnd < previousPeriodEnd}")
        assertEquals("Clock crossed midnight; rerun with one date fixture", today, LocalDate.now())
    }
    @Test fun currentMonthComparesEqualDaysAndOverviewIncludesFutureRecords() = currentComparison(false)
    @Test fun currentYearComparesEqualDaysAndOverviewIncludesFutureRecords() = currentComparison(true)

    @Test fun rapidPeriodAndSnapshotChangesNeverLeaveStaleVisibleResults() {
        openStats(); overview("100.01", "16.42", "83.59")
        repeat(3) { modeYear(); tag("stats-year-prev").performClick(); tag("stats-year-next").performClick(); tag("stats-mode-month").performClick() }
        modeYear(); kind(EntryKind.INCOME); overview("350.01", "45.42", "304.59", true)
        val vm = model()
        val reloadJobs = compose.runOnIdle {
            val previousJobs = vm.viewModelScope.coroutineContext[Job]?.children?.toSet().orEmpty()
            vm.load()
            vm.viewModelScope.coroutineContext[Job]?.children?.filter { it !in previousJobs }?.toList().orEmpty()
        }
        compose.waitUntil(10000) { reloadJobs.all { it.isCompleted } && !vm.state.value.busy }
        assertNull(vm.state.value.error)
        overview("350.01", "45.42", "304.59", true); metrics(4, "87.50")
        val replacement = listOf(Entry(statsId(301), EntryKind.INCOME, 77777, "全新快照", LocalDate.of(2025, 5, 1), "合成"))
        replaceFixture(replacement)
        tag("stats-year-prev").performClick(); overview("0.00", "0.00", "0.00", true)
        tag("stats-year-next").performClick(); overview("777.77", "0.00", "777.77", true)
        metrics(1, "777.77"); category("全新快照", EntryKind.INCOME).assertTextContains("100.0% · 1笔")
        compose.activityRule.scenario.recreate(); overview("777.77", "0.00", "777.77", true)
        drill("全新快照", EntryKind.INCOME)
        val mutable = mutableState(); val loaded = model().state.value
        try {
            compose.runOnIdle { mutable.value = loaded.copy(entries = emptyList(), busy = true) }
            compose.onNodeWithText("正在读取账目…").assertIsDisplayed()
            compose.onNodeWithText("此分类在所选期间没有记录").assertDoesNotExist()
            compose.activityRule.scenario.recreate(); compose.onNodeWithText("正在读取账目…").assertIsDisplayed()
            compose.runOnIdle { mutable.value = loaded.copy(entries = emptyList(), busy = false, error = "读取失败") }
            compose.onNodeWithText("无法读取账目，请重试").assertIsDisplayed()
            compose.onNodeWithText("此分类在所选期间没有记录").assertDoesNotExist()
            compose.runOnIdle { mutable.value = loaded }
            allDrillIds(listOf(301))
        } finally { compose.runOnIdle { mutable.value = loaded } }
    }
}

package com.xiaoliumiaomiao.anjizhang

import android.content.res.Configuration
import android.os.Bundle
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.test.*
import androidx.test.platform.app.InstrumentationRegistry
import com.xiaoliumiaomiao.anjizhang.domain.Entry
import com.xiaoliumiaomiao.anjizhang.domain.EntryKind
import java.security.MessageDigest
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

/** All calls used by the before phase exist in the installed 0.1.6 application. */
internal val modernUiFixture: List<Entry> = listOf(
    uiRow(710, EntryKind.INCOME, 1850000, "工资", "2025-05-03", "五月工资（合成）"),
    uiRow(711, EntryKind.INCOME, 280000, "奖金", "2025-05-20", "项目奖金（合成）"),
    uiRow(712, EntryKind.INCOME, 65000, "兼职", "2025-05-26", "周末工作（合成）"),
    uiRow(720, EntryKind.EXPENSE, 32800, " 餐饮 ", "2025-05-02", "家庭晚餐（合成）"),
    uiRow(721, EntryKind.EXPENSE, 14650, "餐饮", "2025-05-05", "日常午餐（合成）"),
    uiRow(722, EntryKind.EXPENSE, 1280, "交通", "2025-05-05", "公交出行（合成）"),
    uiRow(723, EntryKind.EXPENSE, 45900, "购物", "2025-05-12", "生活用品（合成）"),
    uiRow(724, EntryKind.EXPENSE, 360000, "住房", "2025-05-01", "五月房租（合成）"),
    uiRow(725, EntryKind.EXPENSE, 16800, "娱乐", "2025-05-18", "周末电影（合成）"),
    uiRow(726, EntryKind.EXPENSE, 29900, "学习", "2025-05-22", "课程书籍（合成）"),
    uiRow(727, EntryKind.EXPENSE, 8500, "其他", "2025-05-29", "生活杂项（合成）"),
    uiRow(728, EntryKind.EXPENSE, 2670, "餐饮外卖", "2025-05-14", "工作日晚餐（合成）"),
    uiRow(729, EntryKind.EXPENSE, 27700, "旅行", "2025-05-31", "短途出行（合成）"),
    uiRow(740, EntryKind.INCOME, 1600000, "工资", "2025-01-03"),
    uiRow(741, EntryKind.EXPENSE, 300000, "住房", "2025-01-01"),
    uiRow(742, EntryKind.INCOME, 1750000, "工资", "2025-03-03"),
    uiRow(743, EntryKind.EXPENSE, 420000, "购物", "2025-03-16"),
    uiRow(744, EntryKind.INCOME, 1800000, "工资", "2025-04-03"),
    uiRow(745, EntryKind.EXPENSE, 500000, "住房", "2025-04-01"),
    uiRow(746, EntryKind.INCOME, 1980000, "工资", "2025-06-03"),
    uiRow(747, EntryKind.EXPENSE, 390000, "旅行", "2025-06-16"),
    uiRow(748, EntryKind.INCOME, 1850000, "工资", "2025-09-03"),
    uiRow(749, EntryKind.EXPENSE, 560000, "购物", "2025-09-16"),
    uiRow(750, EntryKind.INCOME, 600000, "奖金", "2025-12-20"),
    uiRow(760, EntryKind.INCOME, 1500000, "工资", "2024-05-03"),
    uiRow(761, EntryKind.EXPENSE, 380000, "住房", "2024-05-01")
)

internal fun uiRow(id: Int, kind: EntryKind, cents: Long, category: String, date: String,
    note: String = "界面对比合成账目") = Entry(statsId(id), kind, cents, category, LocalDate.parse(date), note)

class LedgerUiComparisonTest : StatisticsDeviceScenario() {
    private fun configIsComparable() {
        emulatorOnly()
        val config = compose.runOnIdle { Configuration(compose.activity.resources.configuration) }
        assertEquals("Same 360dp comparison width", 360, config.screenWidthDp)
        assertEquals("Same comparison density", 320, config.densityDpi)
        assertEquals("Same comparison font scale", 1f, config.fontScale, .01f)
        assertEquals("Same comparison light theme", Configuration.UI_MODE_NIGHT_NO,
            config.uiMode and Configuration.UI_MODE_NIGHT_MASK)
        assertTrue("Same physical comparison viewport", shell("wm size").contains("720x1280"))
    }

    private fun capture(prefix: String, suffix: String) {
        configIsComparable()
        assertEquals("Screenshot navigation must not alter the synthetic ledger",
            modernUiFixture.sortedBy { it.id }, entries().sortedBy { it.id })
        compose.waitForIdle()
        captureLedgerScreen("$prefix-$suffix")
    }

    private fun top(list: String) {
        compose.onNodeWithTag(list).performScrollToIndex(0)
        compose.waitForIdle()
    }

    private fun assertDailyDateTicksFitFirstViewport() {
        val viewport = compose.onNodeWithTag("stats-list").fetchSemanticsNode()
        for (day in listOf(1, 10, 20, 31)) {
            // Read the current first viewport without revealing or scrolling to a tick.
            val node = compose.onNode(hasText("${day}日") and
                hasAnyAncestor(hasTestTag("stats-period-trend")), useUnmergedTree = true)
                .assertIsDisplayed().fetchSemanticsNode()
            val (raw, area) = compose.runOnIdle {
                listOf(node, viewport).forEach {
                    assertTrue("Date tick and viewport must be attached and placed",
                        it.layoutInfo.isAttached && it.layoutInfo.isPlaced)
                }
                Rect(node.positionInRoot, Size(node.size.width.toFloat(), node.size.height.toFloat())) to
                    viewport.boundsInRoot
            }
            assertTrue("First monthly viewport must fully contain ${day}日 date tick: raw=$raw viewport=$area",
                raw.left >= area.left - 1f && raw.right <= area.right + 1f &&
                    raw.top >= area.top - 1f && raw.bottom <= area.bottom + 1f)
        }
    }

    @Test fun fixedSyntheticScreensUseTheSameDataPeriodConfigurationAndPositions() {
        val phase = InstrumentationRegistry.getArguments().getString("uiComparisonPhase") ?: "after"
        require(phase in setOf("before", "after")) { "uiComparisonPhase must be before or after" }
        val prefix = "v017-$phase"
        configIsComparable()
        ready()
        replaceFixture(modernUiFixture)
        month(2025, 5)
        top("ledger-list")
        capture(prefix, "ledger")

        compose.onNodeWithTag("nav-stats").performClick()
        awaitAnalysis()
        overview("21950.00", "5402.00", "16548.00")
        top("stats-list")
        if (phase == "after") {
            compose.onNodeWithTag("stats-daily-chart").assertIsDisplayed()
            assertDailyDateTicksFitFirstViewport()
        }
        capture(prefix, "month-overview")
        compose.onNodeWithTag("stats-list").performScrollToKey(if (phase == "after") "category-donut" else "category-title")
        if (phase == "after") compose.onNodeWithTag("stats-category-donut-chart").assertIsDisplayed()
        capture(prefix, "month-category")

        modeYear()
        year(2025)
        top("stats-list")
        if (phase == "after") compose.onNodeWithTag("stats-annual-chart").assertIsDisplayed()
        capture(prefix, "year-overview")
        compose.onNodeWithTag("stats-list").performScrollToKey(if (phase == "after") "category-donut" else "category-title")
        if (phase == "after") compose.onNodeWithTag("stats-category-donut-chart").assertIsDisplayed()
        capture(prefix, "year-category")

        compose.onNodeWithTag("nav-ledger").performClick()
        month(2025, 5)
        detail(720)
        edit()
        compose.onNodeWithText("金额（元）").performScrollTo().assertIsDisplayed()
        compose.setAmountWithKeypad("88.60")
        compose.assertSystemImeVisible(false)
        compose.onNodeWithTag("amount-keypad-panel").assertIsDisplayed()
        capture(prefix, "editor-amount")
        compose.onNodeWithTag("amount-keypad-close").performScrollTo().performClick()
        compose.onNodeWithText("取消").performScrollTo().performClick()
        compose.onNodeWithText("放弃修改").assertIsDisplayed().performClick()
        compose.waitUntil(10000) { compose.onAllNodesWithTag("editor-form").fetchSemanticsNodes().isEmpty() }
        compose.onNodeWithTag("detail-back").performClick()
        compose.onNodeWithTag("nav-settings").performClick()
        top("settings-list")
        capture(prefix, "settings")

        val fixtureText = modernUiFixture.sortedBy { it.id }.joinToString("\n") {
            "${it.id}|${it.kind}|${it.amountCents}|${it.category}|${it.date}|${it.note}"
        }
        val fixtureHash = MessageDigest.getInstance("SHA-256").digest(fixtureText.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it.toInt() and 0xff) }
        InstrumentationRegistry.getInstrumentation().sendStatus(0, Bundle().apply {
            putString("stream", "\nLedgerUiComparison: phase=$phase fixtureSha256=$fixtureHash entries=${modernUiFixture.size} " +
                "period=2025-05 year=2025 width=360 density=320 fontScale=1.0 theme=light " +
                "monthlyIncomeCents=2195000 monthlyExpenseCents=540200 monthlyBalanceCents=1654800\n")
        })
    }
}

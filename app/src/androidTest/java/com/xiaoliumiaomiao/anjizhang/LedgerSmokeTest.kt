package com.xiaoliumiaomiao.anjizhang
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain

class LedgerSmokeTest {
    val compose = createAndroidComposeRule<MainActivity>()
    private val cleanDatabase = object : ExternalResource() {
        override fun before() { ApplicationProvider.getApplicationContext<Context>().deleteDatabase("ledger.db") }
    }
    @get:Rule val rules: RuleChain = RuleChain.outerRule(cleanDatabase).around(compose)
    @Test fun emptyLedgerAndNavigation() {
        compose.onNodeWithText("安记账").assertIsDisplayed()
        compose.onNodeWithText("这个月还没有账目").assertIsDisplayed()
        compose.onAllNodesWithText("记一笔")[0].assertIsDisplayed()
        compose.onNodeWithText("统计").performClick()
        compose.onNodeWithText("支出分类").assertIsDisplayed()
        compose.onNodeWithText("设置").performClick()
        compose.onNodeWithText("本地隐私").assertIsDisplayed()
    }
    @Test fun editorInputSurvivesActivityRecreation() {
        compose.onAllNodesWithText("记一笔")[0].performClick()
        compose.onNodeWithText("金额（元）").performTextInput("12.34")
        compose.activityRule.scenario.recreate()
        compose.onNodeWithText("金额（元）").assertTextContains("12.34")
        compose.onNodeWithText("取消").performScrollTo().performClick()
        compose.onNodeWithText("统计").performClick()
        compose.activityRule.scenario.recreate()
        compose.onNodeWithText("支出分类").assertIsDisplayed()
    }
    @Test fun invalidAmountCannotSaveThenOneCentExpenseAppears() {
        compose.onAllNodesWithText("记一笔")[0].performClick()
        compose.onNodeWithText("金额（元）").performTextInput("0")
        compose.onNodeWithText("保存").performScrollTo().performClick()
        compose.onNodeWithText("金额请输入0.01至999999999.99，最多两位小数").assertIsDisplayed()
        compose.onNodeWithText("金额（元）").performTextReplacement("0.01")
        compose.onNodeWithText("保存").performScrollTo().performClick()
        compose.waitUntil(10000) { compose.onAllNodesWithText("-¥0.01").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("-¥0.01").assertIsDisplayed()
        compose.onNodeWithText("支出 ¥0.01").assertIsDisplayed()
    }
}

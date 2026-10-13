package com.xiaoliumiaomiao.anjizhang
import android.content.Context
import android.os.ParcelFileDescriptor
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
class LedgerLargeTextTest {
    private val instrumentation=InstrumentationRegistry.getInstrumentation()
    private fun shell(command:String):String = ParcelFileDescriptor.AutoCloseInputStream(instrumentation.uiAutomation.executeShellCommand(command)).bufferedReader().use{it.readText().trim()}
    val compose=createAndroidComposeRule<MainActivity>()
    private val clean=object:ExternalResource(){override fun before(){
        assumeTrue("CI-only screen configuration",InstrumentationRegistry.getArguments().getString("ledgerCi")=="true")
        assertEquals("1",shell("getprop ro.kernel.qemu"))
        val name=shell("getprop ro.boot.qemu.avd_name").ifEmpty{shell("getprop ro.kernel.qemu.avd_name")}
        assertEquals("ci-ledger",name)
        ApplicationProvider.getApplicationContext<Context>().deleteDatabase("ledger.db")
    }}
    @get:Rule val rules:RuleChain=RuleChain.outerRule(UiPreferencesIsolation()).around(clean).around(compose)
    private fun setting(kind:String)=shell("wm $kind").lineSequence().firstOrNull{it.startsWith("Override ") }?.substringAfter(": ")
    @Test fun largeTextCanReachNavigationFiltersDetailAndEditorActions(){
        val oldSize=setting("size");val oldDensity=setting("density");val oldFont=shell("settings get system font_scale")
        try {
            shell("wm size 720x1280");shell("wm density 320");shell("settings put system font_scale 2.0")
            compose.activityRule.scenario.recreate()
            compose.waitUntil(10000){compose.activity.resources.configuration.fontScale>=1.99f && compose.activity.resources.configuration.screenWidthDp<=360}
            assertTrue(compose.activity.resources.configuration.screenWidthDp in 320..360)
            val ready=hasTestTag("add-entry") and isEnabled()
            compose.waitUntil(10000){compose.onAllNodes(ready).fetchSemanticsNodes().isNotEmpty()}
            listOf("nav-stats","nav-accounts","nav-settings","nav-ledger").forEach{compose.onNodeWithTag(it).assertIsDisplayed().performClick().assertIsSelected()}
            fun show(text:String){compose.onNodeWithTag("ledger-list").performScrollToNode(hasText(text));compose.onNodeWithText(text).assertIsDisplayed()}
            show("下月");compose.onNodeWithText("下月").performClick();show("本月");compose.onNodeWithText("本月").performClick()
            show("仅支出");compose.onNodeWithText("仅支出").performClick();show("清除筛选");compose.onNodeWithText("清除筛选").performClick()
            compose.onNodeWithTag("add-entry").assertIsDisplayed().performClick()
            compose.setAmountWithKeypad("0")
            compose.onNodeWithText("保存").performScrollTo().assertIsDisplayed().performClick()
            compose.onNodeWithText("金额请输入0.01至999999999.99，最多两位小数").performScrollTo().assertIsDisplayed()
            compose.setAmountWithKeypad("999999999.99")
            compose.addAndSelectCategory("长".repeat(20))
            compose.waitForIdle()
            compose.onNodeWithTag("note-toggle").performScrollTo()
                .assertIsDisplayed().assertIsEnabled().assert(hasText("添加备注")).performClick()
            compose.waitUntil(10000){compose.onAllNodesWithText("备注（可选，最多200字）").fetchSemanticsNodes().size==1}
            compose.onNodeWithTag("note-toggle").assert(hasText("收起备注"))
            compose.onNodeWithText("备注（可选，最多200字）").performScrollTo().performTextReplacement("记".repeat(200))
            compose.closeKeyboard()
            compose.onNodeWithText("金额（元）").performScrollTo().assertIsDisplayed();captureLedgerScreen("narrow-large-editor")
            compose.onNodeWithText("保存").performScrollTo().assertIsDisplayed();captureLedgerScreen("narrow-large-editor-actions")
            compose.onNodeWithText("保存").performClick()
            compose.waitUntil(10000){compose.onAllNodesWithTag("editor-form").fetchSemanticsNodes().isEmpty()}
            show("-¥999999999.99");compose.onNodeWithText("-¥999999999.99").performClick()
            compose.onNodeWithText("分类："+"长".repeat(20)).performScrollTo().assertIsDisplayed()
            compose.onNodeWithText("备注："+"记".repeat(200)).performScrollTo().assertIsDisplayed()
            compose.onNodeWithText("编辑").performScrollTo().assertIsDisplayed().performClick()
            compose.onNodeWithText("取消").performScrollTo().assertIsDisplayed().performClick()
            compose.onNodeWithTag("entry-detail").assertIsDisplayed()
        } finally {
            shell(if(oldSize==null)"wm size reset" else "wm size $oldSize")
            shell(if(oldDensity==null)"wm density reset" else "wm density $oldDensity")
            shell(if(oldFont=="null"||oldFont.isEmpty())"settings delete system font_scale" else "settings put system font_scale $oldFont")
            compose.activityRule.scenario.recreate()
            assertEquals(oldSize,setting("size"));assertEquals(oldDensity,setting("density"))
            assertEquals(oldFont,shell("settings get system font_scale"))
        }
    }
}

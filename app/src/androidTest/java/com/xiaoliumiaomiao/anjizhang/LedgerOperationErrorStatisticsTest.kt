package com.xiaoliumiaomiao.anjizhang

import android.content.Context
import androidx.compose.ui.test.*
import androidx.test.core.app.ApplicationProvider
import com.xiaoliumiaomiao.anjizhang.data.LedgerDatabase
import com.xiaoliumiaomiao.anjizhang.ui.LedgerViewModel
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.OutputStream
import org.junit.Assert.*
import org.junit.Test

/** Real backup failures against the existing synthetic ci-ledger fixture and ViewModel. */
class LedgerOperationErrorStatisticsTest : StatisticsDeviceScenario() {
    private fun failOperation(reason: String, action: (LedgerViewModel) -> Unit): LedgerViewModel {
        val vm = model()
        compose.runOnIdle { action(vm) }
        compose.waitUntil(10000) { !vm.state.value.busy && vm.state.value.error?.contains(reason) == true }
        compose.onNodeWithText(requireNotNull(vm.state.value.error)).assertIsDisplayed()
        assertEquals(statisticsFixture.toSet(), vm.state.value.entries.toSet())
        assertEquals(statisticsFixture.toSet(), entries().toSet())
        return vm
    }
    @Test fun damagedRestoreKeepsMonthlyAndAnnualStatisticsWithOriginalErrorVisible() {
        openStats(); overview("100.01", "16.42", "83.59")
        val vm = failOperation("备份无效") { it.restore { ByteArrayInputStream("broken".toByteArray()) } }
        overview("100.01", "16.42", "83.59")
        category("餐饮").assertTextContains("餐饮  ¥12.35")
        modeYear(); overview("350.01", "45.42", "304.59", annual = true)
        compose.onNodeWithText(requireNotNull(vm.state.value.error)).assertIsDisplayed()
        compose.onNodeWithText("无法读取账目，请重试").assertDoesNotExist()
        assertFalse(vm.state.value.readError)
        captureLedgerScreen("v016-damaged-restore-valid-year-statistics")
    }
    @Test fun failedOutputKeepsOpenCategoryDrillAndOriginalErrorVisible() {
        openStats(); drill("餐饮"); allDrillIds(listOf(3, 4))
        val vm = failOperation("输出失败") { it.export { object : OutputStream() {
            override fun write(value: Int) { throw IOException("合成输出失败") }
        } } }
        allDrillIds(listOf(3, 4))
        compose.onNodeWithText(requireNotNull(vm.state.value.error)).assertIsDisplayed()
        compose.onNodeWithText("无法读取账目，请重试").assertDoesNotExist()
        assertFalse(vm.state.value.readError)
        captureLedgerScreen("v016-failed-output-valid-category-drill")
        drillBack(); overview("100.01", "16.42", "83.59")
        assertEquals(statisticsFixture.toSet(), entries().toSet())
    }
    @Test fun actualLoadFailureStillShowsReadPlaceholderAndSuccessfulReloadRecoversDrill() {
        openStats(); drill("餐饮"); allDrillIds(listOf(3, 4))
        val vm = model()
        LedgerDatabase(ApplicationProvider.getApplicationContext<Context>()).use { outside ->
            outside.writableDatabase.execSQL("UPDATE entries SET date = 'invalid-date' WHERE id = ?", arrayOf(statsId(3)))
            compose.runOnIdle { vm.load() }
            compose.waitUntil(10000) { !vm.state.value.busy && vm.state.value.error?.contains("读取账目失败") == true }
            compose.onNodeWithText("无法读取账目，请重试").assertIsDisplayed()
            compose.onNodeWithText("此分类在所选期间没有记录").assertDoesNotExist()
            assertTrue(vm.state.value.readError)
            assertEquals(statisticsFixture.toSet(), vm.state.value.entries.toSet())
            captureLedgerScreen("v016-actual-read-failure-category-placeholder")
            val source = statisticsFixture.single { it.id == statsId(3) }
            outside.writableDatabase.execSQL("UPDATE entries SET date = ? WHERE id = ?", arrayOf(source.date.toString(), source.id))
            compose.runOnIdle { vm.load() }
            compose.waitUntil(10000) { !vm.state.value.busy && vm.state.value.error == null }
        }
        assertFalse(vm.state.value.readError)
        allDrillIds(listOf(3, 4))
        assertEquals(statisticsFixture.toSet(), entries().toSet())
    }
}

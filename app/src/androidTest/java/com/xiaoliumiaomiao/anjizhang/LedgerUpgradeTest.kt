package com.xiaoliumiaomiao.anjizhang

import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Bundle
import androidx.test.platform.app.InstrumentationRegistry
import com.xiaoliumiaomiao.anjizhang.data.LedgerDatabase
import com.xiaoliumiaomiao.anjizhang.domain.Entry
import com.xiaoliumiaomiao.anjizhang.domain.EntryKind
import com.xiaoliumiaomiao.anjizhang.domain.MonthlySummary
import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Historical schema1/.7 contract. Excluded from current CI; schema2 uses the separate raw seed and verifier. */
class LedgerUpgradeTest {
    private val expected = listOf(
        Entry("01100000-0000-0000-0000-000000000001", EntryKind.INCOME, 10000L, "工资", LocalDate.of(2026, 10, 1), "升级验证收入"),
        Entry("01100000-0000-0000-0000-000000000002", EntryKind.EXPENSE, 1L, "自定义分类", LocalDate.of(2026, 10, 2), "中文、引号\"和换行\n备注"),
        Entry("01100000-0000-0000-0000-000000000003", EntryKind.EXPENSE, 1234L, "交通", LocalDate.of(2026, 9, 30), "跨月支出")
    )

    @Test fun seedOldVersionLedger() {
        val baseline = InstrumentationRegistry.getArguments().getString("ledgerUpgradeBaseline") ?: "0.1.4"
        val context = when (baseline) {
            "0.1.4" -> installedVersion("0.1.4", 6)
            "0.1.6" -> installedVersion("0.1.6", 8)
            else -> error("ledgerUpgradeBaseline must be 0.1.4 or 0.1.6")
        }
        val database = LedgerDatabase(context)
        try {
            assertTrue("Seed requires an empty ledger; existing data must not be overwritten", database.listEntries().isEmpty())
            expected.forEach(database::save)
        } finally {
            database.close()
        }
        assertLedgerOnDisk(context, "seed-reopened")
    }

    @Test fun verifyUpgradedLedger() {
        val context = installedVersion("0.1.7", 9)
        // No save, merge, delete, or reseed: a missing database must fail.
        assertLedgerOnDisk(context, "upgrade-opened")
        assertLedgerOnDisk(context, "upgrade-reopened")
    }

    @Suppress("DEPRECATION")
    private fun installedVersion(name: String, code: Int): Context {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.xiaoliumiaomiao.anjizhang", context.packageName)
        // PackageManager reads the installed app, unlike an inlined BuildConfig constant.
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        assertEquals("Installed versionName", name, info.versionName)
        assertEquals("Installed versionCode", code, info.versionCode)
        val application = context.packageManager.getApplicationInfo(context.packageName, 0)
        assertTrue("Upgrade validation requires a debuggable app", application.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0)
        evidence("Installed package=${context.packageName} versionName=$name versionCode=$code debuggable=true")
        return context
    }

    private fun assertLedgerOnDisk(context: Context, phase: String) {
        assertTrue("Persisted ledger.db must exist", context.getDatabasePath("ledger.db").isFile)
        val database = LedgerDatabase(context)
        try {
            assertEquals("SQLite schema must remain version 1", 1, database.readableDatabase.version)
            val actual = database.listEntries().sortedBy { it.id }
            assertEquals("All entries and every field must survive", expected.sortedBy { it.id }, actual)
            val october = MonthlySummary.calculate(actual, YearMonth.of(2026, 10))
            assertEquals(10000L, october.incomeCents)
            assertEquals(1L, october.expenseCents)
            assertEquals(9999L, october.balanceCents)
            assertEquals(mapOf("自定义分类" to 1L), october.expensesByCategory)
            val september = MonthlySummary.calculate(actual, YearMonth.of(2026, 9))
            assertEquals(0L, september.incomeCents)
            assertEquals(1234L, september.expenseCents)
            assertEquals(-1234L, september.balanceCents)
            assertEquals(mapOf("交通" to 1234L), september.expensesByCategory)
            evidence("$phase schema=1 entries=$actual October=$october balance=9999 September=$september balance=-1234")
        } finally {
            database.close()
        }
    }

    private fun evidence(message: String) {
        InstrumentationRegistry.getInstrumentation().sendStatus(0, Bundle().apply {
            putString("stream", "\nLedgerUpgrade: $message\n")
        })
    }
}

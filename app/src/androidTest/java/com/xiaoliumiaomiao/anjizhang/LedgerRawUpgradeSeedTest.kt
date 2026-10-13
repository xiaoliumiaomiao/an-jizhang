package com.xiaoliumiaomiao.anjizhang

import android.content.Context
import android.content.ContentValues
import android.content.pm.ApplicationInfo
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.os.ParcelFileDescriptor
import androidx.test.platform.app.InstrumentationRegistry
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

/** ABI-safe against every schema1 baseline: no application domain/data classes, fields or rules. */
class LedgerRawUpgradeSeedTest {
    @Test fun seedOldVersionLedger() {
        assertSyntheticDevice()
        val (baseline, code) = baselineIdentity()
        val context = installedVersion(baseline, code)
        val path = context.getDatabasePath("ledger.db")
        val existed = path.isFile
        val expected = fixture()
        context.openOrCreateDatabase("ledger.db", Context.MODE_PRIVATE, null).use { database ->
            database.beginTransaction()
            try {
                if (!existed) {
                    assertEquals("A new raw database must have no schema version", 0, database.version)
                    database.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name NOT IN ('android_metadata','sqlite_sequence')", null).use {
                        assertEquals("Never replace an existing application table", 0, it.count)
                    }
                    database.execSQL("""CREATE TABLE entries (
                        id TEXT PRIMARY KEY NOT NULL,
                        kind TEXT NOT NULL CHECK(kind IN ('EXPENSE','INCOME')),
                        amount_cents INTEGER NOT NULL CHECK(amount_cents BETWEEN 1 AND 99999999999),
                        category TEXT NOT NULL, date TEXT NOT NULL, note TEXT NOT NULL)""")
                    database.version = 1
                }
                assertEquals("Only schema1 may be seeded", 1, database.version)
                assertEquals(legacyColumns, schemaColumns(database))
                assertEquals("Existing synthetic data must never be overwritten", 0L, count(database, "entries"))
                for (index in 0 until expected.length()) {
                    val row = expected.getJSONObject(index)
                    val values = ContentValues().apply {
                        for (field in listOf("id", "kind", "category", "date", "note")) put(field, row.getString(field))
                        put("amount_cents", row.getLong("amountCents"))
                    }
                    assertTrue("Every fixture row must actually insert", database.insertOrThrow("entries", null, values) != -1L)
                }
                database.setTransactionSuccessful()
            } finally { database.endTransaction() }
        }
        // The proof is built from a new OPEN_READONLY connection, never from expected rows.
        emitProof(readLegacyProof(context, "seed", baseline, code, 1, false).put("rawReopened", true))
    }

    companion object {
        internal const val APP = "com.xiaoliumiaomiao.anjizhang"
        internal val legacyColumns = listOf("id", "kind", "amount_cents", "category", "date", "note")

        internal fun fixture() = JSONArray("""[{"id":"01100000-0000-0000-0000-000000000001","kind":"INCOME","amountCents":10000,"category":"工资","date":"2026-10-01","note":"升级验证收入"},{"id":"01100000-0000-0000-0000-000000000002","kind":"EXPENSE","amountCents":1,"category":"自定义分类","date":"2026-10-02","note":"中文、引号\"和换行\n备注"},{"id":"01100000-0000-0000-0000-000000000003","kind":"EXPENSE","amountCents":1234,"category":"交通","date":"2026-09-30","note":"跨月支出"}]""")

        internal fun baselineIdentity(): Pair<String, Int> {
            val baseline = requireNotNull(InstrumentationRegistry.getArguments().getString("ledgerUpgradeBaseline"))
            return baseline to when (baseline) {
                "0.1.4" -> 6
                "0.1.6" -> 8
                "0.1.7" -> 9
                else -> error("ledgerUpgradeBaseline must be 0.1.4, 0.1.6 or 0.1.7")
            }
        }

        internal fun assertSyntheticDevice() {
            fun shell(command: String) = ParcelFileDescriptor.AutoCloseInputStream(
                InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(command))
                .bufferedReader().use { it.readText().trim() }
            assertEquals("Explicit synthetic-run opt-in is required", "true", InstrumentationRegistry.getArguments().getString("ledgerCi"))
            assertEquals("Only a QEMU emulator may be touched", "1", shell("getprop ro.kernel.qemu"))
            assertEquals("Only the dedicated synthetic AVD may be touched", "ci-ledger",
                shell("getprop ro.boot.qemu.avd_name").ifEmpty { shell("getprop ro.kernel.qemu.avd_name") })
        }

        @Suppress("DEPRECATION")
        internal fun installedVersion(name: String, code: Int): Context {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            assertEquals(APP, context.packageName)
            val info = context.packageManager.getPackageInfo(APP, 0)
            assertEquals("Installed versionName", name, info.versionName)
            assertEquals("Installed versionCode", code, info.versionCode)
            assertTrue("Upgrade validation requires a debuggable app",
                context.packageManager.getApplicationInfo(APP, 0).flags and ApplicationInfo.FLAG_DEBUGGABLE != 0)
            InstrumentationRegistry.getInstrumentation().sendStatus(2, Bundle().apply {
                putString("stream", "\nInstalled package=$APP versionName=${info.versionName} versionCode=${info.versionCode} debuggable=true\n")
            })
            return context
        }

        internal fun schemaColumns(database: SQLiteDatabase): List<String> = database.rawQuery("PRAGMA table_info(entries)", null).use { cursor ->
            buildList { while (cursor.moveToNext()) add(cursor.getString(cursor.getColumnIndexOrThrow("name"))) }
        }

        internal fun count(database: SQLiteDatabase, table: String): Long {
            require(table in setOf("entries", "accounts", "transfers", "adjustments", "categories"))
            return database.rawQuery("SELECT COUNT(*) FROM $table", null).use { assertTrue(it.moveToFirst()); it.getLong(0) }
        }

        internal fun readLegacyProof(context: Context, phase: String, baseline: String, code: Int,
            schema: Int, legacyOnly: Boolean): JSONObject {
            val path = context.getDatabasePath("ledger.db")
            assertTrue("Persisted ledger.db must already exist before any readback", path.isFile)
            return SQLiteDatabase.openDatabase(path.absolutePath, null, SQLiteDatabase.OPEN_READONLY).use { database ->
                assertTrue("Readback connection must be read-only", database.isReadOnly)
                assertEquals("Actual persisted schema", schema, database.version)
                val columns = legacyColumns + if (schema == 2) listOf("account_id") else emptyList()
                assertEquals("Exact persisted column order", columns, schemaColumns(database))
                val expected = fixture()
                val ids = (0 until expected.length()).map { expected.getJSONObject(it).getString("id") }
                val where = if (legacyOnly) " WHERE id IN (?,?,?)" else ""
                val args = if (legacyOnly) ids.toTypedArray() else null
                val rows = JSONArray()
                database.rawQuery("SELECT ${columns.joinToString(",")} FROM entries$where ORDER BY id", args).use { cursor ->
                    assertEquals("Exactly the three original rows must be read", 3, cursor.count)
                    while (cursor.moveToNext()) {
                        val row = JSONObject()
                        for (field in listOf("id", "kind", "category", "date", "note")) {
                            val index = cursor.getColumnIndexOrThrow(field)
                            assertEquals("Legacy $field is stored as text", Cursor.FIELD_TYPE_STRING, cursor.getType(index))
                            row.put(field, cursor.getString(index))
                        }
                        val cents = cursor.getColumnIndexOrThrow("amount_cents")
                        assertEquals("Amount remains integer cents", Cursor.FIELD_TYPE_INTEGER, cursor.getType(cents))
                        row.put("amountCents", cursor.getLong(cents))
                        if (schema == 2) {
                            assertTrue("Migration must not assign a fabricated account", cursor.isNull(cursor.getColumnIndexOrThrow("account_id")))
                            row.put("accountId", JSONObject.NULL)
                        }
                        rows.put(row)
                    }
                }
                for (index in 0 until expected.length()) {
                    val actual = rows.getJSONObject(index); val wanted = expected.getJSONObject(index)
                    for (field in listOf("id", "kind", "category", "date", "note")) assertEquals("Original $field", wanted.getString(field), actual.getString(field))
                    assertEquals("Original integer cents", wanted.getLong("amountCents"), actual.getLong("amountCents"))
                }
                assertTrue("Fixture includes an actual LF", rows.getJSONObject(1).getString("note").contains('\n'))
                assertTrue("Fixture includes an actual quote", rows.getJSONObject(1).getString("note").contains('"'))
                val scope = if (legacyOnly) "legacy-ids-only" else "entire-ledger"
                fun totals(start: String, end: String, income: Long, expense: Long): JSONObject {
                    val filter = if (legacyOnly) " AND id IN (?,?,?)" else ""
                    val selection = arrayOf(start, end) + if (legacyOnly) ids.toTypedArray() else emptyArray()
                    return database.rawQuery("SELECT COALESCE(SUM(CASE WHEN kind='INCOME' THEN amount_cents ELSE 0 END),0),COALESCE(SUM(CASE WHEN kind='EXPENSE' THEN amount_cents ELSE 0 END),0) FROM entries WHERE date>=? AND date<?$filter", selection).use {
                        assertTrue(it.moveToFirst())
                        assertEquals(income, it.getLong(0)); assertEquals(expense, it.getLong(1))
                        JSONObject().put("incomeCents", it.getLong(0)).put("expenseCents", it.getLong(1)).put("balanceCents", it.getLong(0) - it.getLong(1))
                    }
                }
                val entryCount = count(database, "entries")
                if (!legacyOnly) assertEquals("The whole ledger still contains only the original fixture", 3L, entryCount)
                else assertTrue(entryCount >= 3L)
                JSONObject().put("phase", phase).put("baselineName", baseline).put("baselineCode", code)
                    .put("schema", database.version).put("schemaColumns", JSONArray(columns)).put("rowScope", scope)
                    .put("entryCount", entryCount).put("rows", rows).put("databaseExisted", path.isFile).put("readOnlyReadback", database.isReadOnly)
                    .put("legacyTotals", JSONObject().put("scope", scope)
                        .put("october", totals("2026-10-01", "2026-11-01", 10000, 1))
                        .put("september", totals("2026-09-01", "2026-10-01", 0, 1234)))
            }
        }

        internal fun emitProof(proof: JSONObject) = InstrumentationRegistry.getInstrumentation().sendStatus(2, Bundle().apply {
            putString("ledgerUpgradeProof", proof.toString())
        })
    }
}

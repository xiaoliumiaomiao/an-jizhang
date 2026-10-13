package com.xiaoliumiaomiao.anjizhang

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.xiaoliumiaomiao.anjizhang.data.*
import com.xiaoliumiaomiao.anjizhang.domain.*
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class AccountBackupTest {
    private fun id(n: Int) = UUID(0, n.toLong()).toString()
    private val date = LocalDate.of(2026, 10, 12)
    private val a = Account(id(1), " 原账户 ", AccountKind.ASSET, AccountSubtype.OTHER)
    private val b = Account(id(2), "负债", AccountKind.LIABILITY, AccountSubtype.CREDIT_CARD, true, 7)
    private val e = Entry(id(1), EntryKind.EXPENSE, 123, " 类别 ", date, "中文\"\n备注", a.id)
    private val t = AccountTransfer(id(1), a.id, b.id, 456, date, "转账")
    private val adj = AccountAdjustment(id(1), a.id, -1000, date, -1579, "调整", AdjustmentReason.INITIAL)
    private val c = Category(id(1), EntryKind.EXPENSE, " 类别 ", 9, true)
    private val all = LedgerSnapshot(listOf(e), listOf(a, b), listOf(t), listOf(adj), listOf(c))
    private fun emptyV2() = """{"format":"an-jizhang","version":2,"currency":"CNY","entries":[],"accounts":[],"transfers":[],"adjustments":[],"categories":[]}"""
    private fun reject(raw: String, reason: String? = null) {
        val failure = assertThrows(IllegalArgumentException::class.java) { BackupCodec.decodeBackup(raw) }
        reason?.let { assertTrue("实际错误：${failure.message}", failure.message.orEmpty().contains(it)) }
    }

    @Test fun format2RoundTripsEveryFieldIncludingRawMetadataAndCrossArrayIds() {
        val decoded = BackupCodec.decodeBackup(BackupCodec.encodeSnapshot(all))
        assertEquals(2, decoded.sourceVersion); assertEquals(all, decoded.snapshot)
        assertEquals(2, BackupCodec.decodeBackup(BackupCodec.encodeSnapshot(LedgerSnapshot())).sourceVersion)
    }

    @Test fun legacyAdaptersKeepSixFieldsAndRejectLinkedEncodeAndV2Decode() {
        val legacy = e.copy(accountId = null)
        val raw = BackupCodec.encode(listOf(legacy))
        assertEquals(listOf(legacy), BackupCodec.decode(raw))
        assertEquals(DecodedBackup(LedgerSnapshot(entries = listOf(legacy)), 1), BackupCodec.decodeBackup(raw))
        assertThrows(IllegalArgumentException::class.java) { BackupCodec.encode(listOf(e)) }
        assertThrows(IllegalArgumentException::class.java) { BackupCodec.decode(emptyV2()) }
    }

    @Test fun strictVersionRootUnknownDuplicateAndEscapedKeysRemainUnified() {
        for (raw in listOf(emptyV2().replace("\"version\":2", "\"version\":2.0"),
            emptyV2().replace("\"version\":2", "\"version\":2e0"),
            emptyV2().replace("\"version\":2", "\"version\":3"), emptyV2() + "garbage",
            emptyV2().replace("\"categories\":[]", "\"categories\":[],\"extra\":1"),
            emptyV2().replace("\"format\":\"an-jizhang\"", "\"format\":\"an-jizhang\",\"\\u0066ormat\":\"an-jizhang\""),
            emptyV2().replace('"', '\''))) reject(raw)
        assertEquals(LedgerSnapshot(), BackupCodec.decodeBackup(emptyV2()).snapshot)
    }

    @Test fun malformedEntityFieldsNumbersBooleansDatesNullAndDepthReject() {
        val raw = BackupCodec.encodeSnapshot(all)
        val variants = listOf(raw.replace("\"amountCents\":123", "\"amountCents\":123.0"),
            raw.replace("\"amountCents\":123", "\"amountCents\":1e2"),
            raw.replace("\"deltaCents\":-1000", "\"deltaCents\":-9223372036854775808"),
            raw.replace("\"targetBalanceCents\":-1579", "\"targetBalanceCents\":9223372036854775808"),
            raw.replace("\"sortOrder\":9", "\"sortOrder\":2147483648"),
            raw.replace("\"archived\":true", "\"archived\":1"),
            raw.replace("\"name\":\"负债\"", "\"name\":null"),
            raw.replace("2026-10-12", "2026-02-30"),
            raw.replace("\"reason\":\"INITIAL\"", "\"reason\":\"OTHER\""),
            raw.replace("\"subtype\":\"OTHER\"", "\"subtype\":\"INVALID\""))
        variants.forEach(::reject)
        reject(emptyV2().replace("\"accounts\":[]", "\"accounts\":" + "[".repeat(9) + "0" + "]".repeat(9)), "JSON 嵌套过深")
    }

    @Test fun duplicateEntityKeysAndEveryArrayDuplicateIdReject() {
        val raw = BackupCodec.encodeSnapshot(all)
        reject(raw.replace("\"targetBalanceCents\":-1579", "\"targetBalanceCents\":-1579,\"targetBalanceCents\":-1579"), "重复字段")
        for (key in listOf("entries", "accounts", "transfers", "adjustments", "categories")) {
            val root = JSONObject(raw)
            val array = root.getJSONArray(key); array.put(array.get(0))
            reject(root.toString(), "重复 ID")
        }
    }

    @Test fun orphansSelfTransfersDuplicateNamesAndInvalidSnapshotEncodeReject() {
        val invalid = listOf(all.copy(accounts = emptyList()),
            all.copy(transfers = listOf(t.copy(fromAccountId = b.id))),
            all.copy(categories = listOf(c, c.copy(id = id(99), name = c.name.trim()))),
            all.copy(adjustments = listOf(adj.copy(deltaCents = Long.MIN_VALUE))),
            all.copy(accounts = listOf(a.copy(sortOrder = -1), b)))
        for (snapshot in invalid) assertThrows(IllegalArgumentException::class.java) { BackupCodec.encodeSnapshot(snapshot) }
        val root = JSONObject(BackupCodec.encodeSnapshot(all)); root.put("accounts", JSONArray())
        reject(root.toString())
    }

    @Test fun capacitiesArePerVersionAndUtf8BytesForBothEntrypoints() {
        val legacy = BackupCodec.encode(emptyList())
        val oversizedV1 = legacy + " ".repeat(BackupCodec.MAX_BYTES - legacy.length + 1)
        assertTrue(oversizedV1.length < BackupCodec.MAX_SNAPSHOT_BYTES)
        reject(oversizedV1, "10 MiB")
        assertThrows(IllegalArgumentException::class.java) { BackupCodec.decode(oversizedV1) }
        val v2 = emptyV2() + " ".repeat(BackupCodec.MAX_BYTES)
        assertEquals(2, BackupCodec.decodeBackup(v2).sourceVersion)
        val oversizedV2 = emptyV2() + "中".repeat(BackupCodec.MAX_SNAPSHOT_BYTES / 3)
        assertTrue(oversizedV2.length < BackupCodec.MAX_SNAPSHOT_BYTES)
        reject(oversizedV2, "32 MiB")
    }

    @Test fun format2AllArrayCapsAcceptBoundaryAndRejectNext() {
        val accounts = (1..1000).map { a.copy(id = id(it)) }
        val accountRoot = JSONObject(BackupCodec.encodeSnapshot(LedgerSnapshot(accounts = accounts)))
        assertEquals(1000, BackupCodec.decodeBackup(accountRoot.toString()).snapshot.accounts.size)
        accountRoot.getJSONArray("accounts").put(JSONObject(accountRoot.getJSONArray("accounts").getJSONObject(0).toString()).put("id", id(1001)))
        reject(accountRoot.toString(), "1000")
        assertThrows(IllegalArgumentException::class.java) { BackupCodec.encodeSnapshot(LedgerSnapshot(accounts = accounts + a.copy(id = id(1001)))) }
        val cats = (1..100000).map { c.copy(id = id(it), name = "分类$it") }
        val categoryRoot = JSONObject(BackupCodec.encodeSnapshot(LedgerSnapshot(categories = cats)))
        assertEquals(100000, BackupCodec.decodeBackup(categoryRoot.toString()).snapshot.categories.size)
        categoryRoot.getJSONArray("categories").put(JSONObject(categoryRoot.getJSONArray("categories").getJSONObject(0).toString()).put("id", id(100001)).put("name", "上限外"))
        reject(categoryRoot.toString(), "100000")
        assertThrows(IllegalArgumentException::class.java) { BackupCodec.encodeSnapshot(LedgerSnapshot(categories = cats + c.copy(id = id(100001), name = "上限外"))) }
        val transfers = (1..50000).map { t.copy(id = id(it)) }
        val adjustments = (1..50000).map { adj.copy(id = id(it), deltaCents = 0) }
        for (snapshot in listOf(LedgerSnapshot(accounts = listOf(a, b), transfers = transfers + t.copy(id = id(50001))),
            LedgerSnapshot(accounts = listOf(a, b), adjustments = adjustments + adj.copy(id = id(50001), deltaCents = 0)))) {
            assertThrows(IllegalArgumentException::class.java) { BackupCodec.encodeSnapshot(snapshot) }
        }
        val edge = LedgerSnapshot(accounts = listOf(a, b), transfers = transfers, adjustments = adjustments)
        assertEquals(edge, BackupCodec.decodeBackup(BackupCodec.encodeSnapshot(edge)).snapshot)
        for (key in listOf("transfers", "adjustments")) {
            val root = JSONObject(BackupCodec.encodeSnapshot(edge))
            val records = root.getJSONArray(key)
            records.put(JSONObject(records.getJSONObject(0).toString()).put("id", id(50001)))
            reject(root.toString(), "50000")
        }
        val entries = (1..50000).map { e.copy(id = id(it)) }
        val entrySnapshot = LedgerSnapshot(entries = entries, accounts = listOf(a))
        assertEquals(entrySnapshot, BackupCodec.decodeBackup(BackupCodec.encodeSnapshot(entrySnapshot)).snapshot)
        assertThrows(IllegalArgumentException::class.java) { BackupCodec.encodeSnapshot(entrySnapshot.copy(entries = entries + e.copy(id = id(50001)))) }
        val entryRoot = JSONObject(BackupCodec.encodeSnapshot(entrySnapshot))
        entryRoot.getJSONArray("entries").put(JSONObject(entryRoot.getJSONArray("entries").getJSONObject(0).toString()).put("id", id(50001)))
        reject(entryRoot.toString(), "50000")
    }

    @Test fun nearTenMiBLegacyFiftyThousandDistinctMultibyteCategoriesImportAndFullFormat2Roundtrip() {
        val legacy = (1..50000).map { e.copy(id = id(it), accountId = null, category = "中文类别$it", note = "") }
        val baseBytes = BackupCodec.encode(legacy).toByteArray(Charsets.UTF_8).size
        val noteChars = (BackupCodec.MAX_BYTES - baseBytes - 1) / (50000 * 3)
        val entries = legacy.map { it.copy(note = "中".repeat(noteChars)) }
        val oldBackup = BackupCodec.encode(entries)
        val oldBytes = oldBackup.toByteArray(Charsets.UTF_8).size
        assertTrue(oldBytes <= BackupCodec.MAX_BYTES)
        assertTrue("需要真实接近10MiB的合法旧输入，实际 $oldBytes", oldBytes > BackupCodec.MAX_BYTES - 150001)
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.deleteDatabase("ledger.db")
        var db = LedgerDatabase(context)
        try {
            assertEquals(50000, db.merge(BackupCodec.decodeBackup(oldBackup)).imported)
            val saved = db.snapshot()
            assertEquals(entries.toSet(), saved.entries.toSet())
            assertEquals(50012, saved.categories.size)
            val complete = BackupCodec.encodeSnapshot(saved)
            val newBytes = complete.toByteArray(Charsets.UTF_8).size
            assertTrue(newBytes > BackupCodec.MAX_BYTES); assertTrue(newBytes < BackupCodec.MAX_SNAPSHOT_BYTES)
            assertEquals(saved, BackupCodec.decodeBackup(complete).snapshot)
            db.close(); db = LedgerDatabase(context)
            assertEquals(saved, db.snapshot())
        } finally { db.close(); context.deleteDatabase("ledger.db") }
    }
}

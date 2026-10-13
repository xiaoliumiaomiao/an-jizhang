package com.xiaoliumiaomiao.anjizhang.data

import com.xiaoliumiaomiao.anjizhang.domain.*
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.time.LocalDate

object BackupCodec {
    const val MAX_BYTES: Int = 10485760
    const val MAX_SNAPSHOT_BYTES: Int = 33554432
    private const val MAX_FINANCIAL = 50000
    private val legacyRootKeys = setOf("format", "version", "currency", "entries")
    private val snapshotRootKeys = legacyRootKeys + setOf("accounts", "transfers", "adjustments", "categories")
    private val legacyEntryKeys = setOf("id", "kind", "amountCents", "category", "date", "note")
    private val accountKeys = setOf("id", "name", "kind", "subtype", "archived", "sortOrder")
    private val transferKeys = setOf("id", "fromAccountId", "toAccountId", "amountCents", "date", "note")
    private val adjustmentKeys = setOf("id", "accountId", "deltaCents", "date", "targetBalanceCents", "note", "reason")
    private val categoryKeys = setOf("id", "kind", "name", "sortOrder", "archived")

    fun encode(entries: List<Entry>): String {
        require(entries.all { it.accountId == null }) { "旧版备份不能丢失账户关联" }
        require(entries.size <= MAX_FINANCIAL) { "备份最多 50000 条" }
        AccountBalances.validateSnapshot(LedgerSnapshot(entries = entries))
        return root(1, array(entries) { entryJson(it, false) }).toString().also { checkBytes(it, MAX_BYTES, "10 MiB") }
    }

    fun encodeSnapshot(snapshot: LedgerSnapshot): String {
        validateSnapshotCapacity(snapshot)
        AccountBalances.validateSnapshot(snapshot)
        return root(2, array(snapshot.entries) { entryJson(it, true) }).apply {
            put("accounts", array(snapshot.accounts) { a -> JSONObject().apply {
                put("id", a.id); put("name", a.name); put("kind", a.kind.name); put("subtype", a.subtype.name)
                put("archived", a.archived); put("sortOrder", a.sortOrder)
            } })
            put("transfers", array(snapshot.transfers) { t -> JSONObject().apply {
                put("id", t.id); put("fromAccountId", t.fromAccountId); put("toAccountId", t.toAccountId)
                put("amountCents", t.amountCents); put("date", t.date.toString()); put("note", t.note)
            } })
            put("adjustments", array(snapshot.adjustments) { a -> JSONObject().apply {
                put("id", a.id); put("accountId", a.accountId); put("deltaCents", a.deltaCents)
                put("date", a.date.toString()); put("targetBalanceCents", a.targetBalanceCents)
                put("note", a.note); put("reason", a.reason.name)
            } })
            put("categories", array(snapshot.categories) { c -> JSONObject().apply {
                put("id", c.id); put("kind", c.kind.name); put("name", c.name)
                put("sortOrder", c.sortOrder); put("archived", c.archived)
            } })
        }.toString().also { checkBytes(it, MAX_SNAPSHOT_BYTES, "32 MiB") }
    }

    fun decode(json: String): List<Entry> {
        checkBytes(json, MAX_BYTES, "10 MiB")
        val decoded = decodeBackup(json)
        require(decoded.sourceVersion == 1) { "不支持的备份版本" }
        return decoded.snapshot.entries
    }

    fun decodeBackup(json: String): DecodedBackup {
        checkBytes(json, MAX_SNAPSHOT_BYTES, "32 MiB")
        try {
            // The exact same syntax gate serves both formats, including escaped duplicate keys.
            StrictJson(json).validate()
            val root = JSONTokener(json).nextValue() as? JSONObject ?: error("备份根须为对象")
            require(root.string("format") == "an-jizhang") { "不是安记账备份" }
            require(root.string("currency") == "CNY") { "不支持的币种" }
            val version = root.integer("version")
            require(version in 1L..2L) { "不支持的备份版本" }
            if (version == 1L) checkBytes(json, MAX_BYTES, "10 MiB")
            root.exactKeys(if (version == 1L) legacyRootKeys else snapshotRootKeys)
            val entries = root.items("entries", MAX_FINANCIAL) { item ->
                item.exactKeys(if (version == 1L) legacyEntryKeys else legacyEntryKeys + "accountId")
                Entry(item.string("id"), EntryKind.valueOf(item.string("kind")), item.integer("amountCents"),
                    item.string("category"), item.date("date"), item.string("note"),
                    if (version == 1L) null else item.nullableString("accountId"))
            }
            val snapshot = if (version == 1L) LedgerSnapshot(entries = entries) else LedgerSnapshot(
                entries = entries,
                accounts = root.items("accounts", 1000) { item ->
                    item.exactKeys(accountKeys)
                    Account(item.string("id"), item.string("name"), AccountKind.valueOf(item.string("kind")),
                        AccountSubtype.valueOf(item.string("subtype")), item.boolean("archived"), item.sortOrder())
                },
                transfers = root.items("transfers", MAX_FINANCIAL) { item ->
                    item.exactKeys(transferKeys)
                    AccountTransfer(item.string("id"), item.string("fromAccountId"), item.string("toAccountId"),
                        item.integer("amountCents"), item.date("date"), item.string("note"))
                },
                adjustments = root.items("adjustments", MAX_FINANCIAL) { item ->
                    item.exactKeys(adjustmentKeys)
                    AccountAdjustment(item.string("id"), item.string("accountId"), item.integer("deltaCents"),
                        item.date("date"), item.integer("targetBalanceCents"), item.string("note"),
                        AdjustmentReason.valueOf(item.string("reason")))
                },
                categories = root.items("categories", 100000) { item ->
                    item.exactKeys(categoryKeys)
                    Category(item.string("id"), EntryKind.valueOf(item.string("kind")), item.string("name"),
                        item.sortOrder(), item.boolean("archived"))
                },
            )
            AccountBalances.validateSnapshot(snapshot)
            return DecodedBackup(snapshot, version.toInt())
        } catch (e: Exception) {
            throw IllegalArgumentException("备份无效：${e.message ?: "无法解析"}", e)
        }
    }

    internal fun validateSnapshotCapacity(snapshot: LedgerSnapshot) {
        require(snapshot.entries.size <= MAX_FINANCIAL) { "备份最多 50000 条" }
        require(snapshot.transfers.size <= MAX_FINANCIAL && snapshot.adjustments.size <= MAX_FINANCIAL) { "每种金融记录最多 50000 条" }
        require(snapshot.accounts.size <= 1000) { "账户最多 1000 个" }
        require(snapshot.categories.size <= 100000) { "类别最多 100000 个" }
    }

    private fun root(version: Int, entries: JSONArray) = JSONObject().apply {
        put("format", "an-jizhang"); put("version", version); put("currency", "CNY"); put("entries", entries)
    }
    private fun entryJson(e: Entry, linked: Boolean) = JSONObject().apply {
        put("id", e.id); put("kind", e.kind.name); put("amountCents", e.amountCents); put("category", e.category)
        put("date", e.date.toString()); put("note", e.note)
        if (linked) put("accountId", e.accountId ?: JSONObject.NULL)
    }
    private fun <T> array(items: List<T>, encode: (T) -> JSONObject) = JSONArray().apply { items.forEach { put(encode(it)) } }
    private fun <T> JSONObject.items(key: String, max: Int, decode: (JSONObject) -> T): List<T> {
        val items = get(key) as? JSONArray ?: error("$key 须为数组")
        require(items.length() <= max) { if (key == "entries") "备份最多 50000 条" else "$key 最多 $max 条" }
        return (0 until items.length()).map { decode(items.get(it) as? JSONObject ?: error("记录须为对象")) }
    }
    private fun JSONObject.exactKeys(keys: Set<String>) { require(keys().asSequence().toSet() == keys) { "备份字段不匹配" } }
    private fun JSONObject.string(key: String): String = get(key) as? String ?: error("$key 须为字符串")
    private fun JSONObject.nullableString(key: String): String? = get(key).let {
        if (it === JSONObject.NULL) null else it as? String ?: error("$key 须为字符串或 null")
    }
    private fun JSONObject.integer(key: String): Long = when (val n = get(key)) {
        is Int -> n.toLong(); is Long -> n; else -> error("$key 须为 JSON 整数")
    }
    private fun JSONObject.sortOrder(): Int {
        val n = integer("sortOrder"); require(n in 0..Int.MAX_VALUE.toLong()) { "排序超出范围" }; return n.toInt()
    }
    private fun JSONObject.boolean(key: String): Boolean = get(key) as? Boolean ?: error("$key 须为布尔值")
    private fun JSONObject.date(key: String): LocalDate {
        val raw = string(key)
        require(Regex("[0-9]{4}-[0-9]{2}-[0-9]{2}").matches(raw)) { "日期格式无效" }
        return LocalDate.parse(raw)
    }
    private fun checkBytes(json: String, max: Int, limit: String) {
        require(json.length <= max && json.toByteArray(Charsets.UTF_8).size <= max) { "备份超过 $limit" }
    }

    /** Small syntax gate: no comments, single quotes, unquoted keys, duplicate keys or trailing data. */
    private class StrictJson(private val source: String) {
        private var at = 0
        fun validate() { value(0); whitespace(); require(at == source.length) { "JSON 尾部有多余内容" } }
        private fun whitespace() { while (at < source.length && source[at] in " \t\r\n") at++ }
        private fun take(c: Char): Boolean { whitespace(); return if (at < source.length && source[at] == c) { at++; true } else false }
        private fun expect(c: Char) { require(take(c)) { "JSON 缺少 $c" } }
        private fun value(depth: Int) {
            require(depth <= 8) { "JSON 嵌套过深" }; whitespace(); require(at < source.length) { "JSON 不完整" }
            when (source[at]) {
                '{' -> {
                    at++; val keys = mutableSetOf<String>()
                    if (take('}')) return
                    do {
                        whitespace(); val start = at; string()
                        val key = JSONTokener(source.substring(start, at)).nextValue() as String
                        require(keys.add(key)) { "JSON 对象含重复字段" }
                        expect(':'); value(depth + 1)
                    } while (take(','))
                    expect('}')
                }
                '[' -> { at++; if (take(']')) return; do { value(depth + 1) } while (take(',')); expect(']') }
                '"' -> string()
                't' -> literal("true")
                'f' -> literal("false")
                'n' -> literal("null")
                else -> {
                    val match = NUMBER.find(source, at)
                    require(match != null && match.range.first == at) { "JSON 数字无效" }
                    at = match.range.last + 1
                }
            }
        }
        private fun literal(text: String) { require(source.startsWith(text, at)) { "JSON 值无效" }; at += text.length }
        private fun string() {
            expect('"')
            while (at < source.length) {
                val c = source[at++]
                if (c == '"') return
                require(c.code >= 32) { "JSON 字符串含控制字符" }
                if (c == '\\') {
                    require(at < source.length) { "JSON 转义不完整" }
                    when (source[at++]) {
                        '"', '\\', '/', 'b', 'f', 'n', 'r', 't' -> Unit
                        'u' -> { require(at + 4 <= source.length && source.substring(at, at + 4).all { it in "0123456789abcdefABCDEF" }) { "JSON Unicode 转义无效" }; at += 4 }
                        else -> error("JSON 转义无效")
                    }
                }
            }
            error("JSON 字符串未结束")
        }
        companion object { private val NUMBER = Regex("-?(?:0|[1-9][0-9]*)(?:\\.[0-9]+)?(?:[eE][+-]?[0-9]+)?") }
    }
}

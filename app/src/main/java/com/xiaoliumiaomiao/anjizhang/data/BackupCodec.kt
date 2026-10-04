package com.xiaoliumiaomiao.anjizhang.data

import com.xiaoliumiaomiao.anjizhang.domain.Entry
import com.xiaoliumiaomiao.anjizhang.domain.EntryKind
import com.xiaoliumiaomiao.anjizhang.domain.LedgerRules
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.time.LocalDate

object BackupCodec {
    const val MAX_BYTES: Int = 10485760
    private const val MAX_ENTRIES = 50000
    private val rootKeys = setOf("format", "version", "currency", "entries")
    private val entryKeys = setOf("id", "kind", "amountCents", "category", "date", "note")

    fun encode(entries: List<Entry>): String {
        validateBatch(entries)
        val items = JSONArray()
        entries.forEach { entry ->
            items.put(JSONObject().apply {
                put("id", entry.id); put("kind", entry.kind.name); put("amountCents", entry.amountCents)
                put("category", entry.category); put("date", entry.date.toString()); put("note", entry.note)
            })
        }
        return JSONObject().apply {
            put("format", "an-jizhang"); put("version", 1); put("currency", "CNY"); put("entries", items)
        }.toString().also { require(it.toByteArray(Charsets.UTF_8).size <= MAX_BYTES) { "备份超过 10 MiB" } }
    }

    fun decode(json: String): List<Entry> {
        require(json.length <= MAX_BYTES && json.toByteArray(Charsets.UTF_8).size <= MAX_BYTES) { "备份超过 10 MiB" }
        try {
            // Android org.json accepts non-JSON extensions; validate the grammar before parsing.
            StrictJson(json).validate()
            val root = JSONTokener(json).nextValue() as? JSONObject ?: error("备份根须为对象")
            require(root.keys().asSequence().toSet() == rootKeys) { "备份字段不匹配" }
            require(root.string("format") == "an-jizhang") { "不是安记账备份" }
            require(root.integer("version") == 1L) { "不支持的备份版本" }
            require(root.string("currency") == "CNY") { "不支持的币种" }
            val items = root.get("entries") as? JSONArray ?: error("entries 须为数组")
            require(items.length() <= MAX_ENTRIES) { "备份最多 50000 条" }
            return buildList {
                for (i in 0 until items.length()) {
                    val item = items.get(i) as? JSONObject ?: error("记录须为对象")
                    require(item.keys().asSequence().toSet() == entryKeys) { "记录字段不匹配" }
                    val date = item.string("date")
                    require(Regex("[0-9]{4}-[0-9]{2}-[0-9]{2}").matches(date)) { "日期格式无效" }
                    add(Entry(item.string("id"), EntryKind.valueOf(item.string("kind")), item.integer("amountCents"),
                        item.string("category"), LocalDate.parse(date), item.string("note")))
                }
            }.also(::validateBatch)
        } catch (e: Exception) {
            throw IllegalArgumentException("备份无效：${e.message ?: "无法解析"}", e)
        }
    }

    private fun validateBatch(entries: List<Entry>) {
        require(entries.size <= MAX_ENTRIES) { "备份最多 50000 条" }
        entries.forEach(LedgerRules::validate)
        require(entries.map { it.id }.toSet().size == entries.size) { "备份含重复 ID" }
    }

    private fun JSONObject.string(key: String): String = get(key) as? String ?: error("$key 须为字符串")
    private fun JSONObject.integer(key: String): Long = when (val value = get(key)) {
        is Int -> value.toLong()
        is Long -> value
        else -> error("$key 须为 JSON 整数")
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

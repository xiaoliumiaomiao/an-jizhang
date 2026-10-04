package com.xiaoliumiaomiao.anjizhang
import com.xiaoliumiaomiao.anjizhang.domain.*
import com.xiaoliumiaomiao.anjizhang.data.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
@RunWith(RobolectricTestRunner::class) @Config(sdk=[28])
class BackupCodecTest {
    private val entry=Entry("00000000-0000-0000-0000-000000000001",EntryKind.EXPENSE,1234,"餐饮",LocalDate.of(2026,10,1),"中文备注")
    private fun json(amount:String="1234",version:String="1",id:String=entry.id)= """{"format":"an-jizhang","version":$version,"currency":"CNY","entries":[{"id":"$id","kind":"EXPENSE","amountCents":$amount,"category":"餐饮","date":"2026-10-01","note":"中文备注"}]}"""
    @Test fun roundtrip(){assertEquals(listOf(entry),BackupCodec.decode(BackupCodec.encode(listOf(entry))));assertEquals(emptyList<Entry>(),BackupCodec.decode(BackupCodec.encode(emptyList())))}
    @Test fun rejectsNonIntegerAndOutOfRangeMoney(){ for(x in listOf("12.34","1234.0","\"1234\"","true","null","0","-1","100000000000","9223372036854775808")) assertThrows(x,IllegalArgumentException::class.java){BackupCodec.decode(json(amount=x))} }
    @Test fun rejectsMalformedAndIncompatible(){for(x in listOf("{", "[]",json(version="2"),json(version="1.0"),json(version="\"1\""),json(id="bad"),json().replace("CNY","USD"),json().replace("2026-10-01","2026-02-30"),json().replace("\"note\":\"中文备注\"","\"note\":10"),json().replace("\"kind\":\"EXPENSE\"","\"kind\":false"),json()+"garbage",json().replace("1234","01"),json().replace("1234","1e3"),json().replace("\"format\"", "format"),json().replace('"',39.toChar()))) assertThrows(x,IllegalArgumentException::class.java){BackupCodec.decode(x)}}
    @Test fun rejectsDuplicateIds(){assertThrows(IllegalArgumentException::class.java){BackupCodec.encode(listOf(entry,entry))};val encoded=json();val item=encoded.substringAfter("[").substringBeforeLast("]");assertThrows(IllegalArgumentException::class.java){BackupCodec.decode(encoded.replace(item,"$item,$item"))}}
    @Test fun rejectsOversizedUtf8(){assertThrows(IllegalArgumentException::class.java){BackupCodec.decode("中".repeat(BackupCodec.MAX_BYTES/3+1))}}
    @Test fun rejectsTooManyEntries(){val records=(1..50001).map{entry.copy(id=java.util.UUID(0,it.toLong()).toString())};assertThrows(IllegalArgumentException::class.java){BackupCodec.encode(records)}}

    @Test fun rejectsDuplicateRootFields() {
        val duplicate = json().replace("\"format\":\"an-jizhang\"", "\"format\":\"an-jizhang\",\"format\":\"an-jizhang\"")
        assertRejectedBecause(duplicate, "JSON 对象含重复字段")
    }

    @Test fun rejectsDuplicateEntryFields() {
        val duplicate = json().replace("\"note\":\"中文备注\"", "\"note\":\"中文备注\",\"note\":\"中文备注\"")
        assertRejectedBecause(duplicate, "JSON 对象含重复字段")
    }

    @Test fun rejectsEscapedDuplicateFields() {
        val rootDuplicate = json().replace("\"format\":\"an-jizhang\"", "\"format\":\"an-jizhang\",\"\\u0066ormat\":\"an-jizhang\"")
        val entryDuplicate = json().replace("\"note\":\"中文备注\"", "\"note\":\"中文备注\",\"\\u006eote\":\"中文备注\"")
        for (duplicate in listOf(rootDuplicate, entryDuplicate)) {
            assertRejectedBecause(duplicate, "JSON 对象含重复字段")
        }
    }

    @Test fun rejectsExcessiveNestingAtSyntaxGate() {
        fun nestedNote(layers: Int) = json().replace("\"note\":\"中文备注\"", "\"note\":" + "[".repeat(layers) + "\"中文备注\"" + "]".repeat(layers))
        val control = assertRejectedBecause(nestedNote(5), "note 须为字符串")
        assertFalse(control.message.orEmpty().contains("JSON 嵌套过深"))
        assertRejectedBecause(nestedNote(6), "JSON 嵌套过深")
    }

    @Test fun acceptsMaximumEntryCount() {
        val expected = records(50000)
        val backup = recordsJson(expected)
        assertTrue("条数边界输入须小于字节上限", backup.toByteArray(Charsets.UTF_8).size < BackupCodec.MAX_BYTES)
        assertEquals(expected, BackupCodec.decode(backup))
    }

    @Test fun rejectsDecodeEntryCountAboveLimit() {
        val backup = recordsJson(records(50001))
        assertTrue("条数边界输入须小于字节上限", backup.toByteArray(Charsets.UTF_8).size < BackupCodec.MAX_BYTES)
        assertRejectedBecause(backup, "备份最多 50000 条")
    }

    private fun assertRejectedBecause(backup: String, reason: String): IllegalArgumentException {
        val failure = assertThrows(IllegalArgumentException::class.java) { BackupCodec.decode(backup) }
        assertTrue("预期拒绝原因包含 $reason，实际为 ${failure.message}", failure.message.orEmpty().contains(reason))
        return failure
    }

    private fun records(count: Int): List<Entry> = (1..count).map {
        entry.copy(id = java.util.UUID(0, it.toLong()).toString())
    }

    private fun recordsJson(records: List<Entry>): String {
        val items = records.joinToString(",") { record ->
            json(id = record.id).substringAfter("[").substringBeforeLast("]")
        }
        return """{"format":"an-jizhang","version":1,"currency":"CNY","entries":[$items]}"""
    }
}

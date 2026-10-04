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
}

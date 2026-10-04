package com.xiaoliumiaomiao.anjizhang
import androidx.test.core.app.ApplicationProvider
import android.content.Context
import com.xiaoliumiaomiao.anjizhang.domain.*
import com.xiaoliumiaomiao.anjizhang.data.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
@RunWith(RobolectricTestRunner::class) @Config(sdk=[28])
class LedgerDatabaseTest {
    private lateinit var context:Context
    private lateinit var db:LedgerDatabase
    private val a=Entry("00000000-0000-0000-0000-000000000001",EntryKind.INCOME,10000,"工资",LocalDate.of(2026,10,1))
    private val b=a.copy(id="00000000-0000-0000-0000-000000000002",kind=EntryKind.EXPENSE,amountCents=1234,category="餐饮")
    @Before fun setup(){context=ApplicationProvider.getApplicationContext();context.deleteDatabase("ledger.db");db=LedgerDatabase(context)}
    @After fun teardown(){db.close();context.deleteDatabase("ledger.db")}
    @Test fun insertEditReopenDelete(){ db.save(a);db.save(b);assertEquals(listOf(a,b),db.listEntries()); val edit=a.copy(amountCents=5,kind=EntryKind.EXPENSE);db.save(edit);db.close();db=LedgerDatabase(context);assertEquals(listOf(edit,b),db.listEntries());assertEquals(1239,MonthlySummary.calculate(db.listEntries(),java.time.YearMonth.of(2026,10)).expenseCents);db.delete(a.id);assertEquals(listOf(b),db.listEntries()) }
    @Test fun roundtripAndRepeatedMerge(){val encoded=BackupCodec.encode(listOf(a,b));assertEquals(ImportResult(2,0),db.merge(BackupCodec.decode(encoded)));assertEquals(ImportResult(0,2),db.merge(BackupCodec.decode(encoded)));assertEquals(listOf(a,b),db.listEntries())}
    @Test fun conflictAtEndRollsBackWholeBatch(){db.save(a);assertThrows(IllegalArgumentException::class.java){db.merge(listOf(b,a.copy(note="冲突")))};assertEquals(listOf(a),db.listEntries())}
    @Test fun invalidBatchAndDuplicateDoNotWrite(){db.save(a);assertThrows(IllegalArgumentException::class.java){db.merge(listOf(b,a.copy(amountCents=0)))};assertThrows(IllegalArgumentException::class.java){db.merge(listOf(b,b))};assertEquals(listOf(a),db.listEntries())}
    @Test fun invalidSaveDoesNotWrite(){assertThrows(IllegalArgumentException::class.java){db.save(a.copy(id="bad"))};assertTrue(db.listEntries().isEmpty())}
}

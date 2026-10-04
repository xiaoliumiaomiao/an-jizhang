package com.xiaoliumiaomiao.anjizhang.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.xiaoliumiaomiao.anjizhang.domain.Entry
import com.xiaoliumiaomiao.anjizhang.domain.EntryKind
import com.xiaoliumiaomiao.anjizhang.domain.LedgerRules
import java.time.LocalDate

data class ImportResult(val imported: Int, val skipped: Int)

class LedgerDatabase(context: Context) : SQLiteOpenHelper(context.applicationContext, "ledger.db", null, 1) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""CREATE TABLE entries (
            id TEXT PRIMARY KEY NOT NULL,
            kind TEXT NOT NULL CHECK(kind IN ('EXPENSE','INCOME')),
            amount_cents INTEGER NOT NULL CHECK(amount_cents BETWEEN 1 AND 99999999999),
            category TEXT NOT NULL,
            date TEXT NOT NULL,
            note TEXT NOT NULL
        )""")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        error("数据库从 $oldVersion 到 $newVersion 需要显式迁移，现有数据未删除")
    }

    fun listEntries(): List<Entry> = readableDatabase.query(
        "entries", null, null, null, null, null, "date DESC, id ASC"
    ).use { cursor -> buildList { while (cursor.moveToNext()) add(cursor.toEntry()) } }

    fun save(entry: Entry) {
        LedgerRules.validate(entry)
        check(writableDatabase.insertWithOnConflict("entries", null, entry.values(), SQLiteDatabase.CONFLICT_REPLACE) != -1L) { "保存失败" }
    }

    fun delete(id: String) {
        writableDatabase.delete("entries", "id = ?", arrayOf(id))
    }

    fun merge(entries: List<Entry>): ImportResult {
        require(entries.size <= 50000) { "备份最多 50000 条" }
        entries.forEach(LedgerRules::validate)
        require(entries.map { it.id }.toSet().size == entries.size) { "备份含重复 ID" }
        val db = writableDatabase
        var imported = 0
        var skipped = 0
        db.beginTransaction()
        try {
            for (entry in entries) {
                val old = db.query("entries", null, "id = ?", arrayOf(entry.id), null, null, null).use {
                    if (it.moveToFirst()) it.toEntry() else null
                }
                if (old == null) {
                    db.insertOrThrow("entries", null, entry.values())
                    imported++
                } else {
                    require(old == entry) { "记录 ${entry.id} 内容冲突，整批未导入" }
                    skipped++
                }
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        return ImportResult(imported, skipped)
    }

    private fun Entry.values() = ContentValues().apply {
        put("id", id); put("kind", kind.name); put("amount_cents", amountCents)
        put("category", category); put("date", date.toString()); put("note", note)
    }

    private fun Cursor.toEntry() = Entry(
        getString(getColumnIndexOrThrow("id")),
        EntryKind.valueOf(getString(getColumnIndexOrThrow("kind"))),
        getLong(getColumnIndexOrThrow("amount_cents")),
        getString(getColumnIndexOrThrow("category")),
        LocalDate.parse(getString(getColumnIndexOrThrow("date"))),
        getString(getColumnIndexOrThrow("note"))
    ).also(LedgerRules::validate)
}

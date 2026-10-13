package com.xiaoliumiaomiao.anjizhang.data

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import com.xiaoliumiaomiao.anjizhang.domain.*
import java.time.LocalDate

/** SQL mapping only. State rules and transaction ownership live in LedgerDatabase. */
internal object LedgerSnapshotStorage {
    fun createParentAndEventTables(db: SQLiteDatabase) {
        db.execSQL("""CREATE TABLE accounts (
            id TEXT PRIMARY KEY NOT NULL, name TEXT NOT NULL,
            kind TEXT NOT NULL CHECK(kind IN ('ASSET','LIABILITY')),
            subtype TEXT NOT NULL CHECK(subtype IN ('CASH','BANK','FUND','CREDIT_CARD','LOAN','OTHER')),
            archived INTEGER NOT NULL CHECK(archived IN (0,1)), sort_order INTEGER NOT NULL CHECK(sort_order >= 0))""")
        db.execSQL("""CREATE TABLE transfers (
            id TEXT PRIMARY KEY NOT NULL,
            from_account_id TEXT NOT NULL REFERENCES accounts(id),
            to_account_id TEXT NOT NULL REFERENCES accounts(id),
            amount_cents INTEGER NOT NULL CHECK(amount_cents BETWEEN 1 AND 99999999999),
            date TEXT NOT NULL, note TEXT NOT NULL, CHECK(from_account_id != to_account_id))""")
        db.execSQL("""CREATE TABLE adjustments (
            id TEXT PRIMARY KEY NOT NULL, account_id TEXT NOT NULL REFERENCES accounts(id),
            delta_cents INTEGER NOT NULL CHECK(delta_cents >= -9223372036854775807),
            date TEXT NOT NULL, target_balance_cents INTEGER NOT NULL CHECK(target_balance_cents >= -9223372036854775807),
            note TEXT NOT NULL, reason TEXT NOT NULL CHECK(reason IN ('INITIAL','CALIBRATION')))""")
        db.execSQL("""CREATE TABLE categories (
            id TEXT PRIMARY KEY NOT NULL, kind TEXT NOT NULL CHECK(kind IN ('EXPENSE','INCOME')),
            name TEXT NOT NULL, sort_order INTEGER NOT NULL CHECK(sort_order >= 0),
            archived INTEGER NOT NULL CHECK(archived IN (0,1)), UNIQUE(kind,name))""")
    }

    fun createEntries(db: SQLiteDatabase) {
        db.execSQL("""CREATE TABLE entries (
            id TEXT PRIMARY KEY NOT NULL,
            kind TEXT NOT NULL CHECK(kind IN ('EXPENSE','INCOME')),
            amount_cents INTEGER NOT NULL CHECK(amount_cents BETWEEN 1 AND 99999999999),
            category TEXT NOT NULL, date TEXT NOT NULL, note TEXT NOT NULL,
            account_id TEXT REFERENCES accounts(id))""")
    }

    fun entries(db: SQLiteDatabase): List<Entry> = rows(db, "entries", "date DESC, id ASC") {
        Entry(s("id"), EntryKind.valueOf(s("kind")), l("amount_cents"), s("category"),
            date("date"), s("note"), nullable("account_id"))
    }

    fun read(db: SQLiteDatabase) = LedgerSnapshot(
        entries = entries(db),
        accounts = rows(db, "accounts", "sort_order ASC, id ASC") {
            Account(s("id"), s("name"), AccountKind.valueOf(s("kind")), AccountSubtype.valueOf(s("subtype")),
                bool("archived"), int("sort_order"))
        },
        transfers = rows(db, "transfers", "date DESC, id ASC") {
            AccountTransfer(s("id"), s("from_account_id"), s("to_account_id"), l("amount_cents"), date("date"), s("note"))
        },
        adjustments = rows(db, "adjustments", "date DESC, id ASC") {
            AccountAdjustment(s("id"), s("account_id"), l("delta_cents"), date("date"), l("target_balance_cents"),
                s("note"), AdjustmentReason.valueOf(s("reason")))
        },
        categories = rows(db, "categories", "kind ASC, sort_order ASC, id ASC") {
            Category(s("id"), EntryKind.valueOf(s("kind")), s("name"), int("sort_order"), bool("archived"))
        },
    )

    fun put(db: SQLiteDatabase, e: Entry, exists: Boolean = false) = write(db, "entries", e.id, exists, ContentValues().apply {
        put("id", e.id); put("kind", e.kind.name); put("amount_cents", e.amountCents); put("category", e.category)
        put("date", e.date.toString()); put("note", e.note); put("account_id", e.accountId)
    })
    fun put(db: SQLiteDatabase, a: Account, exists: Boolean = false) = write(db, "accounts", a.id, exists, ContentValues().apply {
        put("id", a.id); put("name", a.name); put("kind", a.kind.name); put("subtype", a.subtype.name)
        put("archived", a.archived); put("sort_order", a.sortOrder)
    })
    fun put(db: SQLiteDatabase, t: AccountTransfer, exists: Boolean = false) = write(db, "transfers", t.id, exists, ContentValues().apply {
        put("id", t.id); put("from_account_id", t.fromAccountId); put("to_account_id", t.toAccountId)
        put("amount_cents", t.amountCents); put("date", t.date.toString()); put("note", t.note)
    })
    fun put(db: SQLiteDatabase, a: AccountAdjustment, exists: Boolean = false) = write(db, "adjustments", a.id, exists, ContentValues().apply {
        put("id", a.id); put("account_id", a.accountId); put("delta_cents", a.deltaCents); put("date", a.date.toString())
        put("target_balance_cents", a.targetBalanceCents); put("note", a.note); put("reason", a.reason.name)
    })
    fun put(db: SQLiteDatabase, c: Category, exists: Boolean = false) = write(db, "categories", c.id, exists, ContentValues().apply {
        put("id", c.id); put("kind", c.kind.name); put("name", c.name); put("sort_order", c.sortOrder); put("archived", c.archived)
    })

    private fun write(db: SQLiteDatabase, table: String, id: String, exists: Boolean, values: ContentValues) {
        if (exists) check(db.update(table, values, "id = ?", arrayOf(id)) == 1) { "保存失败" }
        else db.insertOrThrow(table, null, values)
    }
    private fun <T> rows(db: SQLiteDatabase, table: String, order: String, read: Cursor.() -> T): List<T> =
        db.query(table, null, null, null, null, null, order).use { c -> buildList { while (c.moveToNext()) add(c.read()) } }
    private fun Cursor.s(key: String): String = getString(getColumnIndexOrThrow(key))
    private fun Cursor.nullable(key: String): String? = getColumnIndexOrThrow(key).let { if (isNull(it)) null else getString(it) }
    private fun Cursor.l(key: String) = getLong(getColumnIndexOrThrow(key))
    private fun Cursor.int(key: String): Int {
        val n = l(key); require(n in 0..Int.MAX_VALUE.toLong()) { "排序超出范围" }; return n.toInt()
    }
    private fun Cursor.bool(key: String): Boolean { val n = l(key); require(n in 0..1) { "布尔值无效" }; return n == 1L }
    private fun Cursor.date(key: String) = LocalDate.parse(s(key))
}

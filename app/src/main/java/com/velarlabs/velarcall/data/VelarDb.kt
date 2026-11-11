package com.velarlabs.velarcall.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

enum class ListType { ALLOW, BLOCK, ONLINE }

data class NumberEntry(val key: String, val type: ListType, val label: String)

data class BlockedCall(val id: Long, val number: String, val reason: String, val at: Long)

/** Banco local simples (SQLite puro, sem geração de código). */
class VelarDb private constructor(context: Context) :
    SQLiteOpenHelper(context.applicationContext, "velarcall.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE numbers (num TEXT NOT NULL, type TEXT NOT NULL, label TEXT NOT NULL DEFAULT '', " +
                "PRIMARY KEY (num, type))"
        )
        db.execSQL(
            "CREATE TABLE blocked (id INTEGER PRIMARY KEY AUTOINCREMENT, num TEXT NOT NULL, " +
                "reason TEXT NOT NULL, at INTEGER NOT NULL)"
        )
        db.execSQL("CREATE INDEX idx_blocked_at ON blocked(at)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit

    // ---------- listas ----------

    fun find(key: String): List<NumberEntry> {
        if (key.isEmpty()) return emptyList()
        return readableDatabase.rawQuery(
            "SELECT num, type, label FROM numbers WHERE num = ?", arrayOf(key)
        ).use { c ->
            buildList {
                while (c.moveToNext()) add(NumberEntry(c.getString(0), ListType.valueOf(c.getString(1)), c.getString(2)))
            }
        }
    }

    fun list(type: ListType): List<NumberEntry> =
        readableDatabase.rawQuery(
            "SELECT num, type, label FROM numbers WHERE type = ? ORDER BY num", arrayOf(type.name)
        ).use { c ->
            buildList {
                while (c.moveToNext()) add(NumberEntry(c.getString(0), type, c.getString(2)))
            }
        }

    fun count(type: ListType): Int =
        readableDatabase.rawQuery("SELECT COUNT(*) FROM numbers WHERE type = ?", arrayOf(type.name))
            .use { c -> if (c.moveToFirst()) c.getInt(0) else 0 }

    fun put(key: String, type: ListType, label: String) {
        if (key.isEmpty()) return
        val db = writableDatabase
        db.beginTransaction()
        try {
            // Um número manual não pode estar ao mesmo tempo em "permitidos" e "bloqueados".
            if (type == ListType.ALLOW) db.delete("numbers", "num = ? AND type = ?", arrayOf(key, ListType.BLOCK.name))
            if (type == ListType.BLOCK) db.delete("numbers", "num = ? AND type = ?", arrayOf(key, ListType.ALLOW.name))
            db.insertWithOnConflict("numbers", null, ContentValues().apply {
                put("num", key); put("type", type.name); put("label", label)
            }, SQLiteDatabase.CONFLICT_REPLACE)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun remove(key: String, type: ListType) {
        writableDatabase.delete("numbers", "num = ? AND type = ?", arrayOf(key, type.name))
    }

    /** Substitui toda a lista online de uma vez. */
    fun replaceOnline(entries: List<Pair<String, String>>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete("numbers", "type = ?", arrayOf(ListType.ONLINE.name))
            val st = db.compileStatement("INSERT OR REPLACE INTO numbers (num, type, label) VALUES (?, 'ONLINE', ?)")
            for ((k, label) in entries) {
                if (k.isEmpty()) continue
                st.bindString(1, k); st.bindString(2, label)
                st.executeInsert(); st.clearBindings()
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    // ---------- histórico ----------

    fun logBlocked(number: String, reason: String, at: Long = System.currentTimeMillis()) {
        writableDatabase.insert("blocked", null, ContentValues().apply {
            put("num", number); put("reason", reason); put("at", at)
        })
        // Mantém só os 1000 registros mais recentes.
        writableDatabase.execSQL(
            "DELETE FROM blocked WHERE id NOT IN (SELECT id FROM blocked ORDER BY at DESC LIMIT 1000)"
        )
    }

    fun blocked(limit: Int = 200): List<BlockedCall> =
        readableDatabase.rawQuery(
            "SELECT id, num, reason, at FROM blocked ORDER BY at DESC LIMIT $limit", null
        ).use { c ->
            buildList {
                while (c.moveToNext()) add(BlockedCall(c.getLong(0), c.getString(1), c.getString(2), c.getLong(3)))
            }
        }

    fun blockedSince(since: Long): Int =
        readableDatabase.rawQuery("SELECT COUNT(*) FROM blocked WHERE at >= ?", arrayOf(since.toString()))
            .use { c -> if (c.moveToFirst()) c.getInt(0) else 0 }

    fun clearHistory() {
        writableDatabase.delete("blocked", null, null)
    }

    companion object {
        @Volatile private var instance: VelarDb? = null
        fun get(context: Context): VelarDb =
            instance ?: synchronized(this) { instance ?: VelarDb(context).also { instance = it } }
    }
}

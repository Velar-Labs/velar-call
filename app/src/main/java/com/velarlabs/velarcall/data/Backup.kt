package com.velarlabs.velarcall.data

import org.json.JSONArray
import org.json.JSONObject

/** Exporta e importa as listas liberada/bloqueada e os prefixos do usuário, em JSON. */
object Backup {

    data class ImportResult(val allow: Int, val block: Int, val prefixes: Int)

    fun export(db: VelarDb, settings: Settings): String {
        val obj = JSONObject()
        obj.put("app", "VelarCall")
        obj.put("version", 1)
        obj.put("exportedAt", System.currentTimeMillis())
        obj.put("allow", listJson(db.list(ListType.ALLOW)))
        obj.put("block", listJson(db.list(ListType.BLOCK)))
        obj.put("prefixes", JSONArray(settings.blockedPrefixes.toList()))
        return obj.toString(2)
    }

    private fun listJson(entries: List<NumberEntry>): JSONArray {
        val arr = JSONArray()
        for (e in entries) arr.put(JSONObject().put("n", e.key).put("label", e.label))
        return arr
    }

    /** Mescla o conteúdo do backup com o que já existe (não apaga o atual). */
    fun import(db: VelarDb, settings: Settings, text: String): ImportResult {
        val obj = JSONObject(text)
        val a = putAll(db, obj.optJSONArray("allow"), ListType.ALLOW)
        val b = putAll(db, obj.optJSONArray("block"), ListType.BLOCK)
        val prefixes = settings.blockedPrefixes.toMutableSet()
        obj.optJSONArray("prefixes")?.let { arr ->
            for (i in 0 until arr.length()) PhoneNumbers.digits(arr.optString(i)).takeIf { it.length >= 3 }?.let(prefixes::add)
        }
        settings.blockedPrefixes = prefixes
        return ImportResult(a, b, prefixes.size)
    }

    private fun putAll(db: VelarDb, arr: JSONArray?, type: ListType): Int {
        if (arr == null) return 0
        var n = 0
        for (i in 0 until arr.length()) {
            val item = arr.opt(i)
            val key: String
            val label: String
            if (item is JSONObject) {
                key = PhoneNumbers.key(item.optString("n", item.optString("number")))
                label = item.optString("label", "")
            } else {
                key = PhoneNumbers.key(item?.toString()); label = ""
            }
            if (key.length >= 8) { db.put(key, type, label); n++ }
        }
        return n
    }
}

package com.velarlabs.velarcall.data

import android.content.Context
import android.provider.ContactsContract.CommonDataKinds.Phone

data class Contact(
    val id: Long,
    val name: String,
    /** Números como estão na agenda. */
    val numbers: List<String>,
) {
    val keys: List<String> get() = numbers.map { PhoneNumbers.key(it) }.filter { it.isNotEmpty() }.distinct()
}

/** Lê os contatos do aparelho que têm telefone. Exige READ_CONTACTS. */
object ContactsRepo {

    fun load(context: Context): List<Contact> {
        val byId = LinkedHashMap<Long, Pair<String, MutableList<String>>>()
        try {
            context.contentResolver.query(
                Phone.CONTENT_URI,
                arrayOf(Phone.CONTACT_ID, Phone.DISPLAY_NAME_PRIMARY, Phone.NUMBER),
                null, null,
                "${Phone.DISPLAY_NAME_PRIMARY} COLLATE LOCALIZED ASC"
            )?.use { c ->
                while (c.moveToNext()) {
                    val id = c.getLong(0)
                    val name = c.getString(1) ?: ""
                    val number = c.getString(2) ?: continue
                    val entry = byId.getOrPut(id) { name to mutableListOf() }
                    if (entry.second.none { PhoneNumbers.key(it) == PhoneNumbers.key(number) }) entry.second.add(number)
                }
            }
        } catch (e: SecurityException) {
            return emptyList()
        }
        return byId.map { (id, v) -> Contact(id, v.first.ifBlank { PhoneNumbers.pretty(v.second.first()) }, v.second) }
    }
}

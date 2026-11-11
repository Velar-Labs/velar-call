package com.velarlabs.velarcall.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract

sealed class Decision {
    data class Allow(val reason: String) : Decision()
    data class Block(val reason: String) : Decision()
}

/**
 * Regras de bloqueio, em ordem de prioridade:
 *  1. Proteção desligada            -> permite
 *  2. Número oculto                 -> bloqueia (se ativado)
 *  3. Origem falsificada (operadora) -> bloqueia (se ativado)
 *  4. Lista permitida do app        -> permite
 *  5. Lista bloqueada manual        -> bloqueia (vale até para contatos)
 *  6. Contato da agenda             -> permite
 *  7. Lista online                  -> bloqueia (se ativado)
 *  8. Prefixo de telemarketing      -> bloqueia (se ativado)
 *  9. Mesmo desconhecido de novo em 3 min -> permite (se ativado)
 * 10. Desconhecido                  -> bloqueia (se ativado e com acesso à agenda)
 */
class CallDecider(private val context: Context) {

    private val settings = Settings(context)
    private val db = VelarDb.get(context)

    fun hasContactsPermission(): Boolean =
        context.checkSelfPermission(Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED

    fun contactName(raw: String): String? {
        if (raw.isBlank() || !hasContactsPermission()) return null
        return try {
            val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(raw))
            context.contentResolver.query(
                uri, arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME), null, null, null
            )?.use { c -> if (c.moveToFirst()) c.getString(0) ?: "" else null }
        } catch (e: Exception) {
            null
        }
    }

    fun decide(
        raw: String?,
        spoofed: Boolean = false,
        ignoreMasterSwitch: Boolean = false,
        checkRepeat: Boolean = false,
    ): Decision {
        if (!settings.protectionOn && !ignoreMasterSwitch) return Decision.Allow("Proteção desligada")

        val key = PhoneNumbers.key(raw)
        if (key.isEmpty()) {
            return if (settings.blockHidden) Decision.Block("Número oculto")
            else Decision.Allow("Número oculto (permitido)")
        }
        if (spoofed && settings.blockSpoofed) return Decision.Block("Origem falsificada")

        val entries = db.find(key)
        entries.firstOrNull { it.type == ListType.ALLOW }?.let {
            return Decision.Allow(if (it.label.isNotBlank()) "Permitido: ${it.label}" else "Lista permitida")
        }
        entries.firstOrNull { it.type == ListType.BLOCK }?.let {
            return Decision.Block(if (it.label.isNotBlank()) "Bloqueado: ${it.label}" else "Bloqueado por você")
        }
        contactName(raw!!)?.let { name ->
            return Decision.Allow(if (name.isNotBlank()) "Contato: $name" else "Contato da agenda")
        }
        if (settings.useOnlineList) {
            entries.firstOrNull { it.type == ListType.ONLINE }?.let {
                return Decision.Block(if (it.label.isNotBlank()) "Lista online: ${it.label}" else "Lista online")
            }
            settings.onlinePrefixes.firstOrNull { p -> p.isNotBlank() && key.startsWith(p) }?.let {
                return Decision.Block("Prefixo suspeito $it")
            }
        }
        if (settings.blockTelemarketingPrefix && key.startsWith("0303")) {
            return Decision.Block("Telemarketing (0303)")
        }
        // Só suaviza o bloqueio de desconhecidos; listas e 0303 continuam valendo.
        if (checkRepeat && settings.allowRepeatCaller) {
            val since = System.currentTimeMillis() - Settings.REPEAT_WINDOW_MS
            if (db.blockedNumbersSince(since).any { PhoneNumbers.key(it) == key }) {
                return Decision.Allow("Ligou de novo em poucos minutos")
            }
        }
        if (settings.blockUnknown) {
            // Sem acesso à agenda não dá para saber quem é contato: não bloqueia para não barrar conhecidos.
            return if (hasContactsPermission()) Decision.Block("Fora da agenda")
            else Decision.Allow("Desconhecido (sem acesso à agenda)")
        }
        return Decision.Allow("Desconhecido")
    }
}

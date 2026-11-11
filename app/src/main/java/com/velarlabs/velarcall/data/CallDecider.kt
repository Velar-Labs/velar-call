package com.velarlabs.velarcall.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import android.provider.ContactsContract.CommonDataKinds.Phone

sealed class Decision {
    data class Allow(val reason: String) : Decision()
    data class Block(val reason: String) : Decision()
}

/**
 * Regras de bloqueio, em ordem de prioridade. Um contato da agenda SEMPRE toca,
 * a não ser que você o tenha bloqueado manualmente.
 *
 *  1. Proteção desligada             -> permite
 *  2. Número oculto                  -> bloqueia só se "bloquear ocultos" estiver ligado (padrão: desligado)
 *  3. Lista permitida do app         -> permite
 *  4. Bloqueado manualmente          -> bloqueia (vale até para contatos)
 *  5. Contato da agenda              -> permite   <-- antes de qualquer bloqueio automático
 *  6. Origem falsificada (operadora) -> bloqueia (se ativado)
 *  7. Lista online / prefixo suspeito -> bloqueia (se ativado)
 *  8. Prefixo de telemarketing 0303  -> bloqueia (se ativado)
 *  9. Mesmo desconhecido de novo em 3 min -> permite (se ativado)
 * 10. Desconhecido (fora da agenda)  -> bloqueia (se ativado e com acesso à agenda)
 */
class CallDecider(private val context: Context) {

    private val settings = Settings(context)
    private val db = VelarDb.get(context)

    fun hasContactsPermission(): Boolean =
        context.checkSelfPermission(Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED

    /** Nome do contato via PhoneLookup do Android (busca rápida e indexada). */
    private fun lookupName(raw: String?): String? {
        if (raw.isNullOrBlank() || !hasContactsPermission()) return null
        return try {
            val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(raw))
            context.contentResolver.query(
                uri, arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME), null, null, null
            )?.use { c -> if (c.moveToFirst()) c.getString(0) ?: "" else null }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Reserva: percorre a agenda e compara pelo número normalizado pelo PRÓPRIO app.
     * Resolve o caso em que o PhoneLookup não casa por diferença de formato
     * (ex.: contato salvo como "(11) 98888-7777" e chamada chegando como "+5511988887777").
     */
    private fun contactNameByKey(key: String): String? {
        if (key.isEmpty() || !hasContactsPermission()) return null
        return try {
            context.contentResolver.query(
                Phone.CONTENT_URI,
                arrayOf(Phone.DISPLAY_NAME_PRIMARY, Phone.NUMBER),
                null, null, null
            )?.use { c ->
                while (c.moveToNext()) {
                    if (PhoneNumbers.key(c.getString(1)) == key) return c.getString(0) ?: ""
                }
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    /** true + nome quando o número é um contato (por qualquer um dos dois métodos). */
    fun resolveContact(raw: String?, key: String): Pair<Boolean, String?> {
        lookupName(raw)?.let { return true to it }
        contactNameByKey(key)?.let { return true to it }
        return false to null
    }

    /** Exposto para a UI de consulta de número. */
    fun contactName(raw: String): String? = resolveContact(raw, PhoneNumbers.key(raw)).second

    /** Quantos números a agenda tem (para o diagnóstico). -1 se sem permissão. */
    fun contactNumberCount(): Int {
        if (!hasContactsPermission()) return -1
        return try {
            context.contentResolver.query(Phone.CONTENT_URI, arrayOf(Phone._ID), null, null, null)
                ?.use { it.count } ?: 0
        } catch (e: Exception) {
            -1
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
            // Sem número não dá para saber se é um contato, então ocultos só são
            // bloqueados quando você pede. Padrão desligado para nunca barrar um conhecido.
            return if (settings.blockHidden) Decision.Block("Número oculto")
            else Decision.Allow("Número oculto (permitido)")
        }

        val entries = db.find(key)

        // 1. Você liberou este número -> toca.
        entries.firstOrNull { it.type == ListType.ALLOW }?.let {
            return Decision.Allow(if (it.label.isNotBlank()) "Permitido: ${it.label}" else "Lista permitida")
        }
        // 2. Você bloqueou este número -> bloqueia (vale até para contatos).
        entries.firstOrNull { it.type == ListType.BLOCK }?.let {
            return Decision.Block(if (it.label.isNotBlank()) "Bloqueado: ${it.label}" else "Bloqueado por você")
        }
        // 3. É um contato da agenda -> SEMPRE toca (antes de qualquer bloqueio automático).
        val (isContact, name) = resolveContact(raw, key)
        if (isContact) {
            return Decision.Allow(if (!name.isNullOrBlank()) "Contato: $name" else "Contato da agenda")
        }

        // --- a partir daqui, só números que não são contatos ---
        if (spoofed && settings.blockSpoofed) return Decision.Block("Origem falsificada")
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
        if (checkRepeat && settings.allowRepeatCaller) {
            val since = System.currentTimeMillis() - Settings.REPEAT_WINDOW_MS
            if (db.blockedNumbersSince(since).any { PhoneNumbers.key(it) == key }) {
                return Decision.Allow("Ligou de novo em poucos minutos")
            }
        }
        if (settings.blockUnknown) {
            // Sem acesso à agenda não dá para saber quem é contato: não bloqueia, para não barrar conhecidos.
            return if (hasContactsPermission()) Decision.Block("Fora da agenda")
            else Decision.Allow("Desconhecido (sem acesso à agenda)")
        }
        return Decision.Allow("Desconhecido")
    }
}

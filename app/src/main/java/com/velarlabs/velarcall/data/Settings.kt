package com.velarlabs.velarcall.data

import android.content.Context
import android.content.SharedPreferences

class Settings(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("velarcall", Context.MODE_PRIVATE)

    var protectionOn: Boolean
        get() = prefs.getBoolean(K_ON, true)
        set(v) = prefs.edit().putBoolean(K_ON, v).apply()

    /** Bloqueia quem não está na agenda nem na lista permitida do app. */
    var blockUnknown: Boolean
        get() = prefs.getBoolean(K_UNKNOWN, true)
        set(v) = prefs.edit().putBoolean(K_UNKNOWN, v).apply()

    var blockHidden: Boolean
        get() = prefs.getBoolean(K_HIDDEN, true)
        set(v) = prefs.edit().putBoolean(K_HIDDEN, v).apply()

    var blockTelemarketingPrefix: Boolean
        get() = prefs.getBoolean(K_0303, true)
        set(v) = prefs.edit().putBoolean(K_0303, v).apply()

    /** Usa a lista de números suspeitos baixada da internet. */
    var useOnlineList: Boolean
        get() = prefs.getBoolean(K_ONLINE, true)
        set(v) = prefs.edit().putBoolean(K_ONLINE, v).apply()

    /** Bloqueia chamadas cuja origem a operadora marcou como falsificada (Android 11+). */
    var blockSpoofed: Boolean
        get() = prefs.getBoolean(K_SPOOF, true)
        set(v) = prefs.edit().putBoolean(K_SPOOF, v).apply()

    /** Deixa tocar se um número desconhecido ligar de novo em poucos minutos (pode ser urgente). */
    var allowRepeatCaller: Boolean
        get() = prefs.getBoolean(K_REPEAT, true)
        set(v) = prefs.edit().putBoolean(K_REPEAT, v).apply()

    var onboardingDone: Boolean
        get() = prefs.getBoolean(K_ONBOARD, false)
        set(v) = prefs.edit().putBoolean(K_ONBOARD, v).apply()

    var notifyOnBlock: Boolean
        get() = prefs.getBoolean(K_NOTIFY, true)
        set(v) = prefs.edit().putBoolean(K_NOTIFY, v).apply()

    var onlineListUrl: String
        get() = prefs.getString(K_URL, DEFAULT_LIST_URL) ?: DEFAULT_LIST_URL
        set(v) = prefs.edit().putString(K_URL, v.trim()).apply()

    var lastSyncAt: Long
        get() = prefs.getLong(K_SYNC_AT, 0L)
        set(v) = prefs.edit().putLong(K_SYNC_AT, v).apply()

    var lastSyncMessage: String
        get() = prefs.getString(K_SYNC_MSG, "Ainda não sincronizado") ?: ""
        set(v) = prefs.edit().putString(K_SYNC_MSG, v).apply()

    /** Prefixos vindos da lista online (além do 0303 fixo). */
    var onlinePrefixes: Set<String>
        get() = prefs.getStringSet(K_PREFIXES, emptySet()) ?: emptySet()
        set(v) = prefs.edit().putStringSet(K_PREFIXES, v).apply()

    companion object {
        const val DEFAULT_LIST_URL =
            "https://raw.githubusercontent.com/Velar-Labs/velar-call/main/blocklist/numeros.json"
        private const val K_ON = "protection_on"
        private const val K_UNKNOWN = "block_unknown"
        private const val K_HIDDEN = "block_hidden"
        private const val K_0303 = "block_0303"
        private const val K_ONLINE = "use_online"
        private const val K_SPOOF = "block_spoofed"
        private const val K_NOTIFY = "notify_on_block"
        private const val K_REPEAT = "allow_repeat"
        private const val K_ONBOARD = "onboarding_done"
        const val REPEAT_WINDOW_MS = 3 * 60 * 1000L
        private const val K_URL = "online_url"
        private const val K_SYNC_AT = "sync_at"
        private const val K_SYNC_MSG = "sync_msg"
        private const val K_PREFIXES = "online_prefixes"
    }
}

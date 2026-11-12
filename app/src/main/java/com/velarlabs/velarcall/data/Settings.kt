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
        get() = prefs.getBoolean(K_HIDDEN, false)
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
        get() = prefs.getBoolean(K_SPOOF, false)
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

    /**
     * Em vez de rejeitar na hora, silencia a chamada bloqueada e a deixa cair no
     * correio de voz da operadora, para a pessoa poder deixar um recado.
     */
    var sendBlockedToVoicemail: Boolean
        get() = prefs.getBoolean(K_VOICEMAIL, true)
        set(v) = prefs.edit().putBoolean(K_VOICEMAIL, v).apply()

    var onlineListUrl: String
        get() = prefs.getString(K_URL, DEFAULT_LIST_URL) ?: DEFAULT_LIST_URL
        set(v) = prefs.edit().putString(K_URL, v.trim()).apply()

    var lastSyncAt: Long
        get() = prefs.getLong(K_SYNC_AT, 0L)
        set(v) = prefs.edit().putLong(K_SYNC_AT, v).apply()

    var lastSyncMessage: String
        get() = prefs.getString(K_SYNC_MSG, "Ainda não sincronizado") ?: ""
        set(v) = prefs.edit().putString(K_SYNC_MSG, v).apply()

    /** Diagnóstico: resumo da última chamada analisada (para depurar bloqueios indevidos). */
    var lastCallDiag: String
        get() = prefs.getString(K_DIAG, "") ?: ""
        set(v) = prefs.edit().putString(K_DIAG, v).apply()

    var lastCallDiagAt: Long
        get() = prefs.getLong(K_DIAG_AT, 0L)
        set(v) = prefs.edit().putLong(K_DIAG_AT, v).apply()

    /** Prefixos que VOCÊ escolheu bloquear (ex.: 0800, 0300, ou um DDD). */
    var blockedPrefixes: Set<String>
        get() = prefs.getStringSet(K_MY_PREFIXES, emptySet()) ?: emptySet()
        set(v) = prefs.edit().putStringSet(K_MY_PREFIXES, v).apply()

    /** Horário silencioso: nesse intervalo, só contatos e números liberados tocam. */
    var quietHoursEnabled: Boolean
        get() = prefs.getBoolean(K_QUIET, false)
        set(v) = prefs.edit().putBoolean(K_QUIET, v).apply()

    /** Minutos desde a meia-noite (0..1439). */
    var quietStart: Int
        get() = prefs.getInt(K_QUIET_START, 22 * 60)
        set(v) = prefs.edit().putInt(K_QUIET_START, v).apply()

    var quietEnd: Int
        get() = prefs.getInt(K_QUIET_END, 7 * 60)
        set(v) = prefs.edit().putInt(K_QUIET_END, v).apply()

    /** Dias do horário silencioso (Calendar.DAY_OF_WEEK: 1=domingo..7=sábado). Vazio = todos os dias. */
    var quietDays: Set<Int>
        get() = (prefs.getStringSet(K_QUIET_DAYS, emptySet()) ?: emptySet()).mapNotNull { it.toIntOrNull() }.toSet()
        set(v) = prefs.edit().putStringSet(K_QUIET_DAYS, v.map { it.toString() }.toSet()).apply()

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
        private const val K_VOICEMAIL = "send_to_voicemail"
        private const val K_REPEAT = "allow_repeat"
        private const val K_ONBOARD = "onboarding_done"
        const val REPEAT_WINDOW_MS = 3 * 60 * 1000L
        private const val K_URL = "online_url"
        private const val K_SYNC_AT = "sync_at"
        private const val K_SYNC_MSG = "sync_msg"
        private const val K_PREFIXES = "online_prefixes"
        private const val K_MY_PREFIXES = "my_prefixes"
        private const val K_QUIET = "quiet_enabled"
        private const val K_QUIET_START = "quiet_start"
        private const val K_QUIET_END = "quiet_end"
        private const val K_QUIET_DAYS = "quiet_days"
        private const val K_DIAG = "last_call_diag"
        private const val K_DIAG_AT = "last_call_diag_at"
    }
}

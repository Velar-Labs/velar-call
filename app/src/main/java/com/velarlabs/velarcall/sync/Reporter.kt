package com.velarlabs.velarcall.sync

import android.content.Context
import com.velarlabs.velarcall.data.PhoneNumbers
import com.velarlabs.velarcall.data.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/**
 * Envia denúncias de spam para o banco compartilhado (Supabase).
 * Um número entra na lista pública só depois de denunciado por 3 aparelhos distintos.
 */
object Reporter {

    private const val ENDPOINT = "https://iznyfavweqoufiznmmxi.supabase.co/functions/v1/report"
    private const val TOKEN = "velar-report-2026-x7k9"

    /** Identificador anônimo do aparelho, em hash — não revela nada sobre você. */
    private fun deviceHash(context: Context): String {
        val id = Settings(context).deviceId
        val bytes = MessageDigest.getInstance("SHA-256").digest(id.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    suspend fun report(context: Context, rawNumber: String, label: String = ""): Boolean =
        withContext(Dispatchers.IO) {
            val key = PhoneNumbers.key(rawNumber)
            if (key.length < 8) return@withContext false
            runCatching {
                val payload = JSONObject()
                    .put("number", key)
                    .put("label", label.take(60))
                    .put("device", deviceHash(context))
                    .toString()
                val conn = URL(ENDPOINT).openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.connectTimeout = 15000
                conn.readTimeout = 20000
                conn.setRequestProperty("content-type", "application/json")
                conn.setRequestProperty("x-velar-token", TOKEN)
                conn.outputStream.use { it.write(payload.toByteArray()) }
                val ok = conn.responseCode in 200..299
                conn.disconnect()
                ok
            }.getOrDefault(false)
        }
}

package com.velarlabs.velarcall.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.velarlabs.velarcall.data.PhoneNumbers
import com.velarlabs.velarcall.data.Settings
import com.velarlabs.velarcall.data.VelarDb
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

/**
 * Baixa a lista de números suspeitos da internet.
 *
 * Formatos aceitos:
 *  - JSON: {"numbers":[{"n":"11999999999","label":"Golpe"}, "21988887777"], "prefixes":["0303"]}
 *  - Texto: um número por linha; "#" inicia comentário; "numero;rotulo" opcional.
 */
object BlocklistSync {

    data class Result(val ok: Boolean, val message: String)

    private const val MAX_BYTES = 8 * 1024 * 1024

    suspend fun run(context: Context): Result = withContext(Dispatchers.IO) {
        val settings = Settings(context)
        val url = settings.onlineListUrl
        try {
            require(url.startsWith("https://")) { "A URL precisa começar com https://" }
            val body = download(url)
            val (numbers, prefixes) = parse(body)
            VelarDb.get(context).replaceOnline(numbers)
            settings.onlinePrefixes = prefixes
            val msg = "${numbers.size} números e ${prefixes.size} prefixos"
            settings.lastSyncAt = System.currentTimeMillis()
            settings.lastSyncMessage = msg
            Result(true, msg)
        } catch (e: Exception) {
            val msg = "Falha: ${e.message ?: e.javaClass.simpleName}"
            settings.lastSyncMessage = msg
            Result(false, msg)
        }
    }

    private fun download(url: String): String {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 15_000
        conn.readTimeout = 20_000
        conn.setRequestProperty("User-Agent", "VelarCall/0.1")
        try {
            val code = conn.responseCode
            if (code !in 200..299) error("HTTP $code")
            val bytes = conn.inputStream.use { input ->
                val buf = java.io.ByteArrayOutputStream()
                val chunk = ByteArray(16 * 1024)
                while (true) {
                    val r = input.read(chunk)
                    if (r < 0) break
                    buf.write(chunk, 0, r)
                    if (buf.size() > MAX_BYTES) error("lista grande demais")
                }
                buf.toByteArray()
            }
            return String(bytes, Charsets.UTF_8)
        } finally {
            conn.disconnect()
        }
    }

    fun parse(body: String): Pair<List<Pair<String, String>>, Set<String>> {
        val text = body.trim()
        val numbers = LinkedHashMap<String, String>()
        val prefixes = LinkedHashSet<String>()
        if (text.startsWith("{") || text.startsWith("[")) {
            val arr: JSONArray
            if (text.startsWith("{")) {
                val obj = JSONObject(text)
                arr = obj.optJSONArray("numbers") ?: JSONArray()
                obj.optJSONArray("prefixes")?.let { p ->
                    for (i in 0 until p.length()) PhoneNumbers.digits(p.optString(i)).takeIf { it.length >= 3 }?.let(prefixes::add)
                }
            } else {
                arr = JSONArray(text)
            }
            for (i in 0 until arr.length()) {
                when (val item = arr.opt(i)) {
                    is JSONObject -> {
                        val k = PhoneNumbers.key(item.optString("n", item.optString("number")))
                        if (k.length >= 8) numbers[k] = item.optString("label", "")
                    }
                    else -> {
                        val k = PhoneNumbers.key(item?.toString())
                        if (k.length >= 8) numbers[k] = ""
                    }
                }
            }
        } else {
            text.lineSequence().forEach { line ->
                val clean = line.substringBefore("#").trim()
                if (clean.isEmpty()) return@forEach
                val parts = clean.split(";", limit = 2)
                val k = PhoneNumbers.key(parts[0])
                if (k.length >= 8) numbers[k] = parts.getOrNull(1)?.trim().orEmpty()
            }
        }
        return numbers.map { it.key to it.value } to prefixes
    }

    fun schedule(context: Context) {
        val req = PeriodicWorkRequestBuilder<BlocklistWorker>(12, TimeUnit.HOURS)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork("velar-blocklist", ExistingPeriodicWorkPolicy.KEEP, req)
    }
}

class BlocklistWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        if (!Settings(applicationContext).useOnlineList) return Result.success()
        return if (BlocklistSync.run(applicationContext).ok) Result.success() else Result.retry()
    }
}

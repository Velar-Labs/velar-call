package com.velarlabs.velarcall.sync

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import com.velarlabs.velarcall.BuildConfig
import com.velarlabs.velarcall.data.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

data class ReleaseInfo(val version: String, val apkUrl: String, val notes: String)

/**
 * Verifica se há uma versão nova no GitHub Releases e instala.
 *
 * Observação honesta: um app instalado fora da Play Store não consegue se
 * atualizar de forma 100% silenciosa — o Android pede uma confirmação. Este
 * atualizador deixa isso em um toque: verifica, baixa e abre a instalação.
 */
object Updater {

    private const val RELEASES_URL = "https://api.github.com/repos/Velar-Labs/velar-call/releases?per_page=5"
    const val INSTALL_ACTION = "com.velarlabs.velarcall.INSTALL_RESULT"

    fun currentVersion(): String = BuildConfig.VERSION_NAME

    /** Compara "0.7.0" com "0.10.0" corretamente (numérico por campo). > 0 se a > b. */
    fun compareVersions(a: String, b: String): Int {
        val pa = parse(a); val pb = parse(b)
        for (i in 0 until maxOf(pa.size, pb.size)) {
            val x = pa.getOrElse(i) { 0 }; val y = pb.getOrElse(i) { 0 }
            if (x != y) return x - y
        }
        return 0
    }

    private fun parse(v: String): List<Int> =
        v.trim().removePrefix("v").split(".").map { it.filter(Char::isDigit).toIntOrNull() ?: 0 }

    fun isNewer(remote: String, local: String = currentVersion()): Boolean = compareVersions(remote, local) > 0

    /** Busca a release mais recente (inclui pré-lançamentos) que tenha um APK. */
    suspend fun latest(): ReleaseInfo? = withContext(Dispatchers.IO) {
        runCatching {
            val body = httpGet(RELEASES_URL)
            val arr = JSONArray(body)
            for (i in 0 until arr.length()) {
                val rel = arr.getJSONObject(i)
                val tag = rel.optString("tag_name")
                val assets = rel.optJSONArray("assets") ?: continue
                for (j in 0 until assets.length()) {
                    val a = assets.getJSONObject(j)
                    if (a.optString("name").endsWith(".apk")) {
                        return@withContext ReleaseInfo(tag, a.optString("browser_download_url"), rel.optString("body"))
                    }
                }
            }
            null
        }.getOrNull()
    }

    private fun httpGet(url: String): String {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 15000; conn.readTimeout = 20000
        conn.setRequestProperty("Accept", "application/vnd.github+json")
        conn.setRequestProperty("User-Agent", "VelarCall/${currentVersion()}")
        try {
            if (conn.responseCode !in 200..299) error("HTTP ${conn.responseCode}")
            return conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }

    fun canInstall(context: Context): Boolean = context.packageManager.canRequestPackageInstalls()

    suspend fun downloadApk(context: Context, url: String): File = withContext(Dispatchers.IO) {
        val out = File(context.cacheDir, "update.apk")
        if (out.exists()) out.delete()
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.instanceFollowRedirects = true
        conn.connectTimeout = 20000; conn.readTimeout = 60000
        conn.setRequestProperty("User-Agent", "VelarCall/${currentVersion()}")
        try {
            if (conn.responseCode !in 200..299) error("HTTP ${conn.responseCode}")
            conn.inputStream.use { input -> out.outputStream.use { input.copyTo(it) } }
        } finally {
            conn.disconnect()
        }
        out
    }

    /** Entrega o APK ao instalador do sistema; o Android pede a confirmação final. */
    fun install(context: Context, apk: File) {
        val installer = context.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
        params.setAppPackageName(context.packageName)
        val sessionId = installer.createSession(params)
        installer.openSession(sessionId).use { session ->
            apk.inputStream().use { input ->
                session.openWrite("velarcall", 0, apk.length()).use { out ->
                    input.copyTo(out); session.fsync(out)
                }
            }
            val intent = Intent(INSTALL_ACTION).setPackage(context.packageName)
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            val pending = PendingIntent.getBroadcast(context, sessionId, intent, flags)
            session.commit(pending.intentSender)
        }
    }

    /** Agenda uma verificação diária de atualização. */
    fun schedule(context: Context) {
        val req = androidx.work.PeriodicWorkRequestBuilder<UpdateWorker>(24, java.util.concurrent.TimeUnit.HOURS)
            .setConstraints(
                androidx.work.Constraints.Builder()
                    .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED).build()
            )
            .build()
        androidx.work.WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork("velar-update", androidx.work.ExistingPeriodicWorkPolicy.KEEP, req)
    }
}

class UpdateWorker(context: Context, params: androidx.work.WorkerParameters) :
    androidx.work.CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val settings = Settings(applicationContext)
        if (!settings.autoUpdate) return Result.success()
        val rel = Updater.latest() ?: return Result.retry()
        settings.lastUpdateCheckAt = System.currentTimeMillis()
        if (Updater.isNewer(rel.version) && rel.version != settings.updateNotifiedVersion) {
            settings.updateNotifiedVersion = rel.version
            UpdateNotifier.notify(applicationContext, rel.version)
        }
        return Result.success()
    }
}

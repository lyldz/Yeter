package com.yeter.app

import android.content.Context
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(val version: String, val downloadUrl: String, val notes: String, val sizeBytes: Long)

// GitHub Releases üzerinden güncelleme denetimi (uygulama açıkken ve arka plan işinde ortak)
object Updater {
    private const val REPO = "lyldz/Yeter"
    const val SNOOZE_MS = 24L * 3600_000L

    // "v1.1.1" > "1.1.0" gibi sayısal karşılaştırma
    fun isNewer(remote: String, current: String): Boolean {
        fun parts(v: String) = v.removePrefix("v").trim().split('.').map { it.takeWhile { c -> c.isDigit() }.toIntOrNull() ?: 0 }
        val r = parts(remote)
        val c = parts(current)
        for (i in 0 until maxOf(r.size, c.size)) {
            val a = r.getOrElse(i) { 0 }
            val b = c.getOrElse(i) { 0 }
            if (a != b) return a > b
        }
        return false
    }

    // Bloklayıcı; IO iş parçacığında çağır. Başarısızlıkta null.
    fun fetchLatest(): UpdateInfo? {
        return try {
            val conn = URL("https://api.github.com/repos/$REPO/releases/latest").openConnection() as HttpURLConnection
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            conn.setRequestProperty("Accept", "application/vnd.github.v3+json")
            conn.setRequestProperty("User-Agent", "YeterApp")
            if (conn.responseCode != 200) { conn.disconnect(); return null }
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            conn.disconnect()
            val json = JSONObject(body)
            val tag = json.optString("tag_name", "")
            if (tag.isEmpty()) return null
            var url: String? = null
            var size = 0L
            val assets = json.optJSONArray("assets")
            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val a = assets.getJSONObject(i)
                    val dl = a.optString("browser_download_url")
                    if (a.optString("name").endsWith(".apk") && dl.isNotEmpty()) {
                        url = dl
                        size = a.optLong("size", 0L)
                        break
                    }
                }
            }
            UpdateInfo(tag, url ?: return null, cleanNotes(json.optString("body", "")), size)
        } catch (e: Exception) {
            null
        }
    }

    // Release gövdesinden yalnızca madde satırlarını al: "- **Başlık:** metin" -> "• Başlık: metin"
    fun cleanNotes(body: String): String =
        body.lines()
            .map { it.trim() }
            .filter { it.startsWith("- ") || it.startsWith("* ") }
            .joinToString("\n") { "• " + it.drop(2).replace("**", "").replace("`", "") }

    // Bloklayıcı; IO iş parçacığında çağır. İlerleme 0..1 (boyut bilinmiyorsa -1). Başarısızlıkta null.
    fun download(context: Context, url: String, onProgress: (Float) -> Unit): File? {
        return try {
            val dir = File(context.cacheDir, "updates").apply { mkdirs() }
            dir.listFiles()?.forEach { it.delete() }
            val out = File(dir, "Yeter.apk")
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.connectTimeout = 15000
            conn.readTimeout = 30000
            conn.setRequestProperty("User-Agent", "YeterApp")
            if (conn.responseCode != 200) { conn.disconnect(); return null }
            val total = conn.contentLengthLong
            var done = 0L
            var lastReport = 0L
            conn.inputStream.use { input ->
                out.outputStream().use { output ->
                    val buf = ByteArray(32 * 1024)
                    while (true) {
                        val n = input.read(buf)
                        if (n < 0) break
                        output.write(buf, 0, n)
                        done += n
                        val now = System.currentTimeMillis()
                        if (now - lastReport > 150) {
                            lastReport = now
                            onProgress(if (total > 0) done.toFloat() / total else -1f)
                        }
                    }
                }
            }
            conn.disconnect()
            if (total > 0 && done != total) { out.delete(); return null }
            out
        } catch (e: Exception) {
            null
        }
    }
}

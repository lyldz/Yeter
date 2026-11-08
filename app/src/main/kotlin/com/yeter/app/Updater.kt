package com.yeter.app

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(val version: String, val downloadUrl: String)

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
            var url = json.optString("html_url", "https://github.com/$REPO/releases")
            val assets = json.optJSONArray("assets")
            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val a = assets.getJSONObject(i)
                    if (a.optString("name").endsWith(".apk")) {
                        url = a.optString("browser_download_url", url)
                        break
                    }
                }
            }
            UpdateInfo(tag, url)
        } catch (e: Exception) {
            null
        }
    }
}

package com.yeter.app

import android.content.SharedPreferences
import org.json.JSONObject
import java.net.HttpURLConnection

data class CurrencyInfo(val code: String, val symbol: String, val name: String)

val CURRENCIES = listOf(
    CurrencyInfo("TRY", "₺", "Türk Lirası"),
    CurrencyInfo("USD", "$", "ABD Doları"),
    CurrencyInfo("EUR", "€", "Euro"),
    CurrencyInfo("GBP", "£", "İngiliz Sterlini"),
    CurrencyInfo("CHF", "CHF ", "İsviçre Frangı"),
    CurrencyInfo("JPY", "¥", "Japon Yeni"),
    CurrencyInfo("CAD", "C$", "Kanada Doları"),
    CurrencyInfo("AUD", "A$", "Avustralya Doları")
)

fun currencySymbol(code: String): String = CURRENCIES.firstOrNull { it.code == code }?.symbol ?: "$code "

object Rates {
    // Yaklaşık yedek kurlar (Kasım 2026). İnternetten gerçek kur gelince bunların yerine geçer.
    private val FALLBACK = mapOf(
        "USD" to 1.0, "TRY" to 50.0, "EUR" to 0.90, "GBP" to 0.75,
        "CHF" to 0.82, "JPY" to 158.0, "CAD" to 1.41, "AUD" to 1.46
    )

    // Kaynaklar sırayla denenir. parse: gövde -> {KOD: 1 USD = x}
    private data class Source(val name: String, val url: String, val parse: (String) -> Map<String, Double>)

    private val SOURCES = listOf(
        Source("exchangerate-api", "https://api.exchangerate-api.com/v4/latest/USD") { body ->
            val o = JSONObject(body).getJSONObject("rates")
            CURRENCIES.mapNotNull { c -> o.optDouble(c.code, 0.0).takeIf { it > 0 }?.let { c.code to it } }.toMap()
        },
        Source("currency-api", "https://cdn.jsdelivr.net/npm/@fawazahmed0/currency-api@latest/v1/currencies/usd.json") { body ->
            val o = JSONObject(body).getJSONObject("usd")
            CURRENCIES.mapNotNull { c -> o.optDouble(c.code.lowercase(), 0.0).takeIf { it > 0 }?.let { c.code to it } }.toMap()
        },
        Source("currency-api-2", "https://latest.currency-api.pages.dev/v1/currencies/usd.json") { body ->
            val o = JSONObject(body).getJSONObject("usd")
            CURRENCIES.mapNotNull { c -> o.optDouble(c.code.lowercase(), 0.0).takeIf { it > 0 }?.let { c.code to it } }.toMap()
        },
        Source("open-er-api", "https://open.er-api.com/v6/latest/USD") { body ->
            val o = JSONObject(body).getJSONObject("rates")
            CURRENCIES.mapNotNull { c -> o.optDouble(c.code, 0.0).takeIf { it > 0 }?.let { c.code to it } }.toMap()
        }
    )

    fun load(prefs: SharedPreferences): Map<String, Double> {
        val s = prefs.getString("rates_json", null) ?: return FALLBACK
        return try {
            val o = JSONObject(s)
            CURRENCIES.associate { it.code to o.optDouble(it.code, FALLBACK[it.code] ?: 1.0) }
        } catch (e: Exception) {
            FALLBACK
        }
    }

    fun convert(amount: Double, from: String, to: String, rates: Map<String, Double>): Double {
        if (from == to) return amount
        val f = rates[from] ?: return amount
        val t = rates[to] ?: return amount
        if (f <= 0.0) return amount
        return amount / f * t
    }

    // Bloklayıcıdır, IO iş parçacığında çağır. Başarılıysa true.
    // Başarıda rates_json + rate_ts + rate_source, başarısızlıkta rate_fail_ts yazar.
    fun fetch(prefs: SharedPreferences): Boolean {
        for (src in SOURCES) {
            try {
                val conn = java.net.URL(src.url).openConnection() as HttpURLConnection
                conn.connectTimeout = 8000
                conn.readTimeout = 8000
                conn.setRequestProperty("User-Agent", "YeterApp")
                conn.setRequestProperty("Accept", "application/json")
                if (conn.responseCode != 200) { conn.disconnect(); continue }
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                conn.disconnect()
                val parsed = src.parse(body)
                // Tüm para birimleri geldiyse ve TRY makulse kabul et
                if (parsed.size < CURRENCIES.size || (parsed["TRY"] ?: 0.0) <= 0.0) continue
                val out = JSONObject()
                parsed.forEach { (k, v) -> out.put(k, v) }
                prefs.edit()
                    .putString("rates_json", out.toString())
                    .putLong("rate_ts", System.currentTimeMillis())
                    .putString("rate_source", src.name)
                    .remove("rate_fail_ts")
                    .apply()
                return true
            } catch (e: Exception) {
                // sıradaki kaynağı dene
            }
        }
        prefs.edit().putLong("rate_fail_ts", System.currentTimeMillis()).apply()
        return false
    }
}

package com.yeter.app

import android.content.SharedPreferences
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

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
    // 1 USD = x birim. İnternet yokken kullanılan yaklaşık değerler; ilk güncellemede gerçek kurlarla değişir.
    private val FALLBACK = mapOf(
        "USD" to 1.0, "TRY" to 40.0, "EUR" to 0.92, "GBP" to 0.78,
        "CHF" to 0.88, "JPY" to 150.0, "CAD" to 1.37, "AUD" to 1.52
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
    fun fetch(prefs: SharedPreferences): Boolean {
        return try {
            val conn = URL("https://open.er-api.com/v6/latest/USD").openConnection() as HttpURLConnection
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            if (conn.responseCode != 200) return false
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            val all = JSONObject(body).getJSONObject("rates")
            val out = JSONObject()
            for (c in CURRENCIES) {
                val v = all.optDouble(c.code, 0.0)
                if (v > 0) out.put(c.code, v)
            }
            if (!out.has("TRY")) return false
            prefs.edit()
                .putString("rates_json", out.toString())
                .putLong("rate_ts", System.currentTimeMillis())
                .apply()
            true
        } catch (e: Exception) {
            false
        }
    }
}

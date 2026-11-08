package com.yeter.app

import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar
import kotlin.math.max
import kotlin.math.min

// Dürtü kaydı: direnildi (resisted=true) ya da yenildi
data class UrgeEvent(val ts: Long, val name: String, val amount: Double, val resisted: Boolean)

// Birikim hesabı: hem ViewModel hem arka plan işi (bildirimler) kullanır.
object Savings {

    // Gün bazlı tutar kayıtları: (başlangıç zamanı, günlük tutar). Son kayıt hâlâ geçerlidir.
    fun loadSegments(prefs: SharedPreferences, now: Long = System.currentTimeMillis()): List<Pair<Long, Double>> {
        val s = prefs.getString("segments_json", null)
        if (s != null) {
            try {
                val arr = JSONArray(s)
                val list = (0 until arr.length()).map {
                    val o = arr.getJSONObject(it)
                    o.getLong("start") to o.getDouble("amount")
                }
                if (list.isNotEmpty()) return list
            } catch (e: Exception) { /* aşağıda göç et */ }
        }
        val start = prefs.getLong("start_timestamp", now)
        val amount = prefs.getFloat("daily_saving", 30f).toDouble()
        val migrated = listOf(start to amount)
        saveSegments(prefs, migrated)
        return migrated
    }

    fun saveSegments(prefs: SharedPreferences, segs: List<Pair<Long, Double>>) {
        val arr = JSONArray()
        for ((start, amount) in segs) {
            arr.put(JSONObject().put("start", start).put("amount", amount))
        }
        prefs.edit().putString("segments_json", arr.toString()).apply()
    }

    fun baseline(segs: List<Pair<Long, Double>>, now: Long): Double {
        var total = 0.0
        for (i in segs.indices) {
            val start = segs[i].first
            val end = if (i + 1 < segs.size) segs[i + 1].first else now
            total += segs[i].second * max(0L, end - start) / 86_400_000.0
        }
        return total
    }

    // Bugün (yerel gece yarısından şimdiye) birikenler
    fun todayPart(segs: List<Pair<Long, Double>>, now: Long): Double {
        val midnight = startOfDay(now)
        var total = 0.0
        for (i in segs.indices) {
            val start = max(segs[i].first, midnight)
            val end = min(if (i + 1 < segs.size) segs[i + 1].first else now, now)
            if (end > start) total += segs[i].second * (end - start) / 86_400_000.0
        }
        return total
    }

    fun startOfDay(now: Long): Long {
        val c = Calendar.getInstance()
        c.timeInMillis = now
        c.set(Calendar.HOUR_OF_DAY, 0)
        c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
        return c.timeInMillis
    }

    fun dayKey(now: Long): String {
        val c = Calendar.getInstance()
        c.timeInMillis = now
        return "${c.get(Calendar.YEAR)}-${c.get(Calendar.MONTH) + 1}-${c.get(Calendar.DAY_OF_MONTH)}"
    }

    // Havuz = taban birikim + direnilen − hedeflere harcanan (ana para biriminde)
    fun pool(prefs: SharedPreferences, now: Long): Double {
        val segs = loadSegments(prefs, now)
        val resisted = prefs.getFloat("resisted_total", 0f).toDouble()
        val spent = prefs.getFloat("spent_total", 0f).toDouble()
        return max(0.0, baseline(segs, now) + resisted - spent)
    }

    fun loadGoals(prefs: SharedPreferences): MutableList<WishlistItem>? {
        val s = prefs.getString("wishlist_json", null) ?: return null
        return try {
            val arr = JSONArray(s)
            (0 until arr.length()).map {
                val o = arr.getJSONObject(it)
                WishlistItem(
                    id = o.getString("id"),
                    name = o.getString("name"),
                    price = o.getDouble("price"),
                    currency = o.optString("currency", "TRY"),
                    recurring = o.optBoolean("recurring", false),
                    addedTimestamp = o.optLong("addedTimestamp", System.currentTimeMillis())
                )
            }.toMutableList()
        } catch (e: Exception) {
            null
        }
    }

    fun saveGoals(prefs: SharedPreferences, list: List<WishlistItem>) {
        val arr = JSONArray()
        for (g in list) {
            arr.put(
                JSONObject()
                    .put("id", g.id).put("name", g.name).put("price", g.price)
                    .put("currency", g.currency).put("recurring", g.recurring)
                    .put("addedTimestamp", g.addedTimestamp)
            )
        }
        prefs.edit().putString("wishlist_json", arr.toString()).apply()
    }

    fun loadEvents(prefs: SharedPreferences): List<UrgeEvent> {
        val str = prefs.getString("events_json", null) ?: return emptyList()
        return try {
            val arr = JSONArray(str)
            (0 until arr.length()).map {
                val o = arr.getJSONObject(it)
                UrgeEvent(o.getLong("ts"), o.optString("name", ""), o.optDouble("amount", 0.0), o.optBoolean("resisted", true))
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun addEvent(prefs: SharedPreferences, e: UrgeEvent) {
        val list = (loadEvents(prefs) + e).takeLast(500)
        val arr = JSONArray()
        for (x in list) {
            arr.put(JSONObject().put("ts", x.ts).put("name", x.name).put("amount", x.amount).put("resisted", x.resisted))
        }
        prefs.edit().putString("events_json", arr.toString()).apply()
    }
}

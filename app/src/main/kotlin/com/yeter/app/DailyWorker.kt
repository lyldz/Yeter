package com.yeter.app

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.ceil
import kotlin.math.max

// Her sabah ~09:00: kurları yeniler, hedef ilerlemesini kontrol eder, hatırlatma gönderir.
class DailyWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        val ctx = applicationContext
        val prefs = ctx.getSharedPreferences("yeter_app_prefs", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("onboarded", prefs.contains("claude_seeded"))) return Result.success()

        withContext(Dispatchers.IO) { Rates.fetch(prefs) }

        val now = System.currentTimeMillis()

        // Arka planda güncelleme denetimi (ertelenmediyse bildirim)
        if (now >= prefs.getLong("update_snooze_until", 0L)) {
            val info = withContext(Dispatchers.IO) { Updater.fetchLatest() }
            val cur = try {
                ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName ?: "0"
            } catch (e: Exception) { "0" }
            if (info != null && Updater.isNewer(info.version, cur)) {
                Notifier.notify(ctx, 3001, "Yeter için yeni sürüm hazır", "${info.version} yayında. Güncellemek için dokun.")
            }
        }

        val home = prefs.getString("home_currency", "TRY") ?: "TRY"
        val sym = currencySymbol(home)
        val rates = Rates.load(prefs)
        val pool = Savings.pool(prefs, now)
        val daily = Savings.loadSegments(prefs, now).last().second
        val goals = Savings.loadGoals(prefs) ?: emptyList()

        fun fmt(v: Double) = String.format(Locale.getDefault(), "%,.2f", v)

        // Hedefe yaklaşma / ulaşma bildirimleri (her eşik bir kez)
        for (g in goals) {
            val priceHome = Rates.convert(g.price, g.currency, home, rates)
            if (priceHome <= 0) continue
            val pct = (pool / priceHome * 100).toInt()
            val threshold = listOf(100, 90, 75, 50).firstOrNull { pct >= it } ?: continue
            val key = "notif_${g.id}_$threshold"
            if (!prefs.getBoolean(key, false)) {
                prefs.edit().putBoolean(key, true).apply()
                val title = if (threshold == 100) "🎉 Hedefe ulaştın!" else "Hedefe yaklaşıyorsun (%$threshold)"
                val text = if (threshold == 100) "${g.name} için yeterli birikim var. Gönül rahatlığıyla alabilirsin."
                else "${g.name}: ${sym}${fmt(pool)} / ${sym}${fmt(priceHome)}"
                Notifier.notify(ctx, 2000 + g.id.hashCode().and(0xFFF), title, text)
            }
        }

        // Sabah hatırlatması
        val first = goals.firstOrNull()
        val text = if (first != null) {
            val priceHome = Rates.convert(first.price, first.currency, home, rates)
            val left = max(0.0, priceHome - pool)
            val days = if (daily > 0) ceil(left / daily).toInt() else 0
            if (left <= 0) "Birikim: ${sym}${fmt(pool)}. ${first.name} için yeterli! Bugünkü tutarını onayla."
            else "Birikim: ${sym}${fmt(pool)}. ${first.name} için yaklaşık $days gün kaldı. Bugün ne kadar tasarruf edeceksin?"
        } else {
            "Birikim: ${sym}${fmt(pool)}. Bugün ne kadar tasarruf edeceksin?"
        }
        Notifier.notify(ctx, 1001, "Günaydın, Yeter!", text)
        return Result.success()
    }

    companion object {
        fun schedule(ctx: Context, hour: Int = 9, policy: ExistingPeriodicWorkPolicy = ExistingPeriodicWorkPolicy.KEEP) {
            val c = Calendar.getInstance()
            val target = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0)
                if (timeInMillis <= c.timeInMillis) add(Calendar.DAY_OF_YEAR, 1)
            }
            val delay = target.timeInMillis - c.timeInMillis
            val req = PeriodicWorkRequestBuilder<DailyWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .build()
            WorkManager.getInstance(ctx).enqueueUniquePeriodicWork("yeter_daily", policy, req)
        }
    }
}

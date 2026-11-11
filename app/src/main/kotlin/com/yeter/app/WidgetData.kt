package com.yeter.app

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.max

data class WidgetGoal(val name: String, val progress: Float, val remaining: Double)

// Widget'ların okuduğu tek özet. ViewModel'e dokunmadan doğrudan SharedPreferences'tan hesaplanır.
// shortcut: ilerleme/gün hesabı ViewModel'deki ile aynı formül; ViewModel hesabı bir yardımcıya taşınırsa buraya bağla.
data class WidgetData(
    val onboarded: Boolean,
    val symbol: String,
    val saved: Double,
    val today: Double,
    val daily: Double,
    val goalName: String,
    val goalPrice: Double,
    val progress: Float,
    val daysLeft: Int,
    val reachDate: String,
    val resistedCount: Int,
    val resistedTotal: Double,
    val goals: List<WidgetGoal>
)

object WidgetDataLoader {
    fun load(ctx: Context): WidgetData {
        val prefs = ctx.getSharedPreferences("yeter_app_prefs", Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        val home = prefs.getString("home_currency", "TRY") ?: "TRY"
        val rates = Rates.load(prefs)
        val segs = Savings.loadSegments(prefs, now)
        val saved = Savings.pool(prefs, now)
        val daily = segs.last().second
        val pinned = prefs.getString("pinned_goal_id", "") ?: ""
        val all = (Savings.loadGoals(prefs) ?: emptyList()).sortedByDescending { it.id == pinned }
        val ui = all.map { g ->
            val price = Rates.convert(g.price, g.currency, home, rates)
            WidgetGoal(g.name, if (price > 0) (saved / price).toFloat().coerceIn(0f, 1f) else 0f, max(0.0, price - saved))
        }
        val first = all.firstOrNull()
        val price = first?.let { Rates.convert(it.price, it.currency, home, rates) } ?: 0.0
        val left = max(0.0, price - saved)
        val days = if (daily > 0 && first != null) ceil(left / daily).toInt() else 0
        val date = if (daily > 0 && first != null) SimpleDateFormat("d MMMM yyyy", Locale("tr", "TR")).format(Date(now + days * 86_400_000L)) else "-"
        return WidgetData(
            onboarded = prefs.getBoolean("onboarded", prefs.contains("claude_seeded")),
            symbol = currencySymbol(home),
            saved = saved,
            today = Savings.todayPart(segs, now),
            daily = daily,
            goalName = first?.name ?: "Hedef ekle",
            goalPrice = price,
            progress = ui.firstOrNull()?.progress ?: 0f,
            daysLeft = days,
            reachDate = date,
            resistedCount = prefs.getInt("resisted_count", 0),
            resistedTotal = prefs.getFloat("resisted_total", 0f).toDouble(),
            goals = ui
        )
    }
}

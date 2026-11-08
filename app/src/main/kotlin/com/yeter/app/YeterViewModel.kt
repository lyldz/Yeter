package com.yeter.app

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.max

data class Milestone(
    val dayThreshold: Int,
    val title: String,
    val suggestedReward: String,
    val icon: String = "🎯"
)

data class WishlistItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val price: Double,
    val currency: String = "TRY",
    val recurring: Boolean = false, // Aylık tekrar eden hedef (ör. Claude Pro)
    val addedTimestamp: Long = System.currentTimeMillis()
)

data class WishlistItemUi(
    val id: String,
    val name: String,
    val price: Double, // ana para biriminde
    val currency: String, // hedefin kendi para birimi
    val symbol: String, // ana para birimi sembolü
    val ownPrice: Double,
    val ownSymbol: String,
    val recurring: Boolean,
    val remainingMoney: Double, // ana para biriminde
    val daysRemaining: Long,
    val hoursRemaining: Long,
    val minutesRemaining: Long,
    val secondsRemaining: Long,
    val progressFraction: Float,
    val estimatedReachDate: String,
    val isCompleted: Boolean
)

data class YeterUiState(
    val currentAppVersion: String = "v1.0.5",
    val selectedTab: Int = 0, // 0: Sayaç, 1: Hedefler
    val onboarded: Boolean = true,

    // Para birimi (tüm birikim değerleri ana para biriminde tutulur)
    val homeCurrency: String = "TRY",
    val symbol: String = "₺",
    val rates: Map<String, Double> = emptyMap(),
    val rateUpdatedAt: Long = 0L,

    val targetName: String = "Hedef ekle",
    val targetAmount: Double = 0.0,
    val dailySaving: Double = 30.0, // Bugün için geçerli günlük tasarruf tutarı
    val startTimestamp: Long = System.currentTimeMillis(),

    // İstatistik
    val resistedTotal: Double = 0.0,
    val spentTotal: Double = 0.0,
    val resistedCount: Int = 0,
    val slipCount: Int = 0,
    val baselineSaved: Double = 0.0,
    val todayEarned: Double = 0.0,

    // Günlük tutar sorusu (her sabah)
    val needsDailyPrompt: Boolean = false,

    // Dürtü oturumu
    val urgeActive: Boolean = false,
    val urgeName: String = "",
    val urgeAmount: Double = 0.0,
    val urgeRemainingSec: Long = 0,

    // Canlı Süre
    val elapsedDays: Long = 0,
    val elapsedHours: Long = 0,
    val elapsedMinutes: Long = 0,
    val elapsedSeconds: Long = 0,

    // Canlı Birikim ve Hedef
    val totalSaved: Double = 0.0,
    val daysRemaining: Int = 0,
    val progressFraction: Float = 0f,
    val estimatedReachDate: String = "",
    val motivationMessage: String = "",

    // Hedefler (Vitrin)
    val wishlist: List<WishlistItemUi> = emptyList(),
    val showAddWishlistDialog: Boolean = false,

    // Dialog Kontrolleri
    val showUrgeDialog: Boolean = false,
    val showEditDialog: Boolean = false,
    val showResetConfirmDialog: Boolean = false,

    // Güncelleme Kontrolü
    val hasUpdate: Boolean = false,
    val updateVersion: String = "",
    val updateDownloadUrl: String = "",
    val showUpdateDialog: Boolean = false,

    // Dönüm Noktaları
    val milestones: List<Milestone> = listOf(
        Milestone(3, "3 Günlük Kararlılık", "Sıcak bir filtre kahve ve teşekkür", "☕"),
        Milestone(7, "1 Hafta Tertemiz", "Gelişimine katkı sağlayacak bir kitap", "📚"),
        Milestone(14, "2 Haftalık İrade", "Sinema bileti veya güzel bir akşam yemeği", "🎬"),
        Milestone(30, "1 Ay Başarısı", "Tebrikler! Ciddi bir birikim oluştu", "⭐"),
        Milestone(48, "48 Gün Şampiyonu", "Hedeflerine büyük adım! Gönül rahatlığıyla ödüllendir", "🏆")
    )
)

class YeterViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("yeter_app_prefs", Context.MODE_PRIVATE)

    private var goals: MutableList<WishlistItem> = mutableListOf()

    private val _uiState = MutableStateFlow(loadInitialState())
    val uiState: StateFlow<YeterUiState> = _uiState.asStateFlow()

    init {
        goals = Savings.loadGoals(prefs) ?: mutableListOf()
        startLiveTimer()
        startRateRefresh()
        checkForUpdates()
    }

    private fun loadInitialState(): YeterUiState {
        val now = System.currentTimeMillis()
        if (!prefs.contains("start_timestamp")) {
            prefs.edit().putLong("start_timestamp", now).apply()
        }
        val segs = Savings.loadSegments(prefs, now)
        val home = prefs.getString("home_currency", "TRY") ?: "TRY"
        return YeterUiState(
            onboarded = prefs.getBoolean("onboarded", prefs.contains("claude_seeded")),
            homeCurrency = home,
            symbol = currencySymbol(home),
            rates = Rates.load(prefs),
            rateUpdatedAt = prefs.getLong("rate_ts", 0L),
            dailySaving = segs.last().second,
            startTimestamp = segs.first().first,
            resistedTotal = prefs.getFloat("resisted_total", 0f).toDouble(),
            spentTotal = prefs.getFloat("spent_total", 0f).toDouble(),
            resistedCount = prefs.getInt("resisted_count", 0),
            slipCount = prefs.getInt("slip_count", 0),
            urgeActive = prefs.contains("urge_end"),
            urgeName = prefs.getString("urge_name", "") ?: "",
            urgeAmount = prefs.getFloat("urge_amount", 0f).toDouble()
        )
    }

    private fun startLiveTimer() {
        viewModelScope.launch {
            while (true) {
                updateTimeAndCalculations()
                delay(1000L)
            }
        }
    }

    // Kurlar uygulama açıkken saatte bir arka planda yenilenir (bildirim işi de günde bir yeniler)
    private fun startRateRefresh() {
        viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                val last = prefs.getLong("rate_ts", 0L)
                if (System.currentTimeMillis() - last > 3600_000L) {
                    if (Rates.fetch(prefs)) {
                        _uiState.update {
                            it.copy(rates = Rates.load(prefs), rateUpdatedAt = prefs.getLong("rate_ts", 0L))
                        }
                    }
                }
                delay(600_000L)
            }
        }
    }

    private fun toHome(item: WishlistItem, state: YeterUiState): Double =
        Rates.convert(item.price, item.currency, state.homeCurrency, state.rates)

    private fun updateTimeAndCalculations() {
        _uiState.update { state ->
            val now = System.currentTimeMillis()
            val segs = Savings.loadSegments(prefs, now)
            val startTs = segs.first().first
            val totalSeconds = max(0L, now - startTs) / 1000L

            val days = totalSeconds / 86400L
            val hours = (totalSeconds % 86400L) / 3600L
            val minutes = (totalSeconds % 3600L) / 60L
            val seconds = totalSeconds % 60L

            val baseline = Savings.baseline(segs, now)
            val savedMoney = max(0.0, baseline + state.resistedTotal - state.spentTotal)
            val daily = segs.last().second
            val todayEarned = Savings.todayPart(segs, now)

            val needsPrompt = state.onboarded &&
                (prefs.getString("last_prompt_day", "") ?: "") != Savings.dayKey(now)

            // Dürtü bekleme süresi
            val urgeEnd = prefs.getLong("urge_end", 0L)
            val urgeRemaining = if (state.urgeActive) max(0L, (urgeEnd - now) / 1000L) else 0L

            // Ana hedef: listedeki ilk hedef
            val primary = goals.firstOrNull()
            val targetName = primary?.name ?: "Hedef ekle"
            val targetAmount = primary?.let { toHome(it, state) } ?: 0.0

            val remainingMoney = max(0.0, targetAmount - savedMoney)
            val daysLeft = if (daily > 0 && primary != null) {
                kotlin.math.ceil(remainingMoney / daily).toInt()
            } else 0

            val progress = if (targetAmount > 0) {
                (savedMoney / targetAmount).toFloat().coerceIn(0f, 1f)
            } else 0f

            val dateFormat = SimpleDateFormat("dd MMMM yyyy", Locale("tr", "TR"))
            val reachDateStr = if (primary != null && daily > 0) {
                dateFormat.format(Date(now + (daysLeft * 86400 * 1000L)))
            } else "-"

            val motivation = when {
                primary == null -> "Önce bir hedef ekle; dürtüye her direnişin onu yaklaştırır."
                progress >= 1f -> "🎉 TEBRİKLER! Hedefine ulaştın! İstediğin şeyi artık gururla alabilirsin!"
                days < 1 -> "Harika bir başlangıç! İlk günün her saniyesi birikimine işliyor."
                days in 1..6 -> "Harika gidiyorsun! Gereksiz harcamaları durdurdukça özgürleşiyorsun."
                days in 7..29 -> "1 haftayı devirdin! Alışveriş dürtünü kontrol edebiliyorsun."
                else -> "$days gündür kararlısın! Hedefe adım adım yaklaşıyorsun."
            }

            val itemsUi = goals.map { item ->
                val priceHome = toHome(item, state)
                val remHome = max(0.0, priceHome - savedMoney)
                val isDone = remHome <= 0.0

                val remSecTotal = if (daily > 0 && !isDone) {
                    ((remHome / daily) * 86400.0).toLong()
                } else 0L

                val itemProgress = if (priceHome > 0) {
                    (savedMoney / priceHome).toFloat().coerceIn(0f, 1f)
                } else 0f

                WishlistItemUi(
                    id = item.id,
                    name = item.name,
                    price = priceHome,
                    currency = item.currency,
                    symbol = state.symbol,
                    ownPrice = item.price,
                    ownSymbol = currencySymbol(item.currency),
                    recurring = item.recurring,
                    remainingMoney = remHome,
                    daysRemaining = remSecTotal / 86400L,
                    hoursRemaining = (remSecTotal % 86400L) / 3600L,
                    minutesRemaining = (remSecTotal % 3600L) / 60L,
                    secondsRemaining = remSecTotal % 60L,
                    progressFraction = itemProgress,
                    estimatedReachDate = if (daily > 0 && !isDone) dateFormat.format(Date(now + remSecTotal * 1000L)) else "-",
                    isCompleted = isDone
                )
            }

            state.copy(
                startTimestamp = startTs,
                dailySaving = daily,
                elapsedDays = days,
                elapsedHours = hours,
                elapsedMinutes = minutes,
                elapsedSeconds = seconds,
                baselineSaved = baseline,
                todayEarned = todayEarned,
                needsDailyPrompt = needsPrompt,
                totalSaved = savedMoney,
                urgeRemainingSec = urgeRemaining,
                targetName = targetName,
                targetAmount = targetAmount,
                daysRemaining = daysLeft,
                progressFraction = progress,
                estimatedReachDate = reachDateStr,
                motivationMessage = motivation,
                wishlist = itemsUi
            )
        }
    }

    fun selectTab(index: Int) {
        _uiState.update { it.copy(selectedTab = index) }
    }

    fun openAddWishlistDialog(open: Boolean) {
        _uiState.update { it.copy(showAddWishlistDialog = open) }
    }

    fun addWishlistItem(name: String, price: Double, currency: String, recurring: Boolean) {
        val trimmed = name.trim()
        if (trimmed.isNotEmpty() && price > 0) {
            goals.add(0, WishlistItem(name = trimmed, price = price, currency = currency, recurring = recurring))
            Savings.saveGoals(prefs, goals)
            _uiState.update { it.copy(showAddWishlistDialog = false) }
            updateTimeAndCalculations()
        }
    }

    fun deleteWishlistItem(id: String) {
        goals.removeAll { it.id == id }
        Savings.saveGoals(prefs, goals)
        updateTimeAndCalculations()
    }

    // ---- Karşılama (ilk açılış) ----
    fun completeOnboarding(
        homeCurrency: String,
        daily: Double,
        goalName: String,
        goalPrice: Double,
        goalCurrency: String,
        recurring: Boolean
    ) {
        val now = System.currentTimeMillis()
        Savings.saveSegments(prefs, listOf(now to daily))
        prefs.edit()
            .putString("home_currency", homeCurrency)
            .putLong("start_timestamp", now)
            .putFloat("daily_saving", daily.toFloat())
            .putString("last_prompt_day", Savings.dayKey(now))
            .putBoolean("onboarded", true)
            .apply()
        if (goalName.isNotBlank() && goalPrice > 0) {
            goals.add(0, WishlistItem(name = goalName.trim(), price = goalPrice, currency = goalCurrency, recurring = recurring))
            Savings.saveGoals(prefs, goals)
        }
        _uiState.update {
            it.copy(
                onboarded = true,
                homeCurrency = homeCurrency,
                symbol = currencySymbol(homeCurrency),
                dailySaving = daily,
                startTimestamp = now
            )
        }
        updateTimeAndCalculations()
    }

    // ---- Günlük tutar (her sabah) ----
    // Bugünün başından itibaren geçerli yeni günlük tutarı kaydeder; aynıysa sadece soruyu kapatır.
    fun confirmDailyAmount(amount: Double) {
        val now = System.currentTimeMillis()
        val segs = Savings.loadSegments(prefs, now).toMutableList()
        val amt = max(0.0, amount)
        if (amt != segs.last().second) {
            val midnight = Savings.startOfDay(now)
            if (segs.last().first >= midnight) {
                segs[segs.size - 1] = segs.last().first to amt
            } else {
                segs.add(midnight to amt)
            }
            Savings.saveSegments(prefs, segs)
            prefs.edit().putFloat("daily_saving", amt.toFloat()).apply()
        }
        prefs.edit().putString("last_prompt_day", Savings.dayKey(now)).apply()
        updateTimeAndCalculations()
    }

    private fun checkForUpdates() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val url = URL("https://api.github.com/repos/lyldz/Yeter/releases/latest")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.setRequestProperty("Accept", "application/vnd.github.v3+json")
                conn.setRequestProperty("User-Agent", "YeterApp")
                conn.connectTimeout = 6000
                conn.readTimeout = 6000

                if (conn.responseCode == 200) {
                    val reader = BufferedReader(InputStreamReader(conn.inputStream))
                    val body = reader.readText()
                    reader.close()

                    val json = JSONObject(body)
                    val latestTag = json.optString("tag_name", "")
                    val htmlUrl = json.optString("html_url", "https://github.com/lyldz/Yeter/releases")

                    var apkDownloadUrl = htmlUrl
                    val assets = json.optJSONArray("assets")
                    if (assets != null) {
                        for (i in 0 until assets.length()) {
                            val asset = assets.getJSONObject(i)
                            if (asset.optString("name").endsWith(".apk")) {
                                apkDownloadUrl = asset.optString("browser_download_url", htmlUrl)
                                break
                            }
                        }
                    }

                    if (isNewVersionAvailable(latestTag, _uiState.value.currentAppVersion)) {
                        _uiState.update {
                            it.copy(
                                hasUpdate = true,
                                updateVersion = latestTag,
                                updateDownloadUrl = apkDownloadUrl,
                                showUpdateDialog = true
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                // Sessizce geç
            }
        }
    }

    private fun isNewVersionAvailable(remoteTag: String, currentTag: String): Boolean {
        val cleanRemote = remoteTag.removePrefix("v").trim()
        val cleanCurrent = currentTag.removePrefix("v").trim()
        return cleanRemote.isNotEmpty() && cleanRemote != cleanCurrent
    }

    fun updateSettings(daily: Double, homeCurrency: String) {
        val now = System.currentTimeMillis()
        val old = _uiState.value
        if (homeCurrency != old.homeCurrency) {
            // Tüm birikim değerlerini yeni para birimine çevir
            val f = { v: Double -> Rates.convert(v, old.homeCurrency, homeCurrency, old.rates) }
            val segs = Savings.loadSegments(prefs, now).map { it.first to f(it.second) }
            Savings.saveSegments(prefs, segs)
            prefs.edit()
                .putString("home_currency", homeCurrency)
                .putFloat("resisted_total", f(old.resistedTotal).toFloat())
                .putFloat("spent_total", f(old.spentTotal).toFloat())
                .apply()
            _uiState.update {
                it.copy(
                    homeCurrency = homeCurrency,
                    symbol = currencySymbol(homeCurrency),
                    resistedTotal = f(it.resistedTotal),
                    spentTotal = f(it.spentTotal)
                )
            }
        }
        confirmDailyAmount(daily)
        _uiState.update { it.copy(showEditDialog = false) }
        updateTimeAndCalculations()
    }

    // Hedefe ulaşıldı: tutarı havuzdan düş. Tekrarlı hedef listede kalır.
    fun claimGoal(id: String) {
        val item = goals.firstOrNull { it.id == id } ?: return
        val cost = toHome(item, _uiState.value)
        val spent = _uiState.value.spentTotal + cost
        val editor = prefs.edit().putFloat("spent_total", spent.toFloat())
        listOf(50, 75, 90, 100).forEach { editor.remove("notif_${id}_$it") }
        editor.apply()
        _uiState.update { it.copy(spentTotal = spent) }
        if (!item.recurring) {
            goals.removeAll { it.id == id }
            Savings.saveGoals(prefs, goals)
        }
        updateTimeAndCalculations()
    }

    // ---- Dürtü akışı ----
    fun startUrge(name: String, amount: Double, waitMinutes: Int = 10) {
        val end = System.currentTimeMillis() + waitMinutes * 60_000L
        prefs.edit()
            .putString("urge_name", name.trim())
            .putFloat("urge_amount", amount.toFloat())
            .putLong("urge_end", end)
            .apply()
        _uiState.update {
            it.copy(urgeActive = true, urgeName = name.trim(), urgeAmount = amount)
        }
        updateTimeAndCalculations()
    }

    private fun clearUrgePrefs() {
        prefs.edit().remove("urge_name").remove("urge_amount").remove("urge_end").apply()
    }

    // Bekleme bitti ve vazgeçti: tutar havuza eklenir
    fun resistUrge() {
        val s = _uiState.value
        if (!s.urgeActive || s.urgeRemainingSec > 0) return
        val total = s.resistedTotal + s.urgeAmount
        val count = s.resistedCount + 1
        prefs.edit()
            .putFloat("resisted_total", total.toFloat())
            .putInt("resisted_count", count)
            .apply()
        clearUrgePrefs()
        _uiState.update {
            it.copy(
                resistedTotal = total, resistedCount = count,
                urgeActive = false, urgeName = "", urgeAmount = 0.0, showUrgeDialog = false
            )
        }
        updateTimeAndCalculations()
    }

    // Yenildi: dürüstçe kaydet, havuza eklenmez
    fun slipUrge() {
        val count = _uiState.value.slipCount + 1
        prefs.edit().putInt("slip_count", count).apply()
        clearUrgePrefs()
        _uiState.update {
            it.copy(slipCount = count, urgeActive = false, urgeName = "", urgeAmount = 0.0, showUrgeDialog = false)
        }
        updateTimeAndCalculations()
    }

    fun resetStreak() {
        val now = System.currentTimeMillis()
        val daily = _uiState.value.dailySaving
        Savings.saveSegments(prefs, listOf(now to daily))
        prefs.edit().putLong("start_timestamp", now).apply()
        _uiState.update { it.copy(startTimestamp = now, showResetConfirmDialog = false) }
        updateTimeAndCalculations()
    }

    fun openUrgeDialog(open: Boolean) {
        _uiState.update { it.copy(showUrgeDialog = open) }
    }

    fun openEditDialog(open: Boolean) {
        _uiState.update { it.copy(showEditDialog = open) }
    }

    fun openResetDialog(open: Boolean) {
        _uiState.update { it.copy(showResetConfirmDialog = open) }
    }

    fun closeUpdateDialog() {
        _uiState.update { it.copy(showUpdateDialog = false) }
    }
}

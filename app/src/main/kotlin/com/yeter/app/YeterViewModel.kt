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
import org.json.JSONArray
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
    val currency: String = "TRY", // "TRY" | "USD"
    val recurring: Boolean = false, // Aylık tekrar eden hedef (ör. Claude Pro)
    val addedTimestamp: Long = System.currentTimeMillis()
)

data class WishlistItemUi(
    val id: String,
    val name: String,
    val price: Double, // gösterim para biriminde
    val currency: String, // hedefin kendi para birimi
    val symbol: String, // gösterim sembolü
    val recurring: Boolean,
    val remainingMoney: Double, // gösterim para biriminde
    val daysRemaining: Long,
    val hoursRemaining: Long,
    val minutesRemaining: Long,
    val secondsRemaining: Long,
    val progressFraction: Float,
    val estimatedReachDate: String,
    val isCompleted: Boolean
)

data class YeterUiState(
    val currentAppVersion: String = "v1.0.4",
    val selectedTab: Int = 0, // 0: Sayaç, 1: Vitrin

    val usdRate: Double = 40.0, // 1 USD kaç TL (günlük otomatik güncellenir)
    val rateUpdatedAt: Long = 0L,
    val displayCurrency: String = "TRY", // Vitrin'de gösterilecek para birimi
    val resistedTotal: Double = 0.0, // Dürtüye direnerek eklenen toplam (TL)
    val spentTotal: Double = 0.0, // Hedeflere harcanan toplam (TL)
    val resistedCount: Int = 0,
    val slipCount: Int = 0,
    val baselineSaved: Double = 0.0,

    // Dürtü oturumu
    val urgeActive: Boolean = false,
    val urgeName: String = "",
    val urgeAmount: Double = 0.0,
    val urgeRemainingSec: Long = 0,

    val targetName: String = "Asıl İhtiyacım / Hedefim",
    val targetAmount: Double = 540.0,
    val dailySaving: Double = 30.0, // Günlük harcanmayan miktar (1 Simit & Çay = 30 TL)
    val startTimestamp: Long = System.currentTimeMillis(),

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

    // Vitrin (Wishlist)
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
        Milestone(48, "48 Gün Şampiyonu", "Toplam paramız hedefe ulaştı! Artık gönül rahatlığıyla alabilirsin", "🏆")
    )
)

class YeterViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("yeter_app_prefs", Context.MODE_PRIVATE)

    private var rawWishlist: MutableList<WishlistItem> = mutableListOf()

    private val _uiState = MutableStateFlow(loadInitialState())
    val uiState: StateFlow<YeterUiState> = _uiState.asStateFlow()

    init {
        loadWishlistFromPrefs()
        seedClaudeProOnce()
        startLiveTimer()
        startDailyRateRefresh()
        checkForUpdates()
    }

    private fun loadInitialState(): YeterUiState {
        val targetName = prefs.getString("target_name", "Sony Kulaklık / Hedef") ?: "Sony Kulaklık / Hedef"
        val targetAmount = prefs.getFloat("target_amount", 540.0f).toDouble()
        val dailySaving = prefs.getFloat("daily_saving", 30.0f).toDouble()

        val now = System.currentTimeMillis()
        val startTimestamp = prefs.getLong("start_timestamp", now)
        if (!prefs.contains("start_timestamp")) {
            prefs.edit().putLong("start_timestamp", now).apply()
        }

        return YeterUiState(
            usdRate = prefs.getFloat("usd_rate", 40.0f).toDouble(),
            rateUpdatedAt = prefs.getLong("rate_ts", 0L),
            displayCurrency = prefs.getString("display_currency", "TRY") ?: "TRY",
            resistedTotal = prefs.getFloat("resisted_total", 0f).toDouble(),
            spentTotal = prefs.getFloat("spent_total", 0f).toDouble(),
            resistedCount = prefs.getInt("resisted_count", 0),
            slipCount = prefs.getInt("slip_count", 0),
            urgeActive = prefs.contains("urge_end"),
            urgeName = prefs.getString("urge_name", "") ?: "",
            urgeAmount = prefs.getFloat("urge_amount", 0f).toDouble(),
            targetName = targetName,
            targetAmount = targetAmount,
            dailySaving = dailySaving,
            startTimestamp = startTimestamp
        )
    }

    private fun loadWishlistFromPrefs() {
        val jsonStr = prefs.getString("wishlist_json", null)
        rawWishlist = if (jsonStr != null) {
            try {
                val array = JSONArray(jsonStr)
                val list = mutableListOf<WishlistItem>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        WishlistItem(
                            id = obj.getString("id"),
                            name = obj.getString("name"),
                            price = obj.getDouble("price"),
                            currency = obj.optString("currency", "TRY"),
                            recurring = obj.optBoolean("recurring", false),
                            addedTimestamp = obj.optLong("addedTimestamp", System.currentTimeMillis())
                        )
                    )
                }
                list
            } catch (e: Exception) {
                createDefaultWishlist()
            }
        } else {
            createDefaultWishlist()
        }
    }

    private fun seedClaudeProOnce() {
        if (prefs.getBoolean("claude_seeded", false)) return
        val demoNames = setOf("Altın Bileklik", "Kablosuz Kulaklık")
        if (rawWishlist.isNotEmpty() && rawWishlist.all { it.name in demoNames }) rawWishlist.clear()
        if (rawWishlist.none { it.name.contains("Claude", true) }) {
            rawWishlist.add(0, WishlistItem(name = "Claude Pro (aylık)", price = 20.0, currency = "USD", recurring = true))
        }
        saveWishlistToPrefs(rawWishlist)
        prefs.edit().putBoolean("claude_seeded", true).apply()
    }

    // Kuru günde bir kez internetten çek; olmazsa son bilinen/elle girilen kur kalır
    private fun startDailyRateRefresh() {
        viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                val last = prefs.getLong("rate_ts", 0L)
                if (System.currentTimeMillis() - last > 24L * 3600_000L) fetchUsdRate()
                delay(3600_000L)
            }
        }
    }

    private fun fetchUsdRate() {
        try {
            val conn = URL("https://open.er-api.com/v6/latest/USD").openConnection() as HttpURLConnection
            conn.connectTimeout = 6000
            conn.readTimeout = 6000
            if (conn.responseCode == 200) {
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val rate = JSONObject(body).getJSONObject("rates").getDouble("TRY")
                if (rate > 0) {
                    val now = System.currentTimeMillis()
                    prefs.edit().putFloat("usd_rate", rate.toFloat()).putLong("rate_ts", now).apply()
                    _uiState.update { it.copy(usdRate = rate, rateUpdatedAt = now) }
                    updateTimeAndCalculations()
                }
            }
        } catch (e: Exception) {
            // Sessizce geç: son bilinen kur kullanılır
        }
    }

    private fun createDefaultWishlist(): MutableList<WishlistItem> {
        val defaults = mutableListOf(
            WishlistItem(name = "Claude Pro (aylık)", price = 20.0, currency = "USD", recurring = true)
        )
        saveWishlistToPrefs(defaults)
        return defaults
    }

    private fun saveWishlistToPrefs(list: List<WishlistItem>) {
        val array = JSONArray()
        for (item in list) {
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("name", item.name)
            obj.put("price", item.price)
            obj.put("currency", item.currency)
            obj.put("recurring", item.recurring)
            obj.put("addedTimestamp", item.addedTimestamp)
            array.put(obj)
        }
        prefs.edit().putString("wishlist_json", array.toString()).apply()
    }

    private fun startLiveTimer() {
        viewModelScope.launch {
            while (true) {
                updateTimeAndCalculations()
                delay(1000L)
            }
        }
    }

    private fun updateTimeAndCalculations() {
        _uiState.update { state ->
            val now = System.currentTimeMillis()
            val diffMillis = max(0L, now - state.startTimestamp)
            val totalSeconds = diffMillis / 1000L

            val days = totalSeconds / 86400L
            val hours = (totalSeconds % 86400L) / 3600L
            val minutes = (totalSeconds % 3600L) / 60L
            val seconds = totalSeconds % 60L

            // Canlı birikim hesabı: Günlük tasarruf / 86400 * saniye
            val baseline = (totalSeconds.toDouble() / 86400.0) * state.dailySaving
            val savedMoney = max(0.0, baseline + state.resistedTotal - state.spentTotal)

            // Dürtü bekleme süresi
            val urgeEnd = prefs.getLong("urge_end", 0L)
            val urgeRemaining = if (state.urgeActive) max(0L, (urgeEnd - now) / 1000L) else 0L

            // Ana hedef: listedeki ilk hedef (TL karşılığı)
            val primary = rawWishlist.firstOrNull()
            val targetName = primary?.name ?: state.targetName
            val targetAmount = primary?.let { toTry(it, state.usdRate) } ?: state.targetAmount

            val remainingMoney = max(0.0, targetAmount - savedMoney)
            val daysLeft = if (state.dailySaving > 0) {
                (remainingMoney / state.dailySaving).toInt() + 1
            } else 0

            val progress = if (targetAmount > 0) {
                (savedMoney / targetAmount).toFloat().coerceIn(0f, 1f)
            } else 0f

            val dateFormat = SimpleDateFormat("dd MMMM yyyy", Locale("tr", "TR"))
            val targetReachTime = now + (daysLeft * 86400 * 1000L)
            val reachDateStr = dateFormat.format(Date(targetReachTime))

            val motivation = when {
                progress >= 1f -> "🎉 TEBRİKLER! Hedefine ulaştın! İstediğin şeyi artık gururla alabilirsin!"
                days < 1 -> "Harika bir başlangıç! İlk günün her saniyesi birikimine işliyor."
                days in 1..6 -> "Harika gidiyorsun! Gereksiz harcamaları durdurdukça özgürleşiyorsun."
                days in 7..29 -> "1 haftayı devirdin! Alışveriş dürtünü kontrol edebiliyorsun."
                else -> "${days} gündür nefsine hakimsin! Hedefe adım adım yaklaşıyorsun."
            }

            // Vitrin (Wishlist) Geri Sayım Hesaplamaları
            val updatedWishlistUi = rawWishlist.map { item ->
                val priceTry = toTry(item, state.usdRate)
                val remTry = max(0.0, priceTry - savedMoney)
                val isDone = remTry <= 0.0
                val toDisp = { tl: Double -> if (state.displayCurrency == "USD" && state.usdRate > 0) tl / state.usdRate else tl }
                val remMoney = toDisp(remTry)
                val dispPrice = toDisp(priceTry)
                val dispSymbol = if (state.displayCurrency == "USD") "$" else "₺"

                val remSecTotal = if (state.dailySaving > 0 && !isDone) {
                    ((remTry / state.dailySaving) * 86400.0).toLong()
                } else 0L

                val rDays = remSecTotal / 86400L
                val rHours = (remSecTotal % 86400L) / 3600L
                val rMin = (remSecTotal % 3600L) / 60L
                val rSec = remSecTotal % 60L

                val itemProgress = if (priceTry > 0) {
                    (savedMoney / priceTry).toFloat().coerceIn(0f, 1f)
                } else 0f

                val itemReachTime = now + (remSecTotal * 1000L)
                val itemDateStr = dateFormat.format(Date(itemReachTime))

                WishlistItemUi(
                    id = item.id,
                    name = item.name,
                    price = dispPrice,
                    currency = item.currency,
                    symbol = dispSymbol,
                    recurring = item.recurring,
                    remainingMoney = remMoney,
                    daysRemaining = rDays,
                    hoursRemaining = rHours,
                    minutesRemaining = rMin,
                    secondsRemaining = rSec,
                    progressFraction = itemProgress,
                    estimatedReachDate = itemDateStr,
                    isCompleted = isDone
                )
            }

            state.copy(
                elapsedDays = days,
                elapsedHours = hours,
                elapsedMinutes = minutes,
                elapsedSeconds = seconds,
                baselineSaved = baseline,
                totalSaved = savedMoney,
                urgeRemainingSec = urgeRemaining,
                targetName = targetName,
                targetAmount = targetAmount,
                daysRemaining = daysLeft,
                progressFraction = progress,
                estimatedReachDate = reachDateStr,
                motivationMessage = motivation,
                wishlist = updatedWishlistUi
            )
        }
    }

    fun selectTab(index: Int) {
        _uiState.update { it.copy(selectedTab = index) }
    }

    fun openAddWishlistDialog(open: Boolean) {
        _uiState.update { it.copy(showAddWishlistDialog = open) }
    }

    private fun toTry(item: WishlistItem, rate: Double): Double =
        if (item.currency == "USD") item.price * rate else item.price

    fun addWishlistItem(name: String, price: Double, currency: String, recurring: Boolean) {
        val trimmed = name.trim()
        if (trimmed.isNotEmpty() && price > 0) {
            val item = WishlistItem(name = trimmed, price = price, currency = currency, recurring = recurring)
            rawWishlist.add(0, item)
            saveWishlistToPrefs(rawWishlist)
            _uiState.update { it.copy(showAddWishlistDialog = false) }
            updateTimeAndCalculations()
        }
    }

    fun deleteWishlistItem(id: String) {
        rawWishlist.removeAll { it.id == id }
        saveWishlistToPrefs(rawWishlist)
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

    fun updateSettings(daily: Double, usdRate: Double, displayCurrency: String) {
        prefs.edit()
            .putFloat("daily_saving", daily.toFloat())
            .putFloat("usd_rate", usdRate.toFloat())
            .putString("display_currency", displayCurrency)
            .apply()
        _uiState.update {
            it.copy(dailySaving = daily, usdRate = usdRate, displayCurrency = displayCurrency, showEditDialog = false)
        }
        updateTimeAndCalculations()
    }

    // Hedefe ulaşıldı: tutarı havuzdan düş. Tekrarlı hedef listede kalır.
    fun claimGoal(id: String) {
        val item = rawWishlist.firstOrNull { it.id == id } ?: return
        val cost = toTry(item, _uiState.value.usdRate)
        val spent = _uiState.value.spentTotal + cost
        prefs.edit().putFloat("spent_total", spent.toFloat()).apply()
        _uiState.update { it.copy(spentTotal = spent) }
        if (!item.recurring) {
            rawWishlist.removeAll { it.id == id }
            saveWishlistToPrefs(rawWishlist)
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
        prefs.edit().putLong("start_timestamp", now).apply()
        _uiState.update {
            it.copy(
                startTimestamp = now,
                showResetConfirmDialog = false
            )
        }
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

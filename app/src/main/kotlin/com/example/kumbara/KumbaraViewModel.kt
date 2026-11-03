package com.example.kumbara

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
    val addedTimestamp: Long = System.currentTimeMillis()
)

data class WishlistItemUi(
    val id: String,
    val name: String,
    val price: Double,
    val remainingMoney: Double,
    val daysRemaining: Long,
    val hoursRemaining: Long,
    val minutesRemaining: Long,
    val secondsRemaining: Long,
    val progressFraction: Float,
    val estimatedReachDate: String,
    val isCompleted: Boolean
)

data class KumbaraUiState(
    val currentAppVersion: String = "v1.0.3",
    val selectedTab: Int = 0, // 0: Kumbara, 1: Vitrin

    val targetName: String = "Asıl İhtiyacım / Hedefim",
    val targetAmount: Double = 540.0,
    val dailySaving: Double = 30.0, // Günlük harcanmayan miktar (1 Simit & Çay = 30 TL)
    val startTimestamp: Long = System.currentTimeMillis(),

    // Canlı Süre
    val elapsedDays: Long = 0,
    val elapsedHours: Long = 0,
    val elapsedMinutes: Long = 0,
    val elapsedSeconds: Long = 0,

    // Canlı Kumbara ve Hedef
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

class KumbaraViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("yeter_app_prefs", Context.MODE_PRIVATE)

    private var rawWishlist: MutableList<WishlistItem> = mutableListOf()

    private val _uiState = MutableStateFlow(loadInitialState())
    val uiState: StateFlow<KumbaraUiState> = _uiState.asStateFlow()

    init {
        loadWishlistFromPrefs()
        startLiveTimer()
        checkForUpdates()
    }

    private fun loadInitialState(): KumbaraUiState {
        val targetName = prefs.getString("target_name", "Sony Kulaklık / Hedef") ?: "Sony Kulaklık / Hedef"
        val targetAmount = prefs.getFloat("target_amount", 540.0f).toDouble()
        val dailySaving = prefs.getFloat("daily_saving", 30.0f).toDouble()

        val now = System.currentTimeMillis()
        val startTimestamp = prefs.getLong("start_timestamp", now)
        if (!prefs.contains("start_timestamp")) {
            prefs.edit().putLong("start_timestamp", now).apply()
        }

        return KumbaraUiState(
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

    private fun createDefaultWishlist(): MutableList<WishlistItem> {
        val defaults = mutableListOf(
            WishlistItem(name = "Altın Bileklik", price = 9500.0),
            WishlistItem(name = "Kablosuz Kulaklık", price = 4500.0)
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

            // Canlı kumbara hesabı: Günlük tasarruf / 86400 * saniye
            val savedMoney = (totalSeconds.toDouble() / 86400.0) * state.dailySaving
            val remainingMoney = max(0.0, state.targetAmount - savedMoney)
            val daysLeft = if (state.dailySaving > 0) {
                (remainingMoney / state.dailySaving).toInt() + 1
            } else 0

            val progress = if (state.targetAmount > 0) {
                (savedMoney / state.targetAmount).toFloat().coerceIn(0f, 1f)
            } else 0f

            val dateFormat = SimpleDateFormat("dd MMMM yyyy", Locale("tr", "TR"))
            val targetReachTime = now + (daysLeft * 86400 * 1000L)
            val reachDateStr = dateFormat.format(Date(targetReachTime))

            val motivation = when {
                progress >= 1f -> "🎉 TEBRİKLER! Hedefine ulaştın! İstediğin şeyi artık gururla alabilirsin!"
                days < 1 -> "Harika bir başlangıç! İlk günün her saniyesi kumbarana işliyor."
                days in 1..6 -> "Harika gidiyorsun! Gereksiz harcamaları durdurdukça özgürleşiyorsun."
                days in 7..29 -> "1 haftayı devirdin! Alışveriş dürtünü kontrol edebiliyorsun."
                else -> "${days} gündür nefsine hakimsin! Hedefe adım adım yaklaşıyorsun."
            }

            // Vitrin (Wishlist) Geri Sayım Hesaplamaları
            val updatedWishlistUi = rawWishlist.map { item ->
                val remMoney = max(0.0, item.price - savedMoney)
                val isDone = remMoney <= 0.0

                val remSecTotal = if (state.dailySaving > 0 && !isDone) {
                    ((remMoney / state.dailySaving) * 86400.0).toLong()
                } else 0L

                val rDays = remSecTotal / 86400L
                val rHours = (remSecTotal % 86400L) / 3600L
                val rMin = (remSecTotal % 3600L) / 60L
                val rSec = remSecTotal % 60L

                val itemProgress = if (item.price > 0) {
                    (savedMoney / item.price).toFloat().coerceIn(0f, 1f)
                } else 0f

                val itemReachTime = now + (remSecTotal * 1000L)
                val itemDateStr = dateFormat.format(Date(itemReachTime))

                WishlistItemUi(
                    id = item.id,
                    name = item.name,
                    price = item.price,
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
                totalSaved = savedMoney,
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

    fun addWishlistItem(name: String, price: Double) {
        val trimmed = name.trim()
        if (trimmed.isNotEmpty() && price > 0) {
            val item = WishlistItem(name = trimmed, price = price)
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

    fun updateGoal(name: String, amount: Double, daily: Double) {
        prefs.edit()
            .putString("target_name", name)
            .putFloat("target_amount", amount.toFloat())
            .putFloat("daily_saving", daily.toFloat())
            .apply()

        _uiState.update {
            it.copy(
                targetName = name,
                targetAmount = amount,
                dailySaving = daily,
                showEditDialog = false
            )
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

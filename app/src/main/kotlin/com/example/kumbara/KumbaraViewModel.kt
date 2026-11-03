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
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max

data class Milestone(
    val dayThreshold: Int,
    val title: String,
    val suggestedReward: String,
    val icon: String = "🎯"
)

data class KumbaraUiState(
    val currentAppVersion: String = "v1.0.1",
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

    private val _uiState = MutableStateFlow(loadInitialState())
    val uiState: StateFlow<KumbaraUiState> = _uiState.asStateFlow()

    init {
        startLiveTimer()
        checkForUpdates()
    }

    private fun loadInitialState(): KumbaraUiState {
        val targetName = prefs.getString("target_name", "Sony Kulaklık / Hedef") ?: "Sony Kulaklık / Hedef"
        val targetAmount = prefs.getFloat("target_amount", 540.0f).toDouble()
        // Günlük tasarruf: 1 simit ve çay parası (30 TL)
        val dailySaving = prefs.getFloat("daily_saving", 30.0f).toDouble()
        
        // İlk açılışta süreyi şu andan başlat (0 gün 0 saat)
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

            val targetReachTime = now + (daysLeft * 86400 * 1000L)
            val dateFormat = SimpleDateFormat("dd MMMM yyyy", Locale("tr", "TR"))
            val reachDateStr = dateFormat.format(Date(targetReachTime))

            val motivation = when {
                progress >= 1f -> "🎉 TEBRİKLER! Hedefine ulaştın! İstediğin şeyi artık gururla alabilirsin!"
                days < 1 -> "Harika bir başlangıç! İlk günün her saniyesi kumbarana işliyor."
                days in 1..6 -> "Harika gidiyorsun! Gereksiz harcamaları durdurdukça özgürleşiyorsun."
                days in 7..29 -> "1 haftayı devirdin! Alışveriş dürtünü kontrol edebiliyorsun."
                else -> "${days} gündür nefsine hakimsin! Hedefe adım adım yaklaşıyorsun."
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
                motivationMessage = motivation
            )
        }
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
                // Sessizce geç (çevrimdışıysa çökmesin)
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

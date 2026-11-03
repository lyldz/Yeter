package com.example.kumbara

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
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
    val targetName: String = "Asıl İhtiyacım / Hedefim",
    val targetAmount: Double = 540.0,
    val dailySaving: Double = 12.0, // Günlük harcanmayan miktar (TL)
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
    }

    private fun loadInitialState(): KumbaraUiState {
        val targetName = prefs.getString("target_name", "Sony Kulaklık / Hedef") ?: "Sony Kulaklık / Hedef"
        val targetAmount = prefs.getFloat("target_amount", 540.0f).toDouble()
        val dailySaving = prefs.getFloat("daily_saving", 15.0f).toDouble()
        val defaultStart = System.currentTimeMillis() - (2 * 86400 * 1000L) // Örnek olarak 2 gün önce başlasın
        val startTimestamp = prefs.getLong("start_timestamp", defaultStart)

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

            // Tahmini ulaşma tarihi
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
}

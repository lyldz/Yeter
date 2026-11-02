package com.example.kumbara

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.max

/** UI state exposed to the composable UI */
data class KumbaraUiState(
    val streakDays: Int = 0,
    val targetAmount: Int = 540, // TL – can be configurable later
    val dailySaving: Int = 1, // TL per day – configurable later
    val daysRemaining: Int = 0,
    val totalSaved: Int = 0,
    val message: String = ""
)

class KumbaraViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(KumbaraUiState())
    val uiState: StateFlow<KumbaraUiState> = _uiState

    init { recalculate() }

    fun incrementStreak() {
        _uiState.update { it.copy(streakDays = it.streakDays + 1) }
        recalculate()
    }

    private fun recalculate() {
        val s = _uiState.value
        val saved = s.streakDays * s.dailySaving
        val daysLeft = if (s.dailySaving > 0) {
            max(0, (s.targetAmount - saved + s.dailySaving - 1) / s.dailySaving)
        } else 0
        val msg = if (saved >= s.targetAmount) {
            "Hedefe ulaştınız!"
        } else {
            "${s.dailySaving} TL günlük biriktirerek ${s.targetAmount} TL hedefine ${daysLeft} gün kaldı."
        }
        _uiState.value = s.copy(
            totalSaved = saved,
            daysRemaining = daysLeft,
            message = msg
        )
    }
}

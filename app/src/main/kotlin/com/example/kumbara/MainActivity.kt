package com.example.kumbara

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

class MainActivity : ComponentActivity() {

    private val viewModel: KumbaraViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val darkTheme = isSystemInDarkTheme()
            val colorScheme = if (darkTheme) darkColorScheme() else lightColorScheme()

            MaterialTheme(colorScheme = colorScheme) {
                val state by viewModel.uiState.collectAsState()

                KumbaraScreen(
                    state = state,
                    onOpenUrge = { viewModel.openUrgeDialog(true) },
                    onCloseUrge = { viewModel.openUrgeDialog(false) },
                    onOpenEdit = { viewModel.openEditDialog(true) },
                    onCloseEdit = { viewModel.openEditDialog(false) },
                    onSaveGoal = { name, amount, daily -> viewModel.updateGoal(name, amount, daily) },
                    onOpenReset = { viewModel.openResetDialog(true) },
                    onCloseReset = { viewModel.openResetDialog(false) },
                    onConfirmReset = { viewModel.resetStreak() },
                    onCloseUpdate = { viewModel.closeUpdateDialog() },
                    onDownloadUpdate = { url ->
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            startActivity(intent)
                        } catch (e: Exception) {
                            // Intent açılamazsa fallback
                        }
                    }
                )
            }
        }
    }
}

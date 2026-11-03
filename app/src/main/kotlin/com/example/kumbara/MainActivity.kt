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
import androidx.compose.ui.graphics.Color

// Kırmızı & Beyaz Tema Renk Paleti
val YeterRedPrimary = Color(0xFFD32F2F)       // Canlı Kırmızı
val YeterRedDark = Color(0xFFB71C1C)          // Koyu Kırmızı
val YeterRedLight = Color(0xFFFFEBEE)         // Açık Kırmızı / Pembe zemin
val YeterRedSurface = Color(0xFFFFF5F5)       // Yumuşak Kırmızı Zemin
val YeterWhite = Color(0xFFFFFFFF)            // Saf Beyaz

val YeterLightColorScheme = lightColorScheme(
    primary = YeterRedPrimary,
    onPrimary = YeterWhite,
    primaryContainer = YeterRedLight,
    onPrimaryContainer = YeterRedDark,
    secondary = YeterRedDark,
    onSecondary = YeterWhite,
    secondaryContainer = Color(0xFFFFCDD2),
    onSecondaryContainer = Color(0xFF5D0005),
    surface = YeterWhite,
    onSurface = Color(0xFF1E1E1E),
    surfaceVariant = YeterRedSurface,
    onSurfaceVariant = Color(0xFF49454F),
    background = Color(0xFFFAFAFA),
    onBackground = Color(0xFF1E1E1E),
    error = Color(0xFFC62828),
    onError = YeterWhite
)

val YeterDarkColorScheme = darkColorScheme(
    primary = Color(0xFFFF897D),
    onPrimary = Color(0xFF68000A),
    primaryContainer = Color(0xFF8F0013),
    onPrimaryContainer = Color(0xFFFFDAD6),
    secondary = Color(0xFFFFB4AB),
    onSecondary = Color(0xFF561E1A),
    secondaryContainer = Color(0xFF73332E),
    onSecondaryContainer = Color(0xFFFFDAD6),
    surface = Color(0xFF1A1111),
    onSurface = Color(0xFFEDE0DF),
    surfaceVariant = Color(0xFF2E1C1C),
    onSurfaceVariant = Color(0xFFD7C1C0),
    background = Color(0xFF120B0B),
    onBackground = Color(0xFFEDE0DF),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005)
)

class MainActivity : ComponentActivity() {

    private val viewModel: KumbaraViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val darkTheme = isSystemInDarkTheme()
            val colorScheme = if (darkTheme) YeterDarkColorScheme else YeterLightColorScheme

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

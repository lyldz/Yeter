package com.yeter.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color

// "Kehribar" teması: sıcak koyu zemin + kehribar/turuncu vurgu (açık tema yok)
val YeterAmber = Color(0xFFFFB020)       // Ana vurgu
val YeterOrange = Color(0xFFFF8A1F)      // İkincil vurgu
val YeterBackground = Color(0xFF120D09)  // Zemin
val YeterSurface = Color(0xFF1B1410)     // Yüzey
val YeterCard = Color(0xFF241A12)        // Kart
val YeterText = Color(0xFFF4E9DC)        // Metin
val YeterGreen = Color(0xFF5BD6A0)       // Hedefe ulaşıldı
val YeterAlert = Color(0xFFFF5A4F)       // Dürtü uyarısı

val YeterColorScheme = darkColorScheme(
    primary = YeterAmber,
    onPrimary = Color(0xFF2B1A00),
    primaryContainer = Color(0xFF4A2F0A),
    onPrimaryContainer = Color(0xFFFFDDB0),
    secondary = YeterOrange,
    onSecondary = Color(0xFF2B1200),
    secondaryContainer = Color(0xFF3A2514),
    onSecondaryContainer = Color(0xFFFFDCC0),
    tertiary = YeterGreen,
    onTertiary = Color(0xFF00311F),
    tertiaryContainer = Color(0xFF12392A),
    onTertiaryContainer = Color(0xFFB6F2D5),
    background = YeterBackground,
    onBackground = YeterText,
    surface = YeterSurface,
    onSurface = YeterText,
    surfaceVariant = YeterCard,
    onSurfaceVariant = Color(0xFFCDBBA8),
    outline = Color(0xFF6B5846),
    error = YeterAlert,
    onError = Color(0xFF3C0805),
    errorContainer = Color(0xFF5A1713),
    onErrorContainer = Color(0xFFFFDAD6)
)

class MainActivity : ComponentActivity() {

    private val viewModel: YeterViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = YeterColorScheme) {
                val state by viewModel.uiState.collectAsState()

                YeterScreen(
                    state = state,
                    onSelectTab = { viewModel.selectTab(it) },
                    onOpenUrge = { viewModel.openUrgeDialog(true) },
                    onCloseUrge = { viewModel.openUrgeDialog(false) },
                    onStartUrge = { name, amount -> viewModel.startUrge(name, amount) },
                    onResistUrge = { viewModel.resistUrge() },
                    onSlipUrge = { viewModel.slipUrge() },
                    onClaimGoal = { id -> viewModel.claimGoal(id) },
                    onOpenEdit = { viewModel.openEditDialog(true) },
                    onCloseEdit = { viewModel.openEditDialog(false) },
                    onSaveSettings = { daily, rate, cur -> viewModel.updateSettings(daily, rate, cur) },
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
                    },
                    onOpenAddWishlist = { viewModel.openAddWishlistDialog(it) },
                    onAddWishlist = { name, price, cur, rec -> viewModel.addWishlistItem(name, price, cur, rec) },
                    onDeleteWishlist = { id -> viewModel.deleteWishlistItem(id) }
                )
            }
        }
    }
}

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

// "Gece laciverti + nane" teması: koyu lacivert zemin, nane/turkuaz vurgu, mercan sadece uyarı için
val YeterMint = Color(0xFF34D6B4)        // Ana vurgu (birikim, olumlu)
val YeterBlue = Color(0xFF6FA8FF)        // İkincil vurgu
val YeterBackground = Color(0xFF0B1220)  // Zemin
val YeterSurface = Color(0xFF111B2E)     // Yüzey
val YeterCard = Color(0xFF17233B)        // Kart
val YeterText = Color(0xFFE8EEF8)        // Metin
val YeterLime = Color(0xFFA6E36A)        // Hedefe ulaşıldı / pozitif
val YeterCoral = Color(0xFFFF6B6B)       // Dürtü uyarısı (DUR)

val YeterColorScheme = darkColorScheme(
    primary = YeterMint,
    onPrimary = Color(0xFF003528),
    primaryContainer = Color(0xFF0F4A3E),
    onPrimaryContainer = Color(0xFFB4F5E3),
    secondary = YeterBlue,
    onSecondary = Color(0xFF002A55),
    secondaryContainer = Color(0xFF1C3358),
    onSecondaryContainer = Color(0xFFD3E4FF),
    tertiary = YeterLime,
    onTertiary = Color(0xFF1C3300),
    tertiaryContainer = Color(0xFF2B4A12),
    onTertiaryContainer = Color(0xFFD2F5A8),
    background = YeterBackground,
    onBackground = YeterText,
    surface = YeterSurface,
    onSurface = YeterText,
    surfaceVariant = YeterCard,
    onSurfaceVariant = Color(0xFF9FB0C9),
    outline = Color(0xFF4A5D7A),
    outlineVariant = Color(0xFF2A3B57),
    error = YeterCoral,
    onError = Color(0xFF4A0808),
    errorContainer = Color(0xFF5A1A1A),
    onErrorContainer = Color(0xFFFFDAD6)
)

class MainActivity : ComponentActivity() {

    private val viewModel: YeterViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = YeterColorScheme) {
                val state by viewModel.uiState.collectAsState()

                if (!state.onboarded) {
                    OnboardingScreen(
                        onFinish = { home, daily, name, price, cur, rec ->
                            viewModel.completeOnboarding(home, daily, name, price, cur, rec)
                        }
                    )
                } else YeterScreen(
                    state = state,
                    onSelectTab = { viewModel.selectTab(it) },
                    onOpenUrge = { viewModel.openUrgeDialog(true) },
                    onCloseUrge = { viewModel.openUrgeDialog(false) },
                    onStartUrge = { name, amount -> viewModel.startUrge(name, amount) },
                    onResistUrge = { viewModel.resistUrge() },
                    onSlipUrge = { viewModel.slipUrge() },
                    onClaimGoal = { id -> viewModel.claimGoal(id) },
                    onPinGoal = { id -> viewModel.togglePinGoal(id) },
                    onOpenEdit = { viewModel.openEditDialog(true) },
                    onCloseEdit = { viewModel.openEditDialog(false) },
                    onSaveSettings = { daily, cur, wage, hour -> viewModel.updateSettings(daily, cur, wage, hour) },
                    onExport = {
                        val send = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "Yeter yedek")
                            putExtra(Intent.EXTRA_TEXT, viewModel.exportJson())
                        }
                        startActivity(Intent.createChooser(send, "Yedeği paylaş"))
                    },
                    onConfirmDaily = { viewModel.confirmDailyAmount(it) },
                    onRefreshRates = { viewModel.refreshRatesNow() },
                    onOpenReset = { viewModel.openResetDialog(true) },
                    onCloseReset = { viewModel.openResetDialog(false) },
                    onConfirmReset = { viewModel.resetStreak() },
                    onCloseUpdate = { viewModel.closeUpdateDialog() },
                    onDownloadUpdate = { url ->
                        viewModel.closeUpdateDialog()
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

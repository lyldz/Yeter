package com.yeter.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

class MainActivity : ComponentActivity() {

    private val viewModel: YeterViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            YeterTheme {
                val state by viewModel.uiState.collectAsState()

                if (!state.onboarded) {
                    OnboardingScreen(
                        onFinish = { home, daily, name, price, cur, rec ->
                            viewModel.completeOnboarding(home, daily, name, price, cur, rec)
                        }
                    )
                } else if (state.showUpdateDialog) {
                    UpdateScreen(
                        state = state,
                        onUpdate = { viewModel.startUpdateDownload() },
                        onLater = { viewModel.closeUpdateDialog() }
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
                    onSelectCounter = { viewModel.setCounterStyle(it) },
                    onOpenReset = { viewModel.openResetDialog(true) },
                    onCloseReset = { viewModel.openResetDialog(false) },
                    onConfirmReset = { viewModel.resetStreak() },
                    onOpenAddWishlist = { viewModel.openAddWishlistDialog(it) },
                    onAddWishlist = { name, price, cur, rec -> viewModel.addWishlistItem(name, price, cur, rec) },
                    onDeleteWishlist = { id -> viewModel.deleteWishlistItem(id) }
                )
            }
        }
    }
}

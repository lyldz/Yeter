package com.yeter.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.util.Locale

// Tam ekran güncelleme ekranı: sürüm, değişiklik listesi, uygulama içi indirme. Logo yok.
@Composable
fun UpdateScreen(state: YeterUiState, onUpdate: () -> Unit, onLater: () -> Unit) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = Spacing.screenEdge, vertical = 24.dp)
        ) {
            Text("Yeni sürüm hazır", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                text = "${state.currentAppVersion}  →  ${state.updateVersion}" +
                    if (state.updateSizeBytes > 0) " · " + String.format(Locale.getDefault(), "%.1f MB", state.updateSizeBytes / 1_048_576.0) else "",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(24.dp))
            Text("Neler değişti", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                Text(
                    text = state.updateNotes.ifBlank { "Bu sürümün değişiklik notu yok." },
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (state.updateNotes.isBlank()) muted else MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(Modifier.height(16.dp))
            if (state.updateDownloading) {
                if (state.updateProgress >= 0f) {
                    LinearProgressIndicator(progress = { state.updateProgress }, modifier = Modifier.fillMaxWidth().height(8.dp))
                } else {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(8.dp))
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = if (state.updateProgress >= 0f) "İndiriliyor... %${(state.updateProgress * 100).toInt()}" else "İndiriliyor...",
                    style = MaterialTheme.typography.bodySmall,
                    color = muted
                )
            } else {
                if (state.updateMessage.isNotBlank()) {
                    Text(
                        text = state.updateMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = muted,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }
                Button(onClick = onUpdate, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                    Text("Güncelle", style = MaterialTheme.typography.titleMedium)
                }
                TextButton(onClick = onLater, modifier = Modifier.fillMaxWidth().align(Alignment.CenterHorizontally)) {
                    Text("Sonra", color = muted)
                }
            }
        }
    }
}

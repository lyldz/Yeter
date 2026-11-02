package com.example.kumbara

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun KumbaraScreen(state: KumbaraUiState, onNoSpend: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Günlük harcama yapmadınız mı?", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(12.dp))
        Button(onClick = onNoSpend) {
            Text("Hayır, harcamadım")
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(state.message, style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Toplam biriktirdiğiniz: ${state.totalSaved} TL")
        Text("Kalan gün: ${state.daysRemaining}")
    }
}

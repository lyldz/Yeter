package com.example.kumbara

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KumbaraScreen(
    state: KumbaraUiState,
    onOpenUrge: () -> Unit,
    onCloseUrge: () -> Unit,
    onOpenEdit: () -> Unit,
    onCloseEdit: () -> Unit,
    onSaveGoal: (name: String, amount: Double, daily: Double) -> Unit,
    onOpenReset: () -> Unit,
    onCloseReset: () -> Unit,
    onConfirmReset: () -> Unit,
    onCloseUpdate: () -> Unit,
    onDownloadUpdate: (url: String) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Yeter!",
                                fontWeight = FontWeight.Black,
                                fontSize = 24.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = state.currentAppVersion,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "Harcama Kısıtlama ve Canlı Kumbara",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onOpenEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Hedefi Düzenle")
                    }
                    IconButton(onClick = onOpenReset) {
                        Icon(Icons.Default.Refresh, contentDescription = "Sıfırla")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { Spacer(modifier = Modifier.height(2.dp)) }

            // 1. Motivasyonel Üst Mesaj
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "💡",
                            fontSize = 24.sp,
                            modifier = Modifier.padding(end = 10.dp)
                        )
                        Text(
                            text = state.motivationMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // 2. Canlı Quitzilla Zaman Sayacı (Taşmayan, %100 Duyarlı Izgara)
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "HARCAMADAN GEÇEN SÜRE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // 4 Zaman Kutucuğu (weight 1f ile her ekrana %100 sığar, asla taşmaz)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TimeUnitBox(value = state.elapsedDays, unit = "GÜN", modifier = Modifier.weight(1f))
                            Text(":", fontSize = 16.sp, fontWeight = FontWeight.Black)
                            TimeUnitBox(value = state.elapsedHours, unit = "SAAT", modifier = Modifier.weight(1f))
                            Text(":", fontSize = 16.sp, fontWeight = FontWeight.Black)
                            TimeUnitBox(value = state.elapsedMinutes, unit = "DK", modifier = Modifier.weight(1f))
                            Text(":", fontSize = 16.sp, fontWeight = FontWeight.Black)
                            TimeUnitBox(value = state.elapsedSeconds, unit = "SN", modifier = Modifier.weight(1f))
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Divider(color = MaterialTheme.colorScheme.outlineVariant)
                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "KUMBARADA CANLI BİRİKEN",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = String.format(Locale.getDefault(), "₺ %.2f", state.totalSaved),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // 3. Hedef ve İlerleme Kartı
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "🎯 HEDEF",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                Text(
                                    text = state.targetName,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                            Text(
                                text = String.format(Locale.getDefault(), "₺ %.2f", state.targetAmount),
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        LinearProgressIndicator(
                            progress = { state.progressFraction },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(RoundedCornerShape(5.dp)),
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = String.format(Locale.getDefault(), "%%%d Tamamlandı", (state.progressFraction * 100).toInt()),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Günde ₺${String.format(Locale.getDefault(), "%.1f", state.dailySaving)} tasarruf",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp)
                            ) {
                                Text(
                                    text = "⏳ Kalan Süre: ${state.daysRemaining} Gün",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    text = "📅 Tahmini Ulaşma: ${state.estimatedReachDate}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f)
                                )
                            }
                        }
                    }
                }
            }

            // 4. "Ne Almak İstersin? / Dönüm Noktaları"
            item {
                Text(
                    text = "🏆 Ne Almak İstersin? (Dönüm Noktaları)",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    items(state.milestones) { milestone ->
                        val reached = state.elapsedDays >= milestone.dayThreshold
                        Card(
                            modifier = Modifier.width(170.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (reached) MaterialTheme.colorScheme.tertiaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(milestone.icon, fontSize = 22.sp)
                                    Text(
                                        text = if (reached) "KAZANDIN 🎉" else "${milestone.dayThreshold}. GÜN",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black,
                                        color = if (reached) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = milestone.title,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = milestone.suggestedReward,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 14.sp
                                )
                            }
                        }
                    }
                }
            }

            // 5. Acil Durum / "Yeter!" Panik Butonu
            item {
                Button(
                    onClick = onOpenUrge,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = "Dürtü",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "DUR! (Harcama İsteği Geldi)",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(14.dp)) }
        }
    }

    // DIALOG 1: GÜNCELLEME BİLDİRİMİ ("ARTIK YETER!")
    if (state.showUpdateDialog) {
        AlertDialog(
            onDismissRequest = onCloseUpdate,
            icon = { Icon(Icons.Default.NewReleases, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = {
                Text(
                    text = "Artık Yeter!",
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "🎉 Yeni Sürüm Bulundu!",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Yeter uygulamasının yeni sürümü (${state.updateVersion}) hazır. Daha stabil ve güncel deneyim için güncelleyebilirsiniz.",
                        fontSize = 13.sp
                    )
                }
            },
            confirmButton = {
                Button(onClick = { onDownloadUpdate(state.updateDownloadUrl) }) {
                    Text("Hemen Güncelle")
                }
            },
            dismissButton = {
                TextButton(onClick = onCloseUpdate) {
                    Text("Daha Sonra")
                }
            }
        )
    }

    // DIALOG 2: Alışveriş Dürtüsü Geldiğinde
    if (state.showUrgeDialog) {
        AlertDialog(
            onDismissRequest = onCloseUrge,
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = {
                Text(
                    text = "YETER! Bir saniye dur.",
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Şu an gereksiz bir şey satın alma dürtüsü hissediyorsun. Kendine şu 3 soruyu sor:",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Divider()
                    Text("1️⃣ 24 Saat Kuralı: Gerçekten lazımsa yarın da alabilirsin. Şimdi sepeti kapat.")
                    Text("2️⃣ İhtiyaç mı, Anlık Heves mi? 1 hafta sonra bunu hatırlayacak mısın?")
                    Text("3️⃣ Hedefin: '${state.targetName}' için ₺${String.format(Locale.getDefault(), "%.2f", state.targetAmount)} biriktiriyorsun. Kalan gün: ${state.daysRemaining}. Bu hedefe ihanet etmeye değer mi?")
                }
            },
            confirmButton = {
                Button(onClick = onCloseUrge) {
                    Text("İrademe Sahip Çıkıyorum 💪")
                }
            }
        )
    }

    // DIALOG 3: Hedef Düzenleme
    if (state.showEditDialog) {
        var tempName by remember { mutableStateOf(state.targetName) }
        var tempAmount by remember { mutableStateOf(state.targetAmount.toString()) }
        var tempDaily by remember { mutableStateOf(state.dailySaving.toString()) }

        AlertDialog(
            onDismissRequest = onCloseEdit,
            title = { Text("Hedefi Düzenle", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = tempName,
                        onValueChange = { tempName = it },
                        label = { Text("Asıl İhtiyacın / Hedefin") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = tempAmount,
                        onValueChange = { tempAmount = it },
                        label = { Text("Hedef Tutar (TL)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = tempDaily,
                        onValueChange = { tempDaily = it },
                        label = { Text("Günlük Harcamama Miktarı (TL)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = tempAmount.toDoubleOrNull() ?: state.targetAmount
                        val daily = tempDaily.toDoubleOrNull() ?: state.dailySaving
                        onSaveGoal(tempName, amount, daily)
                    }
                ) {
                    Text("Kaydet")
                }
            },
            dismissButton = {
                TextButton(onClick = onCloseEdit) {
                    Text("İptal")
                }
            }
        )
    }

    // DIALOG 4: Sıfırlama Onayı
    if (state.showResetConfirmDialog) {
        AlertDialog(
            onDismissRequest = onCloseReset,
            icon = { Icon(Icons.Default.Refresh, contentDescription = null) },
            title = { Text("Kumbarayı Sıfırla?", fontWeight = FontWeight.Bold) },
            text = {
                Text("Gereksiz bir harcama yaptıysan dürüstçe sıfırlayıp yeniden başlayabilirsin. Bu sefer daha güçlü olacaksın!")
            },
            confirmButton = {
                Button(
                    onClick = onConfirmReset,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Evet, Sıfırla")
                }
            },
            dismissButton = {
                TextButton(onClick = onCloseReset) {
                    Text("Vazgeç")
                }
            }
        )
    }
}

@Composable
fun TimeUnitBox(value: Long, unit: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 4.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = String.format(Locale.getDefault(), "%02d", value),
            fontSize = 17.sp,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = unit,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

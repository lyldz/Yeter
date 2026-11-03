package com.example.kumbara

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
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
    onSelectTab: (Int) -> Unit,
    onOpenUrge: () -> Unit,
    onCloseUrge: () -> Unit,
    onOpenEdit: () -> Unit,
    onCloseEdit: () -> Unit,
    onSaveGoal: (name: String, amount: Double, daily: Double) -> Unit,
    onOpenReset: () -> Unit,
    onCloseReset: () -> Unit,
    onConfirmReset: () -> Unit,
    onCloseUpdate: () -> Unit,
    onDownloadUpdate: (url: String) -> Unit,
    onOpenAddWishlist: (Boolean) -> Unit,
    onAddWishlist: (name: String, price: Double) -> Unit,
    onDeleteWishlist: (id: String) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = painterResource(id = R.drawable.app_logo),
                                contentDescription = "Yeter Logo",
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (state.selectedTab == 0) "Yeter!" else "Vitrin",
                                fontWeight = FontWeight.Black,
                                fontSize = 22.sp,
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
                            text = if (state.selectedTab == 0) "Harcama Kısıtlama ve Canlı Kumbara" else "Sabırsızlıkla Beklenen Hedef Ürünler",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    if (state.selectedTab == 0) {
                        IconButton(onClick = onOpenEdit) {
                            Icon(Icons.Default.Edit, contentDescription = "Hedefi Düzenle")
                        }
                        IconButton(onClick = onOpenReset) {
                            Icon(Icons.Default.Refresh, contentDescription = "Sıfırla")
                        }
                    } else {
                        IconButton(onClick = { onOpenAddWishlist(true) }) {
                            Icon(Icons.Default.Add, contentDescription = "Ürün Ekle")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ) {
                NavigationBarItem(
                    selected = state.selectedTab == 0,
                    onClick = { onSelectTab(0) },
                    icon = { Icon(Icons.Default.Savings, contentDescription = "Kumbara") },
                    label = { Text("Kumbara", fontWeight = FontWeight.Bold) }
                )
                NavigationBarItem(
                    selected = state.selectedTab == 1,
                    onClick = { onSelectTab(1) },
                    icon = { Icon(Icons.Default.Storefront, contentDescription = "Vitrin") },
                    label = { Text("Vitrin", fontWeight = FontWeight.Bold) }
                )
            }
        },
        floatingActionButton = {
            if (state.selectedTab == 1) {
                FloatingActionButton(
                    onClick = { onOpenAddWishlist(true) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Ürün Ekle")
                }
            }
        }
    ) { paddingValues ->
        if (state.selectedTab == 0) {
            KumbaraTabContent(
                state = state,
                paddingValues = paddingValues,
                onOpenUrge = onOpenUrge
            )
        } else {
            VitrinTabContent(
                state = state,
                paddingValues = paddingValues,
                onDelete = onDeleteWishlist,
                onOpenAdd = { onOpenAddWishlist(true) }
            )
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

    // DIALOG 5: Vitrine Yeni Ürün Ekleme
    if (state.showAddWishlistDialog) {
        var prodName by remember { mutableStateOf("") }
        var prodPrice by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { onOpenAddWishlist(false) },
            icon = { Icon(Icons.Default.Storefront, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Vitrine Ürün Ekle", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Almak istediğin ürünü ekle; günlük birikimin ile kaç gün, saat ve saniye kaldığını canlı geri sayım ile takip et!",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = prodName,
                        onValueChange = { prodName = it },
                        label = { Text("Ürün Adı (örn: Altın Bileklik)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = prodPrice,
                        onValueChange = { prodPrice = it },
                        label = { Text("Güncel Fiyatı (TL)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val p = prodPrice.toDoubleOrNull() ?: 0.0
                        if (prodName.isNotBlank() && p > 0) {
                            onAddWishlist(prodName, p)
                        }
                    }
                ) {
                    Text("Vitrine Ekle")
                }
            },
            dismissButton = {
                TextButton(onClick = { onOpenAddWishlist(false) }) {
                    Text("İptal")
                }
            }
        )
    }
}

// ----------------- SEKME 1: KUMBARA İÇERİĞİ -----------------
@Composable
fun KumbaraTabContent(
    state: KumbaraUiState,
    paddingValues: PaddingValues,
    onOpenUrge: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { Spacer(modifier = Modifier.height(2.dp)) }

        // Motivasyonel Üst Mesaj
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

        // Canlı Zaman Sayacı
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

        // Hedef ve İlerleme Kartı
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

        // Dönüm Noktaları
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

        // Dur Butonu
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

// ----------------- SEKME 2: VİTRİN (WISHLIST & GERİ SAYIM) -----------------
@Composable
fun VitrinTabContent(
    state: KumbaraUiState,
    paddingValues: PaddingValues,
    onDelete: (String) -> Unit,
    onOpenAdd: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { Spacer(modifier = Modifier.height(2.dp)) }

        // Vitrin Bilgi Bannerı
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
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Storefront,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(36.dp)
                            .padding(end = 10.dp)
                    )
                    Column {
                        Text(
                            text = "🛍️ Almak İçin Beklenenler",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Günlük ₺${String.format(Locale.getDefault(), "%.0f", state.dailySaving)} birikimin ile her ürün için geri sayım canlı çalışıyor!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                        )
                    }
                }
            }
        }

        if (state.wishlist.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 30.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("🛍️", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Vitrininde Henüz Ürün Yok",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Almak istediğin hayalindeki ürünleri ekle, kaç gün saat kaldığını canlı geri sayım ile takip et!",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onOpenAdd) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("İlk Ürünü Ekle")
                        }
                    }
                }
            }
        } else {
            items(state.wishlist, key = { it.id }) { item ->
                WishlistItemCard(item = item, onDelete = { onDelete(item.id) })
            }
        }

        item { Spacer(modifier = Modifier.height(70.dp)) } // FAB boşluğu
    }
}

@Composable
fun WishlistItemCard(
    item: WishlistItemUi,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (item.isCompleted) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Başlık ve Fiyat Satırı
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Güncel Fiyat: ₺${String.format(Locale.getDefault(), "%,.2f", item.price)}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Sil",
                        tint = MaterialTheme.colorScheme.outline
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // CANLI GERİ SAYIM SAYAÇ KUTULARI VEYA HEDEF ULAŞILDI ROZETİ
            if (item.isCompleted) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF2E7D32),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "🎉 HEDEFE ULAŞILDI! Gönül rahatlığıyla alabilirsin!",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                Text(
                    text = "⏳ HEDEFE GERİ SAYIM",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TimeUnitBox(value = item.daysRemaining, unit = "GÜN", modifier = Modifier.weight(1f))
                    Text(":", fontSize = 16.sp, fontWeight = FontWeight.Black)
                    TimeUnitBox(value = item.hoursRemaining, unit = "SAAT", modifier = Modifier.weight(1f))
                    Text(":", fontSize = 16.sp, fontWeight = FontWeight.Black)
                    TimeUnitBox(value = item.minutesRemaining, unit = "DK", modifier = Modifier.weight(1f))
                    Text(":", fontSize = 16.sp, fontWeight = FontWeight.Black)
                    TimeUnitBox(value = item.secondsRemaining, unit = "SN", modifier = Modifier.weight(1f))
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // İlerleme Çubuğu
            LinearProgressIndicator(
                progress = { item.progressFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Alt Bilgiler
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (item.isCompleted) "Tamamı Birikti (%100)"
                    else "Kalan: ₺${String.format(Locale.getDefault(), "%,.2f", item.remainingMoney)}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = if (item.isCompleted) "Hazır!" else "Hedef: ${item.estimatedReachDate}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
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

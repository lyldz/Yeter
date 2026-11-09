package com.yeter.app

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import kotlinx.coroutines.delay
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material.icons.rounded.NewReleases
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.Warning
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
fun YeterScreen(
    state: YeterUiState,
    onSelectTab: (Int) -> Unit,
    onOpenUrge: () -> Unit,
    onCloseUrge: () -> Unit,
    onStartUrge: (name: String, amount: Double) -> Unit,
    onResistUrge: () -> Unit,
    onSlipUrge: () -> Unit,
    onClaimGoal: (id: String) -> Unit,
    onPinGoal: (id: String) -> Unit,
    onOpenEdit: () -> Unit,
    onCloseEdit: () -> Unit,
    onSaveSettings: (daily: Double, homeCurrency: String, hourlyWage: Double, notifyHour: Int) -> Unit,
    onExport: () -> Unit,
    onConfirmDaily: (amount: Double) -> Unit,
    onRefreshRates: () -> Unit,
    onOpenReset: () -> Unit,
    onCloseReset: () -> Unit,
    onConfirmReset: () -> Unit,
    onCloseUpdate: () -> Unit,
    onDownloadUpdate: (url: String) -> Unit,
    onOpenAddWishlist: (Boolean) -> Unit,
    onAddWishlist: (name: String, price: Double, currency: String, recurring: Boolean) -> Unit,
    onDeleteWishlist: (id: String) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_y_mark),
                                contentDescription = "Yeter Logo",
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(10.dp))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = when (state.selectedTab) { 0 -> "Yeter!"; 1 -> "Vitrin"; else -> "Geçmiş" },
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
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = when (state.selectedTab) { 0 -> "Dürtüne karşı dur, hedefine yürü"; 1 -> "Sabırsızlıkla beklenen hedefler"; else -> "İlerlemen, rozetlerin ve kayıtların" },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    if (state.selectedTab == 0) {
                        IconButton(onClick = onOpenEdit) {
                            Icon(Icons.Rounded.Edit, contentDescription = "Hedefi Düzenle")
                        }
                        IconButton(onClick = onOpenReset) {
                            Icon(Icons.Rounded.Refresh, contentDescription = "Sıfırla")
                        }
                    } else if (state.selectedTab == 1) {
                        IconButton(onClick = { onOpenAddWishlist(true) }) {
                            Icon(Icons.Rounded.Add, contentDescription = "Ürün Ekle")
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
                    icon = { Icon(if (state.selectedTab == 0) Icons.Rounded.Savings else Icons.Outlined.Savings, contentDescription = null) },
                    label = { Text("Sayaç", fontWeight = FontWeight.Bold) }
                )
                NavigationBarItem(
                    selected = state.selectedTab == 1,
                    onClick = { onSelectTab(1) },
                    icon = { Icon(if (state.selectedTab == 1) Icons.Rounded.Storefront else Icons.Outlined.Storefront, contentDescription = null) },
                    label = { Text("Vitrin", fontWeight = FontWeight.Bold) }
                )
                NavigationBarItem(
                    selected = state.selectedTab == 2,
                    onClick = { onSelectTab(2) },
                    icon = { Icon(if (state.selectedTab == 2) Icons.Rounded.History else Icons.Outlined.History, contentDescription = null) },
                    label = { Text("Geçmiş", fontWeight = FontWeight.Bold) }
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
                    Icon(Icons.Rounded.Add, contentDescription = "Ürün Ekle")
                }
            }
        }
    ) { paddingValues ->
        if (state.selectedTab == 0) {
            SayacTabContent(
                state = state,
                paddingValues = paddingValues,
                onOpenUrge = onOpenUrge
            )
        } else if (state.selectedTab == 2) {
            HistoryTabContent(state = state, paddingValues = paddingValues)
        } else {
            VitrinTabContent(
                state = state,
                paddingValues = paddingValues,
                onDelete = onDeleteWishlist,
                onClaim = onClaimGoal,
                onPin = onPinGoal,
                onRefreshRates = onRefreshRates,
                onOpenAdd = { onOpenAddWishlist(true) }
            )
        }
    }

    // DIALOG 1: GÜNCELLEME BİLDİRİMİ ("ARTIK YETER!")
    if (state.showUpdateDialog) {
        AlertDialog(
            onDismissRequest = onCloseUpdate,
            icon = { Icon(Icons.Rounded.NewReleases, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
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
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Yeter uygulamasının yeni sürümü (${state.updateVersion}) hazır. Daha stabil ve güncel deneyim için güncelleyebilirsiniz.",
                        fontSize = 14.sp
                    )
                }
            },
            confirmButton = {
                Button(onClick = { onDownloadUpdate(state.updateDownloadUrl) }) {
                    Text("Güncelle")
                }
            },
            dismissButton = {
                TextButton(onClick = onCloseUpdate) {
                    Text("Sonra (24 saat)")
                }
            }
        )
    }

    // DIALOG 2: Alışveriş Dürtüsü (form -> bekleme -> karar)
    if (state.showUrgeDialog || state.urgeActive) {
        if (!state.urgeActive) {
            var urgeName by remember { mutableStateOf("") }
            var urgeAmount by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = onCloseUrge,
                icon = { Icon(Icons.Rounded.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                title = { Text("YETER! Bir saniye dur.", fontWeight = FontWeight.Black, textAlign = TextAlign.Center) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Ne almak istiyorsun ve ne kadar? Yazınca 10 dakikalık bekleme başlar.", fontSize = 14.sp)
                        OutlinedTextField(
                            value = urgeName, onValueChange = { urgeName = it },
                            label = { Text("Ne almak istiyorsun?") },
                            singleLine = true, modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = urgeAmount, onValueChange = { urgeAmount = it },
                            label = { Text("Tutar (${state.symbol.trim()})") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true, modifier = Modifier.fillMaxWidth()
                        )
                        val urgeVal = urgeAmount.replace(',', '.').toDoubleOrNull()
                        if (state.hourlyWage > 0 && urgeVal != null && urgeVal > 0) {
                            Text(
                                "⏱️ Bu, yaklaşık ${String.format(Locale.getDefault(), "%.1f", urgeVal / state.hourlyWage)} saatlik çalışmana denk.",
                                fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error
                            )
                        }
                        Text(
                            "Hedefin: '${state.targetName}'. Bu hedefe ihanet etmeye değer mi?",
                            fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        val a = urgeAmount.replace(',', '.').toDoubleOrNull() ?: 0.0
                        if (urgeName.isNotBlank() && a > 0) onStartUrge(urgeName, a)
                    }) { Text("Beklemeyi Başlat") }
                },
                dismissButton = { TextButton(onClick = onCloseUrge) { Text("Kapat") } }
            )
        } else {
            val waiting = state.urgeRemainingSec > 0
            AlertDialog(
                onDismissRequest = { },
                icon = { Icon(Icons.Rounded.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                title = {
                    Text(
                        if (waiting) "Bekle... %02d:%02d".format(state.urgeRemainingSec / 60, state.urgeRemainingSec % 60)
                        else "Süre doldu. Karar zamanı",
                        fontWeight = FontWeight.Black, textAlign = TextAlign.Center
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            "${state.urgeName} • ${state.symbol}${String.format(Locale.getDefault(), "%,.2f", state.urgeAmount)}",
                            fontWeight = FontWeight.Bold, fontSize = 16.sp
                        )
                        if (state.hourlyWage > 0) {
                            Text(
                                "⏱️ ≈ ${String.format(Locale.getDefault(), "%.1f", state.urgeAmount / state.hourlyWage)} saatlik çalışma",
                                fontSize = 14.sp, color = MaterialTheme.colorScheme.error
                            )
                        }
                        if (waiting) {
                            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { BreathingCircle() }
                            val tips = listOf(
                                "Gerçekten ihtiyacım var mı, yoksa anlık heves mi?",
                                "1 hafta sonra bunu hatırlayacak mıyım?",
                                "Yarın da istersem, o zaman düşünürüm.",
                                "Bu para, hedefine bir adım daha demek."
                            )
                            Text(
                                tips[((600 - state.urgeRemainingSec) / 12).toInt().coerceAtLeast(0) % tips.size],
                                fontSize = 14.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            Text("Hâlâ istiyor musun? Dürüstçe karar ver. Yenilirsen de sorun değil, kaydedip devam edersin.")
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = onResistUrge, enabled = !waiting) {
                        Text("Vazgeçtim, birikime ekle 💪")
                    }
                },
                dismissButton = {
                    TextButton(onClick = onSlipUrge) { Text("Aldım / Yenildim") }
                }
            )
        }
    }

    // DIALOG 3: Ayarlar
    if (state.showEditDialog) {
        var tempDaily by remember { mutableStateOf(state.dailySaving.toString()) }
        var tempCur by remember { mutableStateOf(state.homeCurrency) }
        var tempWage by remember { mutableStateOf(if (state.hourlyWage > 0) state.hourlyWage.toString() else "") }
        var tempHour by remember { mutableStateOf(state.notifyHour) }

        AlertDialog(
            onDismissRequest = onCloseEdit,
            title = { Text("Ayarlar", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    OutlinedTextField(
                        value = tempDaily, onValueChange = { tempDaily = it },
                        label = { Text("Günlük tasarruf (${currencySymbol(tempCur).trim()})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true, modifier = Modifier.fillMaxWidth()
                    )
                    Text("Ana para birimi", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    CurrencyPicker(selected = tempCur, onSelect = { tempCur = it })
                    RateStatusRow(state = state, onRefresh = onRefreshRates)
                    Text(
                        "Para birimini değiştirirsen birikimin güncel kurla çevrilir.",
                        fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = tempWage, onValueChange = { tempWage = it },
                        label = { Text("Saatlik kazancın (${currencySymbol(tempCur).trim()}) – isteğe bağlı") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true, modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        "Girersen dürtü anında 'bu kaç saatlik çalışmana denk' gösterilir.",
                        fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text("Sabah hatırlatması", fontSize = 14.sp, modifier = Modifier.weight(1f))
                        TextButton(onClick = { tempHour = (tempHour + 23) % 24 }) { Text("−") }
                        Text("%02d:00".format(tempHour), fontWeight = FontWeight.Bold)
                        TextButton(onClick = { tempHour = (tempHour + 1) % 24 }) { Text("+") }
                    }
                    OutlinedButton(onClick = onExport, modifier = Modifier.fillMaxWidth()) {
                        Text("Verileri dışa aktar (yedek)")
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val daily = tempDaily.replace(',', '.').toDoubleOrNull() ?: state.dailySaving
                    onSaveSettings(daily, tempCur, tempWage.replace(',', '.').toDoubleOrNull() ?: 0.0, tempHour)
                }) { Text("Kaydet") }
            },
            dismissButton = { TextButton(onClick = onCloseEdit) { Text("İptal") } }
        )
    }

    // DIALOG: Günaydın, bugünkü tasarruf tutarı
    if (state.onboarded && state.needsDailyPrompt && !state.showUpdateDialog) {
        var amountText by remember { mutableStateOf(state.dailySaving.toString()) }
        AlertDialog(
            onDismissRequest = { onConfirmDaily(state.dailySaving) },
            icon = { Icon(Icons.Rounded.Savings, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Günaydın! Bugün ne kadar tasarruf edeceksin?", fontWeight = FontWeight.Black, textAlign = TextAlign.Center) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Seçtiğin tutar bugün saniye saniye birikime eklenir. Dünkü tutar: ${state.symbol}${String.format(Locale.getDefault(), "%,.2f", state.dailySaving)}",
                        fontSize = 14.sp
                    )
                    OutlinedTextField(
                        value = amountText, onValueChange = { amountText = it },
                        label = { Text("Bugünkü tutar (${state.symbol.trim()})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true, modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    onConfirmDaily(amountText.replace(',', '.').toDoubleOrNull() ?: state.dailySaving)
                }) { Text("Onayla") }
            },
            dismissButton = {
                TextButton(onClick = { onConfirmDaily(state.dailySaving) }) { Text("Dünkü gibi") }
            }
        )
    }

    // DIALOG 4: Sıfırlama Onayı
    if (state.showResetConfirmDialog) {
        AlertDialog(
            onDismissRequest = onCloseReset,
            icon = { Icon(Icons.Rounded.Refresh, contentDescription = null) },
            title = { Text("Sayacı Sıfırla?", fontWeight = FontWeight.Bold) },
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
        var prodCur by remember { mutableStateOf(state.homeCurrency) }
        var prodRecurring by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { onOpenAddWishlist(false) },
            icon = { Icon(Icons.Rounded.Storefront, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Hedef Ekle", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Almak istediğin ürünü ekle; günlük birikimin ile kaç gün, saat ve saniye kaldığını canlı geri sayım ile takip et!",
                        fontSize = 14.sp,
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
                        label = { Text("Fiyat (${prodCur})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    CurrencyPicker(selected = prodCur, onSelect = { prodCur = it })
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(checked = prodRecurring, onCheckedChange = { prodRecurring = it })
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Her ay tekrar eder", fontSize = 14.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val p = prodPrice.replace(',', '.').toDoubleOrNull() ?: 0.0
                        if (prodName.isNotBlank() && p > 0) {
                            onAddWishlist(prodName, p, prodCur, prodRecurring)
                        }
                    }
                ) {
                    Text("Ekle")
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

// ----------------- SEKME 1: SAYAÇ İÇERİĞİ -----------------
@Composable
fun SayacTabContent(
    state: YeterUiState,
    paddingValues: PaddingValues,
    onOpenUrge: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(horizontal = Spacing.screenEdge),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg)
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

        // Canlı Birikim Sayacı (dev, halkalı)
        item { HeroCounter(state = state) }

        // Bugün ve İstatistikler
        item {
            YeterCard {
                    SectionLabel("BUGÜN")
                    Text(
                        text = "+${state.symbol}" + String.format(Locale.getDefault(), "%,.2f", state.todayEarned),
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                    Text(
                        text = "Günlük hedef: ${state.symbol}" + String.format(Locale.getDefault(), "%,.2f", state.dailySaving),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Divider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${state.resistedCount}", fontSize = 20.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.tertiary)
                            Text("Direnilen dürtü", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "${state.symbol}" + String.format(Locale.getDefault(), "%,.0f", state.resistedTotal),
                                fontSize = 20.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary
                            )
                            Text("Direnerek kurtarılan", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${state.slipCount}", fontSize = 20.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.error)
                            Text("Yenilgi", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
            }
        }

        // Hedef ve İlerleme Kartı
        item {
            YeterCard(containerColor = MaterialTheme.colorScheme.surface) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            SectionLabel("🎯 HEDEF", color = MaterialTheme.colorScheme.secondary)
                            Text(
                                text = state.targetName,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                        Text(
                            text = "${state.symbol} " + String.format(Locale.getDefault(), "%,.2f", state.targetAmount),
                            fontSize = 18.sp,
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
                            .clip(RoundedCornerShape(6.dp)),
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
                            text = "Günde ${state.symbol}${String.format(Locale.getDefault(), "%.1f", state.dailySaving)} tasarruf",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Surface(
                        shape = RoundedCornerShape(16.dp),
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
                                fontSize = 14.sp,
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

        // Dönüm Noktaları
        item {
            Text(
                text = "🏆 Ne Almak İstersin? (Dönüm Noktaları)",
                fontSize = 16.sp,
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
                        shape = RoundedCornerShape(16.dp),
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
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (reached) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = milestone.title,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = milestone.suggestedReward,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 16.sp
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
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    Icons.Rounded.Warning,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "DUR! (Harcama İsteği Geldi)",
                    fontSize = 16.sp,
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
    state: YeterUiState,
    paddingValues: PaddingValues,
    onDelete: (String) -> Unit,
    onClaim: (String) -> Unit,
    onPin: (String) -> Unit,
    onRefreshRates: () -> Unit,
    onOpenAdd: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(horizontal = Spacing.screenEdge),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg)
    ) {
        item { Spacer(modifier = Modifier.height(2.dp)) }

        // Kur durumu
        item { RateStatusRow(state = state, onRefresh = onRefreshRates) }

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
                        Icons.Rounded.Storefront,
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
                            text = "Günlük ${state.symbol}${String.format(Locale.getDefault(), "%.0f", state.dailySaving)} birikimin ile her hedef için geri sayım canlı çalışıyor!",
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
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onOpenAdd) {
                            Icon(Icons.Rounded.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("İlk Ürünü Ekle")
                        }
                    }
                }
            }
        } else {
            items(state.wishlist, key = { it.id }) { item ->
                WishlistItemCard(item = item, onDelete = { onDelete(item.id) }, onClaim = { onClaim(item.id) }, onPin = { onPin(item.id) })
            }
        }

        item { Spacer(modifier = Modifier.height(70.dp)) } // FAB boşluğu
    }
}

@Composable
fun WishlistItemCard(
    item: WishlistItemUi,
    onDelete: () -> Unit,
    onClaim: () -> Unit,
    onPin: () -> Unit
) {
    val sym = item.symbol
    YeterCard(
        containerColor = if (item.isCompleted) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceVariant
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
                    if (item.isPinned) {
                        Text(
                            text = "📌 Ana ekranda gösteriliyor",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = (if (item.ownSymbol != item.symbol)
                            "${item.ownSymbol}${String.format(Locale.getDefault(), "%,.2f", item.ownPrice)} ≈ "
                        else "") +
                            "$sym${String.format(Locale.getDefault(), "%,.2f", item.price)}" +
                            (if (item.recurring) " • her ay" else ""),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(onClick = onPin) {
                    Icon(
                        if (item.isPinned) Icons.Rounded.PushPin else Icons.Outlined.PushPin,
                        contentDescription = if (item.isPinned) "Ana ekrandan kaldır" else "Ana ekrana sabitle",
                        tint = if (item.isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                    )
                }

                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Rounded.Delete,
                        contentDescription = "Sil",
                        tint = MaterialTheme.colorScheme.outline
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // CANLI GERİ SAYIM SAYAÇ KUTULARI VEYA HEDEF ULAŞILDI ROZETİ
            if (item.isCompleted) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "🎉 HEDEFE ULAŞILDI! Gönül rahatlığıyla alabilirsin!",
                            color = MaterialTheme.colorScheme.onTertiary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = onClaim, modifier = Modifier.fillMaxWidth()) {
                    Text(if (item.recurring) "Aldım (bu ayki tutar havuzdan düşer)" else "Aldım (tutar havuzdan düşer)")
                }
            } else {
                SectionLabel("⏳ HEDEFE GERİ SAYIM", modifier = Modifier.align(Alignment.CenterHorizontally))
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
                    .clip(RoundedCornerShape(6.dp)),
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Alt Bilgiler
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (item.isCompleted) "Tamamı Birikti (%100)"
                    else "Kalan: $sym${String.format(Locale.getDefault(), "%,.2f", item.remainingMoney)}",
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

@Composable
fun TimeUnitBox(value: Long, unit: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 4.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = String.format(Locale.getDefault(), "%02d", value),
            fontSize = 18.sp,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = unit,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}


// Kur durumu: son güncelleme zamanı, kaynak ve elle yenileme
@Composable
fun RateStatusRow(state: YeterUiState, onRefresh: () -> Unit) {
    val usdTry = state.rates["TRY"]
    val ageMin = if (state.rateUpdatedAt > 0) (System.currentTimeMillis() - state.rateUpdatedAt) / 60_000L else -1L
    val status = when {
        state.ratesRefreshing -> "Kurlar güncelleniyor..."
        state.rateUpdatedAt <= 0L -> "Kur henüz çekilemedi, yaklaşık değerler kullanılıyor"
        state.rateFailed -> "Son deneme başarısız, ${ageMinText(ageMin)} önceki kur kullanılıyor"
        else -> "Güncel (${ageMinText(ageMin)} önce)"
    }
    val ok = !state.rateFailed && state.rateUpdatedAt > 0L
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Kur" + (if (usdTry != null) ": 1 USD = ${String.format(Locale.getDefault(), "%.2f", usdTry)} TRY" else ""),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = status,
                    fontSize = 12.sp,
                    color = if (ok) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
                )
            }
            TextButton(onClick = onRefresh, enabled = !state.ratesRefreshing) {
                Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Yenile", fontSize = 12.sp)
            }
        }
    }
}

private fun ageMinText(min: Long): String = when {
    min < 1 -> "az"
    min < 60 -> "$min dk"
    min < 1440 -> "${min / 60} sa"
    else -> "${min / 1440} gün"
}


// Nefes egzersizi: büyüyen (nefes al) ve küçülen (ver) daire
@Composable
fun BreathingCircle() {
    val t = rememberInfiniteTransition(label = "breath")
    val scale by t.animateFloat(
        initialValue = 0.5f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(4000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "breathScale"
    )
    var inhale by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(4000L)
            inhale = !inhale
        }
    }
    Box(modifier = Modifier.size(110.dp), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size((110f * scale).dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
        )
        Text(
            if (inhale) "Nefes al" else "Nefes ver",
            fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary
        )
    }
}

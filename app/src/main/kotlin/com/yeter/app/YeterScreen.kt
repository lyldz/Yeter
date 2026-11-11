package com.yeter.app

import androidx.compose.animation.core.*
import androidx.compose.animation.Crossfade
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import kotlinx.coroutines.delay
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.NewReleases
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Today
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
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
    onSelectCounter: (Int) -> Unit,
    onCheckUpdate: () -> Unit,
    onShowNotes: () -> Unit,
    onShowGuide: () -> Unit,
    onCloseNotes: () -> Unit,
    onOpenReset: () -> Unit,
    onCloseReset: () -> Unit,
    onConfirmReset: () -> Unit,
    onOpenAddWishlist: (Boolean) -> Unit,
    onAddWishlist: (name: String, price: Double, currency: String, recurring: Boolean) -> Unit,
    onDeleteWishlist: (id: String) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_y_mark),
                            contentDescription = "Yeter Logo",
                            modifier = Modifier
                                .size(30.dp)
                                .clip(RoundedCornerShape(9.dp))
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = when (state.selectedTab) { 0 -> "Yeter!"; 1 -> "Vitrin"; else -> "Geçmiş" },
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    if (state.selectedTab == 1) {
                        IconButton(onClick = { onOpenAddWishlist(true) }) {
                            Icon(Icons.Rounded.Add, contentDescription = "Ürün Ekle")
                        }
                    }
                    // (i): sürüm, güncelleme, sürüm notları, sıfırla
                    var infoOpen by remember { mutableStateOf(false) }
                    IconButton(onClick = { infoOpen = true }) {
                        Icon(Icons.Rounded.Info, contentDescription = "Bilgi")
                    }
                    DropdownMenu(expanded = infoOpen, onDismissRequest = { infoOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("Sürüm ${state.currentAppVersion} · ${BuildConfig.SERIES_NAME}", color = MaterialTheme.colorScheme.outline) },
                            enabled = false,
                            onClick = {}
                        )
                        DropdownMenuItem(
                            text = { Text("Güncellemeyi denetle") },
                            leadingIcon = { Icon(Icons.Rounded.Refresh, contentDescription = null) },
                            onClick = { infoOpen = false; onCheckUpdate() }
                        )
                        DropdownMenuItem(
                            text = { Text("Sürüm notları") },
                            leadingIcon = { Icon(Icons.Rounded.NewReleases, contentDescription = null) },
                            onClick = { infoOpen = false; onShowNotes() }
                        )
                        DropdownMenuItem(
                            text = { Text("Sıfırla") },
                            leadingIcon = { Icon(Icons.Rounded.Delete, contentDescription = null) },
                            onClick = { infoOpen = false; onOpenReset() }
                        )
                    }
                    // Dişli: ayarlar, sayaç görünümü, rehber
                    var menuOpen by remember { mutableStateOf(false) }
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Rounded.Settings, contentDescription = "Ayarlar")
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("Ayarlar") },
                            leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null) },
                            onClick = { menuOpen = false; onOpenEdit() }
                        )
                        listOf("Klasik", "Kumbara", "Para yığını", "Kart", "Kasa").forEachIndexed { i, name ->
                            DropdownMenuItem(
                                text = { Text("Sayaç: $name" + if (state.counterStyle == i) "  ✓" else "") },
                                onClick = { menuOpen = false; onSelectCounter(i) }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Rehberi göster") },
                            leadingIcon = { Icon(Icons.Rounded.Lightbulb, contentDescription = null) },
                            onClick = { menuOpen = false; onShowGuide() }
                        )
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
        Crossfade(targetState = state.selectedTab, animationSpec = tween(220), label = "tab") { tab ->
            if (tab == 0) {
                SayacTabContent(
                    state = state,
                    paddingValues = paddingValues,
                    onOpenUrge = onOpenUrge
                )
            } else if (tab == 2) {
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
            val urgeCtx = LocalContext.current
            val urgePrefs = remember { urgeCtx.getSharedPreferences("yeter_app_prefs", android.content.Context.MODE_PRIVATE) }
            var minimalMode by remember { mutableStateOf(urgePrefs.getBoolean("urge_minimal", false)) }
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
                                fontSize = 14.sp,
                                color = if (minimalMode) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error
                            )
                        }
                        if (waiting) {
                            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { BreathingCircle(minimal = minimalMode) }
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
                            TextButton(
                                onClick = {
                                    minimalMode = !minimalMode
                                    urgePrefs.edit().putBoolean("urge_minimal", minimalMode).apply()
                                },
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            ) { Text(if (minimalMode) "Sade mod: açık" else "Sade mod: kapalı") }
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
                    OutlinedButton(onClick = onSlipUrge) { Text("Aldım / Yenildim") }
                }
            )
        }
    }

    if (state.releaseNotes != null) {
        AlertDialog(
            onDismissRequest = onCloseNotes,
            title = { Text(state.releaseNotesTitle, fontWeight = FontWeight.Bold) },
            text = { Text(state.releaseNotes.ifBlank { "Bu sürüm için not bulunamadı." }, modifier = Modifier.verticalScroll(rememberScrollState())) },
            confirmButton = { TextButton(onClick = onCloseNotes) { Text("Kapat") } }
        )
    }

    // DIALOG 3: Ayarlar
    if (state.showEditDialog) {
        var tempDaily by remember { mutableStateOf(state.dailySaving.toString()) }
        var tempCur by remember { mutableStateOf(state.homeCurrency) }
        var tempWage by remember { mutableStateOf(if (state.hourlyWage > 0) state.hourlyWage.toString() else "") }
        var tempHour by remember { mutableStateOf(state.notifyHour) }
        val ctx = LocalContext.current

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
                    if (tempCur != state.homeCurrency && state.rateUpdatedAt <= 0L) {
                        android.widget.Toast.makeText(ctx, "Kur alınamadı, para birimi şimdilik değiştirilemez", android.widget.Toast.LENGTH_LONG).show()
                        return@Button
                    }
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
                        onValueChange = { v -> prodPrice = if ((v.replace(',', '.').toDoubleOrNull() ?: 0.0) > Savings.MAX_GOAL) Savings.MAX_GOAL.toInt().toString() else v },
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

// ----------------- SEKME 1: SAYAÇ İÇERİĞİ (tek ekran, kaydırmasız) -----------------
@Composable
fun SayacTabContent(
    state: YeterUiState,
    paddingValues: PaddingValues,
    onOpenUrge: () -> Unit
) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(horizontal = Spacing.screenEdge),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        // Canlı birikim: kalan yüksekliği alır
        if (state.counterStyle == 4) {
            CounterD(state = state, modifier = Modifier.weight(1f))
        } else {
            // Eski sabit yükseklikli stiller: kırpılmasın diye kaydırılabilir kutu
            Box(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())) {
                when (state.counterStyle) {
                    1 -> CounterA(state = state)
                    2 -> CounterB(state = state)
                    3 -> CounterC(state = state)
                    else -> HeroCounter(state = state)
                }
            }
        }

        // Hedef: kompakt kart (ad + tutar, ince çubuk, tek küçük satır)
        YeterCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = state.targetName,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(Spacing.sm))
                Text(
                    text = "${state.symbol}" + String.format(Locale.getDefault(), "%,.0f", state.targetAmount),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.height(Spacing.sm))
            val goalProgress by animateFloatAsState(
                targetValue = state.progressFraction,
                animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy),
                label = "goalProgress"
            )
            LinearProgressIndicator(
                progress = { goalProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
            )
            Spacer(Modifier.height(Spacing.xs))
            Text(
                text = "${state.daysRemaining} gün kaldı · ${state.estimatedReachDate}",
                style = MaterialTheme.typography.bodySmall,
                color = muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Motivasyon: sessiz, kısa
        Text(
            text = state.motivationMessage,
            style = MaterialTheme.typography.bodySmall,
            color = muted,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.md)
        )

        // Üç sayı tek kompakt satırda: bugün / direnilen / kurtarılan
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = Spacing.sm, horizontal = Spacing.xs),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatBlock(
                    icon = Icons.Rounded.Today,
                    value = "+${state.symbol}" + String.format(Locale.getDefault(), "%,.2f", state.todayEarned),
                    label = "Bugün",
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.weight(1f)
                )
                VerticalDivider(Modifier.height(28.dp))
                StatBlock(
                    icon = Icons.Rounded.Shield,
                    value = "${state.resistedCount}",
                    label = "Direnilen",
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                VerticalDivider(Modifier.height(28.dp))
                StatBlock(
                    icon = Icons.Rounded.Savings,
                    value = "${state.symbol}" + String.format(Locale.getDefault(), "%,.0f", state.resistedTotal),
                    label = "Kurtarılan",
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // DUR butonu: altta sabit
        val urgeSource = remember { MutableInteractionSource() }
        val urgePressed by urgeSource.collectIsPressedAsState()
        val urgeScale by animateFloatAsState(
            targetValue = if (urgePressed) 0.96f else 1f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
            label = "urgeScale"
        )
        Button(
            onClick = onOpenUrge,
            interactionSource = urgeSource,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .graphicsLayer {
                    scaleX = urgeScale
                    scaleY = urgeScale
                },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(text = "DUR", fontSize = 18.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
        }
        Spacer(Modifier.height(Spacing.sm))
    }
}

@Composable
private fun StatBlock(icon: ImageVector, value: String, label: String, color: Color, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(Spacing.xs))
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ----------------- SEKME 2: VİTRİN (WISHLIST & GERİ SAYIM) -----------------
@OptIn(ExperimentalFoundationApi::class)
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

        if (state.wishlist.isEmpty()) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp, horizontal = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Rounded.Storefront, contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Vitrin boş", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Almak istediğin bir şey ekle, ne zaman alabileceğini gör.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onOpenAdd) { Text("Ürün ekle") }
                }
            }
        } else {
            items(state.wishlist, key = { it.id }) { item ->
                WishlistItemCard(modifier = Modifier.animateItemPlacement(), item = item, onDelete = { onDelete(item.id) }, onClaim = { onClaim(item.id) }, onPin = { onPin(item.id) })
            }
        }

        item { Spacer(modifier = Modifier.height(70.dp)) } // FAB boşluğu
    }
}

@Composable
fun WishlistItemCard(
    modifier: Modifier = Modifier,
    item: WishlistItemUi,
    onDelete: () -> Unit,
    onClaim: () -> Unit,
    onPin: () -> Unit
) {
    val sym = item.symbol
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    YeterCard(
        modifier = modifier,
        containerColor = if (item.isCompleted) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(item.name, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                Text(
                    text = (if (item.ownSymbol != item.symbol)
                        "${item.ownSymbol}${String.format(Locale.getDefault(), "%,.0f", item.ownPrice)} ≈ "
                    else "") +
                        "$sym${String.format(Locale.getDefault(), "%,.0f", item.price)}" +
                        (if (item.recurring) " · her ay" else ""),
                    style = MaterialTheme.typography.bodySmall,
                    color = muted
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
                Icon(Icons.Rounded.Delete, contentDescription = "Sil", tint = MaterialTheme.colorScheme.outline)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        val itemProgress by animateFloatAsState(
            targetValue = item.progressFraction,
            animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy),
            label = "itemProgress"
        )
        LinearProgressIndicator(
            progress = { itemProgress },
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
        )
        Spacer(modifier = Modifier.height(8.dp))

        if (item.isCompleted) {
            Text(
                text = "Hedefe ulaştın, alabilirsin",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(onClick = onClaim, modifier = Modifier.weight(1f)) { Text("Satın alındı") }
                OutlinedButton(onClick = onDelete) { Text("İptal") }
            }
        } else {
            var askBuy by remember { mutableStateOf(false) }
            if (askBuy) {
                AlertDialog(
                    onDismissRequest = { askBuy = false },
                    title = { Text("Satın alındı mı?", fontWeight = FontWeight.Bold) },
                    text = { Text("Hedef tamamlanır ve birikimden ürün fiyatı kadar (en fazla mevcut birikimin) düşülür.") },
                    confirmButton = { Button(onClick = { askBuy = false; onClaim() }) { Text("Evet, aldım") } },
                    dismissButton = { TextButton(onClick = { askBuy = false }) { Text("Vazgeç") } }
                )
            }
            val left = buildString {
                if (item.daysRemaining > 0) append("${item.daysRemaining} gün ")
                append("${item.hoursRemaining} sa ${item.minutesRemaining} dk")
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(left, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                Text(
                    "Kalan $sym${String.format(Locale.getDefault(), "%,.0f", item.remainingMoney)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = muted
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                OutlinedButton(onClick = { askBuy = true }, modifier = Modifier.weight(1f)) { Text("Satın alındı") }
                TextButton(onClick = onDelete) { Text("İptal") }
            }
        }
    }
}

// Kur durumu: tek sessiz satır (sorun varsa renkli)
@Composable
fun RateStatusRow(state: YeterUiState, onRefresh: () -> Unit) {
    val usdTry = state.rates["TRY"]
    val ageMin = if (state.rateUpdatedAt > 0) (System.currentTimeMillis() - state.rateUpdatedAt) / 60_000L else -1L
    val ok = !state.rateFailed && state.rateUpdatedAt > 0L
    val text = when {
        state.ratesRefreshing -> "Kurlar güncelleniyor..."
        state.rateUpdatedAt <= 0L -> "Kur çekilemedi, yaklaşık değerler"
        state.rateFailed -> "Kur güncellenemedi (${ageMinText(ageMin)} önceki)"
        usdTry != null -> "1 USD = ${String.format(Locale.getDefault(), "%.2f", usdTry)} TRY · ${ageMinText(ageMin)} önce"
        else -> "Kur güncel"
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = if (ok || state.ratesRefreshing) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onRefresh, enabled = !state.ratesRefreshing) {
            Icon(Icons.Rounded.Refresh, contentDescription = "Kurları yenile", tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(20.dp))
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
fun BreathingCircle(minimal: Boolean = false) {
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
                .background(
                    if (minimal) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.22f)
                    else MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                )
        )
        Text(
            if (inhale) "Nefes al" else "Nefes ver",
            fontSize = 14.sp, fontWeight = FontWeight.Bold,
            color = if (minimal) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primary
        )
    }
}

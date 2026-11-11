package com.yeter.app

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Canvas
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.launch

private const val PAGE_COUNT = 6

private class GuidePage(val title: String, val bullets: List<String>)

private val pages = listOf(
    GuidePage(
        "Yeter'e hoş geldin",
        listOf(
            "Boşa harcama dürtüsüne \"Yeter!\" de.",
            "Harcamadığın parayı hedefine biriktir.",
            "Paran saniye saniye birikir; her an büyüdüğünü görürsün."
        )
    ),
    GuidePage(
        "Sayaç sekmesi",
        listOf(
            "Kasa: altın paralar düşer, birikim yükselir; %50, %75 ve %100'de parlar.",
            "DUR düğmesi: dürtü gelince 10 dakika bekle. Vazgeçersen tutar birikimine eklenir.",
            "Ana hedef kartı ilerlemeni gösterir.",
            "8 ana ekran widget'ı var: ana ekrana uzun basıp ekleyebilirsin."
        )
    ),
    GuidePage(
        "Vitrin sekmesi",
        listOf(
            "Hedeflerini ve ürünlerini listele; + ile yenisini ekle.",
            "Raptiye ile bir hedefi ana ekrana sabitle.",
            "Hedefe ulaşınca ya da elle \"Satın alındı\" veya \"İptal\" seç.",
            "Bir hedef en fazla 60.000 olabilir."
        )
    ),
    GuidePage(
        "Geçmiş sekmesi",
        listOf(
            "Son 35 günün takvimi: her gün direndiğin ve yenildiğin dürtüler.",
            "Rozetler başarılarını ödüllendirir.",
            "Olay listesinde tüm kayıtlarını görürsün."
        )
    ),
    GuidePage(
        "Ayarlar",
        listOf(
            "Sağ üstteki dişli çark menüsünü aç.",
            "Günlük tutarı ve para birimini değiştir.",
            "Sayaç görünümünü seç, güncellemeyi denetle.",
            "Sıfırlayabilir veya bu rehberi tekrar gösterebilirsin."
        )
    ),
    GuidePage(
        "İzinler",
        listOf("İkisi de isteğe bağlı; vermesen de uygulama çalışır.")
    )
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GuideScreen(onDone: () -> Unit) {
    val pager = rememberPagerState { PAGE_COUNT }
    val scope = rememberCoroutineScope()
    val last = pager.currentPage == PAGE_COUNT - 1

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.statusBarsPadding().navigationBarsPadding()) {
            Row(
                modifier = Modifier.fillMaxWidth().height(48.dp).padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!last) TextButton(onClick = onDone) { Text("Atla") }
            }
            HorizontalPager(state = pager, modifier = Modifier.weight(1f)) { i ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) { Mockup(i) }
                    Spacer(Modifier.height(16.dp))
                    Text(
                        pages[i].title,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(12.dp))
                    Column(
                        modifier = Modifier.widthIn(max = 420.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        pages[i].bullets.forEach { b ->
                            Row {
                                Text("•  ", color = YeterMint, fontWeight = FontWeight.Black)
                                Text(b, color = YeterTextSecondary, fontSize = 15.sp, lineHeight = 21.sp)
                            }
                        }
                    }
                    if (i == PAGE_COUNT - 1) {
                        Spacer(Modifier.height(16.dp))
                        PermissionRows()
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    repeat(PAGE_COUNT) { d ->
                        Box(
                            Modifier
                                .size(if (d == pager.currentPage) 10.dp else 8.dp)
                                .clip(CircleShape)
                                .background(if (d == pager.currentPage) YeterMint else YeterOutlineSoft)
                        )
                    }
                }
                Button(onClick = {
                    if (last) onDone() else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) }
                }) { Text(if (last) "Başla" else "İleri", fontWeight = FontWeight.Bold) }
            }
        }
    }
}

// ---------- Maketler (dekoratif) ----------

@Composable
private fun Phone(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .widthIn(max = 260.dp)
            .fillMaxWidth()
            .height(170.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(YeterSurface)
            .border(2.dp, YeterOutlineSoft, RoundedCornerShape(20.dp))
    ) { content() }
}

@Composable
private fun Mockup(page: Int) {
    when (page) {
        0 -> Phone {
            Column(
                Modifier.fillMaxSize().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Image(
                    painterResource(R.drawable.ic_y_mark), null,
                    Modifier.size(52.dp).clip(RoundedCornerShape(14.dp))
                )
                Spacer(Modifier.height(10.dp))
                Text("1.284,07", color = YeterMint, fontSize = 26.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { 0.62f },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                    color = YeterMint, trackColor = YeterOutlineSoft
                )
            }
        }
        1 -> Phone {
            Column(Modifier.fillMaxSize()) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Canvas(Modifier.size(width = 80.dp, height = 90.dp)) {
                        val r = androidx.compose.ui.geometry.CornerRadius(14.dp.toPx())
                        drawRoundRect(YeterCardHigh, cornerRadius = r)
                        drawRoundRect(
                            YeterAmber.copy(alpha = 0.35f), topLeft = Offset(0f, size.height * 0.45f),
                            size = androidx.compose.ui.geometry.Size(size.width, size.height * 0.55f), cornerRadius = r
                        )
                        listOf(0.3f to 0.55f, 0.55f to 0.7f, 0.7f to 0.45f, 0.45f to 0.85f).forEach { (x, y) ->
                            drawCircle(YeterAmber, 7.dp.toPx(), Offset(size.width * x, size.height * y))
                        }
                    }
                    Box(
                        Modifier.align(Alignment.CenterEnd).padding(end = 14.dp).size(34.dp)
                            .clip(CircleShape).background(YeterCoral),
                        contentAlignment = Alignment.Center
                    ) { Text("DUR", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Black) }
                }
                MiniBar(selected = 0)
            }
        }
        2 -> Phone {
            Column(
                Modifier.fillMaxSize().padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MiniProduct("Kulaklık", 0.7f, pinned = true)
                MiniProduct("Bisiklet", 0.25f, pinned = false)
            }
        }
        3 -> Phone {
            Column(
                Modifier.fillMaxSize().padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                repeat(5) { r ->
                    Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        repeat(7) { c ->
                            val n = r * 7 + c
                            val color = when {
                                n % 7 == 3 -> YeterCoral
                                n % 5 == 0 -> YeterOutlineSoft
                                else -> YeterMint
                            }
                            Box(Modifier.weight(1f).fillMaxSize().clip(RoundedCornerShape(4.dp)).background(color))
                        }
                    }
                }
            }
        }
        4 -> Phone {
            Column(Modifier.fillMaxSize()) {
                Row(
                    Modifier.fillMaxWidth().padding(start = 14.dp, end = 10.dp, top = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(painterResource(R.drawable.ic_y_mark), null, Modifier.size(24.dp).clip(RoundedCornerShape(7.dp)))
                    Spacer(Modifier.size(8.dp))
                    Text("Yeter!", color = YeterText, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
                    Box(
                        Modifier.size(38.dp).clip(CircleShape).background(YeterMint.copy(alpha = 0.2f))
                            .border(2.dp, YeterMint, CircleShape),
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Rounded.Settings, null, tint = YeterMint) }
                }
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    repeat(3) {
                        Box(Modifier.fillMaxWidth().height(14.dp).clip(RoundedCornerShape(6.dp)).background(YeterCard))
                    }
                }
            }
        }
        else -> Phone {
            Row(
                Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BigIcon(Icons.Rounded.Notifications, YeterAmber)
                BigIcon(Icons.Rounded.SystemUpdate, YeterBlue)
            }
        }
    }
}

@Composable
private fun BigIcon(icon: ImageVector, tint: Color) {
    Box(
        Modifier.size(72.dp).clip(CircleShape).background(tint.copy(alpha = 0.18f)),
        contentAlignment = Alignment.Center
    ) { Icon(icon, null, tint = tint, modifier = Modifier.size(36.dp)) }
}

@Composable
private fun MiniBar(selected: Int) {
    Row(
        Modifier.fillMaxWidth().height(40.dp).background(YeterCard),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        listOf(Icons.Rounded.Savings, Icons.Rounded.Storefront, Icons.Rounded.History).forEachIndexed { i, ic ->
            Box(
                Modifier.clip(CircleShape).background(if (i == selected) YeterMint.copy(alpha = 0.25f) else Color.Transparent)
                    .padding(horizontal = 14.dp, vertical = 3.dp)
            ) { Icon(ic, null, tint = if (i == selected) YeterMint else YeterOutline, modifier = Modifier.size(20.dp)) }
        }
    }
}

@Composable
private fun MiniProduct(name: String, progress: Float, pinned: Boolean) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(YeterCard).padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(name, color = YeterText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                color = YeterLime, trackColor = YeterOutlineSoft
            )
        }
        Icon(
            Icons.Rounded.PushPin, null,
            tint = if (pinned) YeterAmber else YeterOutline,
            modifier = Modifier.padding(start = 10.dp).size(20.dp)
        )
    }
}

// ---------- İzinler ----------

private fun canInstall(context: Context): Boolean =
    Build.VERSION.SDK_INT < 26 || context.packageManager.canRequestPackageInstalls()

private fun notifGranted(context: Context): Boolean =
    Build.VERSION.SDK_INT < 33 ||
        context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

@Composable
private fun PermissionRows() {
    val context = LocalContext.current
    var notif by remember { mutableStateOf(notifGranted(context)) }
    var install by remember { mutableStateOf(canInstall(context)) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { notif = it }

    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val obs = LifecycleEventObserver { _, e ->
            if (e == Lifecycle.Event.ON_RESUME) {
                install = canInstall(context)
                notif = notifGranted(context)
            }
        }
        owner.lifecycle.addObserver(obs)
        onDispose { owner.lifecycle.removeObserver(obs) }
    }

    Column(
        modifier = Modifier.widthIn(max = 420.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        PermissionCard(
            title = "Bildirimler",
            why = "Günlük hatırlatma ve hedef %50, %75, %90, %100 bildirimleri için.",
            done = notif,
            doneText = if (Build.VERSION.SDK_INT < 33) "✓ Gerekmiyor" else "✓ Verildi",
            action = "İzin ver",
            onClick = { launcher.launch(Manifest.permission.POST_NOTIFICATIONS) }
        )
        PermissionCard(
            title = "Güncellemeleri kurma",
            why = "Uygulama içinden güncelleme indirip kurmak için.",
            done = install,
            doneText = if (Build.VERSION.SDK_INT < 26) "✓ Gerekmiyor" else "✓ Verildi",
            action = "İzin ver",
            onClick = {
                context.startActivity(
                    Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + context.packageName))
                )
            }
        )
    }
}

@Composable
private fun PermissionCard(
    title: String, why: String, done: Boolean, doneText: String, action: String, onClick: () -> Unit
) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(YeterCard).padding(14.dp)
    ) {
        Text(title, color = YeterText, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(Modifier.height(4.dp))
        Text(why, color = YeterTextSecondary, fontSize = 14.sp, lineHeight = 20.sp)
        Spacer(Modifier.height(10.dp))
        if (done) Text(doneText, color = YeterMint, fontWeight = FontWeight.Bold)
        else OutlinedButton(onClick = onClick) { Text(action) }
    }
}

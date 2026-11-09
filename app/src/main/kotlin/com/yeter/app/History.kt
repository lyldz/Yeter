package com.yeter.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ----------------- SEKME 3: GEÇMİŞ (takvim, rozetler, dürtü kayıtları) -----------------
@Composable
fun HistoryTabContent(state: YeterUiState, paddingValues: PaddingValues) {
    val dayFmt = SimpleDateFormat("d MMM, HH:mm", Locale("tr", "TR"))
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(horizontal = Spacing.screenEdge),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg)
    ) {
        item { Spacer(modifier = Modifier.height(2.dp)) }

        // Takvim: son 35 gün
        item {
            YeterCard {
                run {
                    SectionLabel("SON 35 GÜN")
                    Spacer(modifier = Modifier.height(10.dp))
                    state.calendar.chunked(7).forEach { week ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            week.forEach { cell ->
                                val color = when {
                                    cell.slipped > 0 -> MaterialTheme.colorScheme.error
                                    cell.resisted > 0 -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.outlineVariant
                                }
                                val dayNum = java.util.Calendar.getInstance().apply { timeInMillis = cell.dayStart }
                                    .get(java.util.Calendar.DAY_OF_MONTH)
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .padding(vertical = 3.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(color.copy(alpha = if (cell.resisted + cell.slipped > 0) 1f else 0.45f))
                                        .then(
                                            if (cell.isToday) Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, RoundedCornerShape(10.dp))
                                            else Modifier
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "$dayNum",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (cell.resisted + cell.slipped > 0) MaterialTheme.colorScheme.background
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Yeşil: dürtüye direndin • Kırmızı: yenildin • Soluk: kayıt yok",
                        fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Rozetler
        item {
            Text("🏅 Başarılar (${state.badges.count { it.unlocked }}/${state.badges.size})", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        items(state.badges.chunked(2)) { pair ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                pair.forEach { b ->
                    Card(
                        modifier = Modifier.weight(1f).alpha(if (b.unlocked) 1f else 0.4f),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (b.unlocked) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(b.emoji, fontSize = 26.sp, modifier = Modifier.padding(end = 10.dp))
                            Column {
                                Text(b.title, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text(b.desc, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 16.sp)
                            }
                        }
                    }
                }
                if (pair.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
        }

        // Dürtü kayıtları
        item { Text("📝 Dürtü Kayıtları", fontSize = 16.sp, fontWeight = FontWeight.Bold) }
        if (state.events.isEmpty()) {
            item {
                Text(
                    "Henüz kayıt yok. Ana sayfadaki DUR butonuyla ilk dürtüne direndiğinde burada görünür.",
                    fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(state.events.takeLast(40).reversed()) { e ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(if (e.resisted) "✅" else "❌", fontSize = 20.sp, modifier = Modifier.padding(end = 10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(e.name.ifBlank { "(adsız)" }, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1)
                            Text(dayFmt.format(Date(e.ts)), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(
                            (if (e.resisted) "+" else "") + state.symbol + String.format(Locale.getDefault(), "%,.2f", e.amount),
                            fontWeight = FontWeight.Black,
                            color = if (e.resisted) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(14.dp)) }
    }
}

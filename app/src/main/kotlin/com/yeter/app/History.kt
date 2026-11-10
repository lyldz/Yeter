package com.yeter.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.History
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
                                        .background(color.copy(alpha = if (cell.resisted + cell.slipped > 0) 1f else 0.6f))
                                        .semantics {
                                            contentDescription = "$dayNum. gün: " + when {
                                                cell.resisted + cell.slipped == 0 -> "kayıt yok"
                                                else -> "${cell.resisted} direnç, ${cell.slipped} yenilgi"
                                            }
                                        }
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
                        "Yeşil: direndin · Kırmızı: yenildin",
                        fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Rozetler
        item {
            Text("Başarılar · ${state.badges.count { it.unlocked }}/${state.badges.size}", style = MaterialTheme.typography.titleMedium)
        }
        items(state.badges) { b ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(if (b.unlocked) 1f else 0.4f)
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(b.emoji, fontSize = 22.sp, modifier = Modifier.padding(end = 12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(b.title, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                    Text(b.desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (b.unlocked) {
                    Icon(Icons.Rounded.CheckCircle, contentDescription = "Kazanıldı", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                }
            }
        }

        // Dürtü kayıtları
        item { Text("Kayıtlar", style = MaterialTheme.typography.titleMedium) }
        if (state.events.isEmpty()) {
            item {
                YeterCard {
                    Icon(
                        Icons.Rounded.History, contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(40.dp).align(Alignment.CenterHorizontally)
                    )
                    Spacer(modifier = Modifier.height(Spacing.sm))
                    Text(
                        "Henüz kayıt yok",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                    Spacer(modifier = Modifier.height(Spacing.xs))
                    Text(
                        "Ana sayfadaki DUR butonuyla ilk dürtüne direndiğinde burada görünür.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        } else {
            items(state.events.takeLast(40).reversed()) { e ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (e.resisted) Icons.Rounded.CheckCircle else Icons.Rounded.Cancel,
                        contentDescription = if (e.resisted) "Direnildi" else "Yenildi",
                        tint = if (e.resisted) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(end = 12.dp).size(20.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(e.name.ifBlank { "(adsız)" }, style = MaterialTheme.typography.bodyLarge, maxLines = 1)
                        Text(dayFmt.format(Date(e.ts)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(
                        (if (e.resisted) "+" else "") + state.symbol + String.format(Locale.getDefault(), "%,.2f", e.amount),
                        style = MaterialTheme.typography.titleMedium,
                        color = if (e.resisted) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
                    )
                }
            }
        }
        item { Spacer(modifier = Modifier.height(14.dp)) }
    }
}

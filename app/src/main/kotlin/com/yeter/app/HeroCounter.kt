package com.yeter.app

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.floor

// Canlı birikim sayacı: halka ilerleme göstergesi + akıcı artan dev sayı + nabız gibi atan parıltı
@Composable
fun HeroCounter(state: YeterUiState) {
    val primary = MaterialTheme.colorScheme.primary
    val tertiary = MaterialTheme.colorScheme.tertiary
    val track = MaterialTheme.colorScheme.outlineVariant
    val done = state.progressFraction >= 1f
    val ringColor = if (done) tertiary else primary

    // Akıcı sayaç: durum her saniye güncellenir, aradaki sürede değeri ekranda 60 ms'de bir ilerlet
    val baseValue = state.totalSaved
    val baseTime = remember(state.totalSaved) { System.currentTimeMillis() }
    var nowMs by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            nowMs = System.currentTimeMillis()
            delay(60L)
        }
    }
    val live = baseValue + state.dailySaving * (nowMs - baseTime).coerceIn(0L, 1500L) / 86_400_000.0

    // Nabız
    val pulse = rememberInfiniteTransition(label = "pulse")
    val glow by pulse.animateFloat(
        initialValue = 0.25f, targetValue = 0.6f,
        animationSpec = infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "glow"
    )
    val animProgress by animateFloatAsState(
        targetValue = state.progressFraction,
        animationSpec = tween(900, easing = FastOutSlowInEasing),
        label = "progress"
    )

    val sep = DecimalFormatSymbols.getInstance(Locale.getDefault()).decimalSeparator
    val intPart = floor(live).toLong()
    val frac = ((live - floor(live)) * 100_000).toLong().coerceIn(0L, 99_999L)
    val intText = String.format(Locale.getDefault(), "%,d", intPart)
    val fracText = String.format(Locale.getDefault(), "%05d", frac)
    val intSize = when {
        intText.length <= 5 -> 46.sp
        intText.length <= 7 -> 38.sp
        else -> 30.sp
    }

    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                            MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp, horizontal = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "CANLI BİRİKİM",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 3.sp,
                    color = ringColor
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Halka + sayı
                Box(modifier = Modifier.size(290.dp), contentAlignment = Alignment.Center) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val stroke = 16.dp.toPx()
                        val inset = stroke / 2 + 6.dp.toPx()
                        val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
                        // Parıltı
                        drawCircle(
                            brush = Brush.radialGradient(
                                listOf(ringColor.copy(alpha = glow * 0.55f), Color.Transparent),
                                center = center, radius = size.minDimension / 2
                            ),
                            radius = size.minDimension / 2
                        )
                        // İz
                        drawArc(
                            color = track, startAngle = -90f, sweepAngle = 360f, useCenter = false,
                            topLeft = Offset(inset, inset), size = arcSize,
                            style = Stroke(width = stroke, cap = StrokeCap.Round)
                        )
                        // İlerleme
                        if (animProgress > 0f) {
                            drawArc(
                                brush = Brush.sweepGradient(
                                    listOf(ringColor.copy(alpha = 0.55f), ringColor, ringColor),
                                    center = center
                                ),
                                startAngle = -90f, sweepAngle = 360f * animProgress, useCenter = false,
                                topLeft = Offset(inset, inset), size = arcSize,
                                style = Stroke(width = stroke, cap = StrokeCap.Round)
                            )
                        }
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = state.symbol.trim(),
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 8.dp, end = 4.dp)
                            )
                            Text(
                                text = intText,
                                fontSize = intSize,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1, softWrap = false
                            )
                            Text(
                                text = "$sep$fracText",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = ringColor,
                                maxLines = 1, softWrap = false,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "%${(state.progressFraction * 100).toInt()} • ${state.targetName}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ringColor,
                            maxLines = 1,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 36.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "+${state.symbol}" + String.format(Locale.getDefault(), "%.5f", state.dailySaving / 86_400.0) + " / sn",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "HARCAMADAN GEÇEN SÜRE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
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
            }
        }
    }
}

package com.yeter.app

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.roundToLong

// Canlı birikim sayacı: parlak gradyanlı dev tutar, yumuşak parıltı, ince ilerleme çubuğu, sönük süre satırı.
// Kuruş değişince tutar kısa bir "yuvarlanma" animasyonuyla yeni değere akar.
@Composable
fun HeroCounter(state: YeterUiState) {
    val done = state.progressFraction >= 1f
    val locale = Locale.getDefault()

    // Renk paleti: normalde nane, hedef tamamlanınca altın/lime
    val topColor = if (done) Color(0xFFFFF0B8) else Color(0xFFE6FFF7)
    val midColor = if (done) YeterAmber else YeterMint
    val bottomColor = if (done) YeterLime else Color(0xFF7FE9CF)
    val accent = if (done) YeterAmber else YeterMint

    // Canlı değer: durum her saniye güncellenir; ara süreyi yalnızca 250 ms'de bir kuruşa çevirerek ilerlet.
    // derivedStateOf sayesinde kuruş değişmedikçe gövde yeniden çizilmez.
    val baseCents = state.totalSaved * 100.0
    val baseTime = remember(state.totalSaved) { System.currentTimeMillis() }
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            nowMs = System.currentTimeMillis()
            delay(250L)
        }
    }
    val dailyCents = state.dailySaving * 100.0
    val targetCents by remember(baseCents, baseTime, dailyCents) {
        derivedStateOf {
            val elapsed = (nowMs - baseTime).coerceIn(0L, 1500L)
            (baseCents + dailyCents * elapsed / 86_400_000.0).roundToLong().coerceAtLeast(0L)
        }
    }

    // "Yuvarlanma": önceki gösterilen değerden yeni değere 0..1 ilerleme
    val roll = remember { Animatable(1f) }
    var from by remember { mutableLongStateOf(0L) }
    var to by remember { mutableLongStateOf(0L) }
    LaunchedEffect(targetCents) {
        from = (from + ((to - from) * roll.value).roundToLong())
        to = targetCents
        roll.snapTo(0f)
        roll.animateTo(1f, tween(if (kotlin.math.abs(to - from) > 5L) 900 else 350, easing = FastOutSlowInEasing))
    }
    val shown = (from + ((to - from) * roll.value.toDouble()).roundToLong()).coerceAtLeast(0L)
    val intPart = shown / 100
    val cents = (shown % 100).toInt()

    val sep = DecimalFormatSymbols.getInstance(locale).decimalSeparator
    val intText = String.format(locale, "%,d", intPart)
    val centText = String.format(locale, "%02d", cents)
    val intSize = when {
        intText.length <= 5 -> 60.sp
        intText.length <= 7 -> 50.sp
        else -> 40.sp
    }

    // Parıltı nabzı: yalnızca çizim aşamasında okunur, yeniden bileşim tetiklemez
    val pulse = rememberInfiniteTransition(label = "pulse")
    val glow = pulse.animateFloat(
        initialValue = 0.18f, targetValue = if (done) 0.5f else 0.34f,
        animationSpec = infiniteRepeatable(tween(2400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "glow"
    )
    val animProgress by animateFloatAsState(
        targetValue = state.progressFraction.coerceIn(0f, 1f),
        animationSpec = tween(900, easing = FastOutSlowInEasing),
        label = "progress"
    )

    val amountBrush = Brush.verticalGradient(listOf(topColor, midColor, bottomColor))
    val percent = (state.progressFraction * 100).toInt()
    val a11y = "Birikim: $intText ${state.symbol.trim()}, hedefin yüzde $percent kadarı"

    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Tutar bloğu: ekran okuyucuya sabit, tamsayı tabanlı tek açıklama
        Column(
            modifier = Modifier
                .clearAndSetSemantics { contentDescription = a11y }
                .drawBehind {
                    val r = size.width.coerceAtLeast(size.height) * 0.62f
                    drawCircle(
                        brush = Brush.radialGradient(
                            listOf(accent.copy(alpha = glow.value * 0.45f), Color.Transparent),
                            center = Offset(size.width / 2, size.height / 2), radius = r
                        ),
                        radius = r,
                        center = Offset(size.width / 2, size.height / 2)
                    )
                }
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Text(
                    text = state.symbol.trim(),
                    fontSize = (intSize.value * 0.45f).sp,
                    fontWeight = FontWeight.Bold,
                    color = accent.copy(alpha = 0.85f),
                    modifier = Modifier.padding(top = 6.dp, end = 4.dp)
                )
                Text(
                    text = intText,
                    style = TabularNums.copy(
                        brush = amountBrush,
                        shadow = Shadow(accent.copy(alpha = 0.45f), Offset.Zero, 28f)
                    ),
                    fontSize = intSize,
                    fontWeight = FontWeight.Black,
                    maxLines = 1, softWrap = false
                )
                Text(
                    text = "$sep$centText",
                    style = TabularNums.copy(brush = amountBrush),
                    fontSize = (intSize.value * 0.45f).sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1, softWrap = false,
                    modifier = Modifier.padding(top = 6.dp, start = 2.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // İnce ilerleme çubuğu + yüzde
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(180.dp).height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(MaterialTheme.colorScheme.outlineVariant)
            ) {
                if (animProgress > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(animProgress)
                            .clip(RoundedCornerShape(3.dp))
                            .background(
                                Brush.horizontalGradient(
                                    if (done) listOf(YeterAmber, YeterLime)
                                    else listOf(YeterMint.copy(alpha = 0.7f), YeterMint, Color(0xFFB8FFEC))
                                )
                            )
                    )
                }
            }
            Text(
                text = "%$percent",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = accent
            )
        }

        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = elapsedText(state),
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
    }
}

// Sönük tek satır: "12 gün 4 sa harcamadan" (saniye yok)
private fun elapsedText(s: YeterUiState): String = when {
    s.elapsedDays > 0 -> "${s.elapsedDays} gün ${s.elapsedHours} sa harcamadan"
    s.elapsedHours > 0 -> "${s.elapsedHours} sa ${s.elapsedMinutes} dk harcamadan"
    else -> "${s.elapsedMinutes} dk harcamadan"
}

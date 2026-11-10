package com.yeter.app

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.roundToLong
import kotlin.math.sin

// Konsept A: Kumbara / dolan sıvı.
// Cam kap hedefe ilerlemeyle dolar; yüzey hafifçe dalgalanır. Tutar kabın altında, yalnızca
// tam kuruş değişince (yumuşak çapraz geçişle) güncellenir. Birkaç saniyede bir bir madeni para
// düşer ve "+0,05" gibi küçük bir birikim etiketi yükselip kaybolur.

private val JarWidthA = 150.dp
private val JarHeightA = 168.dp

@Composable
fun CounterA(state: YeterUiState) {
    val done = state.progressFraction >= 1f
    val locale = Locale.getDefault()
    val accent = if (done) YeterLime else YeterMint
    val sep = DecimalFormatSymbols.getInstance(locale).decimalSeparator

    // --- Canlı değer (en fazla 250 ms'de bir), kuruş değişmedikçe yeniden bileşim yok ---
    val baseCents = state.totalSaved * 100.0
    val dailyCents = state.dailySaving * 100.0
    val baseTime = remember(state.totalSaved) { System.currentTimeMillis() }
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            nowMs = System.currentTimeMillis()
            delay(250L)
        }
    }
    val liveCents by remember(baseCents, baseTime, dailyCents) {
        derivedStateOf {
            val elapsed = (nowMs - baseTime).coerceIn(0L, 2000L)
            (baseCents + dailyCents * elapsed / 86_400_000.0).roundToLong().coerceAtLeast(0L)
        }
    }
    val intText = String.format(locale, "%,d", liveCents / 100)
    val centText = String.format(locale, "%02d", (liveCents % 100).toInt())

    // --- Madeni para damlası ---
    val drop = remember { Animatable(1f) }
    var dropKey by remember { mutableIntStateOf(0) }
    var dropDelta by remember { mutableLongStateOf(0L) }
    var lastDropCents by remember { mutableLongStateOf(liveCents) }
    var lastDropTime by remember { mutableLongStateOf(0L) }
    LaunchedEffect(liveCents) {
        val delta = liveCents - lastDropCents
        val now = System.currentTimeMillis()
        when {
            delta <= 0L -> lastDropCents = liveCents.coerceAtMost(lastDropCents)
            delta > 2000L -> lastDropCents = liveCents // büyük sıçrama (ör. ödül): damlatma
            now - lastDropTime >= 4000L && state.progressFraction > 0f -> {
                dropDelta = delta
                lastDropCents = liveCents
                lastDropTime = now
                dropKey += 1
            }
        }
    }
    LaunchedEffect(dropKey) {
        if (dropKey > 0) {
            drop.snapTo(0f)
            drop.animateTo(1f, tween(1500, easing = LinearEasing))
        }
    }
    val dropText = String.format(locale, "+%d%s%02d", dropDelta / 100, sep, (dropDelta % 100).toInt())

    // --- Doluluk ve dalga ---
    val fill by animateFloatAsState(
        targetValue = state.progressFraction.coerceIn(0f, 1f),
        animationSpec = tween(1200, easing = FastOutSlowInEasing),
        label = "fillA"
    )
    val wave = rememberInfiniteTransition(label = "waveA")
    val phase = wave.animateFloat(
        initialValue = 0f, targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(3600, easing = LinearEasing), RepeatMode.Restart),
        label = "phaseA"
    )

    val percent = (state.progressFraction * 100).toInt()
    val a11y = if (done) {
        "Birikim: ${String.format(locale, "%,d", state.totalSaved.toLong())} ${state.symbol.trim()}, hedef tamamlandı"
    } else {
        "Birikim: ${String.format(locale, "%,d", state.totalSaved.toLong())} ${state.symbol.trim()}, hedefin yüzde $percent" + "'si"
    }
    val intSize = when {
        intText.length <= 5 -> 48.sp
        intText.length <= 7 -> 40.sp
        else -> 32.sp
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 4.dp)
            .clearAndSetSemantics { contentDescription = a11y },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Cam kap + para damlası + etiket
        Box(modifier = Modifier.fillMaxWidth().height(JarHeightA), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(JarWidthA, JarHeightA)) {
                drawJarA(
                    fill = fill,
                    phase = phase.value,
                    done = done,
                    dropT = drop.value,
                    dropKey = dropKey
                )
            }
            Text(
                text = dropText,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = accent,
                modifier = Modifier
                    .offset(x = JarWidthA / 2 + 30.dp, y = 10.dp)
                    .graphicsLayer {
                        val t = drop.value
                        val k = ((t - 0.45f) / 0.55f).coerceIn(0f, 1f)
                        alpha = if (t < 0.45f || dropKey == 0) 0f else 1f - k
                        translationY = -k * 36.dp.toPx()
                    }
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Tutar: tamsayı sabit, kuruş çapraz geçişle değişir
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.Center) {
            Text(
                text = state.symbol.trim(),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = accent.copy(alpha = 0.85f),
                modifier = Modifier.padding(end = 4.dp, bottom = 6.dp)
            )
            Text(
                text = intText,
                style = TabularNums,
                fontSize = intSize,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1, softWrap = false
            )
            Crossfade(
                targetState = centText,
                animationSpec = tween(260),
                label = "centsA"
            ) { c ->
                Text(
                    text = "$sep$c",
                    style = TabularNums,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = accent,
                    maxLines = 1, softWrap = false,
                    modifier = Modifier.padding(start = 2.dp, bottom = 5.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        val goal = if (state.targetName.isNotBlank()) " · ${state.targetName}" else ""
        Text(
            text = "%$percent$goal",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = accent,
            maxLines = 1
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = elapsedTextA(state),
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
        )
    }
}

private fun DrawScope.drawJarA(fill: Float, phase: Float, done: Boolean, dropT: Float, dropKey: Int) {
    val w = size.width
    val h = size.height
    val lipH = 12.dp.toPx()
    val lipInset = w * 0.16f
    val stroke = 2.5.dp.toPx()
    val bodyTop = lipH
    val bodyRadius = 30.dp.toPx()

    val bodyPath = Path().apply {
        addRoundRect(
            RoundRect(
                left = stroke, top = bodyTop, right = w - stroke, bottom = h - stroke,
                cornerRadius = CornerRadius(bodyRadius, bodyRadius)
            )
        )
    }
    val glass = Color.White

    // Cam arka planı
    drawPath(bodyPath, glass.copy(alpha = 0.05f))

    // Sıvı
    val innerTop = bodyTop
    val innerH = h - stroke - innerTop
    val level = innerTop + innerH * (1f - fill)
    val liquidTop = if (done) YeterLime else YeterMint
    val liquidBottom = if (done) YeterAmber else Color(0xFF1E9C86).let {
        // altın-nane: alta doğru hafif altın tonu
        Color(
            red = it.red * 0.7f + YeterAmber.red * 0.3f,
            green = it.green * 0.7f + YeterAmber.green * 0.3f,
            blue = it.blue * 0.7f + YeterAmber.blue * 0.3f
        )
    }
    if (fill > 0.001f) {
        // Uçlarda dalga sönsün (boş ve tam doluyken düz)
        val edge = min(fill, 1f - fill)
        val amp = if (done || fill >= 0.995f) 0f else 4.dp.toPx() * (edge * 12f).coerceIn(0.25f, 1f)
        clipPath(bodyPath) {
            // Arka dalga (sönük)
            drawPath(
                wavePathA(stroke, w - stroke, level, h, amp * 0.8f, phase + 1.8f, 0.9f),
                Brush.verticalGradient(
                    listOf(liquidTop.copy(alpha = 0.45f), liquidBottom.copy(alpha = 0.45f)),
                    startY = level, endY = h
                )
            )
            // Ön dalga
            drawPath(
                wavePathA(stroke, w - stroke, level + 2.dp.toPx(), h, amp, phase, 1.2f),
                Brush.verticalGradient(
                    listOf(liquidTop.copy(alpha = 0.9f), liquidBottom.copy(alpha = 0.95f)),
                    startY = level, endY = h
                )
            )
        }
    }

    // Cam kenarı + yansıma
    drawPath(bodyPath, glass.copy(alpha = 0.35f), style = Stroke(width = stroke))
    drawRoundRect(
        color = glass.copy(alpha = 0.14f),
        topLeft = Offset(w * 0.14f, bodyTop + 14.dp.toPx()),
        size = Size(5.dp.toPx(), h * 0.38f),
        cornerRadius = CornerRadius(3.dp.toPx())
    )
    // Kapak ağzı (boyun)
    drawRoundRect(
        color = glass.copy(alpha = 0.35f),
        topLeft = Offset(lipInset, 0f),
        size = Size(w - 2 * lipInset, lipH),
        cornerRadius = CornerRadius(4.dp.toPx()),
        style = Stroke(width = stroke)
    )

    // Madeni para: ağızdan yüzeye düşer, sıvıya girerken silinir
    if (dropKey > 0 && dropT < 0.5f) {
        val p = dropT / 0.5f
        val fall = p * p
        val cx = w * (0.5f + 0.1f * (if (dropKey % 2 == 0) 1f else -1f))
        val r = 7.dp.toPx()
        val cy = -r + (level + r - (-r)) * fall
        val a = (1f - ((p - 0.8f) / 0.2f).coerceIn(0f, 1f))
        if (a > 0f) {
            drawCircle(YeterAmber.copy(alpha = a), radius = r, center = Offset(cx, cy))
            drawCircle(
                Color(0xFFFFE2A8).copy(alpha = a), radius = r * 0.55f, center = Offset(cx, cy),
                style = Stroke(width = 1.5.dp.toPx())
            )
        }
    }
}

private fun DrawScope.wavePathA(
    left: Float, right: Float, level: Float, bottom: Float,
    amp: Float, phase: Float, cycles: Float
): Path {
    val path = Path()
    val width = right - left
    val step = 6.dp.toPx().coerceAtLeast(2f)
    var x = 0f
    path.moveTo(left, level + amp * sin(phase.toDouble()).toFloat())
    while (x < width) {
        x = min(x + step, width)
        val y = level + amp * sin((x / width * cycles * 2 * PI + phase)).toFloat()
        path.lineTo(left + x, y)
    }
    path.lineTo(right, bottom)
    path.lineTo(left, bottom)
    path.close()
    return path
}

// Sönük tek satır: "12 gün 4 sa harcamadan" (saniye yok)
private fun elapsedTextA(s: YeterUiState): String = when {
    s.elapsedDays > 0 -> "${s.elapsedDays} gün ${s.elapsedHours} sa harcamadan"
    s.elapsedHours > 0 -> "${s.elapsedHours} sa ${s.elapsedMinutes} dk harcamadan"
    else -> "${s.elapsedMinutes} dk harcamadan"
}

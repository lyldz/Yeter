package com.yeter.app

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
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
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.sin

// "Kasa" sayacı: üstteki yuvadan sabit tutarlı altın paralar düşer; tutar yalnızca para kasaya inince
// tam coinValue kadar artar. Sürekli akış yok, basamak basamak para sayma hissi.

private const val MaxInFlightD = 3
private const val DropMsD = 650

private val CoinLightD = Color(0xFFFFEFA8)
private val CoinMidD = YeterAmber
private val CoinDarkD = Color(0xFFB9770E)
private val SteelD = Color(0xFF2A3858)

// Okunaklı sabit tutarlar
private val NiceValuesD = doubleArrayOf(
    0.01, 0.02, 0.05, 0.10, 0.25, 0.50, 0.75, 1.0, 1.50, 2.25, 3.0, 5.0, 7.50,
    10.0, 15.0, 25.0, 50.0, 100.0, 250.0, 500.0, 1000.0, 2500.0, 5000.0, 10000.0
)

// Para aralığı en az ~4 sn olacak en küçük güzel değer; hiçbiri yetmezse en büyüğü. Gelir yoksa 0 (para düşmez).
private fun pickCoinValueD(dailySaving: Double): Double {
    if (dailySaving <= 0.0 || dailySaving.isNaN()) return 0.0
    val perSec = dailySaving / 86_400.0
    val need = perSec * 4.0
    for (v in NiceValuesD) if (v >= need) return v
    return NiceValuesD.last()
}

private fun fmtMoneyD(v: Double, locale: Locale): String =
    String.format(locale, "%,.2f", v.coerceAtLeast(0.0))

private fun fmtCoinD(v: Double, locale: Locale): String =
    if (v >= 1.0 && v == floor(v)) String.format(locale, "%,.0f", v) else String.format(locale, "%,.2f", v)

private class SafeGeoD(
    val cx: Float, val x0: Float, val x1: Float,
    val lidTop: Float, val lidH: Float,
    val bodyTop: Float, val bodyBottom: Float,
    val innerL: Float, val innerR: Float, val innerTop: Float, val innerBottom: Float
)

private fun DrawScope.drawCoinD(
    cx: Float, cy: Float, d: Float, sx: Float, sy: Float,
    light: Color, mid: Color, dark: Color
) {
    val w = d * sx
    val h = d * sy
    // kalınlık / kenar
    drawOval(color = dark, topLeft = Offset(cx - w / 2f, cy - h / 2f + d * 0.07f), size = Size(w, h))
    // yüz
    drawOval(
        brush = Brush.verticalGradient(listOf(light, mid), startY = cy - h / 2f, endY = cy + h / 2f),
        topLeft = Offset(cx - w / 2f, cy - h / 2f),
        size = Size(w, h)
    )
    // iç halka
    val rw = w * 0.68f
    val rh = h * 0.68f
    drawOval(
        color = dark.copy(alpha = 0.5f),
        topLeft = Offset(cx - rw / 2f, cy - rh / 2f),
        size = Size(rw, rh),
        style = Stroke(width = d * 0.05f)
    )
    if (sx > 0.5f && sy > 0.5f) {
        drawOval(
            color = Color.White.copy(alpha = 0.5f),
            topLeft = Offset(cx - w * 0.28f, cy - h * 0.32f),
            size = Size(w * 0.2f, h * 0.16f)
        )
    }
}

private fun DrawScope.drawSparkleD(c: Offset, r: Float, color: Color) {
    val w = r * 0.18f
    drawLine(color, Offset(c.x - r, c.y), Offset(c.x + r, c.y), strokeWidth = w)
    drawLine(color, Offset(c.x, c.y - r), Offset(c.x, c.y + r), strokeWidth = w)
}

// Kasa gövdesi (yarı saydam cam), içindeki altın yığını ve üst kapak/yuva.
private fun DrawScope.drawSafeD(
    g: SafeGeoD, dp: Float, fill: Float, done: Boolean, glowA: Float, tickV: Float
) {
    val safeW = g.x1 - g.x0
    val bodyH = g.bodyBottom - g.bodyTop
    val corner = 14f * dp

    if (done) {
        drawCircle(
            brush = Brush.radialGradient(
                listOf(CoinMidD.copy(alpha = glowA * 0.45f), Color.Transparent),
                center = Offset(g.cx, g.bodyTop + bodyH * 0.5f),
                radius = safeW * 0.85f
            ),
            radius = safeW * 0.85f,
            center = Offset(g.cx, g.bodyTop + bodyH * 0.5f)
        )
    }

    // cam gövde
    drawRoundRect(
        color = YeterCard.copy(alpha = 0.72f),
        topLeft = Offset(g.x0, g.bodyTop),
        size = Size(safeW, bodyH),
        cornerRadius = CornerRadius(corner, corner)
    )

    // altın yığını
    val innerH = g.innerBottom - g.innerTop
    val surfaceY = g.innerBottom - fill.coerceIn(0f, 1f) * innerH
    if (fill > 0.001f) {
        val clip = Path().apply {
            addRoundRect(
                RoundRect(
                    g.innerL, g.innerTop, g.innerR, g.innerBottom,
                    CornerRadius(9f * dp, 9f * dp)
                )
            )
        }
        clipPath(clip) {
            drawRect(
                brush = Brush.verticalGradient(
                    listOf(CoinMidD, CoinDarkD), startY = surfaceY, endY = g.innerBottom
                ),
                topLeft = Offset(g.innerL, surfaceY),
                size = Size(g.innerR - g.innerL, g.innerBottom - surfaceY)
            )
            // para kenarı çizgileri
            var ly = surfaceY + 10f * dp
            while (ly < g.innerBottom) {
                drawLine(
                    CoinDarkD.copy(alpha = 0.35f),
                    Offset(g.innerL, ly), Offset(g.innerR, ly), strokeWidth = 1f * dp
                )
                ly += 8f * dp
            }
            // yüzeydeki para yüzleri
            val d = 22f * dp
            var i = 0
            var x = g.innerL + 6f * dp
            while (x < g.innerR + d) {
                val yy = surfaceY + (if (i % 2 == 0) 0f else 4f * dp)
                drawCoinD(x, yy, d, 1f, 0.42f, CoinLightD, CoinMidD, CoinDarkD)
                x += 19f * dp
                i++
            }
        }
        // iniş parıltısı
        if (tickV > 0.01f) {
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(Color(0xFFFFF4C2).copy(alpha = 0.35f * tickV), Color.Transparent),
                    center = Offset(g.cx, surfaceY), radius = 70f * dp
                ),
                radius = 70f * dp,
                center = Offset(g.cx, surfaceY)
            )
        }
    }

    // cam parlaması
    drawRoundRect(
        color = Color.White.copy(alpha = 0.07f),
        topLeft = Offset(g.x0 + 6f * dp, g.bodyTop + 8f * dp),
        size = Size(7f * dp, bodyH - 22f * dp),
        cornerRadius = CornerRadius(4f * dp, 4f * dp)
    )
    // çerçeve
    drawRoundRect(
        color = if (done) CoinMidD.copy(alpha = 0.9f) else YeterOutline.copy(alpha = 0.6f),
        topLeft = Offset(g.x0, g.bodyTop),
        size = Size(safeW, bodyH),
        cornerRadius = CornerRadius(corner, corner),
        style = Stroke(width = 2f * dp)
    )

    // kapak ve yuva
    drawRoundRect(
        brush = Brush.verticalGradient(
            listOf(Color(0xFF4A5C84), SteelD), startY = g.lidTop, endY = g.lidTop + g.lidH
        ),
        topLeft = Offset(g.x0 - 4f * dp, g.lidTop),
        size = Size(safeW + 8f * dp, g.lidH),
        cornerRadius = CornerRadius(7f * dp, 7f * dp)
    )
    val slotW = 64f * dp
    val slotH = 6f * dp
    val slotTop = g.lidTop + (g.lidH - slotH) / 2f
    drawRoundRect(
        color = Color(0xFF05080F),
        topLeft = Offset(g.cx - slotW / 2f, slotTop),
        size = Size(slotW, slotH),
        cornerRadius = CornerRadius(slotH / 2f, slotH / 2f)
    )
    drawRoundRect(
        color = CoinMidD.copy(alpha = 0.55f),
        topLeft = Offset(g.cx - slotW / 2f, slotTop),
        size = Size(slotW, slotH),
        cornerRadius = CornerRadius(slotH / 2f, slotH / 2f),
        style = Stroke(width = 1f * dp)
    )
}

@Composable
fun CounterD(state: YeterUiState) {
    val locale = remember { Locale.getDefault() }
    val done = state.progressFraction >= 1f
    val progress = state.progressFraction.coerceIn(0f, 1f)
    val symbol = state.symbol.trim()
    val percent = (progress * 100).toInt().coerceIn(0, 100)

    val coinValue = remember(state.dailySaving) { pickCoinValueD(state.dailySaving) }
    val latest = rememberUpdatedState(state)

    // Gösterilen tutar ve animasyon değerleri
    var displayed by remember { mutableDoubleStateOf(state.totalSaved) }
    var labelText by remember { mutableStateOf("") }
    val tick = remember { Animatable(0f) }      // tutar "tık" (ölçek/parıltı), 1 -> 0
    val label = remember { Animatable(1f) }     // "+2,25" etiketi, 0 -> 1 (1 = gizli)
    val slots = remember { List(MaxInFlightD) { Animatable(1f) } } // düşen para ilerlemesi (1 = gizli)
    val landX = remember { FloatArray(MaxInFlightD) }               // iniş x kayması (dp)

    // Motor: ticker en fazla 250 ms'de bir. Bileşimi tetiklemez; yalnızca para inince displayed değişir.
    LaunchedEffect(coinValue) {
        val slotBusy = BooleanArray(MaxInFlightD)
        val jobs = arrayOfNulls<Job>(MaxInFlightD)
        var lastTotal = Double.NaN
        var baseTime = 0L
        var seq = 0

        fun live(now: Long): Double {
            val s = latest.value
            if (s.totalSaved != lastTotal) {
                lastTotal = s.totalSaved
                baseTime = now
            }
            val el = (now - baseTime).coerceIn(0L, 2000L)
            return (s.totalSaved + s.dailySaving.coerceAtLeast(0.0) * el / 86_400_000.0).coerceAtLeast(0.0)
        }

        var anchor = live(System.currentTimeMillis())
        var landed = 0L
        var inflight = 0
        displayed = anchor

        while (true) {
            val now = System.currentTimeMillis()
            val l = live(now)
            if (coinValue <= 0.0) {
                anchor = l
                landed = 0L
                displayed = l
            } else {
                val shown = anchor + landed * coinValue
                val target = floor((l - anchor) / coinValue).toLong()
                if (target - landed - inflight > 3 || l < shown - coinValue) {
                    // Ani değişim (uzun ara, ödül, sıfırlama): para yağdırmadan yeniden hizala
                    for (j in jobs) j?.cancelAndJoin()
                    for (s in slots) s.snapTo(1f)
                    inflight = 0
                    landed = 0L
                    anchor = l
                    displayed = l
                } else if (target > landed + inflight && inflight < MaxInFlightD) {
                    val idx = slotBusy.indexOfFirst { !it }
                    if (idx >= 0) {
                        slotBusy[idx] = true
                        inflight++
                        landX[idx] = (((seq++ * 37) % 9) - 4) * 11f
                        jobs[idx] = launch {
                            try {
                                slots[idx].snapTo(0f)
                                slots[idx].animateTo(
                                    1f,
                                    tween(DropMsD, easing = CubicBezierEasing(0.5f, 0f, 1f, 0.8f))
                                )
                                landed++
                                displayed = anchor + landed * coinValue
                                labelText = "+" + fmtCoinD(coinValue, locale)
                                launch {
                                    tick.snapTo(1f)
                                    tick.animateTo(0f, tween(520, easing = FastOutSlowInEasing))
                                }
                                launch {
                                    label.snapTo(0f)
                                    label.animateTo(1f, tween(1100, easing = LinearOutSlowInEasing))
                                }
                            } finally {
                                slotBusy[idx] = false
                                inflight--
                            }
                        }
                    }
                }
            }
            delay(250L)
        }
    }

    // Doluluk (ilerlemeye bağlı); çizimde okunur
    val fillTarget = when {
        done -> 1f
        state.totalSaved <= 0.0 -> 0f
        else -> progress.coerceAtLeast(0.05f)
    }
    val fillState: State<Float> = animateFloatAsState(fillTarget, tween(900), label = "fillD")

    // Hedef tamamlanınca parıltı nabzı (yalnızca çizimde okunur)
    val glowState: State<Float>? = if (done) {
        rememberInfiniteTransition(label = "glowD").animateFloat(
            initialValue = 0.35f, targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "glowDv"
        )
    } else null

    val shownText = fmtMoneyD(displayed, locale)
    val amountSize = when {
        shownText.length <= 8 -> 46.sp
        shownText.length <= 11 -> 38.sp
        else -> 32.sp
    }
    val amountBrush = Brush.verticalGradient(listOf(Color(0xFFFFF4C2), YeterAmber))

    val desc = "Kasada " + String.format(locale, "%,d", displayed.toLong().coerceAtLeast(0L)) +
        " " + symbol + ", hedefin yüzde " + percent + "'i" +
        (if (done) ". Hedefe ulaştın" else "")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clearAndSetSemantics { contentDescription = desc },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp)
        ) {
            val dp = 1.dp.toPx()
            val safeW = min(size.width, 250f * dp)
            val x0 = (size.width - safeW) / 2f
            val cx = size.width / 2f
            val lidTop = 40f * dp
            val lidH = 16f * dp
            val bodyTop = lidTop + lidH - 2f * dp
            val bodyBottom = size.height - 6f * dp
            val g = SafeGeoD(
                cx, x0, x0 + safeW, lidTop, lidH, bodyTop, bodyBottom,
                x0 + 11f * dp, x0 + safeW - 11f * dp, bodyTop + 8f * dp, bodyBottom - 10f * dp
            )
            val fill = fillState.value
            val glowA = glowState?.value ?: 0f
            drawSafeD(g, dp, fill, done, glowA, tick.value)

            // düşen paralar
            val surfaceY = g.innerBottom - fill * (g.innerBottom - g.innerTop)
            val d = 22f * dp
            val startY = lidTop + 1f * dp
            val landY = surfaceY - 4f * dp
            for (i in 0 until MaxInFlightD) {
                val p = slots[i].value
                if (p <= 0f || p >= 1f) continue
                val sx = abs(sin(p * 3.5f * Math.PI.toFloat())).coerceAtLeast(0.2f)
                val x = cx + landX[i] * dp * p
                val y = startY + (landY - startY).coerceAtLeast(0f) * p
                drawCoinD(x, y, d, sx, 1f, CoinLightD, CoinMidD, CoinDarkD)
            }

            if (done) {
                val c = Color(0xFFFFF4C2).copy(alpha = glowA)
                drawSparkleD(Offset(g.x0 - 10f * dp, bodyTop + 24f * dp), 9f * dp, c)
                drawSparkleD(Offset(g.x1 + 10f * dp, bodyTop + 60f * dp), 7f * dp, c)
                drawSparkleD(Offset(cx + 70f * dp, lidTop - 14f * dp), 10f * dp, c)
                drawSparkleD(Offset(cx - 80f * dp, lidTop - 4f * dp), 6f * dp, c)
            }
        }

        // Tutar + yukarı süzülen "+2,25" etiketi
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier
                    .padding(top = 12.dp)
                    .graphicsLayer {
                        val s = 1f + 0.08f * tick.value
                        scaleX = s
                        scaleY = s
                    }
            ) {
                Text(
                    text = symbol,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = YeterAmber.copy(alpha = 0.9f),
                    modifier = Modifier.padding(top = 6.dp, end = 4.dp)
                )
                Text(
                    text = shownText,
                    style = TabularNums.copy(
                        brush = amountBrush,
                        shadow = Shadow(Color.Black.copy(alpha = 0.45f), Offset(0f, 4f), 10f)
                    ),
                    fontSize = amountSize,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    softWrap = false
                )
            }
            Text(
                text = labelText,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = if (done) YeterLime else YeterMint,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .graphicsLayer {
                        val p = label.value
                        translationY = -p * 30.dp.toPx() + 18.dp.toPx()
                        alpha = when {
                            p >= 1f -> 0f
                            p < 0.15f -> p / 0.15f
                            else -> 1f - (p - 0.15f) / 0.85f
                        }
                    }
            )
        }

        // Tek sönük satır
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (coinValue > 0.0) {
                Text(
                    text = "Her altın = $symbol" + fmtCoinD(coinValue, locale),
                    fontSize = 12.sp,
                    color = YeterTextSecondary.copy(alpha = 0.7f),
                    maxLines = 1
                )
                Text(
                    text = "·",
                    fontSize = 12.sp,
                    color = YeterTextSecondary.copy(alpha = 0.5f)
                )
            }
            if (done) {
                Text(
                    text = "Hedefe ulaştın",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = YeterLime,
                    maxLines = 1
                )
            } else {
                Text(
                    text = "Hedef %$percent",
                    fontSize = 12.sp,
                    color = YeterTextSecondary.copy(alpha = 0.7f),
                    maxLines = 1
                )
            }
        }
        Spacer(Modifier.height(2.dp))
    }
}

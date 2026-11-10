package com.yeter.app

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.min
import kotlin.math.roundToLong

// Konsept B: madeni para yığını. Birikim ilerledikçe piramit şeklinde büyüyen parlak altın paralar,
// yeni birikimde tepeye düşüp sekerek yerleşen para, altında sabit (akmayan) altın tonlu tutar.

private const val BaseRowB = 8                 // taban sırasındaki para sayısı
private const val MaxCoinsB = 36               // 8+7+...+1 = 36 çizilen para üst sınırı
private val CoinLightB = Color(0xFFFFE9A0)
private val CoinDarkB = Color(0xFFB36B00)

// Para i'nin (satır, sıra) yeri: alttan yukarı satırlar 8,7,6...1; satır içinde ortadan dışa doğru dolar.
private fun slotOfB(index: Int): Pair<Int, Int> {
    var left = index
    var row = 0
    var width = BaseRowB
    while (left >= width && width > 1) {
        left -= width
        width -= 1
        row += 1
    }
    return row to left
}

// Satır içindeki sıra numarasını (ortadan dışa) gerçek yatay konuma çevirir: -(w-1)/2 .. +(w-1)/2
private fun slotXB(order: Int, width: Int): Float {
    val center = (width - 1) / 2f
    val k = order / 2
    val pos = if (width % 2 == 1) {
        if (order == 0) center else if (order % 2 == 1) center + (k + 1) else center - (k)
    } else {
        if (order % 2 == 0) (width / 2 - 1 - k).toFloat() else (width / 2 + k).toFloat()
    }
    return pos - center
}

private fun DrawScope.drawCoinB(
    cx: Float, cy: Float, cw: Float,
    light: Color, mid: Color, dark: Color
) {
    val eh = cw * 0.36f
    val th = cw * 0.15f
    val left = cx - cw / 2f
    // gövde (kenar)
    drawRect(
        brush = Brush.horizontalGradient(
            listOf(dark, mid, dark), startX = left, endX = left + cw
        ),
        topLeft = Offset(left, cy),
        size = Size(cw, th)
    )
    // alt elips
    drawOval(color = dark, topLeft = Offset(left, cy + th - eh / 2f), size = Size(cw, eh))
    // üst yüz
    drawOval(
        brush = Brush.verticalGradient(
            listOf(light, mid), startY = cy - eh / 2f, endY = cy + eh / 2f
        ),
        topLeft = Offset(left, cy - eh / 2f),
        size = Size(cw, eh)
    )
    // iç halka
    val rw = cw * 0.72f
    val rh = eh * 0.72f
    drawOval(
        color = dark.copy(alpha = 0.55f),
        topLeft = Offset(cx - rw / 2f, cy - rh / 2f),
        size = Size(rw, rh),
        style = Stroke(width = cw * 0.04f)
    )
    // küçük parlama
    drawOval(
        color = Color.White.copy(alpha = 0.55f),
        topLeft = Offset(cx - cw * 0.26f, cy - eh * 0.30f),
        size = Size(cw * 0.22f, eh * 0.2f)
    )
}

private fun DrawScope.drawSparkleB(c: Offset, r: Float, color: Color) {
    val w = r * 0.18f
    drawLine(color, Offset(c.x - r, c.y), Offset(c.x + r, c.y), strokeWidth = w)
    drawLine(color, Offset(c.x, c.y - r), Offset(c.x, c.y + r), strokeWidth = w)
}

@Composable
fun CounterB(state: YeterUiState) {
    val done = state.progressFraction >= 1f
    val locale = Locale.getDefault()

    // Canlı değer: ticker en fazla 250 ms'de bir; tutar yalnızca kuruş değişince yeniden bileşir.
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
            val elapsed = (nowMs - baseTime).coerceIn(0L, 300_000L)
            (baseCents + dailyCents * elapsed / 86_400_000.0).roundToLong().coerceAtLeast(0L)
        }
    }

    val sep = DecimalFormatSymbols.getInstance(locale).decimalSeparator
    val intText = String.format(locale, "%,d", liveCents / 100)
    val centText = String.format(locale, "%02d", (liveCents % 100).toInt())
    val symbol = state.symbol.trim()
    val percent = (state.progressFraction * 100).toInt().coerceIn(0, 100)

    // Para sayısı: ilerlemeye bağlı, 1..36
    val progress = state.progressFraction.coerceIn(0f, 1f)
    val coinCount = when {
        done -> MaxCoinsB
        state.totalSaved <= 0.0 -> 0
        else -> ceil(progress * MaxCoinsB).toInt().coerceIn(1, MaxCoinsB)
    }

    // Düşme animasyonu: h = 1 (yukarıda) -> 0 (yerleşti); küçük sekme. Çizim aşamasında okunur.
    val drop = remember { Animatable(0f) }
    val stepCents = (dailyCents * 20_000.0 / 86_400_000.0).toLong().coerceAtLeast(1L)
    val tickKey = liveCents / stepCents
    var firstRun by remember { mutableLongStateOf(0L) }
    LaunchedEffect(tickKey, coinCount) {
        if (firstRun == 0L) {
            firstRun = 1L
            return@LaunchedEffect
        }
        if (coinCount == 0) return@LaunchedEffect
        drop.snapTo(1f)
        drop.animateTo(0f, tween(360, easing = CubicBezierEasing(0.5f, 0f, 1f, 0.8f)))
        drop.animateTo(0.14f, tween(110, easing = LinearOutSlowInEasing))
        drop.animateTo(0f, tween(110, easing = CubicBezierEasing(0.5f, 0f, 1f, 1f)))
        drop.animateTo(0.04f, tween(60, easing = LinearOutSlowInEasing))
        drop.animateTo(0f, tween(60, easing = CubicBezierEasing(0.5f, 0f, 1f, 1f)))
    }

    // Parıltı nabzı (yalnızca çizimde okunur)
    val pulse = rememberInfiniteTransition(label = "pulseB")
    val glow = pulse.animateFloat(
        initialValue = 0.10f, targetValue = if (done) 0.55f else 0.22f,
        animationSpec = infiniteRepeatable(tween(2600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "glowB"
    )
    val gold = YeterAmber
    val light = if (done) Color(0xFFFFF4C2) else CoinLightB
    val mid = if (done) Color(0xFFFFC857) else gold
    val dark = CoinDarkB
    val halo = if (done) YeterLime else gold

    val amountBrush = Brush.verticalGradient(
        listOf(Color(0xFFFFF1BD), gold, Color(0xFFD98E12))
    )
    val a11y = "Birikim: $intText $symbol, hedefin yüzde $percent'i"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 4.dp)
            .clearAndSetSemantics { contentDescription = a11y },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Yığın
        Canvas(modifier = Modifier.fillMaxWidth().height(170.dp)) {
            val cw = min(44.dp.toPx(), (size.width - 32.dp.toPx()) / 7.3f)
            val eh = cw * 0.36f
            val rowStep = cw * 0.2f
            val spacing = cw * 0.9f
            val baseCy = size.height - 14.dp.toPx() - eh / 2f
            val cx0 = size.width / 2f
            val fall = 90.dp.toPx()

            // Halo
            val hr = size.width * 0.45f
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(halo.copy(alpha = glow.value), Color.Transparent),
                    center = Offset(cx0, baseCy - cw * 0.5f), radius = hr
                ),
                radius = hr,
                center = Offset(cx0, baseCy - cw * 0.5f)
            )
            // Zemin gölgesi
            val sw = cw * BaseRowB * 0.95f
            drawOval(
                color = Color.Black.copy(alpha = 0.35f),
                topLeft = Offset(cx0 - sw / 2f, baseCy + cw * 0.08f),
                size = Size(sw, eh * 1.1f)
            )

            val n = coinCount
            for (i in 0 until n) {
                val (row, order) = slotOfB(i)
                val width = BaseRowB - row
                val x = cx0 + slotXB(order, width) * spacing
                var y = baseCy - row * rowStep
                if (i == n - 1) y -= drop.value * fall
                drawCoinB(x, y, cw, light, mid, dark)
            }

            if (done) {
                val a = (0.35f + glow.value).coerceAtMost(1f)
                val topRow = slotOfB(n - 1).first
                val topY = baseCy - topRow * rowStep - cw * 0.5f
                val c = Color(0xFFFFF4C2).copy(alpha = a)
                drawSparkleB(Offset(cx0 - cw * 2.2f, topY - cw * 0.2f), cw * 0.30f, c)
                drawSparkleB(Offset(cx0 + cw * 2.0f, topY + cw * 0.1f), cw * 0.22f, c)
                drawSparkleB(Offset(cx0 + cw * 0.3f, topY - cw * 0.9f), cw * 0.34f, c)
                drawSparkleB(Offset(cx0 - cw * 0.8f, topY - cw * 1.2f), cw * 0.18f, c)
            }
        }

        Spacer(Modifier.height(6.dp))

        // Tutar: kuruş değişince güncellenir, akmaz
        Row(verticalAlignment = Alignment.Top) {
            Text(
                text = symbol,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = gold.copy(alpha = 0.9f),
                modifier = Modifier.padding(top = 6.dp, end = 4.dp)
            )
            Text(
                text = intText,
                style = TabularNums.copy(
                    brush = amountBrush,
                    shadow = Shadow(Color.Black.copy(alpha = 0.45f), Offset(0f, 4f), 10f)
                ),
                fontSize = if (intText.length <= 6) 48.sp else 40.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1, softWrap = false
            )
            Text(
                text = "$sep$centText",
                style = TabularNums.copy(brush = amountBrush),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1, softWrap = false,
                modifier = Modifier.padding(top = 6.dp, start = 2.dp)
            )
        }

        Spacer(Modifier.height(6.dp))

        // Tek sönük satır: hedef ve yüzde (zaman bilgisi yok)
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (done) "Hedef tamamlandı" else state.targetName,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                maxLines = 1
            )
            Text(
                text = "%$percent",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (done) YeterLime else YeterMint
            )
        }
    }
}

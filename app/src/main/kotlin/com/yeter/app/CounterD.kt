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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
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
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

// "Kasa" sayacı: üstteki yuvadan sabit tutarlı altın paralar düşer; tutar yalnızca para kasaya inince
// tam coinValue kadar artar. Sürekli akış yok, basamak basamak para sayma hissi.

private const val MaxInFlightD = 3
private const val DropMsD = 650

private val CoinLightD = Color(0xFFFFEFA8)
private val CoinMidD = YeterAmber
private val CoinDarkD = Color(0xFFB9770E)
private val CoinEdgeD = Color(0xFF8A5A0B)
private val SteelD = Color(0xFF2A3858)
private val SteelTopD = Color(0xFF1B2640)
private val MetalEdgeD = Color(0xFF8FA0C0)
private val SparkD = Color(0xFFFFF4C2)
private val StackTonesD = arrayOf(
    Color(0xFFFFD36B), Color(0xFFFFC24A), Color(0xFFFFB547), Color(0xFFF2A92E), Color(0xFFFFE08A)
)

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

// Boyuta bağlı her şey (geometri, fırça, yol, yığın ızgarası) bir kez hazırlanır.
private class SafeCacheD(val g: SafeGeoD, val dp: Float) {
    val n = StackRowsD * StackColsD
    val sx = FloatArray(n)
    val sy = FloatArray(n)
    val sw = FloatArray(n)
    val sh = FloatArray(n)
    val tone = IntArray(n)
    val bodyClip = Path()
    val innerClip = Path()
    val shine = Path()
    val pile = Path() // her karede yeniden doldurulur (rewind), yeni nesne üretilmez
    lateinit var bodyBrush: Brush
    lateinit var lidBrush: Brush
    lateinit var cavityBrush: Brush
    lateinit var pileBrush: Brush
    lateinit var stripBrush: Brush
    lateinit var shineBrush: Brush
    lateinit var wallLBrush: Brush
    lateinit var wallRBrush: Brush
    lateinit var glowBrush: Brush
    lateinit var sweepBrush: Brush
}

private const val StackRowsD = 5
private const val StackColsD = 8 // 5 x 8 = 40 para üst sınırı

private fun buildSafeCacheD(w: Float, h: Float, dp: Float): SafeCacheD {
    val safeW = min(w, 250f * dp)
    val x0 = (w - safeW) / 2f
    val cx = w / 2f
    val lidTop = min(40f * dp, h * 0.2f) // kısa kutuda üst boşluk orantılı daralır
    val lidH = 16f * dp
    val bodyTop = lidTop + lidH - 2f * dp
    val bodyBottom = h - 6f * dp
    val g = SafeGeoD(
        cx, x0, x0 + safeW, lidTop, lidH, bodyTop, bodyBottom,
        x0 + 11f * dp, x0 + safeW - 11f * dp, bodyTop + 8f * dp, bodyBottom - 10f * dp
    )
    val c = SafeCacheD(g, dp)
    val bodyH = bodyBottom - bodyTop
    val corner = 14f * dp
    c.bodyClip.addRoundRect(RoundRect(g.x0, bodyTop, g.x1, bodyBottom, CornerRadius(corner, corner)))
    c.innerClip.addRoundRect(
        RoundRect(g.innerL, g.innerTop, g.innerR, g.innerBottom, CornerRadius(9f * dp, 9f * dp))
    )
    // eğik çapraz parlama
    c.shine.apply {
        moveTo(g.x0 + safeW * 0.50f, bodyTop)
        lineTo(g.x0 + safeW * 0.66f, bodyTop)
        lineTo(g.x0 + safeW * 0.36f, bodyTop + bodyH * 0.55f)
        lineTo(g.x0 + safeW * 0.20f, bodyTop + bodyH * 0.55f)
        close()
    }
    c.bodyBrush = Brush.verticalGradient(listOf(SteelTopD, SteelD), startY = bodyTop, endY = bodyBottom)
    c.lidBrush = Brush.verticalGradient(
        listOf(Color(0xFF5A6E98), SteelD), startY = g.lidTop, endY = g.lidTop + g.lidH
    )
    c.cavityBrush = Brush.verticalGradient(
        listOf(Color(0xFF070C18), Color(0xFF0E1628)), startY = g.innerTop, endY = g.innerBottom
    )
    c.pileBrush = Brush.verticalGradient(
        listOf(CoinMidD, CoinDarkD), startY = g.innerTop, endY = g.innerBottom
    )
    c.stripBrush = Brush.verticalGradient(
        listOf(Color.White.copy(alpha = 0.20f), Color.White.copy(alpha = 0.03f)),
        startY = bodyTop, endY = bodyBottom
    )
    c.shineBrush = Brush.verticalGradient(
        listOf(Color.White.copy(alpha = 0.10f), Color.Transparent),
        startY = bodyTop, endY = bodyTop + bodyH * 0.55f
    )
    c.wallLBrush = Brush.horizontalGradient(
        listOf(CoinMidD.copy(alpha = 0.35f), Color.Transparent),
        startX = g.innerL, endX = g.innerL + 10f * dp
    )
    c.wallRBrush = Brush.horizontalGradient(
        listOf(Color.Transparent, CoinMidD.copy(alpha = 0.35f)),
        startX = g.innerR - 10f * dp, endX = g.innerR
    )
    c.glowBrush = Brush.radialGradient(
        listOf(CoinMidD.copy(alpha = 0.45f), Color.Transparent),
        center = Offset(g.cx, bodyTop + bodyH * 0.5f), radius = safeW * 0.85f
    )
    c.sweepBrush = Brush.horizontalGradient(
        listOf(Color.Transparent, SparkD.copy(alpha = 0.55f), Color.Transparent),
        startX = 0f, endX = 60f * dp
    )
    // Yığın: sabit ızgara, indeks tabanlı sahte rastgele ofset/boyut/ton. Hep aynı yerde durur.
    val iw = g.innerR - g.innerL
    val innerH = g.innerBottom - g.innerTop
    val colW = iw / (StackColsD - 1)
    val rowH = innerH / StackRowsD
    for (r in 0 until StackRowsD) for (col in 0 until StackColsD) {
        val i = r * StackColsD + col
        val cw = colW * (1.0f + ((i * 29) % 7) * 0.025f)
        c.sw[i] = cw
        c.sh[i] = cw * 0.42f
        c.sx[i] = g.innerL + (col + 0.5f * (r % 2)) * colW + (((i * 37) % 7) - 3) * 0.8f * dp
        c.sy[i] = g.innerBottom - (r + 0.5f) * rowH + (((i * 53) % 5) - 2) * 1.0f * dp
        c.tone[i] = (i * 37) % StackTonesD.size
    }
    return c
}

// Tek bir beş köşeli yıldız (yarıçap 1, orijinde); paranın damgası için ölçeklenir.
private val StarD = Path().apply {
    for (k in 0 until 10) {
        val r = if (k % 2 == 0) 1f else 0.42f
        val a = (-90.0 + k * 36.0) * PI / 180.0
        val px = (cos(a) * r).toFloat()
        val py = (sin(a) * r).toFloat()
        if (k == 0) moveTo(px, py) else lineTo(px, py)
    }
    close()
}

// Düşen para: ışık sol üstten; yan dönünce kalınlık büyür, yüz kararır.
private fun DrawScope.drawCoinD(
    cx: Float, cy: Float, d: Float, sx: Float, sy: Float,
    light: Color, mid: Color, dark: Color
) {
    val w = d * sx
    val h = d * sy
    val lit = sx.coerceIn(0f, 1f)
    val t = d * (0.08f + 0.14f * (1f - lit))
    // kalınlık (altının koyu tonu)
    drawOval(color = CoinEdgeD, topLeft = Offset(cx - w / 2f, cy - h / 2f + t), size = Size(w, h))
    drawRect(color = CoinEdgeD, topLeft = Offset(cx - w / 2f, cy), size = Size(w, t))
    // yüz
    drawOval(
        brush = Brush.radialGradient(
            listOf(lerp(dark, light, lit), lerp(dark, mid, lit), dark),
            center = Offset(cx - w * 0.2f, cy - h * 0.25f),
            radius = max(max(w, h) * 0.85f, 1f)
        ),
        topLeft = Offset(cx - w / 2f, cy - h / 2f),
        size = Size(w, h)
    )
    drawOval(
        color = CoinEdgeD.copy(alpha = 0.55f),
        topLeft = Offset(cx - w / 2f, cy - h / 2f), size = Size(w, h),
        style = Stroke(width = d * 0.03f)
    )
    if (sx > 0.3f) {
        val rw = w * 0.72f
        val rh = h * 0.72f
        val tl = Offset(cx - rw / 2f, cy - rh / 2f)
        val sz = Size(rw, rh)
        val sw = Stroke(width = d * 0.04f)
        drawArc(Color.White.copy(alpha = 0.35f), 180f, 180f, false, tl, sz, style = sw)
        drawArc(CoinEdgeD.copy(alpha = 0.55f), 0f, 180f, false, tl, sz, style = sw)
    }
    if (sx > 0.35f) {
        withTransform({
            translate(cx, cy)
            scale(sx * d * 0.17f, sy * d * 0.17f, Offset.Zero)
        }) { drawPath(StarD, CoinEdgeD.copy(alpha = 0.7f)) }
    }
    if (sx > 0.5f) {
        drawCircle(Color.White, radius = d * 0.045f, center = Offset(cx - w * 0.28f, cy - h * 0.3f))
    }
}

// Yığındaki para: gradyansız (kare başı ~40 adet), ton farkı renk dizisinden.
private fun DrawScope.drawStackCoinD(cx: Float, cy: Float, w: Float, h: Float, face: Color, dp: Float) {
    val t = 3.5f * dp
    drawOval(color = CoinEdgeD, topLeft = Offset(cx - w / 2f, cy - h / 2f + t), size = Size(w, h))
    drawRect(color = CoinEdgeD, topLeft = Offset(cx - w / 2f, cy), size = Size(w, t))
    drawOval(color = face, topLeft = Offset(cx - w / 2f, cy - h / 2f), size = Size(w, h))
    drawOval(
        color = CoinEdgeD.copy(alpha = 0.4f),
        topLeft = Offset(cx - w * 0.33f, cy - h * 0.33f), size = Size(w * 0.66f, h * 0.66f),
        style = Stroke(width = 0.9f * dp)
    )
    drawCircle(Color.White.copy(alpha = 0.7f), radius = 1.1f * dp, center = Offset(cx - w * 0.26f, cy - h * 0.2f))
}

private fun DrawScope.drawSparkleD(c: Offset, r: Float, color: Color) {
    val w = r * 0.18f
    drawLine(color, Offset(c.x - r, c.y), Offset(c.x + r, c.y), strokeWidth = w)
    drawLine(color, Offset(c.x, c.y - r), Offset(c.x, c.y + r), strokeWidth = w)
}

// Kasa: çelik gövde, cam iç duvar, deterministik altın yığını, kapak ve yuva.
private fun DrawScope.drawSafeD(
    c: SafeCacheD, fill: Float, done: Boolean, glowA: Float, tickV: Float
) {
    val g = c.g
    val dp = c.dp
    val safeW = g.x1 - g.x0
    val bodyH = g.bodyBottom - g.bodyTop
    val corner = 14f * dp
    val innerW = g.innerR - g.innerL
    val innerH = g.innerBottom - g.innerTop

    if (done) {
        drawCircle(
            brush = c.glowBrush, radius = safeW * 0.85f,
            center = Offset(g.cx, g.bodyTop + bodyH * 0.5f), alpha = glowA
        )
    }

    // çelik gövde
    drawRoundRect(
        brush = c.bodyBrush, topLeft = Offset(g.x0, g.bodyTop), size = Size(safeW, bodyH),
        cornerRadius = CornerRadius(corner, corner)
    )
    // iç boşluk
    drawRoundRect(
        brush = c.cavityBrush, topLeft = Offset(g.innerL, g.innerTop), size = Size(innerW, innerH),
        cornerRadius = CornerRadius(9f * dp, 9f * dp)
    )

    // altın yığını
    val f = fill.coerceIn(0f, 1f)
    val surfaceY = g.innerBottom - f * innerH
    if (f > 0.001f) {
        clipPath(c.innerClip) {
            withTransform({ scale(1f, 1f - 0.06f * tickV, Offset(g.cx, g.innerBottom)) }) {
                val bump = 4f * dp
                c.pile.rewind()
                c.pile.moveTo(g.innerL, surfaceY + bump * 0.8f)
                c.pile.cubicTo(
                    g.innerL + innerW * 0.3f, surfaceY - bump * 1.2f,
                    g.innerL + innerW * 0.7f, surfaceY - bump * 1.2f,
                    g.innerR, surfaceY + bump * 0.8f
                )
                c.pile.lineTo(g.innerR, g.innerBottom + 2f * dp)
                c.pile.lineTo(g.innerL, g.innerBottom + 2f * dp)
                c.pile.close()
                clipPath(c.pile) {
                    drawRect(
                        brush = c.pileBrush, topLeft = Offset(g.innerL, g.innerTop), size = Size(innerW, innerH + 2f * dp)
                    )
                    // üst sıralar önce, alt sıralar üstüne
                    for (r in StackRowsD - 1 downTo 0) for (col in 0 until StackColsD) {
                        val i = r * StackColsD + col
                        if (c.sy[i] + c.sh[i] * 0.5f < surfaceY - 8f * dp) continue
                        drawStackCoinD(c.sx[i], c.sy[i], c.sw[i], c.sh[i], StackTonesD[c.tone[i]], dp)
                    }
                    if (done) {
                        val sx0 = g.innerL + ((glowA - 0.35f) / 0.65f) * innerW - 30f * dp
                        withTransform({ translate(sx0, 0f) }) {
                            drawRect(
                                brush = c.sweepBrush, topLeft = Offset(0f, surfaceY - 8f * dp),
                                size = Size(60f * dp, 18f * dp)
                            )
                        }
                    }
                }
            }
            // cam iç duvara altın yansıma (%70 ve üstü)
            if (f >= 0.7f) {
                val a = ((f - 0.7f) / 0.3f).coerceIn(0f, 1f)
                drawRect(c.wallLBrush, Offset(g.innerL, g.innerTop), Size(10f * dp, innerH), alpha = a)
                drawRect(c.wallRBrush, Offset(g.innerR - 10f * dp, g.innerTop), Size(10f * dp, innerH), alpha = a)
            }
        }
        // iniş: temas gölgesi, parıltı, toz
        if (tickV > 0.01f) {
            drawOval(
                color = Color.Black.copy(alpha = 0.30f * tickV),
                topLeft = Offset(g.cx - 32f * dp, surfaceY - 3f * dp), size = Size(64f * dp, 8f * dp)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(SparkD.copy(alpha = 0.35f * tickV), Color.Transparent),
                    center = Offset(g.cx, surfaceY), radius = 70f * dp
                ),
                radius = 70f * dp, center = Offset(g.cx, surfaceY)
            )
            val dust = SparkD.copy(alpha = tickV)
            val dist = (1f - tickV) * 22f * dp
            for (k in 0 until 6) {
                val ang = k * 60f * PI.toFloat() / 180f
                drawCircle(
                    dust, radius = 1.6f * dp,
                    center = Offset(g.cx + cos(ang) * dist, surfaceY - abs(sin(ang)) * dist - 2f * dp)
                )
            }
        }
    }

    // metal yansıma şeritleri ve çapraz parlama
    drawRoundRect(
        brush = c.stripBrush, topLeft = Offset(g.x0 + 3f * dp, g.bodyTop + 8f * dp),
        size = Size(5f * dp, bodyH - 22f * dp), cornerRadius = CornerRadius(2.5f * dp, 2.5f * dp)
    )
    drawRoundRect(
        color = Color.White.copy(alpha = 0.07f),
        topLeft = Offset(g.x1 - 6f * dp, g.bodyTop + 12f * dp),
        size = Size(2.5f * dp, bodyH - 30f * dp), cornerRadius = CornerRadius(1.25f * dp, 1.25f * dp)
    )
    clipPath(c.bodyClip) { drawPath(c.shine, brush = c.shineBrush) }
    // iç kenar çizgisi + dış metal kenar
    drawRoundRect(
        color = Color.White.copy(alpha = 0.10f),
        topLeft = Offset(g.x0 + 2f * dp, g.bodyTop + 2f * dp),
        size = Size(safeW - 4f * dp, bodyH - 4f * dp),
        cornerRadius = CornerRadius(corner - 2f * dp, corner - 2f * dp), style = Stroke(width = 1f * dp)
    )
    drawRoundRect(
        color = if (done) CoinMidD.copy(alpha = 0.9f) else MetalEdgeD.copy(alpha = 0.55f),
        topLeft = Offset(g.x0, g.bodyTop), size = Size(safeW, bodyH),
        cornerRadius = CornerRadius(corner, corner), style = Stroke(width = 1f * dp)
    )

    // kapak
    drawRoundRect(
        brush = c.lidBrush, topLeft = Offset(g.x0 - 4f * dp, g.lidTop),
        size = Size(safeW + 8f * dp, g.lidH), cornerRadius = CornerRadius(7f * dp, 7f * dp)
    )
    drawRoundRect(
        color = Color.White.copy(alpha = 0.22f),
        topLeft = Offset(g.x0 - 4f * dp, g.lidTop), size = Size(safeW + 8f * dp, g.lidH),
        cornerRadius = CornerRadius(7f * dp, 7f * dp), style = Stroke(width = 1f * dp)
    )
    drawLine(
        Color(0xFF05080F).copy(alpha = 0.75f),
        Offset(g.x0 - 2f * dp, g.lidTop + g.lidH), Offset(g.x1 + 2f * dp, g.lidTop + g.lidH),
        strokeWidth = 1.5f * dp
    )
    // yuva
    val slotW = 64f * dp
    val slotH = 6f * dp
    val slotTop = g.lidTop + (g.lidH - slotH) / 2f
    val slotCorner = CornerRadius(slotH / 2f, slotH / 2f)
    if (done) {
        drawRoundRect(
            color = YeterMint.copy(alpha = 0.28f * glowA),
            topLeft = Offset(g.cx - slotW / 2f - 3f * dp, slotTop - 3f * dp),
            size = Size(slotW + 6f * dp, slotH + 6f * dp), cornerRadius = CornerRadius(slotH, slotH)
        )
    }
    drawRoundRect(
        color = Color(0xFF05080F), topLeft = Offset(g.cx - slotW / 2f, slotTop),
        size = Size(slotW, slotH), cornerRadius = slotCorner
    )
    drawRoundRect(
        color = if (done) YeterMint.copy(alpha = 0.5f + 0.5f * glowA) else CoinMidD.copy(alpha = 0.55f),
        topLeft = Offset(g.cx - slotW / 2f, slotTop), size = Size(slotW, slotH),
        cornerRadius = slotCorner, style = Stroke(width = 1f * dp)
    )
}

@Composable
fun CounterD(state: YeterUiState, modifier: Modifier = Modifier) {
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

    // Kasa 60.000'lik kapasiteye göre dolar (hedeften bağımsız); taban yalnızca para varken çizgi görünsün diye %1
    val fillTarget = if (displayed <= 0.0) 0f else (displayed / Savings.MAX_GOAL).toFloat().coerceIn(0.01f, 1f)
    val fillState: State<Float> = animateFloatAsState(fillTarget, tween(350), label = "fillD")

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
        shownText.length <= 8 -> 40.sp
        shownText.length <= 11 -> 34.sp
        else -> 28.sp
    }
    // Gradyan sınırsız: metin boyutuna kendiliğinden uyar, bu yüzden tek sefer üretilir.
    val amountBrush = remember { Brush.verticalGradient(listOf(Color(0xFFFFF4C2), YeterAmber)) }

    // İniş titreşimi (görsel katman): ilk değeri atla
    val haptic = LocalHapticFeedback.current
    val hapticArmed = remember { booleanArrayOf(false) }
    LaunchedEffect(displayed) {
        if (hapticArmed[0]) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        else hapticArmed[0] = true
    }

    // Kilometre taşı (%50/%75/%100): yukarı geçilince tek seferlik parlama. Açılışta mevcut seviye sessizce kabul edilir.
    val ratio = if (state.targetAmount > 0.0) displayed / state.targetAmount else 0.0
    val level = when { ratio >= 1.0 -> 3; ratio >= 0.75 -> 2; ratio >= 0.5 -> 1; else -> 0 }
    val burst = remember { Animatable(1f) }
    val lastLevel = remember { intArrayOf(level) }
    var burstLevel by remember { mutableIntStateOf(0) }
    LaunchedEffect(level) {
        if (level > lastLevel[0]) {
            burstLevel = level
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            burst.snapTo(0f)
            burst.animateTo(1f, tween(3200, easing = FastOutSlowInEasing))
        }
        lastLevel[0] = level
    }

    val desc = "Kasada " + String.format(locale, "%,d", displayed.toLong().coerceAtLeast(0L)) +
        " " + symbol + ", hedefin yüzde " + percent + "'i" +
        (if (done) ". Hedefe ulaştın" else "")

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clearAndSetSemantics { contentDescription = desc },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .heightIn(min = 110.dp)
                .drawWithCache {
                    // Yalnızca boyuta bağlı hazırlık; State okuması yok
                    val c = buildSafeCacheD(size.width, size.height, density)
                    onDrawBehind {
                        val dp = c.dp
                        val g = c.g
                        val fill = fillState.value
                        val glowA = glowState?.value ?: 0f
                        drawSafeD(c, fill, done, glowA, tick.value)

                        // düşen paralar: gölgeli, inişte yığına "girer"
                        val surfaceY = g.innerBottom - fill.coerceIn(0f, 1f) * (g.innerBottom - g.innerTop)
                        val d = 22f * dp
                        val startY = g.lidTop + 1f * dp
                        val landY = surfaceY + d * 0.1f
                        for (i in 0 until MaxInFlightD) {
                            val p = slots[i].value
                            if (p <= 0f || p >= 1f) continue
                            val sx = abs(sin(p * 3.5f * Math.PI.toFloat())).coerceAtLeast(0.2f)
                            val sy = 1f - 0.55f * p * p * p
                            val x = g.cx + landX[i] * dp * p
                            val y = startY + (landY - startY).coerceAtLeast(0f) * p
                            drawOval(
                                color = Color.Black.copy(alpha = 0.22f),
                                topLeft = Offset(x - d * sx * 0.45f + 2f * dp, y + d * 0.3f),
                                size = Size(d * sx * 0.9f, d * 0.22f)
                            )
                            drawCoinD(x, y, d, sx, sy, CoinLightD, CoinMidD, CoinDarkD)
                        }

                        val b = burst.value
                        if (b < 1f && burstLevel > 0) {
                            val col = if (burstLevel == 3) YeterMint else SparkD
                            val a = 1f - b
                            val cy = g.bodyTop + (g.bodyBottom - g.bodyTop) * 0.5f
                            val w = g.x1 - g.x0
                            drawCircle(
                                brush = c.glowBrush, radius = w * (0.6f + 0.5f * b),
                                center = Offset(g.cx, cy), alpha = (0.55f + 0.15f * burstLevel) * a
                            )
                            drawCircle(
                                color = col.copy(alpha = 0.8f * a), radius = w * (0.3f + 0.7f * b),
                                center = Offset(g.cx, cy), style = Stroke(width = (5f - 3f * b) * dp)
                            )
                            val n = 4 + 3 * burstLevel
                            for (i in 0 until n) {
                                val ang = (i.toFloat() / n) * 2f * PI.toFloat() + 0.4f
                                val r = w * (0.35f + 0.6f * b)
                                drawSparkleD(
                                    Offset(g.cx + cos(ang) * r, cy + sin(ang) * r * 0.8f),
                                    (6f + 5f * a) * dp, col.copy(alpha = a)
                                )
                            }
                        }

                        if (done) {
                            val sp = SparkD.copy(alpha = glowA)
                            drawSparkleD(Offset(g.x0 - 10f * dp, g.bodyTop + 24f * dp), 9f * dp, sp)
                            drawSparkleD(Offset(g.x1 + 10f * dp, g.bodyTop + 60f * dp), 7f * dp, sp)
                            drawSparkleD(Offset(g.cx + 70f * dp, g.lidTop - 14f * dp), 10f * dp, sp)
                        }
                    }
                }
        )

        // Tutar + yukarı süzülen "+2,25" etiketi
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier
                    .padding(top = 6.dp)
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
                        translationY = -p * 24.dp.toPx() + 12.dp.toPx()
                        alpha = when {
                            p >= 1f -> 0f
                            p < 0.15f -> p / 0.15f
                            else -> 1f - (p - 0.15f) / 0.85f
                        }
                    }
            )
        }

        // Tek sönük satır; dar ekranda kesilmesin
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (coinValue > 0.0) {
                Text(
                    text = "Her altın = $symbol" + fmtCoinD(coinValue, locale),
                    fontSize = 12.sp,
                    color = YeterTextSecondary.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
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
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            } else {
                Text(
                    text = "Hedef %$percent",
                    fontSize = 12.sp,
                    color = YeterTextSecondary.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Spacer(Modifier.height(2.dp))
    }
}

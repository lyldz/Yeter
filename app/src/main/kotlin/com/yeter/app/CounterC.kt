package com.yeter.app

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.roundToLong

// Konsept C: Premium bakiye kartı.
// Koyu lacivert-nane metalik yüzey, üzerinde yavaş kayan ince ışık yansıması, köşede Y rozeti.
// Tutar yalnızca kuruş değişince dikey kayma/solma ile yenilenir; zaman bilgisi yok.
@Composable
fun CounterC(state: YeterUiState) {
    val done = state.progressFraction >= 1f
    val locale = Locale.getDefault()
    val accent = if (done) YeterAmber else YeterMint
    val shape = RoundedCornerShape(24.dp)

    // Canlı değer: ticker en fazla 250 ms'de bir; yalnızca kuruş değişince yeniden bileşim olur.
    val baseTime = remember(state.totalSaved, state.todayEarned, state.dailySaving) { System.currentTimeMillis() }
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            nowMs = System.currentTimeMillis()
            delay(250L)
        }
    }
    val totalCents by remember(state.totalSaved, state.dailySaving, baseTime) {
        derivedStateOf {
            val elapsed = (nowMs - baseTime).coerceIn(0L, 2000L)
            ((state.totalSaved + state.dailySaving * elapsed / 86_400_000.0) * 100.0)
                .roundToLong().coerceAtLeast(0L)
        }
    }
    val todayCents by remember(state.todayEarned, state.dailySaving, baseTime) {
        derivedStateOf {
            val elapsed = (nowMs - baseTime).coerceIn(0L, 2000L)
            ((state.todayEarned + state.dailySaving * elapsed / 86_400_000.0) * 100.0)
                .roundToLong().coerceAtLeast(0L)
        }
    }

    val symbol = state.symbol.trim()
    val sep = DecimalFormatSymbols.getInstance(locale).decimalSeparator

    // Shimmer ve kenar parıltısı: değerler yalnızca çizim aşamasında okunur (yeniden bileşim yok).
    val infinite = rememberInfiniteTransition(label = "cardFx")
    val shimmer = infinite.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(5200, easing = LinearEasing)),
        label = "shimmerC"
    )
    val glow = infinite.animateFloat(
        initialValue = 0.45f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "glowC"
    )
    val animProgress by animateFloatAsState(
        targetValue = state.progressFraction.coerceIn(0f, 1f),
        animationSpec = tween(900, easing = FastOutSlowInEasing),
        label = "progressC"
    )

    val percent = (state.progressFraction * 100).toInt().coerceAtLeast(0)
    val staticInt = String.format(locale, "%,d", state.totalSaved.toLong().coerceAtLeast(0L))
    val a11y = "Birikim: $staticInt $symbol, hedefin yüzde $percent kadarı"

    val borderBrush = if (done) listOf(YeterAmber, YeterLime, YeterAmber)
    else listOf(YeterMint.copy(alpha = 0.55f), Color(0x33FFFFFF), YeterMint.copy(alpha = 0.25f))

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (done) 14.dp else 8.dp, shape = shape,
                ambientColor = accent, spotColor = accent
            )
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF0C1B33), Color(0xFF123A4A), YeterCardHigh, Color(0xFF0E2F3A)),
                    start = Offset(0f, 0f), end = Offset(900f, 700f)
                )
            )
            .drawWithContent {
                drawContent()
                // Kayan ince ışık bandı (metalik yansıma)
                val w = size.width
                val h = size.height
                val x = -w * 0.5f + (w * 2f) * shimmer.value
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.09f), Color.Transparent),
                        start = Offset(x, 0f),
                        end = Offset(x + w * 0.45f, h * 0.45f)
                    )
                )
                // Kenar çizgisi; hedef tamamlanınca altın/lime ve nabız
                val a = if (done) glow.value else 1f
                val sw = (if (done) 2.5.dp else 1.dp).toPx()
                drawRoundRect(
                    brush = Brush.linearGradient(
                        borderBrush.map { it.copy(alpha = (it.alpha * a).coerceIn(0f, 1f)) },
                        start = Offset(0f, 0f), end = Offset(w, h)
                    ),
                    topLeft = Offset(sw / 2, sw / 2),
                    size = androidx.compose.ui.geometry.Size(w - sw, h - sw),
                    cornerRadius = CornerRadius(24.dp.toPx() - sw / 2),
                    style = Stroke(width = sw)
                )
            }
            .drawBehind {
                // Sağ üstte yumuşak nane ışıma
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(accent.copy(alpha = 0.16f), Color.Transparent),
                        center = Offset(size.width * 0.85f, 0f), radius = size.width * 0.6f
                    ),
                    radius = size.width * 0.6f,
                    center = Offset(size.width * 0.85f, 0f)
                )
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 208.dp)
                .clearAndSetSemantics { contentDescription = a11y }
                .padding(horizontal = 22.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Üst satır: etiket + Y rozeti
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Birikimin",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFFB9C7DC)
                )
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                if (done) listOf(YeterAmber, YeterLime) else listOf(YeterMint, Color(0xFF7FE9CF))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Y",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF0B1220)
                    )
                }
            }

            // Bakiye: kuruş değişince dikey kayma + solma
            AnimatedContent(
                targetState = totalCents,
                transitionSpec = {
                    (slideInVertically(tween(320, easing = FastOutSlowInEasing)) { it / 3 } + fadeIn(tween(320)))
                        .togetherWith(
                            slideOutVertically(tween(260, easing = FastOutSlowInEasing)) { -it / 3 } + fadeOut(tween(200))
                        )
                },
                label = "balanceC"
            ) { cents ->
                val intText = String.format(locale, "%,d", cents / 100)
                val centText = String.format(locale, "%02d", (cents % 100).toInt())
                val intSize = when {
                    intText.length <= 5 -> 48.sp
                    intText.length <= 7 -> 40.sp
                    else -> 32.sp
                }
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = symbol,
                        fontSize = (intSize.value * 0.55f).sp,
                        fontWeight = FontWeight.Bold,
                        color = accent,
                        modifier = Modifier.padding(end = 4.dp, bottom = 5.dp)
                    )
                    Text(
                        text = intText,
                        style = TabularNums,
                        fontSize = intSize,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFF2F7FF),
                        maxLines = 1, softWrap = false
                    )
                    Text(
                        text = "$sep$centText",
                        style = TabularNums,
                        fontSize = (intSize.value * 0.5f).sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFB9C7DC),
                        maxLines = 1, softWrap = false,
                        modifier = Modifier.padding(start = 2.dp, bottom = 5.dp)
                    )
                }
            }

            // Alt blok: bugün + hedef ilerlemesi
            Column {
                val todayText = "Bugün +" + symbol + String.format(
                    locale, "%,d", todayCents / 100
                ) + sep + String.format(locale, "%02d", (todayCents % 100).toInt())
                Text(
                    text = todayText,
                    style = TabularNums,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = accent,
                    maxLines = 1
                )
                Spacer(Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth().height(5.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color(0x33FFFFFF))
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
                Spacer(Modifier.height(8.dp))
                Text(
                    text = if (done) "Hedefe ulaştın · ${state.targetName}" else "%$percent · ${state.targetName}",
                    fontSize = 13.sp,
                    fontWeight = if (done) FontWeight.Bold else FontWeight.Medium,
                    color = if (done) YeterLime else Color(0xFFB9C7DC),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

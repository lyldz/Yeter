package com.yeter.app

import android.content.Context
import android.content.Intent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.Composable
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Box
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import java.util.Locale

// Ortak widget parçaları: renkler, çerçeve, yazı stilleri, para biçimi, tıklama eylemi.
object WColor {
    val bg = ColorProvider(YeterCard)
    val text = ColorProvider(YeterText)
    val muted = ColorProvider(YeterTextSecondary)
    val mint = ColorProvider(YeterMint)
    val gold = ColorProvider(YeterAmber)
    val coral = ColorProvider(YeterCoral)
    val track = ColorProvider(YeterOutlineSoft)
}

fun wText(size: Int, color: ColorProvider = WColor.text, bold: Boolean = false) =
    TextStyle(color = color, fontSize = size.sp, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal)

fun wMoney(symbol: String, v: Double, decimals: Int = 2): String =
    symbol + String.format(Locale.getDefault(), "%,.${decimals}f", v)

// Dokununca uygulamayı açar; openUrge=true ise doğrudan DUR akışını başlatır.
fun wOpenApp(ctx: Context, openUrge: Boolean = false) =
    actionStartActivity(Intent(ctx, MainActivity::class.java).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        if (openUrge) putExtra(EXTRA_OPEN_URGE, true)
    })

const val EXTRA_OPEN_URGE = "open_urge"

@Composable
fun WFrame(ctx: Context, openUrge: Boolean = false, bg: ColorProvider = WColor.bg, content: @Composable () -> Unit) {
    Box(
        modifier = GlanceModifier.fillMaxSize().background(bg).cornerRadius(22.dp).padding(12.dp)
            .clickable(wOpenApp(ctx, openUrge))
    ) { content() }
}

// Uygulama içinden bir değişiklik olunca (dürtü, hedef, ayar) çağrılır.
suspend fun updateAllYeterWidgets(ctx: Context) {
    val all: List<GlanceAppWidget> = listOf(SavingsWidget(), GoalWidget(), TodayWidget(), UrgeWidget(), DaysLeftWidget(), ResistWidget(), ShowcaseWidget(), VaultWidget())
    all.forEach { runCatching { it.updateAll(ctx) } }
}

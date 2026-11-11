package com.yeter.app

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.text.Text
 import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height

// 2) Hedef (4x2): birikim, hedef adı + yüzde, ilerleme çubuğu, kalan gün
class GoalWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val d = WidgetDataLoader.load(context)
        provideContent {
            WFrame(context) {
                Column(modifier = GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                    if (!d.onboarded) {
                        Text("Yeter'i aç", style = wText(14, WColor.muted), maxLines = 1)
                    } else {
                        Text("Birikim", style = wText(11, WColor.muted), maxLines = 1)
                        Text(wMoney(d.symbol, d.saved), style = wText(24, WColor.mint, bold = true), maxLines = 1)
                        if (d.goalPrice <= 0) {
                            Text("Hedef ekle", style = wText(13, WColor.muted), maxLines = 1)
                        } else {
                            Spacer(GlanceModifier.height(4.dp))
                            Text("${d.goalName} · %${(d.progress.coerceIn(0f, 1f) * 100).toInt()}", style = wText(13), maxLines = 1)
                            Spacer(GlanceModifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = d.progress.coerceIn(0f, 1f),
                                modifier = GlanceModifier.fillMaxWidth(),
                                color = WColor.mint,
                                backgroundColor = WColor.track
                            )
                            Spacer(GlanceModifier.height(4.dp))
                            Text(
                                if (d.progress >= 1f) "Hedefe ulaştın" else "${d.daysLeft} gün kaldı · ${d.reachDate}",
                                style = wText(11, WColor.muted), maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

class GoalWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = GoalWidget()
}

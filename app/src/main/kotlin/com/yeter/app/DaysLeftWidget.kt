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

// 5) Hedefe kalan gün (2x2)
class DaysLeftWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val d = WidgetDataLoader.load(context)
        provideContent {
            WFrame(context) {
                Column(modifier = GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                    when {
                        !d.onboarded -> Text("Yeter'i aç", style = wText(14, WColor.muted), maxLines = 1)
                        d.goalPrice <= 0 -> Text("Hedef ekle", style = wText(16, WColor.muted, bold = true), maxLines = 1)
                        d.progress >= 1f -> {
                            Text("Hedefe ulaştın", style = wText(16, WColor.gold, bold = true), maxLines = 1)
                            Text(d.goalName, style = wText(11, WColor.muted), maxLines = 1)
                        }
                        else -> {
                            Text("Hedefe kalan", style = wText(11, WColor.muted), maxLines = 1)
                            Text("${d.daysLeft} gün", style = wText(28, WColor.mint, bold = true), maxLines = 1)
                            Text(d.goalName, style = wText(12), maxLines = 1)
                            Text(d.reachDate, style = wText(10, WColor.muted), maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}

class DaysLeftWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = DaysLeftWidget()
}

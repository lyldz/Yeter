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

// 3) Bugün (2x2): bugün biriken tutar ve günlük hedef
class TodayWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val d = WidgetDataLoader.load(context)
        provideContent {
            WFrame(context) {
                Column(modifier = GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                    if (!d.onboarded) {
                        Text("Yeter'i aç", style = wText(14, WColor.muted), maxLines = 1)
                    } else {
                        Text("Bugün", style = wText(12, WColor.muted), maxLines = 1)
                        Text("+" + wMoney(d.symbol, d.today), style = wText(24, WColor.mint, bold = true), maxLines = 1)
                        Text("Günlük " + wMoney(d.symbol, d.daily, 0), style = wText(11, WColor.muted), maxLines = 1)
                    }
                }
            }
        }
    }
}

class TodayWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TodayWidget()
}

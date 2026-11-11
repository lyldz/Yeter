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

// 1) Birikim (2x1): sadece tutar
class SavingsWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val d = WidgetDataLoader.load(context)
        provideContent {
            WFrame(context) {
                Column(modifier = GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                    androidx.glance.text.Text("Birikim", style = wText(12, WColor.muted))
                    androidx.glance.text.Text(wMoney(d.symbol, d.saved), style = wText(22, WColor.mint, bold = true), maxLines = 1)
                }
            }
        }
    }
}

class SavingsWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SavingsWidget()
}

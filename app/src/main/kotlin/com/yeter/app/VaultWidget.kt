package com.yeter.app

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.size
import androidx.glance.text.Text
import androidx.glance.unit.ColorProvider

// 8) Kasa (2x2): altın para görünümü
class VaultWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val d = WidgetDataLoader.load(context)
        provideContent {
            WFrame(context) {
                Column(modifier = GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically, horizontalAlignment = Alignment.CenterHorizontally) {
                    if (!d.onboarded) {
                        Text("Yeter'i aç", style = wText(14, WColor.muted), maxLines = 1)
                    } else {
                        Box(
                            modifier = GlanceModifier.size(52.dp).background(WColor.gold).cornerRadius(26.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(d.symbol.trim(), style = wText(24, ColorProvider(Color(0xFF3A2A00)), bold = true), maxLines = 1)
                        }
                        Text(wMoney(d.symbol, d.saved, 0), style = wText(16, WColor.mint, bold = true), maxLines = 1)
                        Text("Kasada", style = wText(11, WColor.muted), maxLines = 1)
                    }
                }
            }
        }
    }
}

class VaultWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = VaultWidget()
}

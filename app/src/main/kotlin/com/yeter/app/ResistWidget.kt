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

// 6) Direnç (2x2): direnilen dürtü sayısı
class ResistWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val d = WidgetDataLoader.load(context)
        provideContent {
            WFrame(context) {
                Column(modifier = GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                    when {
                        !d.onboarded -> Text("Yeter'i aç", style = wText(14, WColor.muted), maxLines = 1)
                        d.resistedCount <= 0 -> {
                            Text("Direnç", style = wText(12, WColor.muted), maxLines = 1)
                            Text("İlk dürtüne dur de", style = wText(16, WColor.text, bold = true), maxLines = 3)
                        }
                        else -> {
                            Text("Direnç", style = wText(12, WColor.muted), maxLines = 1)
                            Text("${d.resistedCount}", style = wText(36, WColor.text, bold = true), maxLines = 1)
                            Text("dürtüye dur dedin", style = wText(11, WColor.muted), maxLines = 1)
                            Text(wMoney(d.symbol, d.resistedTotal, 0), style = wText(15, WColor.mint, bold = true), maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}

class ResistWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ResistWidget()
}

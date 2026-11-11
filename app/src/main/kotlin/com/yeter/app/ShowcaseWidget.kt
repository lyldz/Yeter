package com.yeter.app

import android.content.Context
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.text.Text

// 7) Vitrin (4x3): ilk 3 hedef ve ilerlemeleri
class ShowcaseWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val d = WidgetDataLoader.load(context)
        provideContent {
            WFrame(context) {
                if (!d.onboarded) {
                    Column(modifier = GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically, horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Yeter'i aç", style = wText(14, WColor.muted), maxLines = 1)
                    }
                } else {
                    Column(modifier = GlanceModifier.fillMaxSize()) {
                        Text("Vitrin", style = wText(13, WColor.muted, bold = true), maxLines = 1)
                        Spacer(GlanceModifier.height(6.dp))
                        if (d.goals.isEmpty()) {
                            Column(modifier = GlanceModifier.fillMaxWidth().defaultWeight(), verticalAlignment = Alignment.CenterVertically, horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Vitrinine hedef ekle", style = wText(14, WColor.muted), maxLines = 1)
                            }
                        } else {
                            d.goals.take(3).forEachIndexed { i, g ->
                                if (i > 0) Spacer(GlanceModifier.height(8.dp))
                                val p = g.progress.coerceIn(0f, 1f)
                                Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    Text(g.name, style = wText(13), maxLines = 1, modifier = GlanceModifier.defaultWeight())
                                    Text("%${(p * 100).toInt()}", style = wText(13, WColor.mint, bold = true), maxLines = 1)
                                }
                                Spacer(GlanceModifier.height(3.dp))
                                LinearProgressIndicator(
                                    progress = p,
                                    modifier = GlanceModifier.fillMaxWidth().height(5.dp),
                                    color = WColor.mint,
                                    backgroundColor = WColor.track
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

class ShowcaseWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ShowcaseWidget()
}

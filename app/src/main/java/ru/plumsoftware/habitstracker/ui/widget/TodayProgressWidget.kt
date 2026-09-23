package ru.plumsoftware.habitstracker.ui.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle

/** Виджет «Прогресс дня» (2×2) — кольцо выполнения за сегодня. */
class TodayProgressWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Single

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = WidgetDataLoader.load()
        val density = context.resources.displayMetrics.density
        val ring = buildProgressRing((80 * density).toInt(), data.completionRate, 8 * density)
        provideContent {
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .cornerRadius(28.dp)
                    .background(WidgetColors.accent)
                    .padding(16.dp)
                    .clickable(deeplinkAction(context, Deeplinks.TODAY)),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Сегодня", style = capStyle(), modifier = GlanceModifier.defaultWeight())
                    Text(data.dateShort, style = capStyle())
                }
                Spacer(GlanceModifier.defaultWeight())
                Box(contentAlignment = Alignment.Center, modifier = GlanceModifier.size(80.dp)) {
                    Image(provider = ImageProvider(ring), contentDescription = null, modifier = GlanceModifier.size(80.dp))
                    Text(
                        "${(data.completionRate * 100).toInt()}%",
                        style = TextStyle(color = WidgetColors.onAccent, fontSize = 18.sp, fontWeight = FontWeight.Bold),
                    )
                }
                Spacer(GlanceModifier.defaultWeight())
                Text(
                    text = when {
                        data.total == 0 -> "Нет привычек на сегодня"
                        data.completed >= data.total -> "Идеальный день 🎉"
                        else -> "${data.completed} из ${data.total} выполнено"
                    },
                    style = TextStyle(color = WidgetColors.onAccent, fontSize = 13.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
                    modifier = GlanceModifier.fillMaxWidth(),
                )
            }
        }
    }

    @Composable
    private fun capStyle() = TextStyle(color = WidgetColors.onAccentMuted, fontSize = 11.5.sp, fontWeight = FontWeight.Medium)
}

class TodayProgressWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TodayProgressWidget()
}

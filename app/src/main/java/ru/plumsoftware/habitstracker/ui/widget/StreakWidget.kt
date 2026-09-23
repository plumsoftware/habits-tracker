package ru.plumsoftware.habitstracker.ui.widget

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
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
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider

/** Виджет «Серия» (2×2) — дни подряд + текущая неделя. */
class StreakWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Single

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = WidgetDataLoader.load()
        provideContent {
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .cornerRadius(28.dp)
                    .background(WidgetColors.ink)
                    .padding(16.dp)
                    .clickable(deeplinkAction(context, Deeplinks.ANALYTICS)),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🔥", style = TextStyle(fontSize = 13.sp))
                    Spacer(GlanceModifier.width(6.dp))
                    Text("Серия", style = TextStyle(color = WidgetColors.onInk, fontSize = 11.5.sp, fontWeight = FontWeight.Medium))
                }
                Spacer(GlanceModifier.defaultWeight())
                Text(
                    "${data.streak}",
                    style = TextStyle(color = WidgetColors.onInk, fontSize = 44.sp, fontWeight = FontWeight.Bold),
                )
                Text(
                    if (data.streak == 0) "Начните серию сегодня" else "${daysWord(data.streak)} подряд",
                    style = TextStyle(color = ColorProvider(Color(0xFFFFB84D)), fontSize = 12.sp, fontWeight = FontWeight.Medium),
                )
                Spacer(GlanceModifier.defaultWeight())
                Row(modifier = GlanceModifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    data.week.forEachIndexed { i, day ->
                        if (i > 0) Spacer(GlanceModifier.defaultWeight())
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = GlanceModifier.size(15.dp).cornerRadius(5.dp).background(squareColor(day.state)),
                            ) {}
                            Spacer(GlanceModifier.height(4.dp))
                            Text(day.label, style = TextStyle(color = ColorProvider(Color(0x73FFFFFF)), fontSize = 9.sp, fontWeight = FontWeight.Medium))
                        }
                    }
                }
            }
        }
    }

    private fun squareColor(state: DaySquare): ColorProvider = when (state) {
        DaySquare.DONE -> WidgetColors.accent
        DaySquare.PARTIAL -> WidgetColors.heat2
        DaySquare.FROZEN -> ColorProvider(Color(0x66D6EEFF))
        DaySquare.EMPTY -> ColorProvider(Color(0x24FFFFFF))
    }
}

class StreakWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = StreakWidget()
}

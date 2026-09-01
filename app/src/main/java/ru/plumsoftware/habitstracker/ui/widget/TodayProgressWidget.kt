package ru.plumsoftware.habitstracker.ui.widget

import android.content.Context
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
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
import ru.plumsoftware.habitstracker.MainActivity

class TodayProgressWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = WidgetDataLoader.load()
        provideContent { Content(data) }
    }

    @androidx.compose.runtime.Composable
    private fun Content(data: WidgetData) {
        val percent = (data.completionRate * 100).toInt()
        val ring = buildProgressRing(sizePx = 320, rate = data.completionRate, strokePx = 34f)

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .clickable(actionStartActivity<MainActivity>())
        ) {
            // Фон-градиент
            Image(
                provider = ImageProvider(ru.plumsoftware.habitstracker.R.drawable.widget_today_bg),
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = GlanceModifier.fillMaxSize()
            )

            Column(
                modifier = GlanceModifier.fillMaxSize().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Шапка
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Сегодня",
                        style = TextStyle(
                            color = ColorProvider(WidgetColors.onAccentMuted),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        modifier = GlanceModifier.defaultWeight()
                    )
                    Text(
                        text = "${data.completed} из ${data.total}",
                        style = TextStyle(
                            color = ColorProvider(WidgetColors.onAccent),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                Spacer(modifier = GlanceModifier.defaultWeight())

                // Кольцо прогресса с текстом по центру
                Box(
                    modifier = GlanceModifier.size(128.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        provider = ImageProvider(ring),
                        contentDescription = null,
                        modifier = GlanceModifier.size(128.dp)
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$percent%",
                            style = TextStyle(
                                color = ColorProvider(WidgetColors.onAccent),
                                fontSize = 30.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = "выполнено",
                            style = TextStyle(
                                color = ColorProvider(WidgetColors.onAccentMuted),
                                fontSize = 12.sp
                            )
                        )
                    }
                }

                Spacer(modifier = GlanceModifier.defaultWeight())

                // Пилюля с серией
                Row(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .background(ImageProvider(ru.plumsoftware.habitstracker.R.drawable.widget_pill_bg))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🔥",
                        style = TextStyle(fontSize = 14.sp)
                    )
                    Spacer(modifier = GlanceModifier.width(6.dp))
                    Text(
                        text = "Серия ${data.streak} ${daysWord(data.streak)}",
                        style = TextStyle(
                            color = ColorProvider(WidgetColors.onAccent),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }
        }
    }
}

class TodayProgressWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TodayProgressWidget()
}

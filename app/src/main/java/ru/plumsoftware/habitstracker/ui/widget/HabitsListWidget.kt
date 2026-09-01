package ru.plumsoftware.habitstracker.ui.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.ImageProvider
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
import androidx.glance.text.TextDecoration
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import ru.plumsoftware.habitstracker.MainActivity
import ru.plumsoftware.habitstracker.R

class HabitsListWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = WidgetDataLoader.load()
        provideContent { Content(data) }
    }

    @Composable
    private fun Content(data: WidgetData) {
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ImageProvider(R.drawable.widget_list_bg))
                .padding(horizontal = 14.dp, vertical = 12.dp)
                .clickable(actionStartActivity<MainActivity>())
        ) {
            Column(modifier = GlanceModifier.fillMaxSize()) {
                // Шапка
                Row(
                    modifier = GlanceModifier.fillMaxWidth().padding(bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CheckSquare(checked = true, decorative = true)
                    Spacer(modifier = GlanceModifier.width(10.dp))
                    Text(
                        text = "Привычки",
                        style = TextStyle(
                            color = ColorProvider(WidgetColors.onSurface),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = GlanceModifier.defaultWeight()
                    )
                    Text(
                        text = data.dateLabel,
                        style = TextStyle(
                            color = ColorProvider(WidgetColors.accent),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }

                if (data.habits.isEmpty()) {
                    Box(
                        modifier = GlanceModifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Нет привычек на сегодня",
                            style = TextStyle(
                                color = ColorProvider(WidgetColors.onSurfaceMuted),
                                fontSize = 14.sp
                            )
                        )
                    }
                } else {
                    LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                        items(data.habits, itemId = { it.id.hashCode().toLong() }) { habit ->
                            HabitRow(habit)
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun HabitRow(habit: WidgetHabit) {
        Column {
            Row(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .padding(vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = GlanceModifier.clickable(
                        actionRunCallback<ToggleHabitAction>(
                            actionParametersOf(
                                ToggleHabitAction.habitIdKey to habit.id,
                                ToggleHabitAction.completedKey to !habit.completed
                            )
                        )
                    )
                ) {
                    CheckSquare(checked = habit.completed, decorative = false)
                }
                Spacer(modifier = GlanceModifier.width(10.dp))
                Text(
                    text = habit.title,
                    maxLines = 1,
                    style = TextStyle(
                        color = ColorProvider(
                            if (habit.completed) WidgetColors.onSurfaceMuted else WidgetColors.onSurface
                        ),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        textDecoration = if (habit.completed) TextDecoration.LineThrough else TextDecoration.None
                    ),
                    modifier = GlanceModifier.defaultWeight()
                )
                Text(
                    text = if (habit.completed) "готово" else habit.time,
                    style = TextStyle(
                        color = ColorProvider(WidgetColors.onSurfaceMuted),
                        fontSize = 13.sp
                    )
                )
            }
            // Разделитель
            Box(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(ColorProvider(WidgetColors.divider))
            ) {}
        }
    }

    @Composable
    private fun CheckSquare(checked: Boolean, decorative: Boolean) {
        Box(
            modifier = GlanceModifier
                .size(28.dp)
                .background(
                    ImageProvider(
                        if (checked) R.drawable.widget_checkbox_on else R.drawable.widget_checkbox_off
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            if (checked) {
                Text(
                    text = "✓",
                    style = TextStyle(
                        color = ColorProvider(WidgetColors.onAccent),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }
}

class ToggleHabitAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val habitId = parameters[habitIdKey] ?: return
        val completed = parameters[completedKey] ?: return
        WidgetDataLoader.toggle(habitId, completed)
        // Обновляем оба виджета
        HabitsListWidget().updateAll(context)
        TodayProgressWidget().updateAll(context)
    }

    companion object {
        val habitIdKey = ActionParameters.Key<String>("habitId")
        val completedKey = ActionParameters.Key<Boolean>("completed")
    }
}

class HabitsListWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = HabitsListWidget()
}

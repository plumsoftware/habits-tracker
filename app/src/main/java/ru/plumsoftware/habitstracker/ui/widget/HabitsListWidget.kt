package ru.plumsoftware.habitstracker.ui.widget

import android.content.Context
import androidx.compose.runtime.Composable
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
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
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
import androidx.glance.text.TextDecoration
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider

/** Виджет «Привычки на сегодня» (4×3) — список с отметкой прямо с рабочего стола. */
class HabitsListWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = WidgetDataLoader.load()
        provideContent { Content(context, data) }
    }

    @Composable
    private fun Content(context: Context, data: WidgetData) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .cornerRadius(28.dp)
                .background(WidgetColors.surface)
                .padding(16.dp),
        ) {
            // Хедер
            Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = GlanceModifier.defaultWeight()) {
                    Text("Привычки на сегодня", style = TextStyle(color = WidgetColors.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold))
                    Text(
                        "${data.completed}/${data.total} · ${data.weekdayFull}",
                        style = TextStyle(color = WidgetColors.textSecondary, fontSize = 11.5.sp, fontWeight = FontWeight.Medium),
                    )
                }
                Box(
                    modifier = GlanceModifier.size(34.dp).cornerRadius(12.dp).background(WidgetColors.accent)
                        .clickable(deeplinkAction(context, Deeplinks.NEW_HABIT)),
                    contentAlignment = Alignment.Center,
                ) { Text("+", style = TextStyle(color = WidgetColors.onAccent, fontSize = 20.sp, fontWeight = FontWeight.Bold)) }
            }
            Spacer(GlanceModifier.height(10.dp))

            if (data.habits.isEmpty()) {
                EmptyState(context)
            } else {
                if (data.completed >= data.total && data.total > 0) {
                    Box(
                        modifier = GlanceModifier.fillMaxWidth().cornerRadius(14.dp).background(WidgetColors.successSoft).padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center,
                    ) { Text("Идеальный день 🎉", style = TextStyle(color = WidgetColors.success, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)) }
                    Spacer(GlanceModifier.height(8.dp))
                }
                LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                    items(data.habits, itemId = { it.id.hashCode().toLong() }) { habit ->
                        HabitRow(context, habit)
                    }
                }
            }
        }
    }

    @Composable
    private fun HabitRow(context: Context, habit: WidgetHabit) {
        val catColor = Color(habit.categoryArgb)
        Row(
            modifier = GlanceModifier.fillMaxWidth().padding(vertical = 5.dp)
                .clickable(deeplinkAction(context, Deeplinks.habit(habit.id))),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = GlanceModifier.size(36.dp).cornerRadius(12.dp).background(ColorProvider(catColor.copy(alpha = 0.16f))),
                contentAlignment = Alignment.Center,
            ) { Text(habit.emoji, style = TextStyle(fontSize = 17.sp)) }
            Spacer(GlanceModifier.width(11.dp))
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    habit.title,
                    maxLines = 1,
                    style = TextStyle(
                        color = if (habit.completed) WidgetColors.textDisabled else WidgetColors.textPrimary,
                        fontSize = 13.sp, fontWeight = FontWeight.Bold,
                        textDecoration = if (habit.completed) TextDecoration.LineThrough else TextDecoration.None,
                    ),
                )
                Text(
                    habit.chainAfterTitle?.let { "после «$it»" } ?: habit.time,
                    maxLines = 1,
                    style = TextStyle(color = WidgetColors.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.Medium),
                )
            }
            Spacer(GlanceModifier.width(8.dp))
            // Чекбокс / прогресс цели
            if (habit.goal > 1 && !habit.completed) {
                Box(
                    modifier = GlanceModifier.size(30.dp).cornerRadius(15.dp).background(WidgetColors.accentSoft)
                        .clickable(toggleAction(habit.id, true)),
                    contentAlignment = Alignment.Center,
                ) { Text("${habit.count}/${habit.goal}", style = TextStyle(color = WidgetColors.accent, fontSize = 10.sp, fontWeight = FontWeight.Bold)) }
            } else {
                Box(
                    modifier = GlanceModifier.size(30.dp).cornerRadius(15.dp)
                        .background(if (habit.completed) WidgetColors.accent else WidgetColors.surfaceMuted)
                        .clickable(toggleAction(habit.id, !habit.completed)),
                    contentAlignment = Alignment.Center,
                ) { if (habit.completed) Text("✓", style = TextStyle(color = WidgetColors.onAccent, fontSize = 15.sp, fontWeight = FontWeight.Bold)) }
            }
        }
    }

    @Composable
    private fun EmptyState(context: Context) {
        Column(
            modifier = GlanceModifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("🌱", style = TextStyle(fontSize = 32.sp))
            Spacer(GlanceModifier.height(8.dp))
            Text("Здесь появятся ваши привычки", style = TextStyle(color = WidgetColors.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold))
            Spacer(GlanceModifier.height(10.dp))
            Box(
                modifier = GlanceModifier.cornerRadius(999.dp).background(WidgetColors.accentSoft).padding(horizontal = 14.dp, vertical = 8.dp)
                    .clickable(deeplinkAction(context, Deeplinks.TEMPLATES)),
            ) { Text("Выбрать шаблон", style = TextStyle(color = WidgetColors.accent, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)) }
        }
    }
}

class HabitsListWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = HabitsListWidget()
}

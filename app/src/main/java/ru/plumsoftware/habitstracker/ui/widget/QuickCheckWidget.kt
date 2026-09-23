package ru.plumsoftware.habitstracker.ui.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider

/** Ключ настройки виджета: id выбранных привычек (CSV). */
object QuickCheckState {
    val HABIT_IDS = stringPreferencesKey("quick_habit_ids")
}

/** Виджет «Быстрая отметка» (4×1) — до 5 привычек в одну строку. */
class QuickCheckWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = WidgetDataLoader.load()
        provideContent {
            val prefs = currentState<Preferences>()
            val selectedIds = prefs[QuickCheckState.HABIT_IDS]
                ?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
            val chosen = if (selectedIds.isEmpty()) {
                data.habits.take(5)
            } else {
                selectedIds.mapNotNull { sid -> data.habits.firstOrNull { it.id == sid } }.take(5)
            }
            Row(
                modifier = GlanceModifier.fillMaxSize().cornerRadius(28.dp).background(WidgetColors.surface)
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (chosen.isEmpty()) {
                    Text("Нет привычек", style = TextStyle(color = WidgetColors.textSecondary, fontSize = 13.sp))
                } else {
                    chosen.forEachIndexed { i, h ->
                        if (i > 0) Spacer(GlanceModifier.defaultWeight())
                        QuickButton(h)
                    }
                }
            }
        }
    }

    @Composable
    private fun QuickButton(habit: WidgetHabit) {
        val catColor = Color(habit.categoryArgb)
        Box(
            modifier = GlanceModifier.size(54.dp).cornerRadius(19.dp)
                .background(if (habit.completed) WidgetColors.accentSoft else ColorProvider(catColor.copy(alpha = 0.16f)))
                .clickable(toggleAction(habit.id, !habit.completed)),
            contentAlignment = Alignment.Center,
        ) {
            androidx.glance.layout.Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(habit.emoji, style = TextStyle(fontSize = 22.sp))
                if (habit.completed) {
                    Text("✓", style = TextStyle(color = WidgetColors.accent, fontSize = 10.sp, fontWeight = FontWeight.Bold))
                } else if (habit.goal > 1) {
                    Text("${(habit.goal - habit.count).coerceAtLeast(0)}", style = TextStyle(color = WidgetColors.accent, fontSize = 10.sp, fontWeight = FontWeight.Bold))
                }
            }
        }
    }
}

class QuickCheckWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = QuickCheckWidget()
}

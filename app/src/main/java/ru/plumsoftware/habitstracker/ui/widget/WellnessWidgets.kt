package ru.plumsoftware.habitstracker.ui.widget

import android.content.Context
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import org.koin.core.context.GlobalContext
import ru.plumsoftware.habitstracker.R
import ru.plumsoftware.habitstracker.data.database.WellnessActivity
import ru.plumsoftware.habitstracker.data.database.WellnessRepository
import ru.plumsoftware.habitstracker.data.service.StepCounterManager
import java.text.NumberFormat
import java.util.Calendar

internal fun wellnessWidget(activity: WellnessActivity): GlanceAppWidget = when (activity) {
    WellnessActivity.WORKOUTS -> WorkoutWidget()
    WellnessActivity.STEPS -> StepsWidget()
    WellnessActivity.SLEEP -> SleepWidget()
}

class WorkoutWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = wellnessWidget(WellnessActivity.WORKOUTS)
}

class StepsWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = wellnessWidget(WellnessActivity.STEPS)
}

class SleepWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = wellnessWidget(WellnessActivity.SLEEP)
}

private data class WidgetReading(val value: String, val detail: String)

private class WorkoutWidget : WellnessWidget(WellnessActivity.WORKOUTS)
private class StepsWidget : WellnessWidget(WellnessActivity.STEPS)
private class SleepWidget : WellnessWidget(WellnessActivity.SLEEP)

private abstract class WellnessWidget(private val activity: WellnessActivity) : GlanceAppWidget() {
    @OptIn(ExperimentalCoroutinesApi::class)
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repository: WellnessRepository = GlobalContext.get().get()
        val readings = WidgetUpdater.wellnessUpdates(activity).flatMapLatest {
            flow {
                while (true) {
                    val now = System.currentTimeMillis()
                    val day = Calendar.getInstance().apply {
                        timeInMillis = now
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    val start = day.timeInMillis
                    day.add(Calendar.DAY_OF_YEAR, 1)
                    val end = day.timeInMillis
                    emit(read(context, repository, start, end))
                    delay((end - System.currentTimeMillis()).coerceAtLeast(1L))
                }
            }
        }.distinctUntilChanged()
        val initial = readings.first()
        val (title, icon, link) = when (activity) {
            WellnessActivity.WORKOUTS -> Triple(context.getString(R.string.widget_title_workout), R.drawable.equipment_dumbbell_large, Deeplinks.WORKOUTS)
            WellnessActivity.STEPS -> Triple(context.getString(R.string.widget_title_steps), R.drawable.step_sneaker, Deeplinks.STEPS)
            WellnessActivity.SLEEP -> Triple(context.getString(R.string.widget_title_sleep), R.drawable.sleep_crescent, Deeplinks.SLEEP)
        }
        val background = when (activity) {
            WellnessActivity.WORKOUTS -> WidgetColors.accentSoft
            WellnessActivity.STEPS -> WidgetColors.infoSoft
            WellnessActivity.SLEEP -> WidgetColors.surfaceMuted
        }

        provideContent {
            val reading by readings.collectAsState(initial)
            Column(
                modifier = GlanceModifier.fillMaxSize().cornerRadius(24.dp)
                    .background(background).padding(10.dp).clickable(deeplinkAction(context, link)),
            ) {
                Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Image(ImageProvider(icon), contentDescription = null, modifier = GlanceModifier.size(26.dp))
                    Text(title, modifier = GlanceModifier.padding(start = 4.dp), maxLines = 1,
                        style = TextStyle(color = WidgetColors.textPrimary, fontSize = 11.sp, fontWeight = FontWeight.Medium))
                }
                Spacer(GlanceModifier.defaultWeight())
                Text(reading.value, maxLines = 1,
                    style = TextStyle(color = WidgetColors.textPrimary, fontSize = 19.sp, fontWeight = FontWeight.Bold))
                Text(reading.detail, modifier = GlanceModifier.padding(top = 2.dp), maxLines = 1,
                    style = TextStyle(color = WidgetColors.textSecondary, fontSize = 11.sp))
            }
        }
    }

    private suspend fun read(context: Context, repository: WellnessRepository, start: Long, end: Long): WidgetReading =
        when (activity) {
            WellnessActivity.WORKOUTS -> {
                val workouts = repository.workouts().first()
                val active = workouts.firstOrNull { it.endedAtMillis == null }
                if (active != null) {
                    WidgetReading("Идёт", active.title)
                } else {
                    val completed = workouts.filter { it.endedAtMillis != null && it.startedAtMillis in start until end }
                    val minutes = completed.sumOf { ((it.endedAtMillis ?: it.startedAtMillis) - it.startedAtMillis).coerceAtLeast(0) } / 60_000
                    WidgetReading(formatMinutes(minutes), "Сегодня: ${completed.size}")
                }
            }
            WellnessActivity.STEPS -> {
                val steps = repository.stepsForDay(start).first()?.steps ?: 0
                val goal = StepCounterManager.dailyGoal(context)
                val format = NumberFormat.getIntegerInstance()
                WidgetReading(format.format(steps), "Цель ${format.format(goal)}")
            }
            WellnessActivity.SLEEP -> {
                val sleep = repository.sleepForDay(start).first()
                if (sleep == null) WidgetReading("–", "Нет записи")
                else WidgetReading(formatMinutes(sleep.durationMinutes.toLong()), "Сон за сегодня")
            }
        }

    private fun formatMinutes(minutes: Long): String =
        if (minutes < 60) "$minutes мин" else "${minutes / 60} ч ${minutes % 60} м"
}

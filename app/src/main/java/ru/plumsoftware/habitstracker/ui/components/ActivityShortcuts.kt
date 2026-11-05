package ru.plumsoftware.habitstracker.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.koin.compose.koinInject
import ru.plumsoftware.habitstracker.R
import ru.plumsoftware.habitstracker.data.database.WellnessRepository
import ru.plumsoftware.habitstracker.ui.theme.AppTheme
import java.util.Calendar

@Composable
fun ActivityShortcuts(day: Long, onSteps: () -> Unit, onWorkouts: () -> Unit, onSleep: () -> Unit,
    filledIconSize: Dp = 28.dp, emptyIconFraction: Float = 0.44f) {
    val repository: WellnessRepository = koinInject()
    val stepsFlow = remember(repository, day) { repository.stepsForDay(day) }
    val steps by stepsFlow.collectAsState(initial = null)
    val sessions by remember(repository) { repository.workouts() }.collectAsState(initial = emptyList())
    val start = remember(day) { Calendar.getInstance().apply {
        timeInMillis = day
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis }
    val end = remember(start) { Calendar.getInstance().apply { timeInMillis = start; add(Calendar.DAY_OF_YEAR, 1) }.timeInMillis }
    val completed = sessions.filter { it.endedAtMillis != null && it.startedAtMillis in start until end }
    val sleep by remember(repository, day) { repository.sleepForDay(day) }.collectAsState(initial = null)
    val sleepMinutes = sleep?.takeIf { it.dayStartMillis == start }?.durationMinutes
    val recordedSteps = steps?.takeIf { it.dayStartMillis == start }
    val minutes = completed.sumOf { ((it.endedAtMillis!! - it.startedAtMillis).coerceAtLeast(0) / 60_000) }
    val colors = AppTheme.colors
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        val entries = listOf(Triple("Шагомер", R.drawable.step_sneaker, onSteps),
            Triple("Тренировки", R.drawable.equipment_dumbbell_large, onWorkouts),
            Triple("Сон", R.drawable.sleep_crescent, onSleep))
        entries.forEachIndexed { index, (title, icon, click) ->
            val hasData = when (index) {
                0 -> recordedSteps != null
                1 -> completed.isNotEmpty()
                else -> sleepMinutes != null
            }
            BoxWithConstraints(Modifier.weight(1f).aspectRatio(1f)
                .clip(RoundedCornerShape(22.dp)).background(colors.surface)
                .clickableNoRipple(onClick = click)) {
                val cardWidth = maxWidth
                val iconSize by animateDpAsState(if (hasData) filledIconSize.coerceAtMost(cardWidth * 0.44f) else maxWidth * emptyIconFraction,
                    animationSpec = tween(240), label = "activityIconSize")
                val iconX by animateDpAsState(if (hasData) 10.dp else (maxWidth - maxWidth * emptyIconFraction) / 2,
                    animationSpec = tween(240), label = "activityIconX")
                val iconY by animateDpAsState(if (hasData) 8.dp else maxHeight * 0.08f,
                    animationSpec = tween(240), label = "activityIconY")
                val iconModifier = Modifier.offset(x = iconX, y = iconY).size(iconSize)
                Image(painterResource(icon), null, iconModifier)
                if (hasData) {
                    Column(Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(when (index) {
                            0 -> recordedSteps?.steps?.toString() ?: "–"
                            1 -> "$minutes мин"
                            else -> sleepMinutes?.let { if (it < 60) "$it мин" else "${it / 60} ч ${it % 60} м" } ?: "–"
                        },
                            color = colors.textPrimary, style = AppTheme.type.h3,
                            fontSize = if (cardWidth < 110.dp) 18.sp else 22.sp, maxLines = 1)
                        Text(if (index == 0) "Шаги" else title, color = colors.textSecondary,
                            style = AppTheme.type.captionS, maxLines = 1)
                    }
                } else {
                    Text(title, Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 12.dp),
                        color = colors.textPrimary, style = AppTheme.type.label,
                        fontSize = if (cardWidth < 110.dp) 11.sp else 13.sp,
                        textAlign = TextAlign.Center, maxLines = 1)
                }
            }
        }
    }
}

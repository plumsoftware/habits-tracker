package ru.plumsoftware.habitstracker.ui.screen.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.plumsoftware.habitstracker.data.model.Category
import ru.plumsoftware.habitstracker.data.model.Habit
import ru.plumsoftware.habitstracker.data.model.Periodicity
import ru.plumsoftware.habitstracker.ui.components.AppIcon
import ru.plumsoftware.habitstracker.ui.components.HabitIcons
import ru.plumsoftware.habitstracker.ui.components.PrimaryButton
import ru.plumsoftware.habitstracker.ui.components.clickableNoRipple
import ru.plumsoftware.habitstracker.ui.screen.main.home.HabitDetail
import ru.plumsoftware.habitstracker.ui.theme.AppTheme
import ru.plumsoftware.habitstracker.ui.theme.softShadow

/**
 * Экран «Детали привычки» (ТЗ §8): тёмная карточка стрика, тепловая карта 18 недель,
 * график по дням недели, кнопка отметки.
 */
@Composable
fun HabitDetailScreen(
    habit: Habit,
    detail: HabitDetail?,
    category: Category?,
    chainParentTitle: String? = null,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onArchive: () -> Unit,
    onToggleToday: (Boolean) -> Unit,
) {
    val colors = AppTheme.colors
    val spacing = AppTheme.spacing
    val d = detail ?: HabitDetail(habitId = habit.id)
    val catColor = category?.let { Color(it.colorArgb) } ?: colors.accent

    Column(
        modifier = Modifier.fillMaxSize().background(colors.bgScreen).windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        // §8.1 Хедер
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = spacing.screenH, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(40.dp).softShadow(AppTheme.elevation.subtle, RoundedCornerShape(14.dp))
                    .clip(RoundedCornerShape(14.dp)).background(colors.surface).clickableNoRipple(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) { AppIcon(HabitIcons.chevronLeft, tint = colors.textPrimary, size = 24.dp) }
            Spacer(Modifier.weight(1f))
            Box(
                modifier = Modifier.size(40.dp).softShadow(AppTheme.elevation.subtle, RoundedCornerShape(14.dp))
                    .clip(RoundedCornerShape(14.dp)).background(colors.surface).clickableNoRipple(onClick = onArchive),
                contentAlignment = Alignment.Center,
            ) { AppIcon(HabitIcons.repeat, tint = colors.textPrimary, size = 17.dp) }
            Spacer(Modifier.width(8.dp))
            Row(
                modifier = Modifier.softShadow(AppTheme.elevation.subtle, RoundedCornerShape(14.dp))
                    .clip(RoundedCornerShape(14.dp)).background(colors.surface).clickableNoRipple(onClick = onEdit)
                    .padding(horizontal = 15.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                AppIcon(HabitIcons.tune, tint = colors.textPrimary, size = 15.dp)
                Text("Настроить", style = AppTheme.type.label, color = colors.textPrimary)
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(start = spacing.screenH, end = spacing.screenH, top = 4.dp, bottom = spacing.screenBottomPlain),
            verticalArrangement = Arrangement.spacedBy(spacing.sectionGap),
        ) {
            item { StreakCard(habit = habit, category = category, detail = d) }
            item { HeatmapCard(detail = d) }
            item { WeekdayChart(detail = d) }
            item {
                val done = d.doneToday
                PrimaryButton(
                    text = if (done) "Отмечено сегодня" else "Отметить выполнение",
                    onClick = { onToggleToday(!done) },
                    background = if (done) colors.successSoft else colors.accent,
                    contentColor = if (done) colors.success else colors.inkOn,
                    shadow = if (done) ru.plumsoftware.habitstracker.ui.theme.ShadowSpec.None else AppTheme.elevation.accentButton,
                    leading = if (done) {
                        { AppIcon(HabitIcons.check, tint = colors.success, size = 15.dp); Spacer(Modifier.width(8.dp)) }
                    } else null,
                )
            }
            if (chainParentTitle != null) {
                item {
                    Text(
                        "В цепочке: после «${chainParentTitle.trim()}» → ${habit.title.trim()}",
                        style = AppTheme.type.caption, color = colors.textSecondary,
                    )
                }
            }
        }
    }
}

// §8.2 Тёмная карточка стрика

@Composable
private fun StreakCard(habit: Habit, category: Category?, detail: HabitDetail) {
    val colors = AppTheme.colors
    val white = colors.inkOn
    Column(
        modifier = Modifier.fillMaxWidth()
            .softShadow(AppTheme.elevation.inkCard, AppTheme.shapes.cardXL)
            .clip(AppTheme.shapes.cardXL).background(colors.ink).padding(22.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(
                modifier = Modifier.size(56.dp).clip(RoundedCornerShape(20.dp)).background(Color.White.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) { Text(habit.emoji.ifBlank { "🎯" }, fontSize = 27.sp) }
            Column {
                Text(habit.title.trim(), style = AppTheme.type.h3.copy(fontSize = 18.sp), color = white)
                Text(scheduleSummary(habit, category), style = AppTheme.type.caption, color = white.copy(alpha = 0.6f))
            }
        }
        Spacer(Modifier.height(20.dp))
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("${detail.currentStreak}", style = AppTheme.type.numXL, color = white)
            Column(Modifier.padding(bottom = 8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    AppIcon(HabitIcons.flame, tint = Color(0xFFFFB84D), size = 14.dp)
                    Text("дней подряд", style = AppTheme.type.label, color = Color(0xFFFFB84D))
                }
                Text("Лучшая серия: ${detail.bestStreak}", style = AppTheme.type.caption, color = white.copy(alpha = 0.55f))
            }
        }
        Spacer(Modifier.height(18.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            StatTile(Modifier.weight(1f), "${detail.totalChecks}", "всего отметок")
            StatTile(Modifier.weight(1f), "${detail.successRate}%", "успешность")
            StatTile(Modifier.weight(1f), "${detail.missedDays}", "пропуска")
        }
    }
}

@Composable
private fun StatTile(modifier: Modifier, value: String, label: String) {
    val white = AppTheme.colors.inkOn
    Column(
        modifier = modifier.clip(RoundedCornerShape(18.dp)).background(Color.White.copy(alpha = 0.10f)).padding(12.dp),
    ) {
        Text(value, style = AppTheme.type.h5.copy(fontSize = 17.sp), color = white)
        Text(label, style = AppTheme.type.captionS, color = white.copy(alpha = 0.6f))
    }
}

// §8.3 Тепловая карта

@Composable
private fun HeatmapCard(detail: HabitDetail) {
    val colors = AppTheme.colors
    // 126 дней oldest→newest; 0=пусто, 1=выполнено, 2=заморожено
    val today = detail.heatmap.keys.maxOrNull() ?: System.currentTimeMillis()
    val dayMs = 24 * 60 * 60 * 1000L
    val cells = (125 downTo 0).map { i ->
        val d = today - i * dayMs
        when {
            (detail.heatmap[d] ?: 0) > 0 -> 1
            detail.frozenDays.contains(d) -> 2
            else -> 0
        }
    }
    val rate = if (cells.isNotEmpty()) cells.count { it == 1 } * 100 / cells.size else 0

    Column(
        modifier = Modifier.fillMaxWidth().softShadow(AppTheme.elevation.cardRaised, AppTheme.shapes.cardL)
            .clip(AppTheme.shapes.cardL).background(colors.surface).padding(18.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Последние 18 недель", style = AppTheme.type.h5, color = colors.textPrimary)
            Text("$rate% выполнено", style = AppTheme.type.captionS, color = colors.textSecondary)
        }
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            for (col in 0 until 18) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (row in 0 until 7) {
                        val v = cells.getOrElse(col * 7 + row) { 0 }
                        Box(
                            modifier = Modifier.size(11.dp).clip(AppTheme.shapes.heatCell)
                                .background(
                                    when (v) {
                                        1 -> colors.heat3
                                        2 -> colors.info // заморожено
                                        else -> colors.heat0
                                    }
                                )
                        )
                    }
                }
            }
        }
        if (detail.frozenDays.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.size(11.dp).clip(AppTheme.shapes.heatCell).background(colors.info))
                Text("❄ Заморозка сохранила серию (${detail.frozenDays.size})", style = AppTheme.type.captionS, color = colors.textSecondary)
            }
        }
    }
}

// §8.4 График по дням недели

@Composable
private fun WeekdayChart(detail: HabitDetail) {
    val colors = AppTheme.colors
    val counts = detail.weekdayCounts
    val max = (counts.maxOrNull() ?: 1).coerceAtLeast(1)
    val labels = listOf("пн", "вт", "ср", "чт", "пт", "сб", "вс")
    val maxIndex = counts.indexOf(counts.maxOrNull() ?: 0)

    Column(
        modifier = Modifier.fillMaxWidth().softShadow(AppTheme.elevation.cardRaised, AppTheme.shapes.cardL)
            .clip(AppTheme.shapes.cardL).background(colors.surface).padding(18.dp),
    ) {
        Text("Отметки по дням недели", style = AppTheme.type.h5, color = colors.textPrimary)
        Spacer(Modifier.height(16.dp))
        var appeared by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) { appeared = true }
        Row(Modifier.fillMaxWidth().height(130.dp), horizontalArrangement = Arrangement.spacedBy(9.dp), verticalAlignment = Alignment.Bottom) {
            counts.forEachIndexed { i, c ->
                val target = c.toFloat() / max
                val fraction by androidx.compose.animation.core.animateFloatAsState(
                    if (appeared) target else 0f,
                    animationSpec = androidx.compose.animation.core.tween(AppTheme.motion.slow, delayMillis = i * 40),
                    label = "wd",
                )
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                    Box(
                        modifier = Modifier.fillMaxWidth()
                            .height((fraction * 100).dp.coerceAtLeast(4.dp))
                            .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp, bottomStart = 8.dp, bottomEnd = 8.dp))
                            .background(if (i == maxIndex && c > 0) colors.ink else colors.accent)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(labels[i], style = AppTheme.type.captionS, color = colors.textSecondary)
                }
            }
        }
    }
}

// ---------- Хелперы ----------

private val DOW_SHORT = mapOf(2 to "Пн", 3 to "Вт", 4 to "Ср", 5 to "Чт", 6 to "Пт", 7 to "Сб", 1 to "Вс")

private fun scheduleSummary(habit: Habit, category: Category?): String {
    val parts = mutableListOf<String>()
    category?.name?.let { parts.add(it) }
    parts.add(
        when {
            habit.isByWeekdays -> {
                val order = listOf(2, 3, 4, 5, 6, 7, 1)
                habit.weekdaySet.sortedBy { order.indexOf(it) }.joinToString(", ") { DOW_SHORT[it] ?: "" }
            }
            habit.periodicity == Periodicity.WEEKLY -> "${habit.targetCount} раз в неделю"
            else -> "каждый день"
        }
    )
    if (habit.reminderTimeHour >= 0) parts.add("%02d:%02d".format(habit.reminderTimeHour, habit.reminderTimeMinute))
    return parts.joinToString(" · ")
}

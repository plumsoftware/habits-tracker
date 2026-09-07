package ru.plumsoftware.habitstracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ru.plumsoftware.habitstracker.ui.screen.main.home.HabitCorrelation
import ru.plumsoftware.habitstracker.ui.theme.Dimen
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// ─────────────────────────────────────────── Достижения ───────────────────────────────────────────

private data class Achievement(
    val threshold: Int,
    val icon: ImageVector,
    val accent: Color
)

private val achievements = listOf(
    Achievement(7, Icons.Filled.LocalFireDepartment, Color(0xFFF5714E)),
    Achievement(30, Icons.Filled.EmojiEvents, Color(0xFFF5A623)),
    Achievement(100, Icons.Filled.Diamond, Color(0xFF06B6D4))
)

/** Карточка «Достижения»: 7 / 30 / 100 дней подряд. Прогресс — по общей серии дней активности. */
@Composable
fun AchievementsCard(
    overallStreak: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimen.cardPadding),
            verticalArrangement = Arrangement.spacedBy(Dimen.large)
        ) {
            Text(
                text = "Достижения",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Dimen.medium)
            ) {
                achievements.forEach { achievement ->
                    AchievementItem(
                        achievement = achievement,
                        overallStreak = overallStreak,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun AchievementItem(
    achievement: Achievement,
    overallStreak: Int,
    modifier: Modifier = Modifier
) {
    val unlocked = overallStreak >= achievement.threshold
    val remaining = (achievement.threshold - overallStreak).coerceAtLeast(0)

    Column(
        modifier = modifier
            .background(
                color = if (unlocked) achievement.accent.copy(alpha = 0.10f)
                else MaterialTheme.colorScheme.surfaceContainerHighest,
                shape = MaterialTheme.shapes.medium
            )
            .border(
                width = if (unlocked) 1.5.dp else 0.dp,
                color = if (unlocked) achievement.accent else Color.Transparent,
                shape = MaterialTheme.shapes.medium
            )
            .padding(vertical = Dimen.large, horizontal = Dimen.smallPlus),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Dimen.smallPlus)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(
                    color = if (unlocked) Color.White else MaterialTheme.colorScheme.surfaceContainerLowest,
                    shape = MaterialTheme.shapes.medium
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = achievement.icon,
                contentDescription = null,
                tint = if (unlocked) achievement.accent
                else MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(24.dp)
            )
        }

        Text(
            text = "${achievement.threshold} дней",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = if (unlocked) "Открыто" else "Ещё $remaining дн.",
            style = MaterialTheme.typography.labelMedium,
            color = if (unlocked) achievement.accent
            else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

// ─────────────────────────────────────── Тепловая карта ───────────────────────────────────────

/**
 * Карточка «Карта активности» в стиле GitHub: последние 20 недель.
 * Клик по ячейке показывает диалог с числом выполненных привычек за день.
 */
@Composable
fun ActivityHeatmapCard(
    heatmap: Map<Long, Int>,
    endDate: Long,
    firstDayOfWeek: Int,
    modifier: Modifier = Modifier
) {
    val weeks = 20
    val dayMs = 24 * 60 * 60 * 1000L

    // Начало сетки: выравниваем на начало недели, отступаем на 19 недель назад
    val gridStart = remember(endDate, firstDayOfWeek) {
        val cal = Calendar.getInstance().apply {
            this.firstDayOfWeek = firstDayOfWeek
            timeInMillis = startOfDay(endDate)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val delta = (cal.get(Calendar.DAY_OF_WEEK) - firstDayOfWeek + 7) % 7
        cal.add(Calendar.DAY_OF_MONTH, -delta)
        cal.add(Calendar.DAY_OF_MONTH, -(weeks - 1) * 7)
        cal.timeInMillis
    }

    val maxCount = remember(heatmap) { heatmap.values.maxOrNull() ?: 0 }

    var selectedCell by remember { mutableStateOf<Pair<Long, Int>?>(null) }

    selectedCell?.let { (dayStart, count) ->
        AlertDialog(
            onDismissRequest = { selectedCell = null },
            confirmButton = {
                TextButton(onClick = { selectedCell = null }) { Text("Ок") }
            },
            title = { Text(formatFullDate(dayStart)) },
            text = {
                Text("Завершено ${count} ${habitsWord(count)} в этот день")
            }
        )
    }

    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimen.cardPadding),
            verticalArrangement = Arrangement.spacedBy(Dimen.large)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(Dimen.small)) {
                Text(
                    text = "Карта активности",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Последние $weeks недель",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Сетка: 20 столбцов (недель), в каждом 7 ячеек (дней недели)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                for (week in 0 until weeks) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        for (day in 0 until 7) {
                            val dayStart = gridStart + (week * 7 + day) * dayMs
                            val count = heatmap[dayStart] ?: 0
                            val isFuture = dayStart > startOfDay(endDate)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                                    .background(
                                        color = heatColor(count, maxCount, isFuture),
                                        shape = RoundedCornerShape(3.dp)
                                    )
                                    .clickable(enabled = !isFuture) {
                                        selectedCell = dayStart to count
                                    }
                            )
                        }
                    }
                }
            }

            // Легенда «меньше … больше»
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "меньше",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.padding(horizontal = Dimen.smallPlus),
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    listOf(0f, 0.33f, 0.66f, 1f).forEach { level ->
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(
                                    color = heatColorByLevel(level),
                                    shape = RoundedCornerShape(3.dp)
                                )
                        )
                    }
                }
                Text(
                    text = "больше",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun heatColor(count: Int, maxCount: Int, isFuture: Boolean): Color {
    if (isFuture) return MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.4f)
    val level = if (maxCount <= 0 || count == 0) 0f else count.toFloat() / maxCount
    return heatColorByLevel(level)
}

@Composable
private fun heatColorByLevel(level: Float): Color {
    val base = MaterialTheme.colorScheme.primary
    val empty = MaterialTheme.colorScheme.surfaceContainerHighest
    return when {
        level <= 0f -> empty
        level <= 0.33f -> base.copy(alpha = 0.3f)
        level <= 0.66f -> base.copy(alpha = 0.6f)
        else -> base
    }
}

// ─────────────────────────────────── Связи между привычками ───────────────────────────────────

/**
 * Карточка «Связи между привычками». До недели пользования показывает заглушку,
 * затем — прогресс-бары корреляций между привычками.
 */
@Composable
fun HabitCorrelationsCard(
    correlations: List<HabitCorrelation>,
    unlocked: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimen.cardPadding),
            verticalArrangement = Arrangement.spacedBy(Dimen.large)
        ) {
            Text(
                text = "Связи между привычками",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            when {
                !unlocked -> {
                    Text(
                        text = "Здесь появится анализ связей между вашими привычками после недели использования приложения.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                correlations.isEmpty() -> {
                    Text(
                        text = "Пока недостаточно данных, чтобы найти закономерности. Отмечайте привычки регулярнее.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                else -> {
                    correlations.forEach { correlation ->
                        CorrelationRow(correlation)
                    }
                    val top = correlations.first()
                    Text(
                        text = "В дни, когда вы выполняете «${top.fromTitle}», вы почти всегда выполняете «${top.toTitle}». Начните с неё — остальное подтянется само.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun CorrelationRow(correlation: HabitCorrelation) {
    Column(verticalArrangement = Arrangement.spacedBy(Dimen.smallPlus)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${correlation.fromTitle} → ${correlation.toTitle}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "${(correlation.rate * 100).toInt()}%",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        LinearProgressIndicator(
            progress = correlation.rate.coerceIn(0f, 1f),
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.primaryContainer
        )
    }
}

// ─────────────────────────────────────────── Утилиты ───────────────────────────────────────────

private fun startOfDay(timestamp: Long): Long {
    return Calendar.getInstance().apply {
        timeInMillis = timestamp
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}

private fun formatFullDate(timestamp: Long): String {
    return SimpleDateFormat("d MMMM yyyy", Locale.getDefault()).format(Date(timestamp))
}

private fun habitsWord(n: Int): String {
    val mod100 = n % 100
    val mod10 = n % 10
    return when {
        mod100 in 11..14 -> "привычек"
        mod10 == 1 -> "привычку"
        mod10 in 2..4 -> "привычки"
        else -> "привычек"
    }
}

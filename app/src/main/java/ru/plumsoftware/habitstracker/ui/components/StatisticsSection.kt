package ru.plumsoftware.habitstracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ru.plumsoftware.habitstracker.ui.screen.main.home.DailyStats
import ru.plumsoftware.habitstracker.ui.screen.main.home.HomeState
import ru.plumsoftware.habitstracker.ui.screen.main.home.MonthlyStats
import ru.plumsoftware.habitstracker.ui.theme.Dimen
import ru.plumsoftware.habitstracker.ui.theme.LocalExtendedColorScheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun StatisticsSection(
    state: HomeState,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(Dimen.medium),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Карточка месячной статистики
        item {
            MonthlyStatsCard(
                monthlyStats = state.monthlyStats,
                selectedMonth = state.selectedMonth,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Карточка дневной статистики
        item {
            DailyStatsCard(
                dailyStats = state.dailyStats,
                selectedDate = state.selectedDate,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun MonthlyStatsCard(
    monthlyStats: MonthlyStats,
    selectedMonth: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimen.medium),
            verticalArrangement = Arrangement.spacedBy(Dimen.medium)
        ) {
            Text(
                text = "Статистика за месяц",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            // Основные показатели
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatItem(
                    title = "Всего привычек",
                    value = monthlyStats.totalHabits.toString(),
                    modifier = Modifier.weight(1f)
                )
                StatItem(
                    title = "Выполнено",
                    value = monthlyStats.completedHabits.toString(),
                    modifier = Modifier.weight(1f)
                )
                StatItem(
                    title = "Успешность",
                    value = "${(monthlyStats.completionRate * 100).toInt()}%",
                    modifier = Modifier.weight(1f)
                )
            }

            // Прогресс бар как в дневной статистике
            MonthlyProgressBar(
                completionRate = monthlyStats.completionRate,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
            )
        }
    }
}

@Composable
fun MonthlyProgressBar(
    completionRate: Float,
    modifier: Modifier = Modifier
) {
    LinearProgressIndicator(
        progress = completionRate,
        modifier = modifier,
        color = MaterialTheme.colorScheme.primary,
        trackColor = MaterialTheme.colorScheme.primaryContainer
    )
}

@Composable
fun DailyStatsCard(
    dailyStats: DailyStats,
    selectedDate: Long,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimen.medium),
            verticalArrangement = Arrangement.spacedBy(Dimen.medium)
        ) {
            Text(
                text = "Статистика за ${formatDate(selectedDate)}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatItem(
                    title = "Всего на сегодня",
                    value = dailyStats.totalHabits.toString(),
                    modifier = Modifier.weight(1f)
                )
                StatItem(
                    title = "Выполнено",
                    value = "${dailyStats.completedHabits}/${dailyStats.totalHabits}",
                    modifier = Modifier.weight(1f)
                )
                StatItem(
                    title = "Прогресс",
                    value = "${(dailyStats.completionRate * 100).toInt()}%",
                    modifier = Modifier.weight(1f)
                )
            }

            // Прогресс бар
            LinearProgressIndicator(
                progress = dailyStats.completionRate,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primaryContainer
            )
        }
    }
}

@Composable
fun MonthlyHistogram(
    dailyCompletions: Map<Int, Int>,
    modifier: Modifier = Modifier
) {
    val maxValue = dailyCompletions.values.maxOrNull() ?: 1

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "Активность по дням",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            dailyCompletions.entries.sortedBy { it.key }.forEach { (day, completions) ->
                val heightRatio = if (maxValue > 0) completions.toFloat() / maxValue else 0f

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom
                ) {
                    Text(
                        text = day.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height((heightRatio * 60).dp)
                            .background(
                                color = MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(2.dp)
                            )
                    )

                    Text(
                        text = completions.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun StatItem(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

// Вспомогательная функция для форматирования даты
private fun formatDate(timestamp: Long): String {
    val dateFormat = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
    return dateFormat.format(Date(timestamp))
}
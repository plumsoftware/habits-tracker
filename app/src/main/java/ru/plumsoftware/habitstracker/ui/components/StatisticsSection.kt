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
import java.util.Calendar
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
    val motivationMessage = getMonthlyMotivationMessage(monthlyStats.completionRate)

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

            // Показатели статистики - по два в ряд
            Column(
                verticalArrangement = Arrangement.spacedBy(Dimen.medium)
            ) {
                // Первая строка: Всего привычек и Выполнено
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
                }

                // Вторая строка: Не выполнено и Успешность
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatItem(
                        title = "Не выполнено",
                        value = monthlyStats.uncompletedHabits.toString(),
                        modifier = Modifier.weight(1f)
                    )
                    StatItem(
                        title = "Успешность",
                        value = "${(monthlyStats.completionRate * 100).toInt()}%",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Прогресс бар
            MonthlyProgressBar(
                completionRate = monthlyStats.completionRate,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
            )

            // Мотивирующее сообщение
            Text(
                text = motivationMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun DailyStatsCard(
    dailyStats: DailyStats,
    selectedDate: Long,
    modifier: Modifier = Modifier
) {
    val motivationMessage = getDailyMotivationMessage(dailyStats.completionRate)
    val isToday = isToday(selectedDate)

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

            // Показатели статистики - по два в ряд
            Column(
                verticalArrangement = Arrangement.spacedBy(Dimen.medium)
            ) {
                // Первая строка: Всего на сегодня и Выполнено
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
                        value = dailyStats.completedHabits.toString(),
                        modifier = Modifier.weight(1f)
                    )
                }

                // Вторая строка: Не выполнено и Прогресс
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatItem(
                        title = "Не выполнено",
                        value = dailyStats.uncompletedHabits.toString(),
                        modifier = Modifier.weight(1f)
                    )
                    StatItem(
                        title = "Прогресс",
                        value = "${(dailyStats.completionRate * 100).toInt()}%",
                        modifier = Modifier.weight(1f)
                    )
                }
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

            // Мотивирующее сообщение
            Text(
                text = if (isToday) motivationMessage else "Статистика за выбранный день",
                style = MaterialTheme.typography.bodyMedium,
                color = if (isToday) MaterialTheme.colorScheme.onSurfaceVariant
                else MaterialTheme.colorScheme.outline,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
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
            style = MaterialTheme.typography.titleLarge, // Увеличим размер для лучшей читаемости
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp)
        )
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

// Вспомогательная функция для форматирования даты
private fun formatDate(timestamp: Long): String {
    val dateFormat = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
    return dateFormat.format(Date(timestamp))
}

// Функции для мотивирующих сообщений
private fun getDailyMotivationMessage(completionRate: Float): String {
    return when {
        completionRate == 0f -> when ((0..2).random()) {
            0 -> "Начните свой день с маленьких шагов! 🌟"
            1 -> "Каждое начало - это уже победа! 💫"
            else -> "Сегодня отличный день для начала! 🚀"
        }
        completionRate < 0.3f -> when ((0..2).random()) {
            0 -> "Вы на правильном пути! Продолжайте! 👣"
            1 -> "Маленькие шаги ведут к большим результатам! 🌱"
            else -> "За этот день ещё не всё выполнено, но вы справитесь! 💪"
        }
        completionRate < 0.7f -> when ((0..2).random()) {
            0 -> "Хорошая работа! Ещё немного усилий! 🔥"
            1 -> "Вы на полпути! Продолжайте в том же духе! ⚡"
            else -> "Отличный прогресс! Завершите начатое! 🌈"
        }
        completionRate < 1f -> when ((0..2).random()) {
            0 -> "Почти у цели! Осталось совсем немного! 🎯"
            1 -> "Финальный рывок! Вы почти у финиша! 🏁"
            else -> "Не сдавайтесь! Вы так близки к успеху! ✨"
        }
        else -> when ((0..2).random()) {
            0 -> "Идеальный день! Вы великолепны! 🎉"
            1 -> "Потрясающе! Все задачи выполнены! 🌟"
            else -> "100% успеха! Вы заслужили похвалу! 🏆"
        }
    }
}

private fun getMonthlyMotivationMessage(completionRate: Float): String {
    return when {
        completionRate == 0f -> when ((0..2).random()) {
            0 -> "Каждый месяц - новая возможность! 🌟"
            1 -> "Начните с одной привычки - остальное приложится! 💫"
            else -> "Этот месяц может стать turning point! 🚀"
        }
        completionRate < 0.3f -> when ((0..2).random()) {
            0 -> "Есть над чем поработать в следующем месяце 📈"
            1 -> "Небольшие успехи - это тоже прогресс! 🌱"
            else -> "Каждое усилие имеет значение! 💪"
        }
        completionRate < 0.7f -> when ((0..2).random()) {
            0 -> "Стабильный прогресс - ключ к успеху! 🔑"
            1 -> "Хорошая работа! Стремитесь к большему! ⭐"
            else -> "Вы на верном пути! Продолжайте! 🌈"
        }
        completionRate < 1f -> when ((0..2).random()) {
            0 -> "Отличные результаты! Почти идеально! 🎯"
            1 -> "Впечатляющий прогресс за месяц! 🌟"
            else -> "Вы близки к 100%! Следующий месяц будет идеальным! ✨"
        }
        else -> when ((0..2).random()) {
            0 -> "Идеальный месяц! Вы - пример для подражания! 🏆"
            1 -> "100% выполнение! Так держать! 🎉"
            else -> "Потрясающая дисциплина! Вы восхитительны! 🌟"
        }
    }
}

// Функция для проверки, является ли дата сегодняшним днем
private fun isToday(timestamp: Long): Boolean {
    val today = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    val selectedDay = Calendar.getInstance().apply {
        timeInMillis = timestamp
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    return today == selectedDay
}
package ru.plumsoftware.habitstracker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.plumsoftware.habitstracker.ui.theme.Dimen
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.roundToInt

private const val DAY_MS = 24 * 60 * 60 * 1000L
private const val WEEKS_BEFORE = 26   // сколько недель доступно для прокрутки назад
private const val WEEKS_TOTAL = 79    // всего недель (назад + вперёд)

/**
 * Недельная лента дат. Показывает дни, начиная с начала текущей недели (по настройке начала
 * недели). Количество видимых дней зависит от ширины: узкий телефон — 5, обычный — 7, планшет — 14.
 * Прокрутка «прилипает» к границам недель (от начала недели до начала недели).
 */
@Composable
fun CalendarView(
    selectedDate: Long,
    onDateSelected: (Long) -> Unit,
    dayProgress: Map<Long, Float> = emptyMap(),
    firstDow: Int = Calendar.MONDAY
) {
    // Начало недели, содержащей сегодня, минус WEEKS_BEFORE недель — точка отсчёта ленты
    val baseWeekStart = remember(firstDow) {
        val c = Calendar.getInstance().apply {
            firstDayOfWeek = firstDow
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val delta = (c.get(Calendar.DAY_OF_WEEK) - firstDow + 7) % 7
        c.add(Calendar.DAY_OF_MONTH, -delta)
        c.add(Calendar.DAY_OF_MONTH, -WEEKS_BEFORE * 7)
        c.timeInMillis
    }
    val totalDays = WEEKS_TOTAL * 7
    val currentWeekStartIndex = WEEKS_BEFORE * 7

    val listState = rememberLazyListState(initialFirstVisibleItemIndex = currentWeekStartIndex)
    val dateFormat = remember { SimpleDateFormat("EEE", Locale.getDefault()) }
    val selectedStart = remember(selectedDate) { startOfDay(selectedDate) }

    // Прокрутка к неделе выбранной даты (например, после выбора в диалоге календаря)
    LaunchedEffect(selectedStart, baseWeekStart) {
        val idx = ((selectedStart - baseWeekStart).toDouble() / DAY_MS).roundToInt()
        if (idx in 0 until totalDays) {
            val weekStart = (idx / 7) * 7
            listState.animateScrollToItem(weekStart)
        }
    }

    // Прилипание к началу недели после остановки прокрутки
    LaunchedEffect(listState.isScrollInProgress) {
        if (!listState.isScrollInProgress && listState.layoutInfo.totalItemsCount > 0) {
            val firstIndex = listState.firstVisibleItemIndex
            val firstSize = listState.layoutInfo.visibleItemsInfo.firstOrNull()?.size ?: 1
            val offset = listState.firstVisibleItemScrollOffset
            val approx = firstIndex + if (offset > firstSize / 2) 1 else 0
            val target = ((approx / 7.0).roundToInt() * 7).coerceIn(0, totalDays - 1)
            if (target != firstIndex || offset != 0) {
                listState.animateScrollToItem(target)
            }
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        // Сколько дней помещается: узкий экран — 5, обычный телефон — 7, планшет/широкий — 14
        val visibleDays = when {
            maxWidth >= 520.dp -> 14
            maxWidth < 340.dp -> 5
            else -> 7
        }
        val gap = Dimen.small
        val dayWidth = (maxWidth - gap * (visibleDays - 1)) / visibleDays

        LazyRow(
            state = listState,
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(gap)
        ) {
            items(totalDays) { index ->
                val dayCal = Calendar.getInstance().apply {
                    timeInMillis = baseWeekStart
                    add(Calendar.DAY_OF_MONTH, index)
                }
                val dayMillis = startOfDay(dayCal.timeInMillis)
                DayItem(
                    day = dayCal.get(Calendar.DAY_OF_MONTH),
                    dayName = dateFormat.format(dayCal.time),
                    isCurrentMonth = true,
                    isSelected = dayMillis == selectedStart,
                    progress = dayProgress[dayMillis] ?: 0f,
                    onClick = { onDateSelected(dayMillis) },
                    modifier = Modifier.width(dayWidth)
                )
            }
        }
    }
}

private fun startOfDay(millis: Long): Long = Calendar.getInstance().apply {
    timeInMillis = millis
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis

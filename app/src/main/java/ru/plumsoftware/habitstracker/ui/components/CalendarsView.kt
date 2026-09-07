package ru.plumsoftware.habitstracker.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerSnapDistance
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ru.plumsoftware.habitstracker.ui.theme.Dimen
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.roundToInt

private const val DAY_MS = 24 * 60 * 60 * 1000L
private const val WEEKS_BEFORE = 26   // сколько недель доступно для прокрутки назад
private const val WEEKS_TOTAL = 79    // всего недель (назад + вперёд)
private const val TABLET_BREAKPOINT_DP = 520

/**
 * Недельная лента дат. Прокрутка организована как Pager: одна страница = одна неделя.
 * Свайп всегда переключает ровно одну неделю (аналог ViewPager, PagerSnapDistance.atMost(1)),
 * независимо от силы свайпа. В конце каждой недели — вертикальный дивайдер 2dp с равными
 * отступами от предыдущего (последний день недели) и следующего (первый день следующей недели
 * при перетаскивании) элементов.
 * На планшете (широкий экран) в области Pager-а помещается сразу 2 недели.
 */
@OptIn(ExperimentalFoundationApi::class)
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

    val currentWeekPage = WEEKS_BEFORE
    val pagerState = rememberPagerState(initialPage = currentWeekPage) { WEEKS_TOTAL }
    val dateFormat = remember { SimpleDateFormat("EEE", Locale.getDefault()) }
    val selectedStart = remember(selectedDate) { startOfDay(selectedDate) }

    // Прокрутка к неделе выбранной даты (например, после выбора в диалоге календаря)
    LaunchedEffect(selectedStart, baseWeekStart) {
        val dayIndex = ((selectedStart - baseWeekStart).toDouble() / DAY_MS).roundToInt()
        val page = (dayIndex / 7).coerceIn(0, WEEKS_TOTAL - 1)
        if (!pagerState.isScrollInProgress && page != pagerState.currentPage) {
            pagerState.animateScrollToPage(page)
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        // На планшете показываем 2 недели одновременно, на телефоне — 1
        val weeksPerScreen = if (maxWidth >= TABLET_BREAKPOINT_DP.dp) 2 else 1
        val gap = Dimen.small
        val dividerThickness = 2.dp
        val pageWidth: Dp = maxWidth / weeksPerScreen

        // Внутри страницы: 7 дней с gap между ними (6 промежутков) +
        // gap перед дивайдером + сам дивайдер + gap после дивайдера
        val fixedWidth = gap * 6 + gap + dividerThickness + gap
        val dayWidth = (pageWidth - fixedWidth) / 7

        HorizontalPager(
            state = pagerState,
            pageSize = PageSize.Fixed(pageWidth),
            flingBehavior = PagerDefaults.flingBehavior(
                state = pagerState,
                // Ключевая часть: фикс на ViewPager-поведение — не больше одной страницы за свайп,
                // независимо от скорости флинга
                pagerSnapDistance = PagerSnapDistance.atMost(1)
            ),
            modifier = Modifier.fillMaxWidth()
        ) { page ->
            val weekStartMillis = baseWeekStart + page * 7L * DAY_MS

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
            ) {
                for (d in 0 until 7) {
                    val dayCal = Calendar.getInstance().apply {
                        timeInMillis = weekStartMillis
                        add(Calendar.DAY_OF_MONTH, d)
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
                    // Отступ между днями — только между ними, не после последнего
                    if (d != 6) {
                        Spacer(modifier = Modifier.width(gap))
                    }
                }

                // Отступ перед дивайдером — такой же, как между днями
                Spacer(modifier = Modifier.width((gap)))

                // Дивайдер конца недели, 2dp толщиной, виден при перетаскивании между неделями
                Box(
                    modifier = Modifier
                        .padding(vertical = 2.dp)
                        .fillMaxHeight()
                        .width(dividerThickness)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )

                // Отступ после дивайдера — равен отступу перед ним
                Spacer(modifier = Modifier.width((gap)))
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
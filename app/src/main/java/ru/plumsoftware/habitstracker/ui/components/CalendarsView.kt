package ru.plumsoftware.habitstracker.ui.components

import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ru.plumsoftware.habitstracker.ui.theme.Dimen
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs

@Composable
fun CalendarView(
    selectedDate: Long,
    selectedDay: Int,
    onDaySelected: (Int) -> Unit
) {
    val listState = rememberLazyListState()

    LaunchedEffect(selectedDate) {
        delay(1200)

        val cal = Calendar.getInstance().apply {
            timeInMillis = selectedDate
            firstDayOfWeek = Calendar.MONDAY
        }

        val firstDayOfMonth = cal.clone() as Calendar
        firstDayOfMonth.set(Calendar.DAY_OF_MONTH, 1)
        val firstDisplayDay = getFirstDisplayDay(firstDayOfMonth)

        val daysDiff = ((selectedDate - firstDisplayDay.timeInMillis) / (24 * 60 * 60 * 1000)).toInt()
        val weekIndex = daysDiff / 7

        println("AUTOSCROLL DEBUG: weekIndex = $weekIndex, daysDiff = $daysDiff")

        if (weekIndex >= 0) {
            listState.animateScrollToItem(weekIndex)
        }
    }

    LaunchedEffect(listState.isScrollInProgress) {
        if (!listState.isScrollInProgress) {
            delay(50)
            val targetWeek = findTargetWeekIndex(listState)
            listState.animateScrollToItem(targetWeek)
        }
    }

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val layoutInfo = listState.layoutInfo
                    val visibleItems = layoutInfo.visibleItemsInfo

                    if (visibleItems.isNotEmpty()) {
                        val viewportStart = layoutInfo.viewportStartOffset
                        val absoluteOffset = offset.x + viewportStart

                        val tappedItem = visibleItems.find { item ->
                            absoluteOffset >= item.offset && absoluteOffset <= item.offset + item.size
                        }

                        tappedItem?.let { item ->
                            val weekIndex = item.index
                            val cal = Calendar.getInstance().apply {
                                timeInMillis = selectedDate
                                firstDayOfWeek = Calendar.MONDAY
                            }

                            val firstDayOfMonth = cal.clone() as Calendar
                            firstDayOfMonth.set(Calendar.DAY_OF_MONTH, 1)
                            val firstDisplayDay = getFirstDisplayDay(firstDayOfMonth)

                            val weekStartDay = weekIndex * 7
                            val relativeOffset = absoluteOffset - item.offset
                            val dayIndex = (relativeOffset / item.size * 7).toInt()
                            val targetDayIndex = weekStartDay + dayIndex.coerceIn(0, 6)

                            val targetDay = firstDisplayDay.clone() as Calendar
                            targetDay.add(Calendar.DAY_OF_MONTH, targetDayIndex)

                            val dayNumber = targetDay.get(Calendar.DAY_OF_MONTH)
                            val isCurrentMonth = targetDay.get(Calendar.MONTH) == cal.get(Calendar.MONTH)

                            if (isCurrentMonth) {
                                onDaySelected(dayNumber)
                            }
                        }
                    }
                }
            },
        state = listState,
    ) {
        val cal = Calendar.getInstance().apply {
            timeInMillis = selectedDate
            firstDayOfWeek = Calendar.MONDAY
        }

        val dateFormat = SimpleDateFormat("EEE", Locale.getDefault())
        val firstDayOfMonth = (cal.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val firstDisplayDay = getFirstDisplayDay(firstDayOfMonth)
        val lastDayOfMonth = (cal.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
        }
        val lastDisplayDay = getLastDisplayDay(lastDayOfMonth)
        val totalDays = ((lastDisplayDay.timeInMillis - firstDisplayDay.timeInMillis) / (24 * 60 * 60 * 1000)).toInt() + 1
        val weeks = (0 until totalDays).chunked(7)

        items(weeks.size) { weekIndex ->
            WeekRow(
                week = weeks[weekIndex],
                firstDisplayDay = firstDisplayDay,
                dateFormat = dateFormat,
                currentMonth = cal.get(Calendar.MONTH),
                selectedDay = selectedDay,
                onDaySelected = onDaySelected
            )
        }
    }
}

@Composable
fun WeekRow(
    week: List<Int>,
    firstDisplayDay: Calendar,
    dateFormat: SimpleDateFormat,
    currentMonth: Int,
    selectedDay: Int,
    onDaySelected: (Int) -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        val availableWidth = maxWidth - (Dimen.smallPlus * 12) // Отступы между днями
        val dayWidth = availableWidth / 7

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            for (i in 0 until 7) {
                val dayIndex = if (i < week.size) week[i] else -1

                if (dayIndex != -1) {
                    val currentDay = firstDisplayDay.clone() as Calendar
                    currentDay.add(Calendar.DAY_OF_MONTH, dayIndex)

                    val dayNumber = currentDay.get(Calendar.DAY_OF_MONTH)
                    val dayName = dateFormat.format(currentDay.time)
                    val isCurrentMonth = currentDay.get(Calendar.MONTH) == currentMonth
                    val isSelected = isCurrentMonth && dayNumber == selectedDay

                    DayItem(
                        day = dayNumber,
                        dayName = dayName,
                        isCurrentMonth = isCurrentMonth,
                        isSelected = isSelected,
                        onClick = {
                            if (isCurrentMonth) {
                                onDaySelected(dayNumber)
                            }
                        },
                        modifier = Modifier.width(dayWidth)
                    )
                } else {
                    Spacer(modifier = Modifier.width(dayWidth))
                }
            }
        }
    }
}

class InstantSnapFlingBehavior(
    private val listState: LazyListState,
    private val coroutineScope: CoroutineScope
) : FlingBehavior {
    override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
        // Мгновенно snap при любом флинге
        val targetWeek = findTargetWeekIndex(listState)
        coroutineScope.launch {
            listState.animateScrollToItem(targetWeek)
        }
        return 0f
    }
}

class NonInterceptingSnapFlingBehavior(
    private val listState: LazyListState,
    private val coroutineScope: CoroutineScope
) : FlingBehavior {
    override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
        if (abs(initialVelocity) > 100f) {
            val targetWeek = findTargetWeekIndex(listState)
            coroutineScope.launch {
                listState.animateScrollToItem(targetWeek)
            }
        }
        return initialVelocity
    }
}

private fun findTargetWeekIndex(listState: LazyListState): Int {
    val layoutInfo = listState.layoutInfo
    val visibleItems = layoutInfo.visibleItemsInfo
    if (visibleItems.isEmpty()) return listState.firstVisibleItemIndex

    val viewportCenter = layoutInfo.viewportStartOffset + (layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset) / 2

    val targetItem = visibleItems.minByOrNull { item ->
        abs((item.offset + item.size / 2) - viewportCenter)
    }

    return targetItem?.index ?: listState.firstVisibleItemIndex
}

private fun getFirstDisplayDay(firstDayOfMonth: Calendar): Calendar {
    val firstDisplayDay = firstDayOfMonth.clone() as Calendar
    val dayOfWeek = firstDayOfMonth.get(Calendar.DAY_OF_WEEK)
    val daysToSubtract = (dayOfWeek - Calendar.MONDAY + 7) % 7
    firstDisplayDay.add(Calendar.DAY_OF_MONTH, -daysToSubtract)
    return firstDisplayDay
}

private fun getLastDisplayDay(lastDayOfMonth: Calendar): Calendar {
    val lastDisplayDay = lastDayOfMonth.clone() as Calendar
    val dayOfWeek = lastDayOfMonth.get(Calendar.DAY_OF_WEEK)
    val daysToAdd = (Calendar.SUNDAY - dayOfWeek + 7) % 7
    lastDisplayDay.add(Calendar.DAY_OF_MONTH, daysToAdd)
    return lastDisplayDay
}
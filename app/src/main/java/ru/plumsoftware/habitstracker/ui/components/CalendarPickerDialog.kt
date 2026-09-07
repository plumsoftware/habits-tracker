package ru.plumsoftware.habitstracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import ru.plumsoftware.habitstracker.ui.theme.Dimen
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Кастомный диалог выбора даты. В отличие от Material3 DatePicker позволяет
 * задать выбранному дню форму скруглённого квадрата и синюю тень.
 */
@Composable
fun CalendarPickerDialog(
    initialDateMillis: Long,
    onConfirm: (Long) -> Unit,
    onDismiss: () -> Unit,
    firstDow: Int = Calendar.MONDAY
) {
    var selectedMillis by remember { mutableLongStateOf(startOfDay(initialDateMillis)) }
    // Месяц, который сейчас отображается (первое число)
    var visibleMonth by remember {
        mutableStateOf(firstOfMonth(initialDateMillis))
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerLowest,
            tonalElevation = 0.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Dimen.extraLarge)
            ) {
                // Шапка с навигацией по месяцам
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = { visibleMonth = addMonths(visibleMonth, -1) },
                        modifier = Modifier
                            .clip(MaterialTheme.shapes.medium)
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                            contentDescription = "Предыдущий месяц",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = monthTitle(visibleMonth),
                        style = MaterialTheme.typography.titleLarge
                    )
                    IconButton(
                        onClick = { visibleMonth = addMonths(visibleMonth, 1) },
                        modifier = Modifier
                            .clip(MaterialTheme.shapes.medium)
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                            contentDescription = "Следующий месяц",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Дни недели
                Row(modifier = Modifier.fillMaxWidth().padding(top = Dimen.smallPlus)) {
                    weekdayLabels(firstDow).forEach { label ->
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Сетка дней (6 недель)
                val cells = monthCells(visibleMonth, firstDow)
                cells.chunked(7).forEach { week ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        week.forEach { cell ->
                            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                if (cell != null) {
                                    DayCell(
                                        dayMillis = cell,
                                        selected = startOfDay(cell) == selectedMillis,
                                        onClick = { selectedMillis = startOfDay(cell) }
                                    )
                                } else {
                                    Box(modifier = Modifier.aspectRatio(1f))
                                }
                            }
                        }
                    }
                }

                // Кнопки
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = Dimen.large),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(text = "Отмена", style = MaterialTheme.typography.titleSmall)
                    }
                    Button(
                        modifier = Modifier
                            .padding(start = Dimen.smallPlus)
                            .shadow(
                                elevation = 14.dp,
                                shape = MaterialTheme.shapes.large,
                                clip = false,
                                ambientColor = MaterialTheme.colorScheme.primary,
                                spotColor = MaterialTheme.colorScheme.primary
                            ),
                        shape = MaterialTheme.shapes.large,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                        onClick = { onConfirm(selectedMillis) }
                    ) {
                        Text(text = "Готово", style = MaterialTheme.typography.titleSmall)
                    }
                }
            }
        }
    }
}

/**
 * Тот же кастомный календарь, но для выбора диапазона дат (например, для экспорта).
 * Первый тап задаёт начало, второй — конец; третий начинает выбор заново.
 */
@Composable
fun CalendarRangePickerDialog(
    onConfirm: (from: Long, to: Long) -> Unit,
    onDismiss: () -> Unit,
    initialDateMillis: Long = System.currentTimeMillis(),
    firstDow: Int = Calendar.MONDAY
) {
    var rangeStart by remember { mutableStateOf<Long?>(null) }
    var rangeEnd by remember { mutableStateOf<Long?>(null) }
    var visibleMonth by remember { mutableStateOf(firstOfMonth(initialDateMillis)) }

    fun onDayClick(dayMillis: Long) {
        val d = startOfDay(dayMillis)
        val start = rangeStart
        when {
            start == null || rangeEnd != null -> {
                // Начинаем новый диапазон
                rangeStart = d
                rangeEnd = null
            }
            d < start -> {
                // Кликнули раньше начала — меняем местами
                rangeStart = d
                rangeEnd = start
            }
            else -> rangeEnd = d
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerLowest,
            tonalElevation = 0.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Dimen.extraLarge)
            ) {
                // Шапка с навигацией по месяцам
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = { visibleMonth = addMonths(visibleMonth, -1) },
                        modifier = Modifier
                            .clip(MaterialTheme.shapes.medium)
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                            contentDescription = "Предыдущий месяц",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = monthTitle(visibleMonth),
                        style = MaterialTheme.typography.titleLarge
                    )
                    IconButton(
                        onClick = { visibleMonth = addMonths(visibleMonth, 1) },
                        modifier = Modifier
                            .clip(MaterialTheme.shapes.medium)
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                            contentDescription = "Следующий месяц",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Подпись выбранного диапазона
                Text(
                    text = rangeSubtitle(rangeStart, rangeEnd),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = Dimen.smallPlus)
                )

                // Дни недели
                Row(modifier = Modifier.fillMaxWidth().padding(top = Dimen.smallPlus)) {
                    weekdayLabels(firstDow).forEach { label ->
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Сетка дней (6 недель)
                val cells = monthCells(visibleMonth, firstDow)
                val start = rangeStart
                val end = rangeEnd
                cells.chunked(7).forEach { week ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        week.forEach { cell ->
                            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                if (cell != null) {
                                    val d = startOfDay(cell)
                                    val isEndpoint = d == start || d == end
                                    val inRange = start != null && end != null && d in start..end
                                    RangeDayCell(
                                        dayMillis = cell,
                                        isEndpoint = isEndpoint,
                                        inRange = inRange,
                                        onClick = { onDayClick(cell) }
                                    )
                                } else {
                                    Box(modifier = Modifier.aspectRatio(1f))
                                }
                            }
                        }
                    }
                }

                // Кнопки
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = Dimen.large),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(text = "Отмена", style = MaterialTheme.typography.titleSmall)
                    }
                    Button(
                        modifier = Modifier
                            .padding(start = Dimen.smallPlus)
                            .shadow(
                                elevation = 14.dp,
                                shape = MaterialTheme.shapes.large,
                                clip = false,
                                ambientColor = MaterialTheme.colorScheme.primary,
                                spotColor = MaterialTheme.colorScheme.primary
                            ),
                        shape = MaterialTheme.shapes.large,
                        enabled = rangeStart != null,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                        onClick = {
                            val from = rangeStart ?: return@Button
                            val to = rangeEnd ?: from
                            onConfirm(from, endOfDay(to))
                        }
                    ) {
                        Text(text = "Готово", style = MaterialTheme.typography.titleSmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun RangeDayCell(
    dayMillis: Long,
    isEndpoint: Boolean,
    inRange: Boolean,
    onClick: () -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary
    val day = Calendar.getInstance().apply { timeInMillis = dayMillis }.get(Calendar.DAY_OF_MONTH)

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(3.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isEndpoint) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(primary.copy(alpha = 0.5f), Color.Transparent)
                        ),
                        shape = MaterialTheme.shapes.medium
                    )
            )
        }

        Box(
            modifier = Modifier
                .size(40.dp)
                .then(
                    if (isEndpoint) Modifier.shadow(
                        elevation = 12.dp,
                        shape = MaterialTheme.shapes.medium,
                        clip = false,
                        ambientColor = primary,
                        spotColor = primary
                    ) else Modifier
                )
                .background(
                    color = when {
                        isEndpoint -> primary
                        inRange -> MaterialTheme.colorScheme.primaryContainer
                        else -> Color.Transparent
                    },
                    shape = MaterialTheme.shapes.medium
                )
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = day.toString(),
                style = MaterialTheme.typography.bodyLarge,
                color = when {
                    isEndpoint -> MaterialTheme.colorScheme.onPrimary
                    inRange -> MaterialTheme.colorScheme.onPrimaryContainer
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )
        }
    }
}

private fun rangeSubtitle(start: Long?, end: Long?): String {
    val fmt = SimpleDateFormat("d MMM", Locale("ru"))
    return when {
        start == null -> "Выберите начало периода"
        end == null -> "${fmt.format(start)} — выберите конец"
        else -> "${fmt.format(start)} — ${fmt.format(end)}"
    }
}

private fun endOfDay(millis: Long): Long = Calendar.getInstance().apply {
    timeInMillis = millis
    set(Calendar.HOUR_OF_DAY, 23)
    set(Calendar.MINUTE, 59)
    set(Calendar.SECOND, 59)
    set(Calendar.MILLISECOND, 999)
}.timeInMillis

@Composable
private fun DayCell(
    dayMillis: Long,
    selected: Boolean,
    onClick: () -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary
    val day = Calendar.getInstance().apply { timeInMillis = dayMillis }.get(Calendar.DAY_OF_MONTH)

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(3.dp),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            // Синее свечение под выбранным днём
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(primary.copy(alpha = 0.5f), Color.Transparent)
                        ),
                        shape = MaterialTheme.shapes.medium
                    )
            )
        }

        Box(
            modifier = Modifier
                .size(40.dp)
                .then(
                    if (selected) Modifier.shadow(
                        elevation = 12.dp,
                        shape = MaterialTheme.shapes.medium,
                        clip = false,
                        ambientColor = primary,
                        spotColor = primary
                    ) else Modifier
                )
                .background(
                    color = if (selected) primary else Color.Transparent,
                    shape = MaterialTheme.shapes.medium
                )
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = day.toString(),
                style = MaterialTheme.typography.bodyLarge,
                color = if (selected) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

private fun weekdayLabels(firstDow: Int): List<String> {
    // Индекс 0..6, где 0 = воскресенье (Calendar.SUNDAY = 1)
    val base = listOf("вс", "пн", "вт", "ср", "чт", "пт", "сб")
    val start = firstDow - Calendar.SUNDAY // MONDAY(2)->1, SUNDAY(1)->0
    return (0 until 7).map { base[(start + it) % 7] }
}

/** Ячейки месяца: 42 слота, null — пустые до/после месяца. */
private fun monthCells(monthStartMillis: Long, firstDow: Int): List<Long?> {
    val cal = Calendar.getInstance().apply {
        timeInMillis = monthStartMillis
        firstDayOfWeek = firstDow
    }
    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    // Смещение первого дня месяца относительно начала недели
    val dow = cal.get(Calendar.DAY_OF_WEEK) // вс=1..сб=7
    val lead = (dow - firstDow + 7) % 7

    val cells = ArrayList<Long?>(42)
    repeat(lead) { cells.add(null) }
    for (d in 1..daysInMonth) {
        val dayCal = Calendar.getInstance().apply {
            timeInMillis = monthStartMillis
            set(Calendar.DAY_OF_MONTH, d)
        }
        cells.add(dayCal.timeInMillis)
    }
    while (cells.size % 7 != 0) cells.add(null)
    return cells
}

private fun monthTitle(monthStartMillis: Long): String {
    val fmt = SimpleDateFormat("LLLL yyyy", Locale("ru"))
    return fmt.format(monthStartMillis).replaceFirstChar { it.uppercase() }
}

private fun firstOfMonth(millis: Long): Long = Calendar.getInstance().apply {
    timeInMillis = millis
    set(Calendar.DAY_OF_MONTH, 1)
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis

private fun addMonths(millis: Long, delta: Int): Long = Calendar.getInstance().apply {
    timeInMillis = millis
    add(Calendar.MONTH, delta)
}.timeInMillis

private fun startOfDay(millis: Long): Long = Calendar.getInstance().apply {
    timeInMillis = millis
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis

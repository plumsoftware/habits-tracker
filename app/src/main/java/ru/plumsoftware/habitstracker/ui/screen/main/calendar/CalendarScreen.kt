package ru.plumsoftware.habitstracker.ui.screen.main.calendar

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.plumsoftware.habitstracker.data.model.Category
import ru.plumsoftware.habitstracker.data.model.Habit
import ru.plumsoftware.habitstracker.ui.components.AppIcon
import ru.plumsoftware.habitstracker.ui.components.ActivityShortcuts
import ru.plumsoftware.habitstracker.ui.components.HabitIcons
import ru.plumsoftware.habitstracker.ui.components.clickableNoRipple
import ru.plumsoftware.habitstracker.ui.screen.main.home.HomeState
import ru.plumsoftware.habitstracker.ui.theme.AppTheme
import ru.plumsoftware.habitstracker.ui.theme.softShadow
import java.util.Calendar

/**
 * Экран «Календарь» (ТЗ §5): фильтр по привычкам, месячная сетка активности,
 * карточка выбранного дня со списком привычек и их статусами.
 */
@Composable
fun CalendarScreen(
    state: HomeState,
    modifier: Modifier = Modifier,
    onSelectDate: (Long) -> Unit,
    onHabitClick: (Habit) -> Unit,
    onOpenStepCounter: () -> Unit = {},
    onOpenWorkouts: () -> Unit = {},
    onOpenSleep: () -> Unit = {},
) {
    val colors = AppTheme.colors
    val spacing = AppTheme.spacing
    val selectedMillis = if (state.selectedDate > 0L) state.selectedDate else System.currentTimeMillis()

    // Отображаемый месяц (по первому дню). Навигация ←/→.
    var monthAnchor by remember(selectedMillis) { mutableStateOf(firstOfMonth(selectedMillis)) }
    // Фильтр: null = «Все», иначе id привычки.
    var filterHabitId by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = modifier.fillMaxWidth().background(colors.bgScreen),
        contentPadding = PaddingValues(
            start = spacing.screenH, end = spacing.screenH,
            top = spacing.screenTop, bottom = spacing.screenBottomNav,
        ),
        verticalArrangement = Arrangement.spacedBy(spacing.sectionGap),
    ) {
        item {
            Header(
                monthAnchor = monthAnchor,
                onPrev = { monthAnchor = addMonths(monthAnchor, -1) },
                onNext = { monthAnchor = addMonths(monthAnchor, 1) },
            )
        }
        item {
            HabitFilter(
                habits = state.allHabits,
                categories = state.categories,
                selectedId = filterHabitId,
                onSelect = { filterHabitId = it },
            )
        }
        item {
            val selectedHabit = state.allHabits.firstOrNull { it.id == filterHabitId }
            MonthGrid(
                monthAnchor = monthAnchor,
                firstDayOfWeek = state.firstDayOfWeek,
                dayProgress = state.dayProgress,
                frozenDays = state.frozenDays,
                perHabit = selectedHabit?.let { h ->
                    PerHabit(
                        completed = state.habitCompletionDays[h.id] ?: emptySet(),
                        frozen = state.frozenByHabit[h.id] ?: emptySet(),
                        isScheduled = { day -> isScheduledOn(h, day) },
                    )
                },
                selectedMillis = selectedMillis,
                onSelectDate = onSelectDate,
            )
        }
        item {
            ActivityShortcuts(selectedMillis, onOpenStepCounter, onOpenWorkouts, onOpenSleep)
        }
        item {
            SelectedDayCard(
                state = state,
                selectedMillis = selectedMillis,
                filterHabitId = filterHabitId,
                onHabitClick = onHabitClick,
            )
        }
    }
}

/**
 * Планшетная версия «Календаря» (§5): слева — хедер, фильтр и сетка месяца,
 * справа — карточка выбранного дня.
 */
@Composable
fun CalendarScreenTablet(
    state: HomeState,
    modifier: Modifier = Modifier,
    onSelectDate: (Long) -> Unit,
    onHabitClick: (Habit) -> Unit,
    onOpenStepCounter: () -> Unit = {},
    onOpenWorkouts: () -> Unit = {},
    onOpenSleep: () -> Unit = {},
) {
    val colors = AppTheme.colors
    val spacing = AppTheme.spacing
    val selectedMillis = if (state.selectedDate > 0L) state.selectedDate else System.currentTimeMillis()
    var monthAnchor by remember(selectedMillis) { mutableStateOf(firstOfMonth(selectedMillis)) }
    var filterHabitId by remember { mutableStateOf<String?>(null) }

    Row(
        modifier = modifier.fillMaxWidth().background(colors.bgScreen)
            .padding(horizontal = spacing.screenH, vertical = spacing.screenTop),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Column(
            modifier = Modifier.weight(1.2f).fillMaxHeight()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(spacing.sectionGap),
        ) {
            Header(
                monthAnchor = monthAnchor,
                onPrev = { monthAnchor = addMonths(monthAnchor, -1) },
                onNext = { monthAnchor = addMonths(monthAnchor, 1) },
            )
            HabitFilter(state.allHabits, state.categories, filterHabitId) { filterHabitId = it }
            val selectedHabit = state.allHabits.firstOrNull { it.id == filterHabitId }
            MonthGrid(
                monthAnchor = monthAnchor,
                firstDayOfWeek = state.firstDayOfWeek,
                dayProgress = state.dayProgress,
                frozenDays = state.frozenDays,
                perHabit = selectedHabit?.let { h ->
                    PerHabit(
                        completed = state.habitCompletionDays[h.id] ?: emptySet(),
                        frozen = state.frozenByHabit[h.id] ?: emptySet(),
                        isScheduled = { day -> isScheduledOn(h, day) },
                    )
                },
                selectedMillis = selectedMillis,
                onSelectDate = onSelectDate,
            )
            Spacer(Modifier.height(spacing.screenBottomPlain))
        }
        Column(
            modifier = Modifier.weight(1f).fillMaxHeight()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(spacing.sectionGap),
        ) {
            ActivityShortcuts(selectedMillis, onOpenStepCounter, onOpenWorkouts, onOpenSleep)
            SelectedDayCard(state = state, selectedMillis = selectedMillis, filterHabitId = filterHabitId, onHabitClick = onHabitClick)
            Spacer(Modifier.height(spacing.screenBottomPlain))
        }
    }
}

// ---------- §5.1 Хедер ----------

@Composable
private fun Header(monthAnchor: Long, onPrev: () -> Unit, onNext: () -> Unit) {
    val colors = AppTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text("Календарь", style = AppTheme.type.h1, color = colors.textPrimary)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            NavButton(HabitIcons.chevronLeft, onPrev)
            NavButton(HabitIcons.chevronRight, onNext)
        }
    }
}

@Composable
private fun NavButton(icon: Int, onClick: () -> Unit) {
    val colors = AppTheme.colors
    Box(
        modifier = Modifier
            .size(38.dp)
            .softShadow(AppTheme.elevation.subtle, RoundedCornerShape(13.dp))
            .clip(RoundedCornerShape(13.dp))
            .background(colors.surface)
            .clickableNoRipple(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { AppIcon(icon, tint = colors.textPrimary, size = 15.dp) }
}

// ---------- §5.2 Фильтр по привычкам ----------

@Composable
private fun HabitFilter(
    habits: List<Habit>,
    categories: List<Category>,
    selectedId: String?,
    onSelect: (String?) -> Unit,
) {
    val colors = AppTheme.colors
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            FilterChip(
                text = "Все", selected = selectedId == null,
                leadingIcon = HabitIcons.target, onClick = { onSelect(null) },
            )
        }
        items(habits) { habit ->
            val emoji = habit.emoji.ifBlank { habitEmoji(categories.firstOrNull { it.id == habit.categoryId }) }
            FilterChip(
                text = firstWord(habit.title), selected = selectedId == habit.id,
                leadingEmoji = emoji, onClick = { onSelect(habit.id) },
            )
        }
    }
}

@Composable
private fun FilterChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    leadingIcon: Int? = null,
    leadingEmoji: String? = null,
) {
    val colors = AppTheme.colors
    val bg by animateColorAsState(if (selected) colors.ink else colors.surface, label = "chipbg")
    val fg = if (selected) colors.inkOn else colors.textPrimary
    Row(
        modifier = Modifier
            .softShadow(AppTheme.elevation.subtle, AppTheme.shapes.pill)
            .clip(AppTheme.shapes.pill)
            .background(bg)
            .clickableNoRipple(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (leadingIcon != null) AppIcon(leadingIcon, tint = fg, size = 14.dp)
        if (leadingEmoji != null) Text(leadingEmoji, fontSize = 14.sp)
        Text(text, style = AppTheme.type.label, color = fg)
    }
}

// ---------- §5.3 Сетка месяца ----------

private data class PerHabit(
    val completed: Set<Long>,
    val frozen: Set<Long>,
    val isScheduled: (Long) -> Boolean,
)

@Composable
private fun MonthGrid(
    monthAnchor: Long,
    firstDayOfWeek: Int,
    dayProgress: Map<Long, Float>,
    frozenDays: Set<Long>,
    perHabit: PerHabit?,
    selectedMillis: Long,
    onSelectDate: (Long) -> Unit,
) {
    val colors = AppTheme.colors
    val cal = Calendar.getInstance().apply { timeInMillis = monthAnchor }
    val month = cal.get(Calendar.MONTH)
    val year = cal.get(Calendar.YEAR)
    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

    // Смещение до первого числа
    val firstDow = Calendar.getInstance().apply {
        timeInMillis = monthAnchor
        this.firstDayOfWeek = firstDayOfWeek
    }.get(Calendar.DAY_OF_WEEK)
    val leading = ((firstDow - firstDayOfWeek) + 7) % 7

    val todayStart = System.currentTimeMillis().startOfDay()
    val selectedStart = selectedMillis.startOfDay()

    // Сводка месяца (в режиме одной привычки — по её дням)
    var closedDays = 0
    var sum = 0f; var counted = 0
    for (d in 1..daysInMonth) {
        val start = dayStart(year, month, d)
        if (start > todayStart) continue
        if (perHabit != null) {
            if (!perHabit.isScheduled(start)) continue
            val done = perHabit.completed.contains(start) || perHabit.frozen.contains(start)
            if (perHabit.completed.contains(start)) closedDays++
            sum += if (done) 1f else 0f; counted++
        } else {
            val p = dayProgress[start] ?: 0f
            if (p >= 1f) closedDays++
            sum += p; counted++
        }
    }
    val rate = if (counted > 0) (sum / counted * 100).toInt() else 0

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .softShadow(AppTheme.elevation.cardRaised, AppTheme.shapes.cardL)
            .clip(AppTheme.shapes.cardL)
            .background(colors.surface)
            .padding(horizontal = 16.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("${monthName(month)} $year", style = AppTheme.type.h5, color = colors.textPrimary)
            Text("$closedDays дн. · $rate%", style = AppTheme.type.label, color = colors.accent)
        }

        // Дни недели
        Row(Modifier.fillMaxWidth()) {
            weekdayOrder(firstDayOfWeek).forEach { dow ->
                Text(
                    text = weekdayShort(dow),
                    style = AppTheme.type.captionS,
                    color = colors.textTertiary,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }

        // Сетка
        val cells = leading + daysInMonth
        val rows = (cells + 6) / 7
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            for (r in 0 until rows) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    for (c in 0 until 7) {
                        val index = r * 7 + c
                        val day = index - leading + 1
                        Box(Modifier.weight(1f).aspectRatio(1f), contentAlignment = Alignment.Center) {
                            if (day in 1..daysInMonth) {
                                val start = dayStart(year, month, day)
                                val progress: Float
                                val frozen: Boolean
                                val scheduled: Boolean
                                if (perHabit != null) {
                                    scheduled = perHabit.isScheduled(start)
                                    progress = if (perHabit.completed.contains(start)) 1f else 0f
                                    frozen = perHabit.frozen.contains(start)
                                } else {
                                    scheduled = true
                                    progress = dayProgress[start] ?: 0f
                                    frozen = frozenDays.contains(start)
                                }
                                DayCell(
                                    day = day,
                                    progress = progress,
                                    frozen = frozen,
                                    scheduled = scheduled,
                                    selected = start == selectedStart,
                                    isFuture = start > todayStart,
                                    onClick = { onSelectDate(start) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(day: Int, progress: Float, frozen: Boolean, scheduled: Boolean = true, selected: Boolean, isFuture: Boolean, onClick: () -> Unit) {
    val colors = AppTheme.colors
    val level = levelOf(progress)
    val isFrozen = frozen && level == 0
    val bg by animateColorAsState(
        when {
            selected -> colors.accent
            !scheduled -> colors.surfaceSunken.copy(alpha = 0.4f)
            isFrozen -> colors.heat1
            level >= 3 -> colors.accentSoft
            level == 2 -> colors.heat1
            else -> colors.surfaceSunken
        },
        label = "daycellbg",
    )
    val textColor = when {
        selected -> colors.inkOn
        !scheduled -> colors.textTertiary.copy(alpha = 0.5f)
        isFuture -> colors.textTertiary
        else -> colors.textPrimary
    }
    val indicator = when {
        isFuture || !scheduled -> Color.Transparent
        selected -> Color.White.copy(alpha = 0.6f)
        isFrozen -> colors.info
        level >= 1 -> colors.heat(level)
        else -> Color.Transparent
    }
    Box {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .softShadow(if (selected) AppTheme.elevation.accentButton else ru.plumsoftware.habitstracker.ui.theme.ShadowSpec.None, AppTheme.shapes.monthCell)
                .clip(AppTheme.shapes.monthCell)
                .background(bg)
                .clickableNoRipple(enabled = !isFuture, onClick = onClick)
                .padding(vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text("$day", style = AppTheme.type.label.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold), color = textColor)
            Box(
                modifier = Modifier
                    .size(width = 12.dp, height = 3.dp)
                    .clip(AppTheme.shapes.pill)
                    .background(indicator)
            )
        }
        if (isFrozen) {
            AppIcon(
                HabitIcons.snowflake, tint = colors.info, size = 9.dp,
                modifier = Modifier.align(Alignment.TopEnd).padding(2.dp),
            )
        }
    }
}

// ---------- §5.4 Карточка выбранного дня ----------

@Composable
private fun SelectedDayCard(
    state: HomeState,
    selectedMillis: Long,
    filterHabitId: String?,
    onHabitClick: (Habit) -> Unit,
    onOpenStepCounter: () -> Unit = {},
    onOpenWorkouts: () -> Unit = {},
) {
    val colors = AppTheme.colors
    val todayStart = System.currentTimeMillis().startOfDay()
    val isFuture = selectedMillis.startOfDay() > todayStart
    val list = state.habitsWithStatus.filter { filterHabitId == null || it.habit.id == filterHabitId }
    val done = list.count { it.completed == true }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .softShadow(AppTheme.elevation.cardRaised, AppTheme.shapes.cardL)
            .clip(AppTheme.shapes.cardL)
            .background(colors.surface)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = if (selectedMillis.startOfDay() == todayStart) "Сегодня" else dayMonthName(selectedMillis),
                style = AppTheme.type.h5, color = colors.textPrimary,
            )
            Text("$done из ${list.size} выполнено", style = AppTheme.type.label, color = colors.textSecondary)
        }

        if (list.isEmpty()) {
            Text("На этот день привычек нет", style = AppTheme.type.caption, color = colors.textSecondary)
        } else {
            list.forEach { hws ->
                val category = state.categories.firstOrNull { it.id == hws.habit.categoryId }
                val catColor = category?.let { Color(it.colorArgb) } ?: colors.accent
                val isDone = hws.completed == true
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickableNoRipple(onClick = { onHabitClick(hws.habit) }),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(11.dp),
                ) {
                    Box(
                        modifier = Modifier.size(34.dp).clip(RoundedCornerShape(12.dp)).background(catColor.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center,
                    ) { Text(hws.habit.emoji.ifBlank { habitEmoji(category) }, fontSize = 17.sp) }
                    Text(
                        text = hws.habit.title.trim(),
                        style = AppTheme.type.label.copy(
                            textDecoration = if (isDone) TextDecoration.LineThrough else null,
                        ),
                        color = if (isDone) colors.textDisabled else colors.textPrimary,
                        modifier = Modifier.weight(1f),
                    )
                    val (statusText, statusColor) = when {
                        isDone -> "Готово" to colors.success
                        isFuture -> "—" to colors.textTertiary
                        else -> "Пропуск" to colors.danger
                    }
                    Text(statusText, style = AppTheme.type.label, color = statusColor)
                }
            }
        }
    }
}

// ---------- Хелперы ----------

private val MONTHS = listOf(
    "Январь", "Февраль", "Март", "Апрель", "Май", "Июнь",
    "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь",
)
private val MONTHS_GEN = listOf(
    "января", "февраля", "марта", "апреля", "мая", "июня",
    "июля", "августа", "сентября", "октября", "ноября", "декабря",
)
private val WEEKDAYS_SHORT = mapOf(
    Calendar.MONDAY to "пн", Calendar.TUESDAY to "вт", Calendar.WEDNESDAY to "ср",
    Calendar.THURSDAY to "чт", Calendar.FRIDAY to "пт", Calendar.SATURDAY to "сб",
    Calendar.SUNDAY to "вс",
)

private fun monthName(month: Int) = MONTHS[month]
private fun weekdayShort(dow: Int) = WEEKDAYS_SHORT[dow] ?: ""

private fun weekdayOrder(firstDayOfWeek: Int): List<Int> =
    (0 until 7).map { ((firstDayOfWeek - 1 + it) % 7) + 1 }

private fun firstOfMonth(millis: Long): Long = Calendar.getInstance().apply {
    timeInMillis = millis
    set(Calendar.DAY_OF_MONTH, 1)
    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
}.timeInMillis

private fun addMonths(anchor: Long, delta: Int): Long = Calendar.getInstance().apply {
    timeInMillis = anchor
    add(Calendar.MONTH, delta)
}.timeInMillis

private fun dayStart(year: Int, month: Int, day: Int): Long = Calendar.getInstance().apply {
    clear(); set(year, month, day, 0, 0, 0)
}.timeInMillis

private fun Long.startOfDay(): Long = Calendar.getInstance().apply {
    timeInMillis = this@startOfDay
    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
}.timeInMillis

private fun dayMonthName(millis: Long): String {
    val cal = Calendar.getInstance().apply { timeInMillis = millis }
    return "${cal.get(Calendar.DAY_OF_MONTH)} ${MONTHS_GEN[cal.get(Calendar.MONTH)]}"
}

/** Запланирована ли привычка на дату (зеркалит HabitRepository.isHabitActiveOnDate). */
private fun isScheduledOn(habit: ru.plumsoftware.habitstracker.data.model.Habit, dayStart: Long): Boolean {
    val creation = Calendar.getInstance().apply {
        timeInMillis = habit.creationDate
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    if (dayStart < creation) return false
    if (habit.isByWeekdays) {
        val dow = Calendar.getInstance().apply { timeInMillis = dayStart }.get(Calendar.DAY_OF_WEEK)
        return habit.weekdaySet.contains(dow)
    }
    val daysBetween = (dayStart - creation) / (24 * 60 * 60 * 1000)
    return daysBetween % habit.periodicity.days.toLong() == 0L
}

private fun levelOf(progress: Float): Int = when {
    progress <= 0f -> 0
    progress < 0.34f -> 1
    progress < 0.67f -> 2
    else -> 3
}

private fun firstWord(title: String): String {
    val w = title.trim().substringBefore(' ')
    return if (w.length > 14) w.take(14) + "…" else w
}

private fun habitEmoji(category: Category?): String = when (category?.id) {
    Category.ID_HEALTH -> "💧"
    Category.ID_WORK -> "💼"
    Category.ID_STUDY -> "📚"
    Category.ID_PERSONAL -> "🌙"
    else -> "🎯"
}

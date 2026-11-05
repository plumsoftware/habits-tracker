package ru.plumsoftware.habitstracker.ui.screen.main.analytics

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.collectAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import ru.plumsoftware.habitstracker.ui.components.AppScreenTopBar
import ru.plumsoftware.habitstracker.data.database.WellnessRepository
import ru.plumsoftware.habitstracker.data.model.WorkoutSession
import org.koin.compose.koinInject
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.plumsoftware.habitstracker.data.model.Category
import ru.plumsoftware.habitstracker.data.model.Habit
import ru.plumsoftware.habitstracker.ui.components.AppIcon
import ru.plumsoftware.habitstracker.ui.components.HabitIcons
import ru.plumsoftware.habitstracker.ui.components.clickableNoRipple
import ru.plumsoftware.habitstracker.ui.screen.main.home.Achievement
import ru.plumsoftware.habitstracker.ui.screen.main.home.HomeState
import ru.plumsoftware.habitstracker.ui.theme.AppTheme
import ru.plumsoftware.habitstracker.ui.theme.softShadow
import java.util.Calendar

private enum class Period(val title: String, val hint: String) {
    WEEK("Неделя", "последние 7 дней"),
    MONTH("Месяц", "текущий месяц"),
    YEAR("Год", "текущий год"),
}

private data class Bar(val label: String, val value: Int)

/**
 * Экран «Аналитика» (ТЗ §10): переключатель периода, крупные плитки, график динамики,
 * инсайты, серии по привычкам, достижения, карточка «Закономерность».
 */
@Composable
fun AnalyticsScreen(
    state: HomeState,
    modifier: Modifier = Modifier,
    onHabitClick: (Habit) -> Unit = {},
) {
    val colors = AppTheme.colors
    val spacing = AppTheme.spacing
    var period by rememberSaveable { mutableStateOf(Period.WEEK) }
    var workoutsMode by rememberSaveable { mutableStateOf(false) }
    var achievementsOpen by rememberSaveable { mutableStateOf(false) }
    val habitsScroll = rememberLazyListState()
    val workoutsScroll = rememberLazyListState()
    val repository: WellnessRepository = koinInject()
    val sessions by remember(repository) { repository.workouts() }.collectAsState(initial = emptyList())
    BackHandler(enabled = achievementsOpen) { achievementsOpen = false }
    if (achievementsOpen) {
        Column(modifier.fillMaxSize().background(colors.bgScreen)
            .windowInsetsPadding(WindowInsets.safeDrawing)) {
            AppScreenTopBar("Достижения", onBack = { achievementsOpen = false })
            LazyColumn(
                contentPadding = PaddingValues(start = spacing.screenH, end = spacing.screenH,
                    bottom = spacing.screenBottomNav, top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(state.achievements, key = { it.title }) { achievement ->
                    AchievementRow(achievement)
                }
            }
        }
        return
    }

    val bars = remember(period, state) { barsForPeriod(period, state) }
    val checks = bars.sumOf { it.value }
    val rate = (state.monthlyStats.completionRate * 100).toInt()

    LazyColumn(
        state = if (workoutsMode) workoutsScroll else habitsScroll,
        modifier = modifier.fillMaxWidth().background(colors.bgScreen),
        contentPadding = PaddingValues(
            start = spacing.screenH, end = spacing.screenH,
            top = spacing.screenTop, bottom = spacing.screenBottomNav,
        ),
        verticalArrangement = Arrangement.spacedBy(spacing.sectionGap),
    ) {
        item { Text("Аналитика", style = AppTheme.type.h1, color = colors.textPrimary) }
        item {
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(colors.surface).padding(4.dp)) {
                listOf("Привычки", "Тренировки").forEachIndexed { index, title ->
                    val selected = workoutsMode == (index == 1)
                    Box(Modifier.weight(1f).clip(RoundedCornerShape(14.dp))
                        .background(if (selected) colors.accent else Color.Transparent)
                        .clickableNoRipple { workoutsMode = index == 1 }.padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center) {
                        Text(title, color = if (selected) colors.inkOn else colors.textSecondary, style = AppTheme.type.label)
                    }
                }
            }
        }
        item { PeriodSwitch(period = period, onSelect = { period = it }) }
        if (workoutsMode) {
            workoutAnalyticsItems(sessions, period)
        } else {
            item { MetricTiles(rate = rate, checks = checks, period = period) }
            item { DynamicsCard(bars = bars, period = period) }
            item { InsightTiles(state = state) }
            item { StreaksCard(state = state, onHabitClick = onHabitClick) }

            state.correlations.firstOrNull()?.let { corr ->
                item { PatternCard(fromTitle = corr.fromTitle, toTitle = corr.toTitle, rate = corr.rate) }
            }
        }
        item {
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(colors.surface)
                .clickableNoRipple { achievementsOpen = true }.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Достижения", style = AppTheme.type.h3, color = colors.textPrimary)
                    Text("${state.achievements.count { it.unlocked }} из ${state.achievements.size} открыто",
                        style = AppTheme.type.caption, color = colors.textSecondary)
                }
                Text("›", style = AppTheme.type.h2, color = colors.accent)
            }
        }
    }
}

/** Планшетная (альбомная) «Аналитика»: слева — период/метрики/динамика/инсайты,
 *  справа — серии, достижения, закономерность. */
@Composable
fun AnalyticsScreenTablet(
    state: HomeState,
    modifier: Modifier = Modifier,
    onHabitClick: (Habit) -> Unit = {},
) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        AnalyticsScreen(state = state, modifier = Modifier.widthIn(max = 840.dp).fillMaxSize(), onHabitClick = onHabitClick)
    }
}

// ---------- §10.1 Переключатель периода ----------

@Composable
private fun PeriodSwitch(period: Period, onSelect: (Period) -> Unit) {
    val colors = AppTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .softShadow(AppTheme.elevation.subtle, RoundedCornerShape(18.dp))
            .clip(RoundedCornerShape(18.dp))
            .background(colors.surface)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Period.entries.forEach { p ->
            val selected = p == period
            val bg by animateColorAsState(if (selected) colors.accent else Color.Transparent, label = "segbg")
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(15.dp))
                    .background(bg)
                    .clickableNoRipple(onClick = { onSelect(p) })
                    .padding(vertical = 11.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    p.title,
                    style = AppTheme.type.label,
                    color = if (selected) colors.inkOn else colors.textSecondary,
                )
            }
        }
    }
}

// ---------- §10.2 Две крупные плитки ----------

@Composable
private fun MetricTiles(rate: Int, checks: Int, period: Period) {
    val colors = AppTheme.colors
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(11.dp)) {
        Column(
            modifier = Modifier
                .weight(1f)
                .softShadow(AppTheme.elevation.accentGlow, RoundedCornerShape(26.dp))
                .clip(RoundedCornerShape(26.dp))
                .background(colors.accent)
                .padding(17.dp),
        ) {
            Text("Успешность", style = AppTheme.type.caption, color = colors.inkOn.copy(alpha = 0.7f))
            Spacer(Modifier.height(6.dp))
            Text("$rate%", style = AppTheme.type.numL, color = colors.inkOn)
            Spacer(Modifier.height(6.dp))
            Text("за ${period.title.lowercase()}", style = AppTheme.type.label, color = colors.tileMint)
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .softShadow(AppTheme.elevation.cardRaised, RoundedCornerShape(26.dp))
                .clip(RoundedCornerShape(26.dp))
                .background(colors.tileYellow)
                .padding(17.dp),
        ) {
            Text("Отметок", style = AppTheme.type.caption, color = colors.tileYellowText)
            Spacer(Modifier.height(6.dp))
            Text("$checks", style = AppTheme.type.numL, color = colors.ink)
            Spacer(Modifier.height(6.dp))
            Text("за ${period.title.lowercase()}", style = AppTheme.type.label, color = colors.tileYellowText)
        }
    }
}

// ---------- §10.3 График «Динамика» ----------

@Composable
private fun DynamicsCard(bars: List<Bar>, period: Period, title: String = "Динамика",
    emptyMessage: String = "Данных пока мало. Отмечайте привычки — график появится через пару дней") {
    val colors = AppTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .softShadow(AppTheme.elevation.cardRaised, AppTheme.shapes.cardL)
            .clip(AppTheme.shapes.cardL)
            .background(colors.surface)
            .padding(18.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(title, style = AppTheme.type.h5, color = colors.textPrimary)
            Text(period.hint, style = AppTheme.type.captionS, color = colors.textSecondary)
        }
        Spacer(Modifier.height(16.dp))
        if (bars.all { it.value == 0 }) {
            Text(
                emptyMessage,
                style = AppTheme.type.caption, color = colors.textSecondary,
            )
        } else {
            val max = (bars.maxOfOrNull { it.value } ?: 1).coerceAtLeast(1)
            var appeared by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) { appeared = true }
            Row(
                modifier = Modifier.fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .width((bars.size.coerceAtLeast(7) * 44).dp).height(150.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                bars.forEach { bar ->
                    val fraction = bar.value.toFloat() / max
                    val animated by animateFloatAsState(if (appeared) fraction else 0f, label = "bar")
                    val barColor = when {
                        fraction > 0.8f -> colors.accent
                        fraction > 0.6f -> colors.heat2
                        else -> colors.heat1
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom,
                    ) {
                        Text("${bar.value}", style = AppTheme.type.captionS, color = colors.textSecondary)
                        Spacer(Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height((animated * 110).dp.coerceAtLeast(4.dp))
                                .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp, bottomStart = 8.dp, bottomEnd = 8.dp))
                                .background(barColor)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(bar.label, style = AppTheme.type.captionS, color = colors.textSecondary)
                    }
                }
            }
        }
    }
}

// ---------- §10.4 Инсайты ----------

@Composable
private fun InsightTiles(state: HomeState) {
    val colors = AppTheme.colors
    val days = state.weeklyStats.days
    val best = days.maxByOrNull { it.completedCount }?.dayName ?: "–"
    val worst = days.minByOrNull { it.completedCount }?.dayName ?: "–"
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(11.dp)) {
        InsightTile(
            modifier = Modifier.weight(1f),
            bg = colors.tileGreen, icon = HabitIcons.trophy, iconTint = colors.success,
            title = best, subtitle = "Самый сильный день", subtitleColor = colors.tileGreenText,
        )
        InsightTile(
            modifier = Modifier.weight(1f),
            bg = colors.tilePeach, icon = HabitIcons.droplet, iconTint = colors.tilePeachText,
            title = worst, subtitle = "Чаще всего пропуск", subtitleColor = colors.tilePeachText,
        )
    }
}

@Composable
private fun InsightTile(
    modifier: Modifier,
    bg: Color, icon: Int, iconTint: Color,
    title: String, subtitle: String, subtitleColor: Color,
) {
    val colors = AppTheme.colors
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(bg)
            .padding(16.dp),
    ) {
        AppIcon(icon, tint = iconTint, size = 20.dp)
        Spacer(Modifier.height(8.dp))
        Text(title, style = AppTheme.type.body, color = colors.ink)
        Text(subtitle, style = AppTheme.type.caption, color = subtitleColor)
    }
}

// ---------- §10.5 Серии по привычкам ----------

@Composable
private fun StreaksCard(state: HomeState, onHabitClick: (Habit) -> Unit) {
    val colors = AppTheme.colors
    val items = state.allHabits
        .map { it to (state.habitStreaks[it.id] ?: 0) }
        .sortedByDescending { it.second }
        .take(5)
    val maxStreak = (items.maxOfOrNull { it.second } ?: 1).coerceAtLeast(1)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .softShadow(AppTheme.elevation.cardRaised, AppTheme.shapes.cardL)
            .clip(AppTheme.shapes.cardL)
            .background(colors.surface)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Серии по привычкам", style = AppTheme.type.h5, color = colors.textPrimary)
        if (items.isEmpty()) {
            Text("Пока нет активных привычек", style = AppTheme.type.caption, color = colors.textSecondary)
        } else {
            var appeared by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) { appeared = true }
            items.forEach { (habit, streak) ->
                val category = state.categories.firstOrNull { it.id == habit.categoryId }
                val catColor = category?.let { Color(it.colorArgb) } ?: colors.accent
                Row(
                    modifier = Modifier.fillMaxWidth().clickableNoRipple(onClick = { onHabitClick(habit) }),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(11.dp),
                ) {
                    Box(
                        modifier = Modifier.size(36.dp).clip(RoundedCornerShape(13.dp)).background(catColor.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center,
                    ) { Text(habit.emoji.ifBlank { habitEmoji(category) }, fontSize = 17.sp) }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(habit.title.trim(), style = AppTheme.type.label, color = colors.textPrimary, maxLines = 1)
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                AppIcon(HabitIcons.flame, tint = colors.warning, size = 12.dp)
                                Text("$streak", style = AppTheme.type.label, color = colors.warning)
                            }
                        }
                        val fraction by animateFloatAsState(if (appeared) streak.toFloat() / maxStreak else 0f, label = "streakbar")
                        Box(
                            modifier = Modifier.fillMaxWidth().height(7.dp).clip(AppTheme.shapes.pill).background(colors.surfaceMuted),
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(fraction.coerceIn(0f, 1f))
                                    .height(7.dp)
                                    .clip(AppTheme.shapes.pill)
                                    .background(catColor)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ---------- §10.6 Достижения ----------

@Composable
private fun AchievementRow(achievement: Achievement) {
    val colors = AppTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp))
            .background(colors.surface).padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(48.dp).clip(RoundedCornerShape(15.dp))
                .background(if (achievement.unlocked) colors.warningSoft else colors.accentSoft),
            contentAlignment = Alignment.Center,
        ) {
            Text(achievement.emoji, fontSize = 23.sp,
                modifier = Modifier.alpha(if (achievement.unlocked) 1f else 0.55f))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(achievement.title, style = AppTheme.type.h5, color = colors.textPrimary)
            Text(achievement.description, style = AppTheme.type.caption, color = colors.textSecondary)
            Text(achievement.status.replaceFirstChar { it.uppercase() }, style = AppTheme.type.captionS,
                color = if (achievement.unlocked) colors.success else colors.accent)
        }
    }
}

// ---------- §10.7 Закономерность ----------

@Composable
private fun PatternCard(fromTitle: String, toTitle: String, rate: Float) {
    val colors = AppTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(colors.tileLilac)
            .padding(18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AppIcon(HabitIcons.search, tint = colors.notice, size = 15.dp)
            Text("Закономерность", style = AppTheme.type.body, color = colors.ink)
        }
        Spacer(Modifier.height(7.dp))
        Text(
            "Когда вы отмечаете «$fromTitle», «$toTitle» выполняется на ${(rate * 100).toInt()}% чаще. Попробуйте связать их в пару.",
            style = AppTheme.type.caption, color = colors.tileLilacText,
        )
    }
}

// ---------- Данные графика ----------

private fun barsForPeriod(period: Period, state: HomeState): List<Bar> = when (period) {
    Period.WEEK -> state.weeklyStats.days.map { Bar(it.dayName, it.completedCount) }
        .ifEmpty { List(7) { Bar(shortDow(it), 0) } }

    Period.MONTH -> {
        // Группируем дни месяца по неделям (1н..5н)
        val byWeek = IntArray(5)
        state.monthlyStats.dailyCompletions.forEach { (day, count) ->
            val w = ((day - 1) / 7).coerceIn(0, 4)
            byWeek[w] += count
        }
        (0 until 5).map { Bar("${it + 1}н", byWeek[it]) }
    }

    Period.YEAR -> {
        // Группируем heatmap по кварталам
        val byQ = IntArray(4)
        val cal = Calendar.getInstance()
        state.heatmap.forEach { (ms, count) ->
            cal.timeInMillis = ms
            val q = cal.get(Calendar.MONTH) / 3
            byQ[q] += count
        }
        (0 until 4).map { Bar("Q${it + 1}", byQ[it]) }
    }
}

private fun shortDow(index: Int): String =
    listOf("пн", "вт", "ср", "чт", "пт", "сб", "вс").getOrElse(index) { "" }

private fun habitEmoji(category: Category?): String = when (category?.id) {
    Category.ID_HEALTH -> "💧"
    Category.ID_WORK -> "💼"
    Category.ID_STUDY -> "📚"
    Category.ID_PERSONAL -> "🌙"
    else -> "🎯"
}

private fun androidx.compose.foundation.lazy.LazyListScope.workoutAnalyticsItems(
    sessions: List<WorkoutSession>, period: Period,
) {
    val now = System.currentTimeMillis()
    val start = Calendar.getInstance().apply {
        timeInMillis = now
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        when (period) {
            Period.WEEK -> add(Calendar.DAY_OF_YEAR, -6)
            Period.MONTH -> set(Calendar.DAY_OF_MONTH, 1)
            Period.YEAR -> { set(Calendar.DAY_OF_YEAR, 1) }
        }
    }
    val completed = sessions.filter { it.endedAtMillis != null && it.startedAtMillis >= start.timeInMillis && it.startedAtMillis <= now }
    fun minutes(session: WorkoutSession) = ((session.endedAtMillis!! - session.startedAtMillis).coerceAtLeast(0) / 60_000)
    val total = completed.sumOf { minutes(it) }
    val activeDays = completed.map { java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.ROOT).format(java.util.Date(it.startedAtMillis)) }.distinct().size
    val buckets = when (period) {
        Period.WEEK -> 7
        Period.MONTH -> (start.getActualMaximum(Calendar.DAY_OF_MONTH) + 6) / 7
        Period.YEAR -> 12
    }
    val bars = (0 until buckets).map { index ->
        val from = (start.clone() as Calendar).apply {
            when (period) {
                Period.WEEK -> add(Calendar.DAY_OF_YEAR, index)
                Period.MONTH -> add(Calendar.DAY_OF_YEAR, index * 7)
                Period.YEAR -> add(Calendar.MONTH, index)
            }
        }
        val to = (from.clone() as Calendar).apply {
            when (period) {
                Period.WEEK -> add(Calendar.DAY_OF_YEAR, 1)
                Period.MONTH -> { add(Calendar.DAY_OF_YEAR, 7)
                    val nextMonth = (start.clone() as Calendar).apply { add(Calendar.MONTH, 1) }
                    if (timeInMillis > nextMonth.timeInMillis) timeInMillis = nextMonth.timeInMillis
                }
                Period.YEAR -> add(Calendar.MONTH, 1)
            }
        }
        val label = when (period) {
            Period.WEEK -> java.text.SimpleDateFormat("EE", java.util.Locale("ru")).format(from.time)
            Period.MONTH -> "${index + 1} нед."
            Period.YEAR -> "${index + 1}"
        }
        Bar(label, completed.filter { it.startedAtMillis >= from.timeInMillis && it.startedAtMillis < to.timeInMillis }
            .sumOf { minutes(it) }.coerceAtMost(Int.MAX_VALUE.toLong()).toInt())
    }
    item {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            WorkoutMetric("Тренировок", completed.size.toString(), true, Modifier.weight(1f))
            WorkoutMetric("Минут активности", total.toString(), false, Modifier.weight(1f))
        }
    }
    item { DynamicsCard(bars, period, "Время тренировок, мин", "Заверши тренировку — здесь появится время активности за выбранный период") }
    item {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            WorkoutMetric("Средняя, мин", if (completed.isEmpty()) "–" else (total / completed.size).toString(), false, Modifier.weight(1f))
            WorkoutMetric("Дней активности", activeDays.toString(), false, Modifier.weight(1f))
        }
    }
    item {
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(AppTheme.colors.surface).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Последние тренировки", style = AppTheme.type.h3, color = AppTheme.colors.textPrimary)
            if (completed.isEmpty()) Text("Нет завершённых тренировок за этот период", color = AppTheme.colors.textSecondary)
            completed.sortedByDescending { it.startedAtMillis }.take(5).forEach { session ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text(session.title, color = AppTheme.colors.textPrimary, style = AppTheme.type.label)
                        Text(java.text.SimpleDateFormat("d MMM", java.util.Locale("ru")).format(java.util.Date(session.startedAtMillis)),
                            color = AppTheme.colors.textSecondary, style = AppTheme.type.caption)
                    }
                    Text("${minutes(session)} мин", color = AppTheme.colors.accent, style = AppTheme.type.label)
                }
            }
        }
    }
}

@Composable
private fun WorkoutMetric(title: String, value: String, accent: Boolean, modifier: Modifier) {
    val colors = AppTheme.colors
    Column(modifier.clip(RoundedCornerShape(26.dp)).background(if (accent) colors.accent else colors.surface)
        .padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(title, style = AppTheme.type.caption, color = if (accent) colors.inkOn else colors.textSecondary)
        Text(value, style = AppTheme.type.numL, color = if (accent) colors.inkOn else colors.textPrimary)
    }
}

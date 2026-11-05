package ru.plumsoftware.habitstracker.ui.screen.main.today

import ru.plumsoftware.habitstracker.ui.components.ActivityShortcuts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.plumsoftware.habitstracker.data.model.Category
import ru.plumsoftware.habitstracker.data.model.Habit
import ru.plumsoftware.habitstracker.data.model.HabitWithStatus
import ru.plumsoftware.habitstracker.ui.components.AppIcon
import ru.plumsoftware.habitstracker.ui.components.HabitIcons
import ru.plumsoftware.habitstracker.ui.components.PillChip
import ru.plumsoftware.habitstracker.ui.components.clickableNoRipple
import ru.plumsoftware.habitstracker.ui.screen.main.home.DailyStats
import ru.plumsoftware.habitstracker.ui.screen.main.home.HomeState
import ru.plumsoftware.habitstracker.ui.theme.AppTheme
import ru.plumsoftware.habitstracker.ui.theme.EaseOut
import ru.plumsoftware.habitstracker.ui.theme.softShadow
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Главный экран «Сегодня» (ТЗ §4). Вертикальный скролл: хедер → баннер прогресса →
 * недельная полоса → список привычек → блок «Добавить из шаблонов».
 */
@Composable
fun TodayScreen(
    state: HomeState,
    modifier: Modifier = Modifier,
    onSelectDate: (Long) -> Unit,
    onToggleDone: (habitId: String, done: Boolean) -> Unit,
    onHabitClick: (Habit) -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenTemplates: () -> Unit,
    hasUnread: Boolean = false,
    onOpenStepCounter: () -> Unit = {},
    onOpenWorkouts: () -> Unit = {},
    onOpenSleep: () -> Unit = {},
) {
    val colors = AppTheme.colors
    val spacing = AppTheme.spacing
    val habits = state.habitsWithStatus
    val selectedMillis = if (state.selectedDate > 0L) state.selectedDate else System.currentTimeMillis()
    val isToday = isSameDay(selectedMillis, System.currentTimeMillis())

    // Индикатор только при самой первой загрузке (не мигает при отметках)
    var everLoaded by remember { mutableStateOf(false) }
    LaunchedEffect(state.isLoading) { if (!state.isLoading) everLoaded = true }
    if (state.isLoading && habits.isEmpty() && !everLoaded) {
        LoadingScreen(modifier)
        return
    }

    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.bgScreen),
        contentPadding = PaddingValues(
            start = spacing.screenH,
            end = spacing.screenH,
            top = spacing.screenTop,
            bottom = spacing.screenBottomNav,
        ),
        verticalArrangement = Arrangement.spacedBy(spacing.sectionGap),
    ) {
        item { Header(onOpenNotifications = onOpenNotifications, hasUnread = hasUnread) }

        item { WeekStrip(state = state, onSelectDate = onSelectDate) }
        if (habits.isNotEmpty()) item {
            ProgressBanner(dailyStats = state.dailyStats, streak = state.overallStreak)
        }
        item { ActivityShortcuts(selectedMillis, onOpenStepCounter, onOpenWorkouts, onOpenSleep, filledIconSize = 40.dp, emptyIconFraction = 0.54f) }
        item { TemplatesBlock(onOpenTemplates = onOpenTemplates) }

        if (habits.isEmpty()) {
            item { EmptyState() }
        } else {
            item { ListHeader(dailyStats = state.dailyStats, isToday = isToday, selectedMillis = selectedMillis) }
            val ordered = orderChains(habits)
            items(ordered, key = { it.hws.habit.id }) { row ->
                val hws = row.hws
                val category = state.categories.firstOrNull { it.id == hws.habit.categoryId }
                val readOnly = selectedMillis.startOfDay() > System.currentTimeMillis().startOfDay()
                // §7.4.1: пока родитель не закрыт — показываем «после «родитель»»
                val parent = hws.habit.chainedAfterId?.let { pid -> state.allHabits.firstOrNull { it.id == pid } }
                val parentDone = parent?.let { p -> habits.firstOrNull { it.habit.id == p.id }?.completed == true } ?: true
                val chainLabel = if (parent != null && !parentDone) "после «${parent.title.trim()}»" else null
                ChainConnector(indent = row.indent, modifier = Modifier.animateItem()) {
                    SwipeableHabit(
                        isDone = hws.completed ?: false,
                        enabled = !readOnly,
                        onMark = { onToggleDone(hws.habit.id, true) },
                        onSkip = { onToggleDone(hws.habit.id, false) },
                    ) {
                        HabitRow(
                            hws = hws,
                            category = category,
                            streak = state.habitStreaks[hws.habit.id] ?: 0,
                            readOnly = readOnly,
                            chainLabel = chainLabel,
                            onClick = { onHabitClick(hws.habit) },
                            onToggle = { done -> onToggleDone(hws.habit.id, done) },
                        )
                    }
                }
            }
        }
    }
}

// ---------- Цепочки: группировка и коннектор (§7.4.1) ----------

private data class ChainRowItem(val hws: HabitWithStatus, val indent: Int)

/** Упорядочивает список так, чтобы потомки цепочки шли под родителем с отступом. */
private fun orderChains(habits: List<HabitWithStatus>): List<ChainRowItem> {
    val byId = habits.associateBy { it.habit.id }
    val childrenByParent = habits.groupBy { it.habit.chainedAfterId }
    val result = mutableListOf<ChainRowItem>()
    val visited = mutableSetOf<String>()
    fun add(item: HabitWithStatus, indent: Int) {
        if (!visited.add(item.habit.id)) return
        result.add(ChainRowItem(item, indent))
        childrenByParent[item.habit.id]?.forEach { add(it, indent + 1) }
    }
    habits.forEach { h ->
        val pid = h.habit.chainedAfterId
        if (pid == null || byId[pid] == null) add(h, 0)
    }
    habits.forEach { if (it.habit.id !in visited) result.add(ChainRowItem(it, 0)) }
    return result
}

@Composable
private fun ChainConnector(indent: Int, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    if (indent <= 0) { Box(modifier) { content() }; return }
    val colors = AppTheme.colors
    Row(
        modifier = modifier.fillMaxWidth().height(androidx.compose.foundation.layout.IntrinsicSize.Min),
    ) {
        Box(
            modifier = Modifier.width((indent * 20).dp).fillMaxHeight(),
            contentAlignment = Alignment.CenterStart,
        ) {
            // Вертикальная линия-стек
            Box(
                modifier = Modifier
                    .padding(start = ((indent - 1) * 20 + 8).dp)
                    .width(2.dp)
                    .fillMaxHeight()
                    .background(colors.noticeSoft)
            )
            // Стрелка в точке стыка
            AppIcon(HabitIcons.chainArrow, tint = colors.notice, size = 14.dp,
                modifier = Modifier.padding(start = ((indent - 1) * 20 + 3).dp))
        }
        Box(Modifier.weight(1f)) { content() }
    }
}

@Composable
private fun LoadingScreen(modifier: Modifier = Modifier) {
    val colors = AppTheme.colors
    Box(
        modifier = modifier.fillMaxSize().background(colors.bgScreen),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.material3.CircularProgressIndicator(
            color = colors.accent,
            trackColor = colors.accentSoft,
            strokeWidth = 4.dp,
        )
    }
}

// ---------- Свайпы по карточке (§4.5) ----------

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableHabit(
    isDone: Boolean,
    enabled: Boolean,
    onMark: () -> Unit,
    onSkip: () -> Unit,
    content: @Composable () -> Unit,
) {
    val colors = AppTheme.colors
    val dismissState = androidx.compose.material3.rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (!enabled) return@rememberSwipeToDismissBoxState false
            when (value) {
                androidx.compose.material3.SwipeToDismissBoxValue.StartToEnd -> { if (!isDone) onMark() }
                androidx.compose.material3.SwipeToDismissBoxValue.EndToStart -> { if (isDone) onSkip() }
                else -> {}
            }
            false // не убираем карточку — действие выполнено, возвращаем на место
        },
        positionalThreshold = { total -> total * 0.35f },
    )
    androidx.compose.material3.SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = enabled && !isDone,
        enableDismissFromEndToStart = enabled && isDone,
        backgroundContent = {
            val dir = dismissState.dismissDirection
            // Показываем подложку только во время свайпа (в покое — прозрачно, чтобы не «просвечивало»)
            if (dir != androidx.compose.material3.SwipeToDismissBoxValue.Settled) {
                val toStart = dir == androidx.compose.material3.SwipeToDismissBoxValue.StartToEnd
                val bg = if (toStart) colors.successSoft else colors.dangerSoft
                val icon = if (toStart) HabitIcons.check else HabitIcons.close
                val tint = if (toStart) colors.success else colors.danger
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(AppTheme.shapes.card)
                        .background(bg)
                        .padding(horizontal = 22.dp),
                    horizontalArrangement = if (toStart) Arrangement.Start else Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) { AppIcon(icon, tint = tint, size = 22.dp) }
            }
        },
        content = { content() },
    )
}

// ---------- Хедер ----------

@Composable
private fun Header(onOpenNotifications: () -> Unit, hasUnread: Boolean) {
    val colors = AppTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
    ) {
        Column(Modifier.weight(1f)) {
            Text(dateLine(), style = AppTheme.type.caption, color = colors.textSecondary)
            Spacer(Modifier.height(2.dp))
            Text(greeting(), style = AppTheme.type.h1, color = colors.textPrimary)
        }
        Box(
            modifier = Modifier
                .size(44.dp)
                .softShadow(AppTheme.elevation.subtle, RoundedCornerShape(16.dp))
                .clip(RoundedCornerShape(16.dp))
                .background(colors.surface)
                .clickableNoRipple(onClick = onOpenNotifications),
            contentAlignment = Alignment.Center,
        ) {
            AppIcon(HabitIcons.bell, tint = colors.textPrimary, size = 19.dp)
            if (hasUnread) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 9.dp, end = 9.dp)
                        .size(8.dp)
                        .clip(AppTheme.shapes.pill)
                        .background(colors.badgeDot)
                )
            }
        }
    }
}

// ---------- Баннер прогресса ----------

@Composable
private fun ProgressBanner(dailyStats: DailyStats, streak: Int) {
    val colors = AppTheme.colors
    val total = dailyStats.totalHabits
    val done = dailyStats.completedHabits
    val pct = if (total > 0) (done * 100f / total) else 0f
    // Анимация запускается по появлению экрана: кольцо растёт от 0 до pct
    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appeared = true }
    val animatedPct by animateFloatAsState(
        targetValue = if (appeared) pct else 0f,
        animationSpec = androidx.compose.animation.core.tween(AppTheme.motion.slow, easing = EaseOut),
        label = "pct",
    )
    val isPerfect = total > 0 && done >= total

    // §13.4 Празднование: один раз при достижении 100%
    var celebrate by remember { mutableStateOf(false) }
    var played by remember { mutableStateOf(false) }
    LaunchedEffect(isPerfect) {
        if (isPerfect && !played) { celebrate = true; played = true }
        if (!isPerfect) played = false
    }
    val ringScale by animateFloatAsState(if (celebrate) 1.08f else 1f, label = "ringScale")

    Box {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .softShadow(AppTheme.elevation.accentGlow, AppTheme.shapes.cardL)
            .clip(AppTheme.shapes.cardL)
            .background(colors.accent)
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        ProgressRing(pct = animatedPct, scale = ringScale)
        Column(Modifier.weight(1f)) {
            Text(
                text = if (isPerfect) "Идеальный день 🎉" else "$done из $total выполнено",
                style = AppTheme.type.h4,
                color = colors.inkOn,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = if (isPerfect) "Все привычки закрыты"
                else "Осталось ${(total - done).coerceAtLeast(0)} — вы почти там",
                style = AppTheme.type.caption,
                color = colors.inkOn.copy(alpha = 0.75f),
            )
            if (streak > 0) {
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .clip(AppTheme.shapes.pill)
                        .background(Color.White.copy(alpha = 0.18f))
                        .padding(horizontal = 11.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    AppIcon(HabitIcons.flame, tint = colors.inkOn, size = 14.dp)
                    Text("$streak ${dayWord(streak)} подряд", style = AppTheme.type.label, color = colors.inkOn)
                }
            }
        }
    }
        if (celebrate) Confetti(Modifier.matchParentSize()) { celebrate = false }
    }
}

@Composable
private fun Confetti(modifier: Modifier = Modifier, onEnd: () -> Unit) {
    val palette = listOf(Color(0xFFFFE8A3), Color(0xFFC9F2DC), Color(0xFFFFD7C2), Color(0xFFFFFFFF))
    val seeds = remember {
        List(18) {
            val angle = (Math.random() * 2 * Math.PI).toFloat()
            val speed = (0.5f + Math.random() * 0.5f).toFloat()
            Triple(angle, speed, palette[(Math.random() * palette.size).toInt()])
        }
    }
    val progress = remember { androidx.compose.animation.core.Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, androidx.compose.animation.core.tween(420))
        onEnd()
    }
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val p = progress.value
        val originX = 20.dp.toPx() + 44.dp.toPx() // центр кольца по горизонтали
        val originY = size.height / 2f
        val r = 6.dp.toPx() * (1f - p * 0.3f)
        seeds.forEach { (angle, speed, c) ->
            val dist = p * speed * size.width * 0.5f
            val x = originX + kotlin.math.cos(angle) * dist
            val y = originY + kotlin.math.sin(angle) * dist + p * p * 40.dp.toPx()
            drawCircle(color = c, radius = r, center = Offset(x, y), alpha = (1f - p).coerceIn(0f, 1f))
        }
    }
}

@Composable
private fun ProgressRing(pct: Float, scale: Float = 1f) {
    val colors = AppTheme.colors
    val stroke = with(androidx.compose.ui.platform.LocalDensity.current) { 9.dp.toPx() }
    Box(modifier = Modifier.size(88.dp).scale(scale), contentAlignment = Alignment.Center) {
        androidx.compose.foundation.Canvas(modifier = Modifier.size(88.dp)) {
            val d = size.minDimension
            val topLeft = Offset((size.width - d) / 2f + stroke / 2f, (size.height - d) / 2f + stroke / 2f)
            val arcSize = Size(d - stroke, d - stroke)
            drawArc(
                color = Color.White.copy(alpha = 0.28f),
                startAngle = 0f, sweepAngle = 360f, useCenter = false,
                topLeft = topLeft, size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
            drawArc(
                color = Color.White,
                startAngle = -90f, sweepAngle = pct / 100f * 360f, useCenter = false,
                topLeft = topLeft, size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
        }
        Text("${pct.toInt()}%", style = AppTheme.type.numM, color = colors.inkOn)
    }
}

// ---------- Недельная полоса ----------

@Composable
private fun WeekStrip(state: HomeState, onSelectDate: (Long) -> Unit) {
    val colors = AppTheme.colors
    val days = weekDays(state.selectedDate, state.firstDayOfWeek)
    val selectedStart = (if (state.selectedDate > 0L) state.selectedDate else System.currentTimeMillis()).startOfDay()
    val todayStart = System.currentTimeMillis().startOfDay()

    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(days) { millis ->
            val start = millis.startOfDay()
            val selected = start == selectedStart
            val isFuture = start > todayStart
            val progress = state.dayProgress[start] ?: 0f
            val fullyDone = progress >= 1f

            val cal = Calendar.getInstance().apply { timeInMillis = millis }
            val dayNum = cal.get(Calendar.DAY_OF_MONTH)
            val dayShort = shortWeekday(cal.get(Calendar.DAY_OF_WEEK))

            val bg by animateColorAsState(if (selected) colors.accent else colors.surface, label = "daybg")
            Column(
                modifier = Modifier
                    .width(52.dp)
                    .softShadow(if (selected) AppTheme.elevation.accentButton else AppTheme.elevation.subtle, AppTheme.shapes.dayCell)
                    .clip(AppTheme.shapes.dayCell)
                    .background(bg)
                    .clickableNoRipple(onClick = { onSelectDate(millis) })
                    .padding(top = 11.dp, bottom = 9.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Text(
                    text = dayShort,
                    style = AppTheme.type.captionS,
                    color = (if (selected) colors.inkOn else colors.textSecondary).copy(alpha = if (isFuture) 0.5f else 0.65f),
                )
                Text(
                    text = "$dayNum",
                    style = AppTheme.type.numM.copy(fontSize = 16.sp),
                    color = (if (selected) colors.inkOn else colors.textPrimary).copy(alpha = if (isFuture) 0.5f else 1f),
                )
                Box(
                    modifier = Modifier
                        .width(18.dp)
                        .height(4.dp)
                        .clip(AppTheme.shapes.pill)
                        .background(
                            when {
                                isFuture -> Color.Transparent
                                selected -> Color.White.copy(alpha = 0.55f)
                                fullyDone -> colors.accent
                                else -> colors.checkboxOutline
                            }
                        )
                )
            }
        }
    }
}

// ---------- Заголовок списка ----------

@Composable
private fun ListHeader(dailyStats: DailyStats, isToday: Boolean, selectedMillis: Long) {
    val colors = AppTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = if (isToday) "Привычки на сегодня" else "Привычки, ${dayMonth(selectedMillis)}",
            style = AppTheme.type.h4,
            color = colors.textPrimary,
        )
        Text(
            text = "${dailyStats.completedHabits}/${dailyStats.totalHabits}",
            style = AppTheme.type.label,
            color = colors.textSecondary,
        )
    }
}

// ---------- Карточка привычки ----------

@Composable
private fun HabitRow(
    hws: HabitWithStatus,
    category: Category?,
    streak: Int,
    readOnly: Boolean,
    chainLabel: String? = null,
    onClick: () -> Unit,
    onToggle: (Boolean) -> Unit,
) {
    val colors = AppTheme.colors
    val isDone = hws.completed ?: false
    val catColor = category?.let { Color(it.colorArgb) } ?: colors.accent
    val catSoft = catColor.copy(alpha = 0.14f)
    val emoji = hws.habit.emoji.ifBlank { habitEmoji(category) }
    val goal = hws.habit.targetCount.coerceAtLeast(1)
    val hasGoal = goal > 1

    val cardBg by animateColorAsState(
        if (isDone) colors.surfaceTranslucent else colors.surface,
        animationSpec = androidx.compose.animation.core.tween(AppTheme.motion.fast),
        label = "cardbg",
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .softShadow(if (isDone) ru.plumsoftware.habitstracker.ui.theme.ShadowSpec.None else AppTheme.elevation.card, AppTheme.shapes.card)
            .clip(AppTheme.shapes.card)
            .background(cardBg)
            .clickableNoRipple(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(AppTheme.shapes.iconTile)
                .background(catSoft),
            contentAlignment = Alignment.Center,
        ) { Text(emoji, fontSize = 24.sp) }

        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = hws.habit.title.trim(),
                style = AppTheme.type.body.copy(
                    textDecoration = if (isDone) TextDecoration.LineThrough else null,
                ),
                color = if (isDone) colors.textDisabled else colors.textPrimary,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                if (chainLabel != null) {
                    AppIcon(HabitIcons.chainArrow, tint = colors.notice, size = 12.dp)
                    Text(chainLabel, style = AppTheme.type.caption, color = colors.notice)
                } else {
                    Text(reminderTime(hws.habit), style = AppTheme.type.caption, color = colors.textSecondary)
                    if (category != null) {
                        Box(Modifier.size(4.dp).clip(AppTheme.shapes.pill).background(colors.sheetHandle))
                        Text(category.name, style = AppTheme.type.caption, color = catColor)
                    }
                    if (hasGoal) {
                        // §4.5: вместо стрика — прогресс к цели в день
                        Box(Modifier.size(4.dp).clip(AppTheme.shapes.pill).background(colors.sheetHandle))
                        Text("${hws.completedCount}/$goal", style = AppTheme.type.label, color = catColor)
                    } else if (streak > 0) {
                        AppIcon(HabitIcons.flame, tint = colors.warning, size = 12.dp)
                        Text("$streak", style = AppTheme.type.label, color = colors.warning)
                    }
                }
            }
            if (hasGoal) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(AppTheme.shapes.pill).background(colors.accentSoft),
                ) {
                    val frac = (hws.completedCount.toFloat() / goal).coerceIn(0f, 1f)
                    Box(Modifier.fillMaxWidth(frac).height(4.dp).clip(AppTheme.shapes.pill).background(catColor))
                }
            }
        }

        Checkbox(isDone = isDone, enabled = !readOnly, accent = colors.accent) {
            // Для цели>1 тап засчитывает ещё одну отметку; когда набрано — снимает (сброс)
            if (hasGoal && !isDone) onToggle(true) else onToggle(!isDone)
        }
    }
}

@Composable
private fun Checkbox(isDone: Boolean, enabled: Boolean, accent: Color, onClick: () -> Unit) {
    val colors = AppTheme.colors
    val scale by animateFloatAsState(if (isDone) 1f else 1f, label = "cb")
    Box(
        modifier = Modifier
            .size(36.dp)
            .scale(scale)
            .clip(AppTheme.shapes.pill)
            .background(if (isDone) accent else Color.Transparent)
            .then(
                if (isDone) Modifier
                else Modifier.border(2.dp, colors.checkboxOutline, AppTheme.shapes.pill)
            )
            .clickableNoRipple(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (isDone) AppIcon(HabitIcons.check, tint = colors.inkOn, size = 15.dp)
    }
}

// ---------- Блок «Добавить из шаблонов» ----------

@Composable
private fun TemplatesBlock(onOpenTemplates: () -> Unit) {
    val colors = AppTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AppTheme.shapes.cardS)
            .dashedBorder(colors.outlineDashed, AppTheme.shapes.cardS)
            .background(colors.surface.copy(alpha = 0.55f))
            .clickableNoRipple(onClick = onOpenTemplates)
            .padding(15.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(14.dp)).background(colors.accentSoft),
            contentAlignment = Alignment.Center,
        ) { AppIcon(HabitIcons.sparkles, tint = colors.accent, size = 19.dp) }
        Column(Modifier.weight(1f)) {
            Text("Добавить из шаблонов", style = AppTheme.type.bodyS, color = colors.textPrimary)
            Text("Готовые привычки и подборки", style = AppTheme.type.caption, color = colors.textSecondary)
        }
        AppIcon(HabitIcons.chevronRight, tint = colors.accent, size = 16.dp)
    }
}

// ---------- Empty state ----------

@Composable
private fun EmptyState() {
    val colors = AppTheme.colors
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            modifier = Modifier
                .size(148.dp)
                .softShadow(AppTheme.elevation.cardRaised, RoundedCornerShape(44.dp))
                .clip(RoundedCornerShape(44.dp))
                .background(colors.surface),
            contentAlignment = Alignment.Center,
        ) { Text("🌱", fontSize = 66.sp) }
        Text("Здесь появятся ваши привычки", style = AppTheme.type.h4, color = colors.textPrimary)
        Text(
            "Начните с готового шаблона — это 10 секунд",
            style = AppTheme.type.caption, color = colors.textSecondary,
        )
    }
}

/**
 * Планшетная (альбомная) версия «Сегодня» (§4): слева — герой-панель (приветствие, кольцо
 * прогресса, недельная полоса, шаблоны), справа — прокручиваемый список привычек.
 */
@Composable
fun TodayScreenTablet(
    state: HomeState,
    modifier: Modifier = Modifier,
    onSelectDate: (Long) -> Unit,
    onToggleDone: (habitId: String, done: Boolean) -> Unit,
    onHabitClick: (Habit) -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenTemplates: () -> Unit,
    hasUnread: Boolean = false,
    onOpenStepCounter: () -> Unit = {},
    onOpenWorkouts: () -> Unit = {},
    onOpenSleep: () -> Unit = {},
) {
    val colors = AppTheme.colors
    val spacing = AppTheme.spacing
    val habits = state.habitsWithStatus
    val selectedMillis = if (state.selectedDate > 0L) state.selectedDate else System.currentTimeMillis()
    val isToday = isSameDay(selectedMillis, System.currentTimeMillis())

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.bgScreen)
            .padding(horizontal = spacing.screenH, vertical = spacing.screenTop),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        // Левая герой-панель
        Column(
            modifier = Modifier
                .width(360.dp)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(spacing.sectionGap),
        ) {
            Header(onOpenNotifications = onOpenNotifications, hasUnread = hasUnread)
            WeekStrip(state = state, onSelectDate = onSelectDate)
            if (habits.isNotEmpty()) ProgressBanner(dailyStats = state.dailyStats, streak = state.overallStreak)
            ActivityShortcuts(selectedMillis, onOpenStepCounter, onOpenWorkouts, onOpenSleep, filledIconSize = 40.dp, emptyIconFraction = 0.54f)
            TemplatesBlock(onOpenTemplates = onOpenTemplates)
            Spacer(Modifier.height(spacing.screenBottomPlain))
        }

        // Правая колонка — список привычек
        if (habits.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                EmptyState()
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                contentPadding = PaddingValues(bottom = spacing.screenBottomPlain),
                verticalArrangement = Arrangement.spacedBy(spacing.listGap),
            ) {
                item { ListHeader(dailyStats = state.dailyStats, isToday = isToday, selectedMillis = selectedMillis) }
                val ordered = orderChains(habits)
                items(ordered, key = { it.hws.habit.id }) { row ->
                    val hws = row.hws
                    val category = state.categories.firstOrNull { it.id == hws.habit.categoryId }
                    val readOnly = selectedMillis.startOfDay() > System.currentTimeMillis().startOfDay()
                    val parent = hws.habit.chainedAfterId?.let { pid -> state.allHabits.firstOrNull { it.id == pid } }
                    val parentDone = parent?.let { p -> habits.firstOrNull { it.habit.id == p.id }?.completed == true } ?: true
                    val chainLabel = if (parent != null && !parentDone) "после «${parent.title.trim()}»" else null
                    ChainConnector(indent = row.indent, modifier = Modifier.animateItem()) {
                        SwipeableHabit(
                            isDone = hws.completed ?: false,
                            enabled = !readOnly,
                            onMark = { onToggleDone(hws.habit.id, true) },
                            onSkip = { onToggleDone(hws.habit.id, false) },
                        ) {
                            HabitRow(
                                hws = hws,
                                category = category,
                                streak = state.habitStreaks[hws.habit.id] ?: 0,
                                readOnly = readOnly,
                                chainLabel = chainLabel,
                                onClick = { onHabitClick(hws.habit) },
                                onToggle = { done -> onToggleDone(hws.habit.id, done) },
                            )
                        }
                    }
                }
            }
        }
    }
}

// ---------- Хелперы ----------

private val MONTHS_GEN = listOf(
    "января", "февраля", "марта", "апреля", "мая", "июня",
    "июля", "августа", "сентября", "октября", "ноября", "декабря",
)
private val WEEKDAYS_FULL = mapOf(
    Calendar.MONDAY to "Понедельник", Calendar.TUESDAY to "Вторник", Calendar.WEDNESDAY to "Среда",
    Calendar.THURSDAY to "Четверг", Calendar.FRIDAY to "Пятница", Calendar.SATURDAY to "Суббота",
    Calendar.SUNDAY to "Воскресенье",
)
private val WEEKDAYS_SHORT = mapOf(
    Calendar.MONDAY to "пн", Calendar.TUESDAY to "вт", Calendar.WEDNESDAY to "ср",
    Calendar.THURSDAY to "чт", Calendar.FRIDAY to "пт", Calendar.SATURDAY to "сб",
    Calendar.SUNDAY to "вс",
)

private fun shortWeekday(calDow: Int): String = WEEKDAYS_SHORT[calDow] ?: ""

private fun dateLine(): String {
    val cal = Calendar.getInstance()
    val dow = WEEKDAYS_FULL[cal.get(Calendar.DAY_OF_WEEK)] ?: ""
    val day = cal.get(Calendar.DAY_OF_MONTH)
    val month = MONTHS_GEN[cal.get(Calendar.MONTH)]
    return "$dow, $day $month"
}

private fun greeting(): String {
    val h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when {
        h < 11 -> "Доброе утро 👋"
        h < 17 -> "Добрый день 👋"
        h < 23 -> "Добрый вечер 👋"
        else -> "Доброй ночи 🌙"
    }
}

private fun dayMonth(millis: Long): String {
    val cal = Calendar.getInstance().apply { timeInMillis = millis }
    return "${cal.get(Calendar.DAY_OF_MONTH)} ${MONTHS_GEN[cal.get(Calendar.MONTH)]}"
}

/** 7 дней недели, содержащей [selectedDate] (или сегодня), начиная с [firstDayOfWeek]. */
private fun weekDays(selectedDate: Long, firstDayOfWeek: Int): List<Long> {
    val cal = Calendar.getInstance().apply {
        timeInMillis = if (selectedDate > 0L) selectedDate else System.currentTimeMillis()
        this.firstDayOfWeek = firstDayOfWeek
    }
    // Откатываемся к первому дню недели
    while (cal.get(Calendar.DAY_OF_WEEK) != firstDayOfWeek) {
        cal.add(Calendar.DAY_OF_MONTH, -1)
    }
    return (0 until 7).map { i ->
        (cal.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, i) }.timeInMillis
    }
}

private fun Long.startOfDay(): Long {
    val cal = Calendar.getInstance().apply {
        timeInMillis = this@startOfDay
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }
    return cal.timeInMillis
}

private fun isSameDay(a: Long, b: Long): Boolean = a.startOfDay() == b.startOfDay()

private fun reminderTime(habit: Habit): String {
    if (habit.reminderTimeHour < 0) return "Весь день"
    val hh = habit.reminderTimeHour.toString().padStart(2, '0')
    val mm = habit.reminderTimeMinute.toString().padStart(2, '0')
    return "$hh:$mm"
}

private fun dayWord(n: Int): String {
    val mod100 = n % 100
    val mod10 = n % 10
    return when {
        mod100 in 11..14 -> "дней"
        mod10 == 1 -> "день"
        mod10 in 2..4 -> "дня"
        else -> "дней"
    }
}

/** Эмодзи-иконка привычки. Пока нет поля emoji в модели — берём по встроенной категории. */
private fun habitEmoji(category: Category?): String = when (category?.id) {
    Category.ID_HEALTH -> "💧"
    Category.ID_WORK -> "💼"
    Category.ID_STUDY -> "📚"
    Category.ID_PERSONAL -> "🌙"
    else -> "🎯"
}

/** Пунктирная рамка по форме (для блока «Добавить из шаблонов»). */
private fun Modifier.dashedBorder(color: Color, shape: androidx.compose.ui.graphics.Shape): Modifier =
    this.drawWithCache {
        val outline = shape.createOutline(size, layoutDirection, this)
        val path = androidx.compose.ui.graphics.Path().apply { addOutline(outline) }
        val stroke = Stroke(
            width = 1.5.dp.toPx(),
            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f),
        )
        onDrawWithContent {
            drawContent()
            drawPath(path = path, color = color, style = stroke)
        }
    }



@Composable
private fun StepCounterEntry(onClick: () -> Unit) {
    val colors = AppTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(colors.surface)
            .clickableNoRipple(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier.size(48.dp).clip(RoundedCornerShape(16.dp)).background(colors.accentSoft),
            contentAlignment = Alignment.Center
        ) {
            Text("👟", fontSize = 24.sp)
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text("Шагомер", color = colors.textPrimary, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Text("Автоматический подсчёт активности", color = colors.textSecondary, fontSize = 12.sp)
        }
        Text("Открыть  ›", color = colors.accent, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
    }
}


@Composable
private fun WorkoutEntry(onClick: () -> Unit) {
    val colors = AppTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(colors.surface)
            .clickableNoRipple(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier.size(48.dp).clip(RoundedCornerShape(16.dp)).background(colors.accentSoft),
            contentAlignment = Alignment.Center
        ) { Text("🏋️", fontSize = 24.sp) }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text("Тренировки", color = colors.textPrimary, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Text("Упражнения, подходы и рабочие веса", color = colors.textSecondary, fontSize = 12.sp)
        }
        Text("Открыть  ›", color = colors.accent, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
    }
}

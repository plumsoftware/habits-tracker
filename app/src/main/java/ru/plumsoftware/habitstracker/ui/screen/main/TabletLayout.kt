package ru.plumsoftware.habitstracker.ui.screen.main

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.plumsoftware.habitstracker.R
import ru.plumsoftware.habitstracker.data.model.Habit
import ru.plumsoftware.habitstracker.ui.components.AchievementsCard
import ru.plumsoftware.habitstracker.ui.components.DailyStatsCard
import ru.plumsoftware.habitstracker.ui.components.HabitCorrelationsCard
import ru.plumsoftware.habitstracker.ui.components.MonthlyStatsCard
import ru.plumsoftware.habitstracker.ui.components.WeeklyActivityCard
import ru.plumsoftware.habitstracker.ui.components.ActivityHeatmapCard
import ru.plumsoftware.habitstracker.ui.screen.main.home.HomeState
import ru.plumsoftware.habitstracker.ui.theme.Dimen

/**
 * Трёхколоночная вёрстка для планшета/большого экрана в альбомной ориентации:
 * слева — навигационный рельс, по центру — контент экрана, справа — панель статистики.
 * Функциональность полностью совпадает с телефонной вёрсткой.
 */
@Composable
fun ExpandedLayout(
    state: HomeState,
    isDark: Boolean,
    isGrid: Boolean,
    gridState: androidx.compose.foundation.lazy.grid.LazyGridState,
    statsListState: LazyListState,
    onNavigate: (MainScreenStates) -> Unit,
    onToggleGrid: () -> Unit,
    onOpenCalendar: () -> Unit,
    onSelectDate: (Long) -> Unit,
    onHabitClick: (Habit) -> Unit,
    onToggleDone: (habitId: String, done: Boolean) -> Unit,
    onAddHabit: () -> Unit,
    isExpanded: Boolean = true,
    settingsContent: @Composable (PaddingValues) -> Unit,
    notificationsContent: @Composable () -> Unit
) {
    // Surface задаёт фон и корректный цвет контента (onSurface) для всей вёрстки,
    // чтобы заголовки/текст меняли цвет вместе с темой
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
    Row(modifier = Modifier.fillMaxSize()) {
        // ── Левый навигационный рельс ──
        SideNavRail(
            current = state.mainScreenState,
            onNavigate = onNavigate
        )
        VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        // ── Центральный контент ──
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(horizontal = Dimen.extraLarge, vertical = Dimen.extraLarge)
        ) {
            when (state.mainScreenState) {
                MainScreenStates.Home -> HomeCenterContent(
                    state = state,
                    gridState = gridState,
                    isGrid = isGrid,
                    onToggleGrid = onToggleGrid,
                    onOpenCalendar = onOpenCalendar,
                    onSelectDate = onSelectDate,
                    onHabitClick = onHabitClick,
                    onToggleDone = onToggleDone,
                    showGridToggle = isExpanded,
                    modifier = Modifier.fillMaxSize()
                )

                MainScreenStates.Statistics -> ExpandedStatisticsCenter(
                    state = state,
                    listState = statsListState,
                    modifier = Modifier.fillMaxSize()
                )

                MainScreenStates.Notifications -> notificationsContent()

                MainScreenStates.Settings -> Column(modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = "Настройки",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = Dimen.large)
                    )
                    settingsContent(PaddingValues(0.dp))
                }
            }
        }

        VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        // ── Правая панель статистики ──
        StatsSidePanel(
            state = state,
            onAddHabit = onAddHabit,
            modifier = Modifier
                .width(340.dp)
                .fillMaxHeight()
        )
    }
    }
}

@Composable
private fun SideNavRail(
    current: MainScreenStates,
    onNavigate: (MainScreenStates) -> Unit
) {
    Column(
        modifier = Modifier
            .width(80.dp)
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .padding(vertical = Dimen.extraLarge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Dimen.large)
    ) {
        NavRailItem(
            selected = current == MainScreenStates.Home,
            painter = painterResource(if (current == MainScreenStates.Home) R.drawable.house_fill else R.drawable.house_outline),
            contentDescription = "Главная",
            onClick = { onNavigate(MainScreenStates.Home) }
        )
        NavRailItem(
            selected = current == MainScreenStates.Statistics,
            painter = painterResource(if (current == MainScreenStates.Statistics) R.drawable.statistic_filled else R.drawable.statistic_outline),
            contentDescription = "Статистика",
            onClick = { onNavigate(MainScreenStates.Statistics) }
        )
        NavRailItem(
            selected = current == MainScreenStates.Notifications,
            imageVector = if (current == MainScreenStates.Notifications) Icons.Rounded.Notifications else Icons.Outlined.Notifications,
            contentDescription = "Уведомления",
            onClick = { onNavigate(MainScreenStates.Notifications) }
        )

        Spacer(modifier = Modifier.weight(1f))

        // Настройки — внизу
        NavRailItem(
            selected = current == MainScreenStates.Settings,
            imageVector = if (current == MainScreenStates.Settings) Icons.Rounded.Settings else Icons.Outlined.Settings,
            contentDescription = "Настройки",
            onClick = { onNavigate(MainScreenStates.Settings) }
        )
    }
}

@Composable
private fun NavRailItem(
    selected: Boolean,
    contentDescription: String?,
    onClick: () -> Unit,
    painter: Painter? = null,
    imageVector: ImageVector? = null
) {
    val tint = if (selected) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(
                if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
            )
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        when {
            painter != null -> Icon(
                painter = painter,
                contentDescription = contentDescription,
                tint = tint,
                modifier = Modifier.size(26.dp)
            )
            imageVector != null -> Icon(
                imageVector = imageVector,
                contentDescription = contentDescription,
                tint = tint,
                modifier = Modifier.size(26.dp)
            )
        }
    }
}

/** Правая панель: прогресс за сегодня, достижения, связи и кнопка новой привычки. */
@Composable
private fun StatsSidePanel(
    state: HomeState,
    onAddHabit: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(Dimen.extraLarge),
        verticalArrangement = Arrangement.spacedBy(Dimen.large)
    ) {
        TodayProgressCard(state = state, modifier = Modifier.fillMaxWidth())

        AchievementsCard(
            overallStreak = state.overallStreak,
            modifier = Modifier.fillMaxWidth()
        )

        HabitCorrelationsCard(
            correlations = state.correlations,
            unlocked = state.patternsUnlocked,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.weight(1f))

        // Кнопка «Новая привычка»
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.large)
                .background(MaterialTheme.colorScheme.primary)
                .clickable(role = Role.Button, onClick = onAddHabit)
                .padding(vertical = Dimen.large),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimen.smallPlus)
            ) {
                Icon(
                    painter = painterResource(R.drawable.add),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Новая привычка",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

/** Синяя карточка «Прогресс сегодня» с круговым индикатором. */
@Composable
private fun TodayProgressCard(
    state: HomeState,
    modifier: Modifier = Modifier
) {
    val daily = state.dailyStats
    val progress = daily.completionRate.coerceIn(0f, 1f)
    val percent = (progress * 100).toInt()

    Column(
        modifier = modifier
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.primary)
            .padding(Dimen.extraLarge),
        verticalArrangement = Arrangement.spacedBy(Dimen.large)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Прогресс сегодня",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
            )
            Text(
                text = "${daily.completedHabits} из ${daily.totalHabits}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = Dimen.small),
            contentAlignment = Alignment.Center
        ) {
            val ring = MaterialTheme.colorScheme.onPrimary
            val track = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.25f)
            Canvas(modifier = Modifier.size(140.dp)) {
                val stroke = 16.dp.toPx()
                val inset = stroke / 2
                val arcSize = Size(size.width - stroke, size.height - stroke)
                drawArc(
                    color = track,
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
                drawArc(
                    color = ring,
                    startAngle = -90f,
                    sweepAngle = 360f * progress,
                    useCenter = false,
                    topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
            }
            Text(
                text = "$percent%",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }

        // Плашка «Лучшая серия»
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.18f))
                .padding(vertical = Dimen.mediumPlus),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "🔥 Лучшая серия — ${state.overallStreak} ${daysWord(state.overallStreak)}",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

/** Центр экрана статистики для планшета (без карточек, ушедших в правую панель). */
@Composable
private fun ExpandedStatisticsCenter(
    state: HomeState,
    listState: LazyListState,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        state = listState,
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(Dimen.large)
    ) {
        item {
            Text(
                text = "Статистика",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }
        item {
            MonthlyStatsCard(
                monthlyStats = state.monthlyStats,
                selectedMonth = state.selectedMonth,
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            ActivityHeatmapCard(
                heatmap = state.heatmap,
                endDate = state.selectedDate,
                firstDayOfWeek = state.firstDayOfWeek,
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            WeeklyActivityCard(
                weeklyStats = state.weeklyStats,
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            DailyStatsCard(
                dailyStats = state.dailyStats,
                selectedDate = state.selectedDate,
                modifier = Modifier.fillMaxWidth()
            )
        }
        item { Spacer(modifier = Modifier.height(Dimen.extraLarge)) }
    }
}

private fun daysWord(n: Int): String {
    val mod100 = n % 100
    val mod10 = n % 10
    return when {
        mod100 in 11..14 -> "дней"
        mod10 == 1 -> "день"
        mod10 in 2..4 -> "дня"
        else -> "дней"
    }
}

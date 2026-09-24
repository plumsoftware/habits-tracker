package ru.plumsoftware.habitstracker.ui.screen.main

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import ru.plumsoftware.habitstracker.data.model.Habit
import ru.plumsoftware.habitstracker.ui.components.AppIcon
import ru.plumsoftware.habitstracker.ui.components.HabitIcons
import ru.plumsoftware.habitstracker.ui.components.clickableNoRipple
import ru.plumsoftware.habitstracker.ui.screen.main.analytics.AnalyticsScreenTablet
import ru.plumsoftware.habitstracker.ui.screen.main.calendar.CalendarScreenTablet
import ru.plumsoftware.habitstracker.ui.screen.main.home.HomeState
import ru.plumsoftware.habitstracker.ui.screen.main.settings.SettingsScreenTablet
import ru.plumsoftware.habitstracker.ui.screen.main.settings.SettingsViewModel
import ru.plumsoftware.habitstracker.ui.screen.main.today.TodayScreenTablet
import ru.plumsoftware.habitstracker.ui.theme.AppTheme
import ru.plumsoftware.habitstracker.ui.theme.softShadow

/**
 * Планшетная (альбомная) оболочка в новом стиле: слева — навигационный рейл с FAB,
 * справа — контент выбранной вкладки (двухпанельные «Сегодня»/«Календарь», центрированные
 * «Аналитика»/«Настройки»). Автоповорот включён, экран не лочится.
 */
@Composable
fun TabletShell(
    state: HomeState,
    settingsViewModel: SettingsViewModel,
    isDark: Boolean,
    hasUnread: Boolean,
    onNav: (MainScreenStates) -> Unit,
    onAdd: () -> Unit,
    onSelectDate: (Long) -> Unit,
    onToggleDone: (String, Boolean) -> Unit,
    onHabitClick: (Habit) -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenTemplates: () -> Unit,
    onUnarchive: (String) -> Unit,
) {
    val colors = AppTheme.colors
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bgScreen)
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        NavRail(current = state.mainScreenState, onNav = onNav, onAdd = onAdd)

        Box(Modifier.weight(1f).fillMaxHeight()) {
            when (state.mainScreenState) {
                MainScreenStates.Calendar -> CalendarScreenTablet(
                    state = state, onSelectDate = onSelectDate, onHabitClick = onHabitClick,
                    modifier = Modifier.fillMaxSize(),
                )
                MainScreenStates.Statistics -> AnalyticsScreenTablet(
                    state = state, onHabitClick = onHabitClick, modifier = Modifier.fillMaxSize(),
                )
                MainScreenStates.Settings -> SettingsScreenTablet(
                    settingsViewModel = settingsViewModel,
                    isDark = isDark,
                    overallStreak = state.overallStreak,
                    frozenDays = state.frozenDays,
                    firstDayOfWeek = state.firstDayOfWeek,
                    archivedHabits = state.archivedHabits,
                    onUnarchive = onUnarchive,
                )
                else -> TodayScreenTablet(
                    state = state,
                    onSelectDate = onSelectDate,
                    onToggleDone = onToggleDone,
                    onHabitClick = onHabitClick,
                    onOpenNotifications = onOpenNotifications,
                    onOpenTemplates = onOpenTemplates,
                    hasUnread = hasUnread,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@Composable
private fun Centered(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Box(Modifier.widthIn(max = 820.dp).fillMaxHeight()) { content() }
    }
}

private data class RailSlot(val state: MainScreenStates, val icon: Int, val iconFilled: Int, val label: String)

@Composable
private fun NavRail(current: MainScreenStates, onNav: (MainScreenStates) -> Unit, onAdd: () -> Unit) {
    val colors = AppTheme.colors
    val slots = listOf(
        RailSlot(MainScreenStates.Home, HabitIcons.navHome, HabitIcons.navHomeFilled, "Сегодня"),
        RailSlot(MainScreenStates.Calendar, HabitIcons.navCalendar, HabitIcons.navCalendarFilled, "Календарь"),
        RailSlot(MainScreenStates.Statistics, HabitIcons.navAnalytics, HabitIcons.navAnalyticsFilled, "Аналитика"),
        RailSlot(MainScreenStates.Settings, HabitIcons.navSettings, HabitIcons.navSettingsFilled, "Настройки"),
    )
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .padding(start = 16.dp, top = 16.dp, bottom = 16.dp)
            .width(92.dp)
            .softShadow(AppTheme.elevation.navBar, RoundedCornerShape(28.dp))
            .clip(RoundedCornerShape(28.dp))
            .background(colors.navBarBg)
            .padding(vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        // FAB
        Box(
            modifier = Modifier
                .size(56.dp)
                .softShadow(AppTheme.elevation.fab, AppTheme.shapes.fab)
                .clip(AppTheme.shapes.fab)
                .background(colors.accent)
                .clickableNoRipple(onClick = onAdd),
            contentAlignment = Alignment.Center,
        ) { AppIcon(HabitIcons.plus, tint = colors.inkOn, size = 24.dp) }

        Spacer(Modifier.size(10.dp))

        slots.forEach { slot ->
            val selected = slot.state == current
            val tint by animateColorAsState(if (selected) colors.accent else colors.textTertiary, label = "railtint")
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .then(if (selected) Modifier.background(colors.accentSoft) else Modifier)
                    .clickableNoRipple(onClick = { onNav(slot.state) })
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                AppIcon(if (selected) slot.iconFilled else slot.icon, tint = tint, size = 22.dp)
                Text(slot.label, style = AppTheme.type.navLabel, color = tint)
            }
        }
    }
}

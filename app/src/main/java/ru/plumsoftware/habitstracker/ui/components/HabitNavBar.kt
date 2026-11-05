package ru.plumsoftware.habitstracker.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import ru.plumsoftware.habitstracker.ui.screen.main.MainScreenStates
import ru.plumsoftware.habitstracker.ui.theme.AppTheme
import ru.plumsoftware.habitstracker.ui.theme.softShadow

private data class NavSlot(
    val state: MainScreenStates,
    val icon: Int,
    val iconFilled: Int,
    val label: String,
)

/**
 * Плавающая нижняя навигация + FAB (ТЗ §4.7). Пять слотов равной ширины, центральный —
 * под кнопку «+». Активный слот — accent, неактивный — приглушённый.
 */
@Composable
fun HabitNavBar(
    current: MainScreenStates,
    onSelect: (MainScreenStates) -> Unit,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier,
    visible: Boolean = true,
) {
    val colors = AppTheme.colors
    // §13: при скролле вниз панель уезжает вниз, при скролле вверх — возвращается.
    val translate by androidx.compose.animation.core.animateDpAsState(
        if (visible) 0.dp else 140.dp,
        animationSpec = androidx.compose.animation.core.tween(AppTheme.motion.fast),
        label = "navTranslate",
    )
    val leftSlots = listOf(
        NavSlot(MainScreenStates.Home, HabitIcons.navHome, HabitIcons.navHomeFilled, "Сегодня"),
        NavSlot(MainScreenStates.Calendar, HabitIcons.navCalendar, HabitIcons.navCalendarFilled, "Календарь"),
    )
    val rightSlots = listOf(
        NavSlot(MainScreenStates.Statistics, HabitIcons.navAnalytics, HabitIcons.navAnalyticsFilled, "Аналитика"),
        NavSlot(MainScreenStates.Settings, HabitIcons.navSettings, HabitIcons.navSettingsFilled, "Настройки"),
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .offset(y = translate)
            .windowInsetsPadding(WindowInsets.navigationBars),
    ) {
        // Панель: 4 вкладки + FAB строго по центру (в отдельном слоте равной ширины)
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = AppTheme.spacing.navBarMargin)
                .padding(bottom = AppTheme.spacing.navBarMargin)
                .softShadow(AppTheme.elevation.navBar, AppTheme.shapes.navBar)
                .clip(AppTheme.shapes.navBar)
                .background(colors.navBarBg)
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            leftSlots.forEach { NavItem(it, it.state == current, Modifier.weight(1f)) { onSelect(it.state) } }
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Fab(onAdd = onAdd)
            }
            rightSlots.forEach { NavItem(it, it.state == current, Modifier.weight(1f)) { onSelect(it.state) } }
        }
    }
}

@Composable
private fun Fab(onAdd: () -> Unit) {
    val colors = AppTheme.colors
    Box(
        modifier = Modifier
            .size(58.dp)
            .softShadow(AppTheme.elevation.fab, androidx.compose.foundation.shape.CircleShape)
            .clip(androidx.compose.foundation.shape.CircleShape)
            .background(colors.accent)
            .clickableNoRipple(onClick = onAdd),
        contentAlignment = Alignment.Center,
    ) {
        AppIcon(HabitIcons.plus, tint = colors.inkOn, size = 28.dp)
    }
}

@Composable
private fun NavItem(
    slot: NavSlot,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val colors = AppTheme.colors
    val tint by animateColorAsState(
        if (selected) colors.accent else colors.textTertiary,
        label = "navtint",
    )
    Column(
        modifier = modifier.clickableNoRipple(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        AppIcon(if (selected) slot.iconFilled else slot.icon, tint = tint, size = 26.dp)
        Text(slot.label, style = AppTheme.type.navLabel, color = tint)
    }
}

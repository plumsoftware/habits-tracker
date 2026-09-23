package ru.plumsoftware.habitstracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import org.koin.androidx.compose.koinViewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.unit.dp
import ru.plumsoftware.habitstracker.data.model.Notification
import ru.plumsoftware.habitstracker.data.model.NotificationType
import ru.plumsoftware.habitstracker.ui.screen.main.notifications.NotificationEvent
import ru.plumsoftware.habitstracker.ui.screen.main.notifications.NotificationsViewModel
import ru.plumsoftware.habitstracker.ui.theme.AppTheme
import java.util.Calendar

/**
 * Bottom-sheet «Уведомления» (ТЗ §9.1). Список записей с векторной иконкой по типу,
 * выделением непрочитанных и кнопкой «Прочитать все».
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsSheet(onDismiss: () -> Unit) {
    val colors = AppTheme.colors
    val vm: NotificationsViewModel = koinViewModel()
    val state by vm.state.collectAsState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(true),
        containerColor = colors.bgScreen,
    ) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 26.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Уведомления", style = AppTheme.type.h3, color = colors.textPrimary)
                if (state.notifications.any { !it.isRead }) {
                    Text(
                        "Прочитать все",
                        style = AppTheme.type.label,
                        color = colors.accent,
                        modifier = Modifier
                            .clip(AppTheme.shapes.pill)
                            .clickableNoRipple(onClick = { vm.onEvent(NotificationEvent.OnMarkAllRead) })
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                    )
                }
            }
            Spacer(Modifier.height(14.dp))

            if (state.notifications.isEmpty()) {
                Text("Пока нет уведомлений", style = AppTheme.type.caption, color = colors.textSecondary,
                    modifier = Modifier.padding(vertical = 24.dp))
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 460.dp),
                    verticalArrangement = Arrangement.spacedBy(9.dp),
                ) {
                    items(state.notifications, key = { it.id }) { n ->
                        SwipeToDelete(onDelete = { vm.onEvent(NotificationEvent.OnDelete(n)) }) {
                            NotificationCardRow(n) { vm.onEvent(NotificationEvent.OnNotification(n)) }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToDelete(onDelete: () -> Unit, content: @Composable () -> Unit) {
    val colors = AppTheme.colors
    val state = androidx.compose.material3.rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value != androidx.compose.material3.SwipeToDismissBoxValue.Settled) { onDelete(); true } else false
        },
        positionalThreshold = { total -> total * 0.4f },
    )
    androidx.compose.material3.SwipeToDismissBox(
        state = state,
        backgroundContent = {
            // Подложку показываем только во время свайпа и на всю высоту карточки
            if (state.dismissDirection != androidx.compose.material3.SwipeToDismissBoxValue.Settled) {
                val toStart = state.dismissDirection == androidx.compose.material3.SwipeToDismissBoxValue.StartToEnd
                Box(
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(20.dp)).background(colors.dangerSoft)
                        .padding(horizontal = 22.dp),
                    contentAlignment = if (toStart) Alignment.CenterStart else Alignment.CenterEnd,
                ) { AppIcon(HabitIcons.close, tint = colors.danger, size = 20.dp) }
            }
        },
        content = { content() },
    )
}

@Composable
private fun NotificationCardRow(n: Notification, onClick: () -> Unit) {
    val colors = AppTheme.colors
    val (icon, tint, tile) = visualFor(n.type, colors)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(if (!n.isRead) colors.accentSoft.copy(alpha = 0.4f) else colors.surface)
            .clickableNoRipple(onClick = onClick)
            .padding(13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (!n.isRead) {
            Box(Modifier.width(3.dp).height(20.dp).clip(AppTheme.shapes.pill).background(colors.accent))
        }
        IconTile(icon, tint, tile, 40.dp, 18.dp, RoundedCornerShape(14.dp))
        Column(Modifier.weight(1f)) {
            Text(n.title, style = AppTheme.type.bodyS, color = colors.textPrimary)
            if (n.body.isNotBlank()) {
                Text(n.body, style = AppTheme.type.caption, color = colors.textSecondary)
            }
        }
        Text(relativeTime(n.sendAt), style = AppTheme.type.captionS, color = colors.textTertiary)
    }
}

private fun visualFor(type: NotificationType, colors: ru.plumsoftware.habitstracker.ui.theme.HabitColors): Triple<Int, Color, Color> =
    when (type) {
        NotificationType.STREAK -> Triple(HabitIcons.flame, colors.warning, colors.warningSoft)
        NotificationType.HABIT_DONE -> Triple(HabitIcons.check, colors.success, colors.successSoft)
        NotificationType.HABIT_REMINDER -> Triple(HabitIcons.clock, colors.accent, colors.accentSoft)
        NotificationType.WEEKLY_REPORT -> Triple(HabitIcons.trophy, colors.warning, colors.warningSoft)
        NotificationType.FREEZE -> Triple(HabitIcons.snowflake, colors.info, colors.infoSoft)
        NotificationType.PUSH, NotificationType.GENERAL -> Triple(HabitIcons.bell, colors.accent, colors.accentSoft)
    }

private fun relativeTime(ms: Long): String {
    val now = Calendar.getInstance()
    val then = Calendar.getInstance().apply { timeInMillis = ms }
    fun day(c: Calendar) = c.get(Calendar.YEAR) * 1000 + c.get(Calendar.DAY_OF_YEAR)
    return when (day(now) - day(then)) {
        0 -> "%02d:%02d".format(then.get(Calendar.HOUR_OF_DAY), then.get(Calendar.MINUTE))
        1 -> "вчера"
        else -> "%02d.%02d".format(then.get(Calendar.DAY_OF_MONTH), then.get(Calendar.MONTH) + 1)
    }
}

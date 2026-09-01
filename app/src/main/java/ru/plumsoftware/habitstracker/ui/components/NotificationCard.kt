package ru.plumsoftware.habitstracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import ru.plumsoftware.habitstracker.data.model.Notification
import ru.plumsoftware.habitstracker.data.model.NotificationType
import ru.plumsoftware.habitstracker.ui.theme.Dimen
import ru.plumsoftware.habitstracker.ui.theme.LocalExtendedColorScheme
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun NotificationCard(notification: Notification, onClick: (Notification) -> Unit) {
    val cal = Calendar.getInstance().apply { timeInMillis = notification.sendAt }

    val accent = MaterialTheme.colorScheme.primary
    val good = LocalExtendedColorScheme.current.goodColor.color
    val series = Color(0xFFF5A623)

    val (icon, tint) = iconAndTint(notification.type, accent, good, series)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        onClick = { onClick(notification) }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimen.large),
            horizontalArrangement = Arrangement.spacedBy(Dimen.mediumPlus),
            verticalAlignment = Alignment.Top
        ) {
            // Чип с иконкой типа
            Box(
                modifier = Modifier
                    .size(Dimen.Size.iconChip)
                    .background(color = tint.copy(alpha = 0.15f), shape = MaterialTheme.shapes.medium),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(22.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Dimen.small)
            ) {
                Text(
                    text = notification.title,
                    style = MaterialTheme.typography.titleMedium
                )
                if (notification.body.isNotEmpty()) {
                    Text(
                        text = notification.body,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(cal.time),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Точка «непрочитано»
            if (!notification.isRead) {
                Box(
                    modifier = Modifier
                        .size(Dimen.Size.unreadDot)
                        .background(color = accent, shape = CircleShape)
                )
            }
        }
    }
}

private fun iconAndTint(
    type: NotificationType,
    accent: Color,
    good: Color,
    series: Color
): Pair<ImageVector, Color> = when (type) {
    NotificationType.HABIT_REMINDER -> Icons.Filled.Notifications to accent
    NotificationType.HABIT_DONE -> Icons.Filled.CheckCircle to good
    NotificationType.STREAK -> Icons.Filled.LocalFireDepartment to series
    NotificationType.WEEKLY_REPORT -> Icons.Filled.BarChart to accent
    NotificationType.PUSH -> Icons.Filled.Notifications to accent
    NotificationType.GENERAL -> Icons.Filled.Schedule to accent
}

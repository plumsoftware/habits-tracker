package ru.plumsoftware.habitstracker.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import ru.plumsoftware.habitstracker.data.model.Habit
import ru.plumsoftware.habitstracker.data.model.HabitWithStatus
import ru.plumsoftware.habitstracker.ui.theme.Dimen

@Composable
fun HabitCard(
    habitWithStatus: HabitWithStatus,
    onClick: (Habit) -> Unit,
    onDone: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDone = habitWithStatus.completed ?: false
    val hasReminder = habitWithStatus.habit.reminderInterval > 0

    Card(
        modifier = modifier
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = if (isDone) BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
        ) else null,
        shape = MaterialTheme.shapes.medium,
        onClick = {
            onClick(habitWithStatus.habit)
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimen.extraLarge, vertical = Dimen.extraLarge),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = Dimen.medium)
            ) {
                // TimeCard сверху
                TimeCard(
                    time = "${habitWithStatus.habit.reminderTimeHour}:${if (habitWithStatus.habit.reminderTimeMinute == 0) "00" else habitWithStatus.habit.reminderTimeMinute}",
                    isClickable = false,
                    onSelectedTime = { _, _ -> }
                )

                // Затем текст
                Column(
                    verticalArrangement = Arrangement.spacedBy(Dimen.small)
                ) {
                    Text(
                        text = habitWithStatus.habit.title.trim(),
                        style = MaterialTheme.typography.titleLarge
                    )

                    if (habitWithStatus.habit.body.isNotEmpty())
                        Text(
                            text = habitWithStatus.habit.body.trim(),
                            style = MaterialTheme.typography.titleMedium.copy(color = MaterialTheme.colorScheme.outline)
                        )
                }

                // Показываем напоминание с иконкой, если оно включено
                if (hasReminder) {
                    Row(
                        modifier = Modifier.padding(top = Dimen.medium),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Dimen.small)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Напоминание включено",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Напоминание за ${habitWithStatus.habit.reminderInterval} мин.",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.primary)
                        )
                    }
                }
            }

            // Чекбокс справа по центру. Внешний Box фиксированного размера,
            // поэтому позиция чекбокса одинакова в отмеченном и неотмеченном состоянии.
            Box(
                modifier = Modifier
                    .padding(Dimen.smallPlus)
                    .align(Alignment.CenterVertically)
                    .size(Dimen.Size.checkbox),
                contentAlignment = Alignment.Center
            ) {
                // Кастомный чекбокс с иконкой
                Box(
                    modifier = Modifier
                        .size(Dimen.Size.checkbox)
                        .shadow(
                            elevation = if (isDone) 12.dp else 0.dp,
                            shape = MaterialTheme.shapes.small,
                            clip = false,
                            ambientColor = MaterialTheme.colorScheme.primary,
                            spotColor = MaterialTheme.colorScheme.primary
                        )
                        .clip(MaterialTheme.shapes.small)
                        .background(
                            color = if (isDone) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceContainerLowest,
                            shape = MaterialTheme.shapes.small
                        )
                        .border(
                            width = 1.5.dp,
                            color = if (isDone) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outlineVariant,
                            shape = MaterialTheme.shapes.small
                        )
                        .clickable { onDone(!isDone) },
                    contentAlignment = Alignment.Center
                ) {
                    if (isDone) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Выполнено",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
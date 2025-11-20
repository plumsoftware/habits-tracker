package ru.plumsoftware.habitstracker.ui.components

import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import ru.plumsoftware.habitstracker.ui.NotificationUtils
import ru.plumsoftware.habitstracker.ui.theme.Dimen

@Composable
fun IntervalCard(
    initialInterval: Int? = null,
    onSelectedTime: (Int) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    // Временное хранение выбранного интервала до получения разрешения
    var pendingInterval by remember { mutableStateOf<Int?>(null) }

    // Для запроса разрешений
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Разрешение получено - устанавливаем выбранный интервал
            pendingInterval?.let { interval ->
                onSelectedTime(interval)
                pendingInterval = null // Сбрасываем временное значение
            }
        } else {
            // Разрешение не получено - показываем сообщение
            Toast.makeText(
                context,
                "Для напоминаний необходимо разрешение на уведомления",
                Toast.LENGTH_LONG
            ).show()
            // Устанавливаем "Не напоминать", так как разрешения нет
            onSelectedTime(0)
            pendingInterval = null
        }
    }

    val intervals = listOf(
        "Не напоминать" to 0,
        "5 минут" to 5,
        "10 минут" to 10,
        "15 минут" to 15,
        "20 минут" to 20,
        "30 минут" to 30,
        "1 час" to 60,
        "2 часа" to 120
    )

    // Находим начальный интервал по значению или берем "Не напоминать" по умолчанию
    val initialSelected = remember(initialInterval) {
        intervals.find { it.second == initialInterval } ?: intervals.first()
    }

    var currentInterval by remember { mutableStateOf(initialSelected) }

    Card(
        modifier = Modifier
            .wrapContentSize(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.elevatedCardElevation(0.dp, 0.dp, 0.dp),
        onClick = {
            showDialog = true
        }
    ) {
        Row(
            modifier = Modifier
                .wrapContentSize()
                .padding(horizontal = Dimen.medium, vertical = Dimen.smallPlus),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(
                space = Dimen.smallPlus,
                alignment = Alignment.CenterHorizontally
            )
        ) {
            Text(
                text = if (currentInterval.second == 0) "Не напоминать" else "за ${currentInterval.first}",
                style = MaterialTheme.typography.bodyMedium
            )

            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = "Выбрать интервал",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = {
                Text(
                    text = "Напомнить за",
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                Column {
                    intervals.forEach { (intervalText, minutes) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    // Обновляем текущее отображаемое значение
                                    currentInterval = intervalText to minutes

                                    if (minutes == 0) {
                                        // Для "Не напоминать" сразу устанавливаем значение
                                        onSelectedTime(0)
                                        showDialog = false
                                    } else {
                                        // Для напоминаний проверяем разрешение
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                            // Для Android 13+ проверяем разрешение
                                            if (NotificationUtils.areNotificationsEnabled(context)) {
                                                // Разрешение уже есть
                                                onSelectedTime(minutes)
                                                showDialog = false
                                            } else {
                                                // Разрешения нет - запрашиваем
                                                pendingInterval = minutes
                                                notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                                                showDialog = false
                                            }
                                        } else {
                                            // Для версий ниже Android 13 разрешение не требуется
                                            onSelectedTime(minutes)
                                            showDialog = false
                                        }
                                    }
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = currentInterval.second == minutes,
                                onClick = {
                                    // Обновляем текущее отображаемое значение
                                    currentInterval = intervalText to minutes

                                    if (minutes == 0) {
                                        onSelectedTime(0)
                                        showDialog = false
                                    } else {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                            if (NotificationUtils.areNotificationsEnabled(context)) {
                                                onSelectedTime(minutes)
                                                showDialog = false
                                            } else {
                                                pendingInterval = minutes
                                                notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                                                showDialog = false
                                            }
                                        } else {
                                            onSelectedTime(minutes)
                                            showDialog = false
                                        }
                                    }
                                }
                            )
                            Text(
                                text = intervalText,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showDialog = false }
                ) {
                    Text(
                        text = "Отмена",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        )
    }
}
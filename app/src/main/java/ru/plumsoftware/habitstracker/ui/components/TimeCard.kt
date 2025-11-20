package ru.plumsoftware.habitstracker.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.plumsoftware.habitstracker.ui.theme.Dimen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeCard(
    initialHour: Int? = null,
    initialMinute: Int? = null,
    time: String? = null,
    isClickable: Boolean,
    onSelectedTime: (Int, Int) -> Unit
) {
    var showTimePicker by remember { mutableStateOf(false) }

    // Инициализируем выбранное время с правильным форматированием
    val (selectedTime, setSelectedTime) = remember(initialHour, initialMinute, time) {
        val initialTime = if (time != null) {
            // Если время пришло в формате "6:5", форматируем его правильно
            formatTimeString(time)
        } else if (initialHour != null && initialMinute != null) {
            // Форматируем с ведущими нулями
            "${initialHour.toString().padStart(2, '0')}:${initialMinute.toString().padStart(2, '0')}"
        } else {
            "12:00"
        }
        mutableStateOf(initialTime)
    }

    // Парсим текущее выбранное время для TimePicker
    val (currentHour, currentMinute) = remember(selectedTime) {
        val parts = selectedTime.split(":")
        if (parts.size == 2) {
            parts[0].toIntOrNull() to parts[1].toIntOrNull()
        } else {
            null to null
        }
    }

    Card(
        modifier = Modifier
            .wrapContentSize(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.elevatedCardElevation(0.dp, 0.dp, 0.dp),
        onClick = {
            if (isClickable) {
                showTimePicker = true
            }
        }
    ) {
        Text(
            text = selectedTime,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = Dimen.medium, vertical = Dimen.smallPlus)
        )
    }

    if (showTimePicker) {
        val timeState = rememberTimePickerState(
            initialHour = currentHour ?: 12,
            initialMinute = currentMinute ?: 0,
            is24Hour = true
        )

        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = {
                Text(
                    text = "Выберите время",
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                Column {
                    TimePicker(state = timeState)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        // Форматируем время с ведущими нулями
                        val newTime = "${timeState.hour.toString().padStart(2, '0')}:${timeState.minute.toString().padStart(2, '0')}"
                        setSelectedTime(newTime)
                        onSelectedTime(timeState.hour, timeState.minute)
                        showTimePicker = false
                    }
                ) {
                    Text(
                        text = "Ок",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text(
                        text = "Отмена",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        )
    }
}

// Вспомогательная функция для форматирования времени
private fun formatTimeString(timeString: String): String {
    return try {
        val parts = timeString.split(":")
        if (parts.size == 2) {
            val hour = parts[0].toIntOrNull() ?: 0
            val minute = parts[1].toIntOrNull() ?: 0
            "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"
        } else {
            // Если формат неправильный, возвращаем время по умолчанию
            "12:00"
        }
    } catch (e: Exception) {
        "12:00"
    }
}
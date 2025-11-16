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
    isClickable: Boolean,
    onSelectedTime: (Int, Int) -> Unit
) {
    var showTimePicker by remember { mutableStateOf(false) }
    var selectedTime by remember { mutableStateOf("12:00") }

    Card(
        modifier = Modifier
            .wrapContentSize(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.elevatedCardElevation(0.dp, 0.dp, 0.dp),
        onClick = {
            if (isClickable)
                showTimePicker = true
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
            initialHour = 12,
            initialMinute = 0,
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
                        selectedTime =
                            "${timeState.hour}:${timeState.minute.toString().padStart(2, '0')}"
                        showTimePicker = false

                        onSelectedTime(timeState.hour, timeState.minute)
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
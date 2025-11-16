package ru.plumsoftware.habitstracker.ui.components

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
import androidx.compose.ui.unit.dp
import ru.plumsoftware.habitstracker.ui.theme.Dimen

@Composable
fun IntervalCard(
    onSelectedTime: (Int) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    var selectedInterval by remember { mutableStateOf("5 минут") }

    val intervals = listOf(
        "5 минут" to 5,
        "10 минут" to 10,
        "15 минут" to 15,
        "20 минут" to 20,
        "30 минут" to 30
    )

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
                .padding(horizontal = Dimen.medium, vertical = Dimen.small),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(
                space = Dimen.smallPlus,
                alignment = Alignment.CenterHorizontally
            )
        ) {
            Text(
                text = selectedInterval,
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
                    text = "Выберите интервал",
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
                                    selectedInterval = intervalText
                                    onSelectedTime(minutes)
                                    showDialog = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedInterval == intervalText,
                                onClick = {
                                    selectedInterval = intervalText
                                    onSelectedTime(minutes)
                                    showDialog = false
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
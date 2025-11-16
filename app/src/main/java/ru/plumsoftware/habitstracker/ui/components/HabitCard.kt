package ru.plumsoftware.habitstracker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import ru.plumsoftware.habitstracker.data.model.Habit
import ru.plumsoftware.habitstracker.ui.theme.Dimen
import ru.plumsoftware.habitstracker.ui.theme.doneHabitColor

@Composable
fun HabitCard(
    habit: Habit,
    onClick: (Habit) -> Unit,
    onDone: (Boolean) -> Unit
) {
    var isDone by rememberSaveable { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(all = Dimen.medium),
        colors = CardDefaults.cardColors(
            containerColor = if (isDone) doneHabitColor else MaterialTheme.colorScheme.surfaceContainer
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.wrapContentSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(
                    space = Dimen.large,
                    alignment = Alignment.CenterVertically
                )
            ) {
                TimeCard(
                    isClickable = false,
                    onSelectedTime = { _, _ -> }
                )

                Column(
                    modifier = Modifier.wrapContentSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(
                        space = Dimen.medium,
                        alignment = Alignment.CenterVertically
                    )
                ) {
                    Text(
                        text = habit.title,
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Text(
                        text = habit.body,
                        style = MaterialTheme.typography.headlineSmall.copy(color = MaterialTheme.colorScheme.outline)
                    )
                }
            }

            Checkbox(
                checked = isDone,
                onCheckedChange = {
                    isDone = it
                    onDone(it)
                }
            )
        }
    }
}
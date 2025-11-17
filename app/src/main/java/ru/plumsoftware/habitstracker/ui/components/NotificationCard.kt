package ru.plumsoftware.habitstracker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import ru.plumsoftware.habitstracker.data.model.Notification
import ru.plumsoftware.habitstracker.ui.theme.Dimen
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun NotificationCard(notification: Notification, onClick: (Notification) -> Unit) {
    val cal = Calendar.getInstance().apply {
        timeInMillis = notification.sendAt
    }

    Card(
        modifier = Modifier
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        shape = MaterialTheme.shapes.medium,
        onClick = {
            onClick(notification)
        }
    ) {
        Column(
            modifier = Modifier
                .wrapContentSize()
                .padding(all = Dimen.large),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(
                space = Dimen.large,
                alignment = Alignment.CenterVertically
            )
        ) {
            Column(
                modifier = Modifier.wrapContentSize(),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(
                    space = Dimen.small,
                    alignment = Alignment.CenterVertically
                )
            ) {
                Text(
                    text = notification.title,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = notification.body,
                    style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.outline)
                )
            }

            Text(
                text = SimpleDateFormat(
                    "dd.MM.yyyy HH:mm",
                    Locale.getDefault()
                ).format(cal.timeInMillis),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
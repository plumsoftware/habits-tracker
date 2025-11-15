package ru.plumsoftware.habitstracker.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.plumsoftware.habitstracker.ui.theme.Dimen

@Composable
fun TimeCard(time: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .wrapContentSize()
            .padding(all = Dimen.small),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.outline,
            contentColor = MaterialTheme.colorScheme.onBackground
        ),
        elevation = CardDefaults.elevatedCardElevation(0.dp, 0.dp, 0.dp),
        onClick = onClick
    ) {
        Text(
            text = time,
            style = MaterialTheme.typography.bodySmall
        )
    }
}
package ru.plumsoftware.habitstracker.ui.screen.main.notifications

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import org.koin.androidx.compose.koinViewModel
import ru.plumsoftware.habitstracker.data.model.Notification
import ru.plumsoftware.habitstracker.ui.components.NotificationCard
import ru.plumsoftware.habitstracker.ui.theme.Dimen
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScreen() {

    val viewMode: NotificationsViewModel = koinViewModel()
    val state = viewMode.state.collectAsState()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = {
                    when (state.value.notificationsScreenState) {
                        NotificationsScreenState.AboutNotification -> {
                            Text(
                                text = "Уведомления",
                                style = MaterialTheme.typography.titleLarge
                            )
                        }

                        NotificationsScreenState.Notifications -> {
                            Text(
                                text = "Уведомления",
                                style = MaterialTheme.typography.titleLarge
                            )
                        }
                    }
                },
                navigationIcon = {
                    if (state.value.notificationsScreenState == NotificationsScreenState.AboutNotification)
                        IconButton(
                            modifier = Modifier.clip(CircleShape),
                            onClick = { viewMode.onEvent(NotificationEvent.OnBack) },
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainer
                            )
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                                contentDescription = null
                            )
                        }
                },
                actions = {
                    if (state.value.notificationsScreenState == NotificationsScreenState.Notifications &&
                        state.value.unreadCount > 0
                    ) {
                        TextButton(onClick = { viewMode.onEvent(NotificationEvent.OnMarkAllRead) }) {
                            Text(
                                text = "Прочитать все",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->

        when (state.value.notificationsScreenState) {
            NotificationsScreenState.AboutNotification -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = Dimen.medium)
                        .padding(top = Dimen.large),
                    verticalArrangement = Arrangement.spacedBy(Dimen.large, Alignment.Top),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = state.value.selectedNotification?.title ?: "",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = state.value.selectedNotification?.sendAt?.let {
                            SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(it)
                        } ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = state.value.selectedNotification?.body ?: "",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            NotificationsScreenState.Notifications -> {
                if (state.value.notifications.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Пока нет уведомлений",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    val today = state.value.notifications.filter { isToday(it.sendAt) }
                    val earlier = state.value.notifications.filterNot { isToday(it.sendAt) }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(horizontal = Dimen.medium)
                            .padding(top = Dimen.smallPlus),
                        verticalArrangement = Arrangement.spacedBy(Dimen.medium, Alignment.Top)
                    ) {
                        if (today.isNotEmpty()) {
                            item { SectionHeader("Сегодня") }
                            items(today, key = { it.id }) { NotificationItem(it, viewMode) }
                        }
                        if (earlier.isNotEmpty()) {
                            item { SectionHeader("Ранее") }
                            items(earlier, key = { it.id }) { NotificationItem(it, viewMode) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationItem(notification: Notification, viewMode: NotificationsViewModel) {
    NotificationCard(
        notification = notification,
        onClick = { viewMode.onEvent(NotificationEvent.OnNotification(it)) }
    )
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .wrapContentSize()
            .padding(start = Dimen.smallPlus, top = Dimen.smallPlus)
    )
}

private fun isToday(timestamp: Long): Boolean {
    val now = Calendar.getInstance()
    val other = Calendar.getInstance().apply { timeInMillis = timestamp }
    return now.get(Calendar.YEAR) == other.get(Calendar.YEAR) &&
            now.get(Calendar.DAY_OF_YEAR) == other.get(Calendar.DAY_OF_YEAR)
}

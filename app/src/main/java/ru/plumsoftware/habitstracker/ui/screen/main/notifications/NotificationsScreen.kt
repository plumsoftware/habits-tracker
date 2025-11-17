package ru.plumsoftware.habitstracker.ui.screen.main.notifications

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import org.koin.androidx.compose.koinViewModel
import ru.plumsoftware.habitstracker.ui.components.NotificationCard
import ru.plumsoftware.habitstracker.ui.theme.Dimen
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScreen() {

    val viewMode: NotificationsViewModel = koinViewModel()
    val state = viewMode.state.collectAsState()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    when (state.value.notificationsScreenState) {
                        NotificationsScreenState.AboutNotification -> {
                            Column(
                                modifier = Modifier.wrapContentSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(
                                    space = Dimen.small,
                                    alignment = Alignment.CenterVertically
                                )
                            ) {
                                Text(
                                    text = "Уведомления",
                                    style = MaterialTheme.typography.titleLarge
                                )
                                Text(
                                    text = SimpleDateFormat(
                                        "dd.MM.yyyy HH:mm",
                                        Locale.getDefault()
                                    ).format(state.value.selectedNotification?.sendAt),
                                    style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.outline)
                                )
                            }
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
                            onClick = {
                                viewMode.onEvent(NotificationEvent.OnBack)
                            },
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainer
                            )
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                                contentDescription = null
                            )
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
                    verticalArrangement = Arrangement.spacedBy(
                        space = Dimen.large,
                        alignment = Alignment.Top
                    ),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = state.value.selectedNotification?.title ?: "",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = state.value.selectedNotification?.body ?: "",
                        style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.outline)
                    )
                }
            }

            NotificationsScreenState.Notifications -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = Dimen.medium)
                        .padding(top = Dimen.large),
                    verticalArrangement = Arrangement.spacedBy(
                        space = Dimen.medium,
                        alignment = Alignment.Top
                    ),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    itemsIndexed(state.value.notifications) { index, item ->
                        NotificationCard(
                            notification = item,
                            onClick = { notification ->
                                viewMode.onEvent(NotificationEvent.OnNotification(notification))
                            }
                        )
                    }
                }
            }
        }
    }
}
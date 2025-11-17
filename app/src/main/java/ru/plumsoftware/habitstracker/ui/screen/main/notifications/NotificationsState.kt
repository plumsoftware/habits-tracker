package ru.plumsoftware.habitstracker.ui.screen.main.notifications

import ru.plumsoftware.habitstracker.data.model.Notification

data class NotificationsState(
    val notifications: List<Notification> = emptyList(),
    val selectedNotification: Notification? = null,
    val notificationsScreenState: NotificationsScreenState = NotificationsScreenState.Notifications
)

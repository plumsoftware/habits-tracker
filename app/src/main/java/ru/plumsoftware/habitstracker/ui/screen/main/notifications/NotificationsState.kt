package ru.plumsoftware.habitstracker.ui.screen.main.notifications

import ru.plumsoftware.habitstracker.data.model.Notification

data class NotificationsState(
    val notifications: List<Notification> = emptyList(),
    val selectedNotification: Notification? = null,
    val unreadCount: Int = 0,
    val notificationsScreenState: NotificationsScreenState = NotificationsScreenState.Notifications
)

package ru.plumsoftware.habitstracker.ui.screen.main.notifications

import ru.plumsoftware.habitstracker.data.model.Notification

sealed interface NotificationEvent {
    data class OnNotification(val notif: Notification) : NotificationEvent
    data class OnDelete(val notif: Notification) : NotificationEvent
    data object OnBack : NotificationEvent
    data object OnMarkAllRead : NotificationEvent
}
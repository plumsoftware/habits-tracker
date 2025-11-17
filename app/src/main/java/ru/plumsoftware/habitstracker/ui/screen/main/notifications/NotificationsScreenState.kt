package ru.plumsoftware.habitstracker.ui.screen.main.notifications

sealed interface NotificationsScreenState {
    data object Notifications : NotificationsScreenState
    data object AboutNotification : NotificationsScreenState
}
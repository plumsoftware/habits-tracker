package ru.plumsoftware.habitstracker.ui.screen.main.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import ru.plumsoftware.habitstracker.data.database.NotificationRepository

class NotificationsViewModel(
    private val notificationRepository: NotificationRepository
) : ViewModel() {
    private val _state = MutableStateFlow(NotificationsState())
    val state: StateFlow<NotificationsState> = _state.asStateFlow()

    init {
        initNotifications()
    }

    private fun initNotifications() {
        viewModelScope.launch {
            notificationRepository.notifications.collect { notifications ->
                _state.update { current ->
                    current.copy(
                        notifications = notifications,
                        unreadCount = notifications.count { !it.isRead }
                    )
                }
            }
        }
    }

    fun onEvent(event: NotificationEvent) {
        when (event) {
            NotificationEvent.OnBack -> {
                _state.update {
                    it.copy(
                        notificationsScreenState = NotificationsScreenState.Notifications
                    )
                }
            }

            is NotificationEvent.OnNotification -> {
                // Открытие уведомления помечает его прочитанным
                viewModelScope.launch {
                    notificationRepository.markRead(event.notif.id)
                }
                _state.update {
                    it.copy(
                        selectedNotification = event.notif,
                        notificationsScreenState = NotificationsScreenState.AboutNotification
                    )
                }
            }

            NotificationEvent.OnMarkAllRead -> {
                viewModelScope.launch {
                    notificationRepository.markAllRead()
                }
            }

            is NotificationEvent.OnDelete -> {
                viewModelScope.launch {
                    notificationRepository.delete(event.notif)
                }
            }
        }
    }

    companion object {
        val viewModelModule = module {
            viewModel { NotificationsViewModel(get()) }
        }
    }
}

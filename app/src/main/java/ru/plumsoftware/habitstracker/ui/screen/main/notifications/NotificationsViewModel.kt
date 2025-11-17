package ru.plumsoftware.habitstracker.ui.screen.main.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import ru.plumsoftware.habitstracker.BuildConfig
import ru.plumsoftware.habitstracker.data.model.Notification
import ru.plumsoftware.habitstracker.data.model.dao.NotificationsDao

class NotificationsViewModel(
    private val notificationsDao: NotificationsDao
) : ViewModel() {
    private val _state = MutableStateFlow(NotificationsState())
    val state: StateFlow<NotificationsState> = _state.asStateFlow()

    init {
        initNotifications()
    }

    private fun initNotifications() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                notificationsDao.getAll().collect { notifications ->
                    withContext(Dispatchers.Main) {
                        _state.update {
                            it.copy(notifications = notifications)
                        }
                    }

                    if (BuildConfig.DEBUG) {
                        withContext(Dispatchers.Main) {
                            _state.update {
                                it.copy(
                                    notifications = listOf(
                                        Notification(
                                            id = 0,
                                            title = "Тестовый заголовок",
                                            body = "Тестовое описание уведомления",
                                            sendAt = System.currentTimeMillis(),
                                            resourceUrl = ""
                                        )
                                    ) + notifications
                                )
                            }
                        }
                    }
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
                _state.update {
                    it.copy(
                        selectedNotification = event.notif,
                        notificationsScreenState = NotificationsScreenState.AboutNotification
                    )
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
package ru.plumsoftware.habitstracker.data.database

import kotlinx.coroutines.flow.Flow
import org.koin.dsl.module
import ru.plumsoftware.habitstracker.data.model.Notification
import ru.plumsoftware.habitstracker.data.model.NotificationType
import ru.plumsoftware.habitstracker.data.model.dao.NotificationsDao

/**
 * Единая точка записи/чтения уведомлений в БД. Используется:
 * - локальными напоминаниями о привычках (WorkManager) — тип [NotificationType.HABIT_REMINDER];
 * - отметкой выполнения/серий — [NotificationType.HABIT_DONE] / [NotificationType.STREAK];
 * - push из Firebase — [NotificationType.PUSH];
 * - экраном «Уведомления».
 */
class NotificationRepository(private val dao: NotificationsDao) {

    val notifications: Flow<List<Notification>> = dao.getAll()
    val unreadCount: Flow<Int> = dao.unreadCount()

    suspend fun add(
        title: String,
        body: String,
        type: NotificationType,
        sendAt: Long = System.currentTimeMillis(),
        resourceUrl: String = ""
    ) {
        dao.insert(
            Notification(
                id = 0,
                title = title,
                body = body,
                sendAt = sendAt,
                resourceUrl = resourceUrl,
                type = type,
                isRead = false
            )
        )
    }

    suspend fun delete(notification: ru.plumsoftware.habitstracker.data.model.Notification) = dao.delete(notification)
    suspend fun markRead(id: Int) = dao.markRead(id)
    suspend fun markAllRead() = dao.markAllRead()
    suspend fun clearAll() = dao.clearAll()

    companion object {
        val notificationRepositoryModule = module {
            single<NotificationRepository> { NotificationRepository(get()) }
        }
    }
}

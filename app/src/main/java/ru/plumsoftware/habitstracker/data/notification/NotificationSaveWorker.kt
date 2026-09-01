package ru.plumsoftware.habitstracker.data.notification

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import org.koin.java.KoinJavaComponent.getKoin
import ru.plumsoftware.habitstracker.data.model.Notification
import ru.plumsoftware.habitstracker.data.model.NotificationType
import ru.plumsoftware.habitstracker.data.model.dao.NotificationsDao

class NotificationSaveWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val title = inputData.getString("title") ?: ""
            val body = inputData.getString("body") ?: ""
            val sendAt = inputData.getString("sendAt")?.toLongOrNull() ?: System.currentTimeMillis()
            val resourceUrl = inputData.getString("resourceUrl") ?: ""
            val type = NotificationType.fromName(inputData.getString("type"))

            val notification = Notification(
                id = 0,
                title = title,
                body = body,
                sendAt = sendAt,
                resourceUrl = resourceUrl,
                type = type
            )

            // Получаем DAO через Koin или другую DI
            val notificationsDao = getKoin().get<NotificationsDao>()
            notificationsDao.insert(notification)

            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }
}
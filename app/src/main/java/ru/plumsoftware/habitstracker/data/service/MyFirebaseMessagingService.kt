package ru.plumsoftware.habitstracker.data.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import ru.plumsoftware.habitstracker.MainActivity
import ru.plumsoftware.habitstracker.R
import ru.plumsoftware.habitstracker.data.model.Notification
import ru.plumsoftware.habitstracker.data.notification.NotificationSaveWorker

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        // Обработка нового токена (если нужно)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        // Обработка входящего сообщения
        handleNotification(remoteMessage)
    }

    private fun handleNotification(remoteMessage: RemoteMessage) {
        val title = remoteMessage.notification?.title ?: "Новое уведомление"
        val body = remoteMessage.notification?.body ?: ""
        val data = remoteMessage.data

        // Создаем объект Notification
        val notification = Notification(
            id = 0, // autoGenerate = true, поэтому 0
            title = title,
            body = body,
            sendAt = System.currentTimeMillis(),
            resourceUrl = data["resourceUrl"] ?: ""
        )

        // Сохраняем в базу данных
        saveNotificationToDatabase(notification)

        // Показываем уведомление в системном трее (опционально)
        showSystemNotification(notification)
    }

    private fun saveNotificationToDatabase(notification: Notification) {
        // Используем WorkManager или Coroutines для сохранения в базу
        val workRequest = OneTimeWorkRequestBuilder<NotificationSaveWorker>()
            .setInputData(
                workDataOf(
                    "title" to notification.title,
                    "body" to notification.body,
                    "sendAt" to notification.sendAt.toString(),
                    "resourceUrl" to notification.resourceUrl
                )
            )
            .build()

        WorkManager.getInstance(this).enqueue(workRequest)
    }

    private fun showSystemNotification(notification: Notification) {
        // Создаем уведомление для системного трея
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Создаем канал уведомлений (для Android O и выше)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "notifications_channel",
                "Уведомления",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = NotificationCompat.Builder(this, "notifications_channel")
            .setContentTitle(notification.title)
            .setContentText(notification.body)
            .setSmallIcon(R.drawable.icon)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        notificationManager.notify(notification.id, notificationBuilder.build())
    }
}
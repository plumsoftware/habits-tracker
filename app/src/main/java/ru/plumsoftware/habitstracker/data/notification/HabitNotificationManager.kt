package ru.plumsoftware.habitstracker.data.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Data
import ru.plumsoftware.habitstracker.R
import ru.plumsoftware.habitstracker.data.model.Habit
import ru.plumsoftware.habitstracker.data.model.Periodicity
import ru.plumsoftware.habitstracker.ui.NotificationUtils
import java.util.concurrent.TimeUnit
import java.util.Calendar


class HabitNotificationManager(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "habit_reminders"
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Напоминания о привычках",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Уведомления о предстоящих привычках"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500)
            }

            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun scheduleHabitNotification(habit: Habit) {
        // Проверяем, есть ли разрешение на уведомления
        if (!NotificationUtils.areNotificationsEnabled(context)) {
            return
        }

        // Если интервал напоминания = 0, не планируем уведомления
        if (habit.reminderInterval == 0) {
            cancelHabitNotification(habit.id)
            return
        }

        // Отменяем предыдущие уведомления для этой привычки
        cancelHabitNotification(habit.id)

        // Вычисляем время уведомления (время привычки - интервал)
        val notificationTime = calculateNotificationTime(habit)

        // Если время уведомления уже прошло, не планируем
        if (notificationTime <= System.currentTimeMillis()) {
            return
        }

        val inputData = Data.Builder()
            .putString("habit_id", habit.id)
            .putString("habit_title", habit.title)
            .putString("habit_body", habit.body)
            .putInt("hour", habit.reminderTimeHour)
            .putInt("minute", habit.reminderTimeMinute)
            .putInt("interval", habit.reminderInterval)
            .build()

        val delay = notificationTime - System.currentTimeMillis()

        val workRequest = OneTimeWorkRequestBuilder<HabitNotificationWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(inputData)
            .addTag("habit_${habit.id}")
            .build()

        WorkManager.getInstance(context).enqueue(workRequest)

        // Для периодических привычек планируем следующие уведомления
        if (habit.reminderInterval > 0) {
            if (habit.periodicity == Periodicity.DAILY) {
                scheduleDailyHabitNotifications(habit)
            } else if (habit.periodicity == Periodicity.WEEKLY) {
                scheduleWeeklyHabitNotifications(habit)
            }
        }
    }

    private fun calculateNotificationTime(habit: Habit): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, habit.reminderTimeHour)
            set(Calendar.MINUTE, habit.reminderTimeMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // Вычитаем интервал напоминания
        calendar.add(Calendar.MINUTE, -habit.reminderInterval)

        return calendar.timeInMillis
    }

    private fun scheduleDailyHabitNotifications(habit: Habit) {
        // Планируем уведомления на следующие 7 дней
        for (day in 1..7) {
            val notificationTime = calculateNotificationTimeForDay(habit, day)

            if (notificationTime > System.currentTimeMillis()) {
                val inputData = Data.Builder()
                    .putString("habit_id", habit.id)
                    .putString("habit_title", habit.title)
                    .putString("habit_body", habit.body)
                    .putInt("interval", habit.reminderInterval)
                    .build()

                val delay = notificationTime - System.currentTimeMillis()

                val workRequest = OneTimeWorkRequestBuilder<HabitNotificationWorker>()
                    .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                    .setInputData(inputData)
                    .addTag("habit_${habit.id}_future")
                    .build()

                WorkManager.getInstance(context).enqueue(workRequest)
            }
        }
    }

    private fun scheduleWeeklyHabitNotifications(habit: Habit) {
        // Планируем уведомления на следующие 4 недели
        for (week in 1..4) {
            val notificationTime = calculateNotificationTimeForWeek(habit, week)

            if (notificationTime > System.currentTimeMillis()) {
                val inputData = Data.Builder()
                    .putString("habit_id", habit.id)
                    .putString("habit_title", habit.title)
                    .putString("habit_body", habit.body)
                    .putInt("interval", habit.reminderInterval)
                    .build()

                val delay = notificationTime - System.currentTimeMillis()

                val workRequest = OneTimeWorkRequestBuilder<HabitNotificationWorker>()
                    .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                    .setInputData(inputData)
                    .addTag("habit_${habit.id}_future")
                    .build()

                WorkManager.getInstance(context).enqueue(workRequest)
            }
        }
    }

    private fun calculateNotificationTimeForDay(habit: Habit, daysFromNow: Int): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            add(Calendar.DAY_OF_YEAR, daysFromNow)
            set(Calendar.HOUR_OF_DAY, habit.reminderTimeHour)
            set(Calendar.MINUTE, habit.reminderTimeMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        calendar.add(Calendar.MINUTE, -habit.reminderInterval)
        return calendar.timeInMillis
    }

    private fun calculateNotificationTimeForWeek(habit: Habit, weeksFromNow: Int): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            add(Calendar.WEEK_OF_YEAR, weeksFromNow)
            set(Calendar.HOUR_OF_DAY, habit.reminderTimeHour)
            set(Calendar.MINUTE, habit.reminderTimeMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        calendar.add(Calendar.MINUTE, -habit.reminderInterval)
        return calendar.timeInMillis
    }

    fun cancelHabitNotification(habitId: String) {
        WorkManager.getInstance(context).cancelAllWorkByTag("habit_$habitId")
        WorkManager.getInstance(context).cancelAllWorkByTag("habit_${habitId}_future")
    }

    fun showNotification(habit: Habit) {
        // Проверяем разрешение перед показом уведомления
        if (!NotificationUtils.areNotificationsEnabled(context)) {
            return
        }

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.icon)
            .setContentTitle(habit.title)
            .setContentText("${habit.title} - через ${habit.reminderInterval} минут")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(
                        habit.body
//                        "Время: ${habit.reminderTimeHour}:${habit.reminderTimeMinute.toString().padStart(2, '0')}"
                    )
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(
            "${habit.id}_${System.currentTimeMillis()}".hashCode(),
            notification
        )
    }
}
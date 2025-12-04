package ru.plumsoftware.habitstracker.data.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
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
        // Отменяем старые уведомления
        cancelHabitNotification(habit.id)

        // Рассчитываем время первого уведомления
        val firstNotificationTime = calculateFirstNotificationTime(habit)

        // Создаем рабочую задачу
        val notificationWork = OneTimeWorkRequestBuilder<HabitNotificationWorker>()
            .setInitialDelay(firstNotificationTime, TimeUnit.MILLISECONDS)
            .setInputData(
                Data.Builder()
                    .putString("habit_id", habit.id)
                    .putString("habit_title", habit.title)
                    .putString("habit_body", habit.body)
                    .putInt("hour", habit.reminderTimeHour)
                    .putInt("minute", habit.reminderTimeMinute)
                    .putInt("interval", habit.reminderInterval)
                    .build()
            )
            .addTag("habit_notification_${habit.id}")
            .build()

        // Для периодических уведомлений создаем цепочку
        val periodicWork = PeriodicWorkRequestBuilder<HabitNotificationWorker>(
            when (habit.periodicity) {
                Periodicity.DAILY -> 1
                Periodicity.WEEKLY -> 7
            },
            TimeUnit.DAYS
        )
            .setInitialDelay(firstNotificationTime, TimeUnit.MILLISECONDS)
            .setInputData(
                Data.Builder()
                    .putString("habit_id", habit.id)
                    .putString("habit_title", habit.title)
                    .putString("habit_body", habit.body)
                    .putInt("hour", habit.reminderTimeHour)
                    .putInt("minute", habit.reminderTimeMinute)
                    .putInt("interval", habit.reminderInterval)
                    .build()
            )
            .addTag("habit_periodic_${habit.id}")
            .build()

        // Используйте OneTimeWorkRequest для цепочки или PeriodicWorkRequest
        // для истинно периодических уведомлений

        WorkManager.getInstance(context)
            .beginUniqueWork(
                "habit_notification_${habit.id}",
                ExistingWorkPolicy.REPLACE,
                notificationWork
            )
            .enqueue()
    }

    private fun calculateFirstNotificationTime(habit: Habit): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, habit.reminderTimeHour)
            set(Calendar.MINUTE, habit.reminderTimeMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // Если время уже прошло сегодня, планируем на завтра/следующую неделю
        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            when (habit.periodicity) {
                Periodicity.DAILY -> calendar.add(Calendar.DAY_OF_YEAR, 1)
                Periodicity.WEEKLY -> calendar.add(Calendar.DAY_OF_YEAR, 7)
            }
        }

        return calendar.timeInMillis - System.currentTimeMillis()
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
            .setContentTitle("Напоминание о привычке")
            .setContentText("${habit.title} - через ${habit.reminderInterval} минут")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(
                        "Привычка: ${habit.title}\n" +
                                "Описание: ${habit.body}\n" +
                                "Время: ${habit.reminderTimeHour}:${
                                    habit.reminderTimeMinute.toString().padStart(2, '0')
                                }"
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
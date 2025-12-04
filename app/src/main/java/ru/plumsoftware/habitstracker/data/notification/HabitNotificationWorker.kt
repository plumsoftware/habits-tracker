package ru.plumsoftware.habitstracker.data.notification

import android.content.Context
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import kotlinx.coroutines.runBlocking
import org.koin.java.KoinJavaComponent.inject
import ru.plumsoftware.habitstracker.data.database.HabitRepository
import ru.plumsoftware.habitstracker.data.model.Habit
import ru.plumsoftware.habitstracker.data.model.Periodicity
import java.util.concurrent.TimeUnit

class HabitNotificationWorker(
    private val context: Context,
    params: WorkerParameters
) : Worker(context, params) {

    private val habitNotificationManager: HabitNotificationManager by inject(HabitNotificationManager::class.java)
    private val habitRepository: HabitRepository by inject(HabitRepository::class.java)

    override fun doWork(): Result {
        val habitId = inputData.getString("habit_id")

        if (habitId != null) {
            // Загружаем привычку из базы
            runBlocking {
                val habit = habitRepository.getHabitById(habitId)
                if (habit != null) {
                    habitNotificationManager.showNotification(habit)

                    // Планируем следующее уведомление
                    scheduleNextNotification(habit)
                }
            }
        }

        return Result.success()
    }

    private fun scheduleNextNotification(habit: Habit) {
        val intervalMillis = when (habit.periodicity) {
            Periodicity.DAILY -> 24 * 60 * 60 * 1000L // 1 день
            Periodicity.WEEKLY -> 7 * 24 * 60 * 60 * 1000L // 1 неделя
        }

        val delay = if (habit.reminderInterval > 0) {
            habit.reminderInterval * 60 * 1000L // минуты в миллисекунды
        } else {
            intervalMillis
        }

        val workRequest = OneTimeWorkRequestBuilder<HabitNotificationWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
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

        WorkManager.getInstance(context).enqueue(workRequest)
    }
}
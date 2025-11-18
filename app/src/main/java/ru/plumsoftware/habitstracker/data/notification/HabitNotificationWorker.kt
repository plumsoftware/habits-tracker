package ru.plumsoftware.habitstracker.data.notification

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import org.koin.java.KoinJavaComponent.inject
import ru.plumsoftware.habitstracker.data.model.Habit
import ru.plumsoftware.habitstracker.data.model.Periodicity

class HabitNotificationWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {

    private val habitNotificationManager: HabitNotificationManager by inject(HabitNotificationManager::class.java)

    override fun doWork(): Result {
        val habitId = inputData.getString("habit_id")
        val habitTitle = inputData.getString("habit_title")
        val habitBody = inputData.getString("habit_body")

        if (habitId != null && habitTitle != null) {
            val habit = Habit(
                id = habitId,
                title = habitTitle,
                body = habitBody ?: "",
                periodicity = Periodicity.DAILY,
                creationDate = System.currentTimeMillis(),
                targetCount = 1,
                reminderTimeHour = inputData.getInt("hour", 12),
                reminderTimeMinute = inputData.getInt("minute", 0),
                reminderInterval = inputData.getInt("interval", 5)
            )

            habitNotificationManager.showNotification(habit)
        }

        return Result.success()
    }
}
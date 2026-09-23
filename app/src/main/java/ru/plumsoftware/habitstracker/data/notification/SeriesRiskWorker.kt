package ru.plumsoftware.habitstracker.data.notification

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import kotlinx.coroutines.runBlocking
import org.koin.java.KoinJavaComponent.inject
import ru.plumsoftware.habitstracker.data.database.HabitRepository

/**
 * Ежедневная проверка в 20:00 (ТЗ §16): если остались незакрытые привычки с серией ≥ 3 —
 * показать одно уведомление «Серия под угрозой». Затем перепланировать себя на следующий день.
 */
class SeriesRiskWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {

    private val repository: HabitRepository by inject(HabitRepository::class.java)
    private val manager: HabitNotificationManager by inject(HabitNotificationManager::class.java)

    override fun doWork(): Result {
        try {
            val count = runBlocking { repository.habitsAtRiskCountToday() }
            if (count > 0) manager.showSeriesRisk(count)
        } catch (_: Exception) {
        } finally {
            // Перепланируем на следующие 20:00
            manager.scheduleSeriesRiskCheck()
        }
        return Result.success()
    }
}

package ru.plumsoftware.habitstracker.data.database

import kotlinx.coroutines.flow.Flow
import ru.plumsoftware.habitstracker.data.model.DailySteps
import ru.plumsoftware.habitstracker.data.model.dao.WellnessDao
import java.util.Calendar

/** Локальное хранилище дневной статистики шагомера. */
class WellnessRepository(private val dao: WellnessDao) {
    fun stepsForDay(day: Long = System.currentTimeMillis()): Flow<DailySteps?> =
        dao.observeSteps(startOfDay(day))

    fun stepsHistory(from: Long, to: Long): Flow<List<DailySteps>> =
        dao.observeStepsRange(startOfDay(from), startOfDay(to))

    suspend fun saveSteps(day: Long, steps: Int) {
        dao.saveSteps(DailySteps(startOfDay(day), steps.coerceAtLeast(0)))
    }

    private fun startOfDay(timestamp: Long): Long = Calendar.getInstance().run {
        timeInMillis = timestamp
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        timeInMillis
    }
}

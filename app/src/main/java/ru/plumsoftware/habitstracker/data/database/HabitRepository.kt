package ru.plumsoftware.habitstracker.data.database

import kotlinx.coroutines.flow.Flow
import org.koin.dsl.module
import ru.plumsoftware.habitstracker.data.model.Habit
import ru.plumsoftware.habitstracker.data.model.HabitCompletion
import ru.plumsoftware.habitstracker.data.model.HabitWithStatus
import ru.plumsoftware.habitstracker.data.model.Periodicity
import ru.plumsoftware.habitstracker.data.model.dao.HabitCompletionDao
import ru.plumsoftware.habitstracker.data.model.dao.HabitDao
import java.util.Calendar
import java.util.UUID

class HabitRepository(
    private val habitDao: HabitDao,
    private val habitCompletionDao: HabitCompletionDao // Добавляем новый DAO
) {

    fun getAllHabits(): Flow<List<Habit>> = habitDao.getAllHabits()

    suspend fun getHabitById(id: String): Habit? = habitDao.getHabitById(id)

    suspend fun insertHabit(habit: Habit) = habitDao.insertHabit(habit)

    suspend fun updateHabit(habit: Habit) = habitDao.updateHabit(habit)

    suspend fun deleteHabit(habit: Habit) = habitDao.deleteHabit(habit)

    suspend fun deleteHabitById(id: String) = habitDao.deleteHabitById(id)

    fun searchHabits(query: String): Flow<List<Habit>> = habitDao.searchHabits("%$query%")

    fun getHabitsByPeriodicity(periodicity: Int): Flow<List<Habit>> =
        habitDao.getHabitsByPeriodicity(periodicity)

    /**
     * Получить все привычки со статусом выполнения на конкретную дату
     */
    fun getHabitsWithStatus(date: Long): Flow<List<HabitWithStatus>> =
        habitDao.getHabitsWithStatus(date)

    /**
     * Получить привычки со статусом на сегодня
     */
    fun getHabitsForToday(): Flow<List<HabitWithStatus>> {
        val today = getStartOfDay()
        return habitDao.getHabitsWithStatus(today)
    }

    /**
     * Получить привычки со статусом на конкретную дату с фильтром по периодичности
     */
    fun getHabitsWithStatusByPeriodicity(date: Long, periodicity: Periodicity): Flow<List<HabitWithStatus>> {
        return habitDao.getHabitsWithStatusByPeriodicity(date, periodicity)
    }

    /**
     * Отметить привычку как выполненную на конкретную дату
     */
    suspend fun markHabitCompleted(habitId: String, date: Long = getStartOfDay()) {
        val completion = habitCompletionDao.getCompletion(habitId, date)
        if (completion != null) {
            habitCompletionDao.updateCompletion(completion.copy(completed = true))
        } else {
            habitCompletionDao.insertCompletion(
                HabitCompletion(
                    id = UUID.randomUUID().toString(),
                    habitId = habitId,
                    completionDate = date,
                    completed = true
                )
            )
        }
    }

    /**
     * Снять отметку о выполнении привычки на конкретную дату
     */
    suspend fun unmarkHabitCompleted(habitId: String, date: Long = getStartOfDay()) {
        val completion = habitCompletionDao.getCompletion(habitId, date)
        if (completion != null) {
            habitCompletionDao.updateCompletion(completion.copy(completed = false))
        } else {
            habitCompletionDao.insertCompletion(
                HabitCompletion(
                    id = UUID.randomUUID().toString(),
                    habitId = habitId,
                    completionDate = date,
                    completed = false
                )
            )
        }
    }

    /**
     * Получить историю выполнений привычки за период
     */
    suspend fun getCompletionHistory(habitId: String, startDate: Long, endDate: Long): List<HabitCompletion> {
        return habitCompletionDao.getCompletionsInPeriod(habitId, startDate, endDate)
    }

    /**
     * Получить историю выполнений привычки за последние N дней
     */
    suspend fun getCompletionHistoryForLastDays(habitId: String, days: Int): List<HabitCompletion> {
        val endDate = getStartOfDay()
        val startDate = endDate - (days * 24 * 60 * 60 * 1000L)
        return habitCompletionDao.getCompletionsInPeriod(habitId, startDate, endDate)
    }

    /**
     * Получить статус выполнения привычки на конкретную дату
     */
    suspend fun getHabitStatus(habitId: String, date: Long): Boolean {
        return habitCompletionDao.getCompletion(habitId, date)?.completed == true
    }

    /**
     * Утилитарный метод для получения начала дня (00:00:00) в timestamp
     */
    private fun getStartOfDay(timestamp: Long = System.currentTimeMillis()): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }

    companion object {
        val repositoryModule = module {
            single<HabitRepository> { HabitRepository(get(), get()) } // Обновляем с двумя зависимостями
        }
    }
}
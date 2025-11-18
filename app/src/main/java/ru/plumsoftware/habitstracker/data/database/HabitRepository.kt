package ru.plumsoftware.habitstracker.data.database

import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import ru.plumsoftware.habitstracker.data.model.Habit
import ru.plumsoftware.habitstracker.data.model.HabitCompletion
import ru.plumsoftware.habitstracker.data.model.HabitWithStatus
import ru.plumsoftware.habitstracker.data.model.Periodicity
import ru.plumsoftware.habitstracker.data.model.dao.HabitCompletionDao
import ru.plumsoftware.habitstracker.data.model.dao.HabitDao
import ru.plumsoftware.habitstracker.data.notification.HabitNotificationManager
import java.util.Calendar
import java.util.Date
import java.util.UUID

class HabitRepository(
    private val habitDao: HabitDao,
    private val habitCompletionDao: HabitCompletionDao,
    private val habitNotificationManager: HabitNotificationManager
) {

    fun getHabitsWithStatus(date: Long): Flow<List<HabitWithStatus>> {
        Log.d("HabitRepository", "Запрос привычек для даты: ${Date(date)}")
        return habitDao.getHabitsWithStatus(date) // Используем исправленный метод
            .map { allHabits ->
                // Фильтруем по периодичности на стороне Kotlin
                allHabits.filter { habitWithStatus ->
                    isHabitActiveOnDate(habitWithStatus.habit, date)
                }.also { filteredHabits ->
                    Log.d("HabitRepository", "После фильтрации: ${filteredHabits.size} привычек")
                    filteredHabits.forEach { habit ->
                        Log.d(
                            "HabitRepository",
                            " - ${habit.habit.title}, период: ${habit.habit.periodicity}, дата создания: ${
                                Date(
                                    habit.habit.creationDate
                                )
                            }"
                        )
                    }
                }
            }
    }

    private fun isHabitActiveOnDate(habit: Habit, targetDate: Long): Boolean {
        val startOfTargetDay = getStartOfDay(targetDate)
        val startOfCreationDay = getStartOfDay(habit.creationDate)

        // Привычка не активна до даты создания
        if (startOfTargetDay < startOfCreationDay) {
            Log.d("HabitRepository", "Привычка ${habit.title} не активна - дата создания в будущем")
            return false
        }

        // Проверяем периодичность
        val daysBetween = (startOfTargetDay - startOfCreationDay) / (24 * 60 * 60 * 1000)
        val periodicityDays = habit.periodicity.days.toLong()
        val isActive = daysBetween % periodicityDays == 0L

        Log.d(
            "HabitRepository",
            "Привычка ${habit.title}: daysBetween=$daysBetween, periodicityDays=$periodicityDays, isActive=$isActive"
        )

        return isActive
    }

    fun getAllHabits(): Flow<List<Habit>> = habitDao.getAllHabits()

    suspend fun getHabitById(id: String): Habit? = habitDao.getHabitById(id)

    suspend fun insertHabit(habit: Habit) {
        habitDao.insertHabit(habit)
        // Планируем уведомления для новой привычки
        habitNotificationManager.scheduleHabitNotification(habit)
    }

    suspend fun updateHabit(habit: Habit) {
        habitDao.updateHabit(habit)

        // Перепланируем уведомления для обновленной привычки
        habitNotificationManager.cancelHabitNotification(habit.id)
        habitNotificationManager.scheduleHabitNotification(habit)
    }

    suspend fun deleteHabit(habit: Habit) {
        habitDao.deleteHabit(habit)

        // Отменяем уведомления для удаленной привычки
        habitNotificationManager.cancelHabitNotification(habit.id)
    }

    suspend fun deleteHabitById(id: String) {
        habitDao.deleteHabitById(id)

        // Отменяем уведомления для удаленной привычки
        habitNotificationManager.cancelHabitNotification(id)
    }

    fun searchHabits(query: String): Flow<List<Habit>> = habitDao.searchHabits("%$query%")

    fun getHabitsByPeriodicity(periodicity: Int): Flow<List<Habit>> =
        habitDao.getHabitsByPeriodicity(periodicity)

    /**
     * Получить все привычки со статусом выполнения на конкретную дату
     */
//    fun getHabitsWithStatus(date: Long): Flow<List<HabitWithStatus>> =
//        habitDao.getHabitsWithStatus(date)

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
    fun getHabitsWithStatusByPeriodicity(
        date: Long,
        periodicity: Periodicity
    ): Flow<List<HabitWithStatus>> {
        return habitDao.getHabitsWithStatusByPeriodicity(date, periodicity)
    }

    /**
     * Отметить привычку как выполненную на конкретную дату
     */
    suspend fun markHabitCompleted(habitId: String, date: Long) {
        val startOfDay = getStartOfDay(date)

        val existingCompletion = habitCompletionDao.getCompletion(habitId, startOfDay)

        if (existingCompletion != null) {
            // Обновляем существующую запись
            val updatedCompletion = existingCompletion.copy(
                completed = true,
                completedCount = existingCompletion.completedCount + 1
            )
            habitCompletionDao.updateCompletion(updatedCompletion)
        } else {
            // Создаем новую запись
            val completion = HabitCompletion(
                id = UUID.randomUUID().toString(),
                habitId = habitId,
                completionDate = startOfDay,
                completed = true,
                completedCount = 1
            )
            habitCompletionDao.insertCompletion(completion)
        }
    }

    private fun getPeriodForHabit(habit: Habit, date: Long): Pair<Long, Long> {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = date
        }

        return when (habit.periodicity) {
            Periodicity.DAILY -> {
                val start = getStartOfDay(date)
                val end = start + 24 * 60 * 60 * 1000 - 1
                start to end
            }

            Periodicity.WEEKLY -> {
                calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
                val weekStart = getStartOfDay(calendar.timeInMillis)
                calendar.add(Calendar.DAY_OF_WEEK, 6)
                val weekEnd = getStartOfDay(calendar.timeInMillis) + 24 * 60 * 60 * 1000 - 1
                weekStart to weekEnd
            }
        }
    }

    fun getHabitsForDisplay(date: Long): Flow<List<HabitWithStatus>> {
        return habitDao.getAllHabits().map { habits ->
            habits.map { habit ->
                val status = getHabitStatusForDate(habit, date)
                HabitWithStatus(habit, status)
            }
        }
    }

    /**
     * Получить статус привычки для конкретной даты
     */
    private suspend fun getHabitStatusForDate(habit: Habit, date: Long): Boolean {
        val (periodStart, periodEnd) = getPeriodForHabit(habit, date)
        val completions =
            habitCompletionDao.getCompletionForPeriod(habit.id, periodStart, periodEnd)
        return completions.any { it.completed }
    }

    /**
     * Снять отметку о выполнении привычки на конкретную дату
     */
    suspend fun unmarkHabitCompleted(habitId: String, date: Long) {
        val startOfDay = getStartOfDay(date)

        val existingCompletion = habitCompletionDao.getCompletion(habitId, startOfDay)

        if (existingCompletion != null) {
            if (existingCompletion.completedCount > 1) {
                // Если счетчик больше 1, уменьшаем его
                val updatedCompletion = existingCompletion.copy(
                    completedCount = existingCompletion.completedCount - 1,
                    completed = existingCompletion.completedCount - 1 > 0
                )
                habitCompletionDao.updateCompletion(updatedCompletion)
            } else {
                // Если счетчик 1 или меньше, удаляем запись полностью
                habitCompletionDao.deleteCompletion(existingCompletion.id)
            }
        }
        // Если записи нет, ничего не делаем
    }

    /**
     * Получить историю выполнений привычки за период
     */
    suspend fun getCompletionHistory(
        habitId: String,
        startDate: Long,
        endDate: Long
    ): List<HabitCompletion> {
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
            single<HabitNotificationManager> {
                HabitNotificationManager(context = androidContext())
            }

            single<HabitRepository> {
                HabitRepository(
                    get(),
                    get(),
                    get()
                )
            } // Обновляем с двумя зависимостями
        }
    }
}
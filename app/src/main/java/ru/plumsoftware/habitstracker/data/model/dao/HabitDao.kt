package ru.plumsoftware.habitstracker.data.model.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import ru.plumsoftware.habitstracker.data.model.Habit
import ru.plumsoftware.habitstracker.data.model.HabitWithStatus
import ru.plumsoftware.habitstracker.data.model.Periodicity

@Dao
interface HabitDao {
    @Insert
    suspend fun insertHabit(habit: Habit)

    @Update
    suspend fun updateHabit(habit: Habit)

    @Delete
    suspend fun deleteHabit(habit: Habit)

    @Query("DELETE FROM habits WHERE id = :id")
    suspend fun deleteHabitById(id: String)

    // Полная очистка привычек (отметки удалятся каскадом по FK)
    @Query("DELETE FROM habits")
    suspend fun deleteAllHabits()

    // Активные привычки (без архивных) — для всех рабочих списков.
    @Query("SELECT * FROM habits WHERE archived = 0")
    fun getAllHabits(): Flow<List<Habit>>

    // Все привычки, включая архивные — для бэкапа.
    @Query("SELECT * FROM habits")
    suspend fun getAllRaw(): List<Habit>

    // Архивные привычки (ТЗ §11.3).
    @Query("SELECT * FROM habits WHERE archived = 1")
    fun getArchivedHabits(): Flow<List<Habit>>

    @Query("UPDATE habits SET archived = :archived WHERE id = :id")
    suspend fun setArchived(id: String, archived: Boolean)

    @Query("SELECT * FROM habits WHERE id = :id")
    suspend fun getHabitById(id: String): Habit?

    @Query("SELECT * FROM habits WHERE (title LIKE :query OR body LIKE :query) AND archived = 0")
    fun searchHabits(query: String): Flow<List<Habit>>

    @Query("SELECT * FROM habits WHERE periodicity = :periodicity")
    fun getHabitsByPeriodicity(periodicity: Int): Flow<List<Habit>>

    /**
     * Получить привычки со статусом на дату
     */
    @Transaction
    @Query("""
        SELECT habits.*, habit_completions.completed, IFNULL(habit_completions.completedCount, 0) as count
        FROM habits
        LEFT JOIN habit_completions ON habits.id = habit_completions.habitId 
        AND habit_completions.completionDate = :date
        WHERE habits.creationDate <= :date AND habits.archived = 0
    """)
    fun getHabitsWithStatus(date: Long): Flow<List<HabitWithStatus>>

    /**
     * Получить привычки со статусом на дату с фильтром по периодичности
     */
    @Transaction
    @Query("""
        SELECT habits.*, habit_completions.completed, IFNULL(habit_completions.completedCount, 0) as count
        FROM habits
        LEFT JOIN habit_completions ON habits.id = habit_completions.habitId 
        AND habit_completions.completionDate = :date
        WHERE habits.periodicity = :periodicity
        AND habits.creationDate <= :date AND habits.archived = 0
    """)
    fun getHabitsWithStatusByPeriodicity(date: Long, periodicity: Periodicity): Flow<List<HabitWithStatus>>

    /**
     * Новый метод DAO без фильтрации по периодичности
     */
    @Transaction
    @Query("""
        SELECT habits.*, habit_completions.completed, IFNULL(habit_completions.completedCount, 0) as count
        FROM habits
        LEFT JOIN habit_completions ON habits.id = habit_completions.habitId 
        AND habit_completions.completionDate = :date
        WHERE habits.creationDate <= :date AND habits.archived = 0
    """)
    fun getAllHabitsWithCompletions(date: Long): Flow<List<HabitWithStatus>>
}
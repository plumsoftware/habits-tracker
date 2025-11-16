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

    @Query("SELECT * FROM habits")
    fun getAllHabits(): Flow<List<Habit>>

    @Query("SELECT * FROM habits WHERE id = :id")
    suspend fun getHabitById(id: String): Habit?

    @Query("SELECT * FROM habits WHERE title LIKE :query OR body LIKE :query")
    fun searchHabits(query: String): Flow<List<Habit>>

    @Query("SELECT * FROM habits WHERE periodicity = :periodicity")
    fun getHabitsByPeriodicity(periodicity: Int): Flow<List<Habit>>

    /**
     * Получить привычки со статусом на дату, которые активны на эту дату
     * (дата создания привычки <= выбранной даты)
     */
    @Transaction
    @Query("""
        SELECT habits.*, habit_completions.completed 
        FROM habits 
        LEFT JOIN habit_completions ON habits.id = habit_completions.habitId 
        AND habit_completions.completionDate = :date
        WHERE habits.creationDate <= :date
    """)
    fun getHabitsWithStatus(date: Long): Flow<List<HabitWithStatus>>

    /**
     * Получить привычки со статусом на дату с фильтром по периодичности
     */
    @Transaction
    @Query("""
        SELECT habits.*, habit_completions.completed 
        FROM habits 
        LEFT JOIN habit_completions ON habits.id = habit_completions.habitId 
        AND habit_completions.completionDate = :date
        WHERE habits.periodicity = :periodicity
        AND habits.creationDate <= :date
    """)
    fun getHabitsWithStatusByPeriodicity(date: Long, periodicity: Periodicity): Flow<List<HabitWithStatus>>

    /**
     * Новый метод DAO без фильтрации по периодичности
     */
    @Transaction
    @Query("""
        SELECT habits.*, habit_completions.completed 
        FROM habits 
        LEFT JOIN habit_completions ON habits.id = habit_completions.habitId 
        AND habit_completions.completionDate = :date
        WHERE habits.creationDate <= :date
    """)
    fun getAllHabitsWithCompletions(date: Long): Flow<List<HabitWithStatus>>
}
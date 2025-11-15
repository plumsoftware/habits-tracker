package ru.plumsoftware.habitstracker.data.model.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import ru.plumsoftware.habitstracker.data.model.Habit

@Dao
interface HabitDao {

    @Query("SELECT * FROM habits ORDER BY creationDate DESC")
    fun getAllHabits(): Flow<List<Habit>>

    @Query("SELECT * FROM habits WHERE id = :id")
    suspend fun getHabitById(id: String): Habit?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabit(habit: Habit)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllHabits(habits: List<Habit>)

    @Update
    suspend fun updateHabit(habit: Habit)

    @Delete
    suspend fun deleteHabit(habit: Habit)

    @Query("DELETE FROM habits WHERE id = :id")
    suspend fun deleteHabitById(id: String)

    @Query("DELETE FROM habits")
    suspend fun deleteAllHabits()

    @Query("SELECT * FROM habits WHERE title LIKE :query OR body LIKE :query")
    fun searchHabits(query: String): Flow<List<Habit>>

    @Query("SELECT * FROM habits WHERE periodicity = :periodicity")
    fun getHabitsByPeriodicity(periodicity: Int): Flow<List<Habit>>
}
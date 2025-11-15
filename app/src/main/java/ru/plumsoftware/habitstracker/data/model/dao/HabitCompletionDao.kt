package ru.plumsoftware.habitstracker.data.model.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import ru.plumsoftware.habitstracker.data.model.HabitCompletion

@Dao
interface HabitCompletionDao {
    @Insert
    suspend fun insertCompletion(completion: HabitCompletion)

    @Update
    suspend fun updateCompletion(completion: HabitCompletion)

    @Query("DELETE FROM habit_completions WHERE id = :id")
    suspend fun deleteCompletion(id: String)

    @Query("SELECT * FROM habit_completions WHERE habitId = :habitId AND completionDate = :date")
    suspend fun getCompletion(habitId: String, date: Long): HabitCompletion?

    @Query("SELECT * FROM habit_completions WHERE habitId = :habitId AND completionDate BETWEEN :startDate AND :endDate")
    suspend fun getCompletionsInPeriod(habitId: String, startDate: Long, endDate: Long): List<HabitCompletion>

    @Query("SELECT * FROM habit_completions WHERE habitId = :habitId")
    suspend fun getAllCompletionsForHabit(habitId: String): List<HabitCompletion>
}
package ru.plumsoftware.habitstracker.data.model.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import ru.plumsoftware.habitstracker.data.model.DailySteps

@Dao
interface WellnessDao {
    @Query("SELECT * FROM daily_steps WHERE dayStartMillis = :dayStart")
    fun observeSteps(dayStart: Long): Flow<DailySteps?>

    @Query("SELECT * FROM daily_steps WHERE dayStartMillis BETWEEN :from AND :to ORDER BY dayStartMillis")
    fun observeStepsRange(from: Long, to: Long): Flow<List<DailySteps>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSteps(value: DailySteps)
}

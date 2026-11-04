package ru.plumsoftware.habitstracker.data.model.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import ru.plumsoftware.habitstracker.data.model.*

@Dao
interface WellnessDao {
    @Query("SELECT * FROM daily_steps WHERE dayStartMillis = :dayStart")
    fun observeSteps(dayStart: Long): Flow<DailySteps?>

    @Query("SELECT * FROM daily_steps WHERE dayStartMillis BETWEEN :from AND :to ORDER BY dayStartMillis")
    fun observeStepsRange(from: Long, to: Long): Flow<List<DailySteps>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSteps(value: DailySteps)

    @Query("SELECT * FROM water_entries WHERE consumedAtMillis >= :from AND consumedAtMillis < :to ORDER BY consumedAtMillis")
    fun observeWaterForDay(from: Long, to: Long): Flow<List<WaterEntry>>

    @Insert
    suspend fun addWater(value: WaterEntry)

    @Query("DELETE FROM water_entries WHERE id = :id")
    suspend fun deleteWater(id: String)

    @Query("SELECT * FROM workout_sessions ORDER BY startedAtMillis DESC")
    fun observeWorkouts(): Flow<List<WorkoutSession>>

    @Query("SELECT * FROM workout_sessions WHERE id = :id")
    suspend fun getWorkout(id: String): WorkoutSession?

    @Insert
    suspend fun addWorkout(value: WorkoutSession)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveWorkout(value: WorkoutSession)

    @Query("SELECT * FROM workout_sets WHERE workoutId = :workoutId ORDER BY exerciseName, setNumber")
    fun observeWorkoutSets(workoutId: String): Flow<List<WorkoutSet>>

    @Insert
    suspend fun addWorkoutSet(value: WorkoutSet)

    @Query("DELETE FROM workout_sets WHERE id = :id")
    suspend fun deleteWorkoutSet(id: String)

    @Query("SELECT * FROM sleep_sessions ORDER BY startedAtMillis DESC")
    fun observeSleepSessions(): Flow<List<SleepSession>>

    @Insert
    suspend fun addSleepSession(value: SleepSession)

    @Query("SELECT * FROM body_measurements ORDER BY measuredAtMillis DESC")
    fun observeBodyMeasurements(): Flow<List<BodyMeasurement>>

    @Insert
    suspend fun addBodyMeasurement(value: BodyMeasurement)

    @Query("SELECT * FROM mood_entries WHERE recordedAtMillis >= :from AND recordedAtMillis < :to ORDER BY recordedAtMillis")
    fun observeMoodForDay(from: Long, to: Long): Flow<List<MoodEntry>>

    @Insert
    suspend fun addMood(value: MoodEntry)
}

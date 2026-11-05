package ru.plumsoftware.habitstracker.data.model.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import ru.plumsoftware.habitstracker.data.model.*

@Dao
interface WellnessDao {
    @Query("SELECT * FROM sleep_records ORDER BY dayStartMillis DESC")
    fun observeSleepRecords(): Flow<List<SleepRecord>>

    @Query("SELECT * FROM sleep_records WHERE dayStartMillis = :dayStart")
    fun observeSleep(dayStart: Long): Flow<SleepRecord?>

    @Insert
    suspend fun insertSleep(value: SleepRecord)

    @Update
    suspend fun updateSleep(value: SleepRecord): Int

    @Query("SELECT * FROM daily_steps WHERE dayStartMillis = :dayStart")
    fun observeSteps(dayStart: Long): Flow<DailySteps?>

    @Query("SELECT * FROM daily_steps WHERE dayStartMillis BETWEEN :from AND :to ORDER BY dayStartMillis")
    fun observeStepsRange(from: Long, to: Long): Flow<List<DailySteps>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSteps(value: DailySteps)

    @Query("SELECT * FROM workout_sessions ORDER BY startedAtMillis DESC")
    fun observeWorkouts(): Flow<List<WorkoutSession>>

    @Query("SELECT * FROM workout_sessions WHERE id = :id LIMIT 1")
    fun observeWorkout(id: String): Flow<WorkoutSession?>

    @Insert
    suspend fun insertWorkout(session: WorkoutSession)

    @Query("SELECT * FROM workout_sessions WHERE id = :id")
    suspend fun getWorkout(id: String): WorkoutSession?

    @Query("SELECT EXISTS(SELECT 1 FROM workout_sessions WHERE startedAtMillis >= :start AND startedAtMillis < :end AND endedAtMillis IS NOT NULL)")
    suspend fun hasFinishedWorkout(start: Long, end: Long): Boolean

    @Query("UPDATE workout_sessions SET endedAtMillis = :endedAt WHERE id = :id AND endedAtMillis IS NULL")
    suspend fun finishWorkout(id: String, endedAt: Long)

    @Query("UPDATE workout_sessions SET title = :title WHERE id = :id")
    suspend fun updateWorkoutTitle(id: String, title: String)

    @Query("UPDATE workout_sessions SET notes = :notes WHERE id = :id")
    suspend fun updateWorkoutNotes(id: String, notes: String)

    @Query("DELETE FROM workout_sessions WHERE id = :id")
    suspend fun deleteWorkout(id: String)

    @Query("SELECT * FROM workout_exercises WHERE sessionId = :sessionId ORDER BY sortOrder")
    fun observeExercises(sessionId: String): Flow<List<WorkoutExercise>>

    @Query("SELECT COALESCE(MAX(sortOrder), -1) + 1 FROM workout_exercises WHERE sessionId = :sessionId")
    suspend fun nextExerciseOrder(sessionId: String): Int

    @Insert
    suspend fun insertExercise(exercise: WorkoutExercise)

    @Query("DELETE FROM workout_exercises WHERE id = :id")
    suspend fun deleteExercise(id: String)

    @Query("SELECT * FROM workout_sets WHERE exerciseId = :exerciseId ORDER BY setNumber")
    fun observeSets(exerciseId: String): Flow<List<WorkoutSet>>

    @Query("SELECT COALESCE(MAX(setNumber), 0) + 1 FROM workout_sets WHERE exerciseId = :exerciseId")
    suspend fun nextSetNumber(exerciseId: String): Int

    @Insert
    suspend fun insertSet(set: WorkoutSet)

    @Update
    suspend fun updateSet(set: WorkoutSet)

    @Query("DELETE FROM workout_sets WHERE id = :id")
    suspend fun deleteSet(id: String)
}

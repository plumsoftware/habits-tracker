package ru.plumsoftware.habitstracker.data.database

import kotlinx.coroutines.flow.Flow
import ru.plumsoftware.habitstracker.data.model.*
import ru.plumsoftware.habitstracker.data.model.dao.WellnessDao
import java.util.Calendar
import java.util.UUID

/** Локальное хранилище активности и тренировок. */
class WellnessRepository(private val dao: WellnessDao) {
    fun stepsForDay(day: Long = System.currentTimeMillis()): Flow<DailySteps?> =
        dao.observeSteps(startOfDay(day))

    fun stepsHistory(from: Long, to: Long): Flow<List<DailySteps>> =
        dao.observeStepsRange(startOfDay(from), startOfDay(to))

    suspend fun saveSteps(day: Long, steps: Int) {
        dao.saveSteps(DailySteps(startOfDay(day), steps.coerceAtLeast(0)))
    }

    fun workouts(): Flow<List<WorkoutSession>> = dao.observeWorkouts()
    fun workout(id: String): Flow<WorkoutSession?> = dao.observeWorkout(id)
    fun exercises(sessionId: String): Flow<List<WorkoutExercise>> = dao.observeExercises(sessionId)
    fun sets(exerciseId: String): Flow<List<WorkoutSet>> = dao.observeSets(exerciseId)

    suspend fun startWorkout(title: String = "Тренировка"): String {
        val id = UUID.randomUUID().toString()
        dao.insertWorkout(WorkoutSession(id, title.trim().ifBlank { "Тренировка" }, System.currentTimeMillis()))
        return id
    }

    suspend fun finishWorkout(id: String) = dao.finishWorkout(id, System.currentTimeMillis())
    suspend fun renameWorkout(id: String, title: String) =
        dao.updateWorkoutTitle(id, title.trim().ifBlank { "Тренировка" })
    suspend fun updateNotes(id: String, notes: String) = dao.updateWorkoutNotes(id, notes)
    suspend fun deleteWorkout(id: String) = dao.deleteWorkout(id)

    suspend fun addExercise(sessionId: String, name: String): String {
        val cleanName = name.trim()
        require(cleanName.isNotEmpty()) { "Название упражнения не может быть пустым" }
        val id = UUID.randomUUID().toString()
        dao.insertExercise(WorkoutExercise(id, sessionId, cleanName, dao.nextExerciseOrder(sessionId)))
        return id
    }

    suspend fun deleteExercise(id: String) = dao.deleteExercise(id)

    suspend fun addSet(exerciseId: String, reps: Int = 10, weightKg: Double = 0.0): String {
        val id = UUID.randomUUID().toString()
        dao.insertSet(
            WorkoutSet(
                id = id,
                exerciseId = exerciseId,
                setNumber = dao.nextSetNumber(exerciseId),
                reps = reps.coerceAtLeast(0),
                weightKg = weightKg.coerceAtLeast(0.0)
            )
        )
        return id
    }

    suspend fun updateSet(set: WorkoutSet) = dao.updateSet(set)
    suspend fun deleteSet(id: String) = dao.deleteSet(id)

    private fun startOfDay(timestamp: Long): Long = Calendar.getInstance().run {
        timeInMillis = timestamp
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        timeInMillis
    }
}

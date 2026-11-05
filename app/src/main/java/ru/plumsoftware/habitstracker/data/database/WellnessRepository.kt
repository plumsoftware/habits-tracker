package ru.plumsoftware.habitstracker.data.database

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import ru.plumsoftware.habitstracker.data.model.*
import ru.plumsoftware.habitstracker.data.model.dao.WellnessDao
import java.util.Calendar
import java.util.UUID

enum class WellnessActivity { WORKOUTS, STEPS, SLEEP }

/** Локальное хранилище активности и тренировок. */
class WellnessRepository(
    private val dao: WellnessDao,
    private val database: HabitDatabase,
    private val habits: HabitRepository,
    private val onActivityChanged: (WellnessActivity) -> Unit,
) {
    fun stepsForDay(day: Long = System.currentTimeMillis()): Flow<DailySteps?> =
        dao.observeSteps(startOfDay(day))

    fun stepsHistory(from: Long, to: Long): Flow<List<DailySteps>> =
        dao.observeStepsRange(startOfDay(from), startOfDay(to))

    suspend fun saveSteps(day: Long, steps: Int) {
        dao.saveSteps(DailySteps(startOfDay(day), steps.coerceAtLeast(0)))
        onActivityChanged(WellnessActivity.STEPS)
    }

    fun sleepRecords(): Flow<List<SleepRecord>> = dao.observeSleepRecords()

    fun sleepForDay(day: Long): Flow<SleepRecord?> = dao.observeSleep(startOfDay(day))

    suspend fun saveSleep(startedAt: Long, endedAt: Long, editing: Boolean) {
        require(endedAt - startedAt in 60_000L..(24 * 60 * 60_000L))
        require(endedAt <= System.currentTimeMillis())
        val record = SleepRecord(startOfDay(endedAt), startedAt, endedAt)
        database.withTransaction {
            if (editing) check(dao.updateSleep(record) == 1) else dao.insertSleep(record)
            completeLinkedHabits(HabitActivityLink.SLEEP, record.dayStartMillis)
        }
        onActivityChanged(WellnessActivity.SLEEP)
    }

    fun workouts(): Flow<List<WorkoutSession>> = dao.observeWorkouts()
    fun workout(id: String): Flow<WorkoutSession?> = dao.observeWorkout(id)
    fun exercises(sessionId: String): Flow<List<WorkoutExercise>> = dao.observeExercises(sessionId)
    fun sets(exerciseId: String): Flow<List<WorkoutSet>> = dao.observeSets(exerciseId)

    suspend fun startWorkout(title: String = "Тренировка"): String {
        val id = UUID.randomUUID().toString()
        dao.insertWorkout(WorkoutSession(id, title.trim().ifBlank { "Тренировка" }, System.currentTimeMillis()))
        onActivityChanged(WellnessActivity.WORKOUTS)
        return id
    }

    suspend fun finishWorkout(id: String) {
        val finished = database.withTransaction {
            val session = dao.getWorkout(id) ?: return@withTransaction false
            if (session.endedAtMillis != null) return@withTransaction false
            dao.finishWorkout(id, System.currentTimeMillis())
            completeLinkedHabits(HabitActivityLink.WORKOUT, startOfDay(session.startedAtMillis))
            true
        }
        if (finished) onActivityChanged(WellnessActivity.WORKOUTS)
    }

    suspend fun completeWorkoutHabitsForDay(day: Long): Boolean = database.withTransaction {
        val start = startOfDay(day)
        val end = Calendar.getInstance().apply {
            timeInMillis = start
            add(Calendar.DAY_OF_YEAR, 1)
        }.timeInMillis
        val recorded = dao.hasFinishedWorkout(start, end)
        if (recorded) completeLinkedHabits(HabitActivityLink.WORKOUT, start)
        recorded
    }

    private suspend fun completeLinkedHabits(activity: String, dayStart: Long) {
        val completionDao = database.habitCompletionDao()
        for (habit in database.habitDao().getLinkedHabits(activity)) {
            if (!habits.isActiveOn(habit, dayStart)) continue
            val existing = completionDao.getCompletion(habit.id, dayStart)
            if (existing?.completed == true) continue
            val completion = HabitCompletion(
                id = existing?.id ?: UUID.randomUUID().toString(),
                habitId = habit.id,
                completionDate = dayStart,
                completed = true,
                completedCount = habit.targetCount.coerceAtLeast(1),
                completedAtMillis = System.currentTimeMillis(),
            )
            if (existing == null) completionDao.insertCompletion(completion)
            else completionDao.updateCompletion(completion)
        }
    }

    suspend fun renameWorkout(id: String, title: String) {
        dao.updateWorkoutTitle(id, title.trim().ifBlank { "Тренировка" })
        onActivityChanged(WellnessActivity.WORKOUTS)
    }

    suspend fun updateNotes(id: String, notes: String) = dao.updateWorkoutNotes(id, notes)

    suspend fun deleteWorkout(id: String) {
        dao.deleteWorkout(id)
        onActivityChanged(WellnessActivity.WORKOUTS)
    }

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

package ru.plumsoftware.habitstracker.data.database

import kotlinx.coroutines.flow.Flow
import ru.plumsoftware.habitstracker.data.model.*
import ru.plumsoftware.habitstracker.data.model.dao.WellnessDao
import java.util.Calendar
import java.util.UUID

/** Единая точка доступа к данным новых трекеров. Все записи остаются локальными в Room. */
class WellnessRepository(private val dao: WellnessDao) {
    fun stepsForDay(day: Long = System.currentTimeMillis()): Flow<DailySteps?> =
        dao.observeSteps(startOfDay(day))

    fun stepsHistory(from: Long, to: Long): Flow<List<DailySteps>> =
        dao.observeStepsRange(startOfDay(from), startOfDay(to))

    suspend fun saveSteps(day: Long, steps: Int) {
        dao.saveSteps(DailySteps(startOfDay(day), steps.coerceAtLeast(0)))
    }

    fun waterForDay(day: Long = System.currentTimeMillis()): Flow<List<WaterEntry>> {
        val start = startOfDay(day)
        return dao.observeWaterForDay(start, nextDay(start))
    }

    suspend fun addWater(amountMl: Int, atMillis: Long = System.currentTimeMillis()) {
        require(amountMl in 1..5000) { "Объём воды должен быть от 1 до 5000 мл" }
        dao.addWater(WaterEntry(UUID.randomUUID().toString(), amountMl, atMillis))
    }

    suspend fun deleteWater(id: String) = dao.deleteWater(id)

    fun workouts(): Flow<List<WorkoutSession>> = dao.observeWorkouts()
    fun workoutSets(workoutId: String): Flow<List<WorkoutSet>> = dao.observeWorkoutSets(workoutId)

    suspend fun startWorkout(atMillis: Long = System.currentTimeMillis()): String {
        val id = UUID.randomUUID().toString()
        dao.addWorkout(WorkoutSession(id = id, startedAtMillis = atMillis))
        return id
    }

    suspend fun finishWorkout(id: String, atMillis: Long = System.currentTimeMillis()) {
        val workout = dao.getWorkout(id) ?: return
        dao.saveWorkout(workout.copy(endedAtMillis = atMillis))
    }

    suspend fun addSet(
        workoutId: String,
        exerciseName: String,
        setNumber: Int,
        repetitions: Int,
        weightKg: Double
    ) {
        require(exerciseName.isNotBlank())
        require(setNumber > 0 && repetitions > 0 && weightKg >= 0.0)
        dao.addWorkoutSet(WorkoutSet(
            id = UUID.randomUUID().toString(),
            workoutId = workoutId,
            exerciseName = exerciseName.trim(),
            setNumber = setNumber,
            repetitions = repetitions,
            weightKg = weightKg
        ))
    }

    fun sleepSessions(): Flow<List<SleepSession>> = dao.observeSleepSessions()
    suspend fun addSleep(start: Long, end: Long, quality: Int? = null, note: String = "") {
        require(end > start) { "Время окончания сна должно быть позже начала" }
        require(quality == null || quality in 1..5)
        dao.addSleepSession(SleepSession(UUID.randomUUID().toString(), start, end, quality, note))
    }

    fun bodyMeasurements(): Flow<List<BodyMeasurement>> = dao.observeBodyMeasurements()
    suspend fun addBodyMeasurement(
        measuredAt: Long = System.currentTimeMillis(),
        weightKg: Double? = null,
        bodyFatPercent: Double? = null,
        waistCm: Double? = null,
        chestCm: Double? = null,
        hipsCm: Double? = null,
        note: String = ""
    ) {
        require(listOfNotNull(weightKg, bodyFatPercent, waistCm, chestCm, hipsCm).all { it > 0.0 })
        require(bodyFatPercent == null || bodyFatPercent <= 100.0)
        dao.addBodyMeasurement(BodyMeasurement(
            UUID.randomUUID().toString(), measuredAt, weightKg, bodyFatPercent,
            waistCm, chestCm, hipsCm, note
        ))
    }

    fun moodForDay(day: Long = System.currentTimeMillis()): Flow<List<MoodEntry>> {
        val start = startOfDay(day)
        return dao.observeMoodForDay(start, nextDay(start))
    }

    suspend fun addMood(mood: Int, note: String = "", atMillis: Long = System.currentTimeMillis()) {
        require(mood in 1..5) { "Настроение должно быть от 1 до 5" }
        dao.addMood(MoodEntry(UUID.randomUUID().toString(), atMillis, mood, note))
    }

    private fun startOfDay(timestamp: Long): Long = Calendar.getInstance().run {
        timeInMillis = timestamp
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        timeInMillis
    }

    private fun nextDay(start: Long): Long = Calendar.getInstance().run {
        timeInMillis = start
        add(Calendar.DAY_OF_YEAR, 1)
        timeInMillis
    }
}

package ru.plumsoftware.habitstracker.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Итог шагомера за локальный календарный день. */
@Entity(tableName = "daily_steps")
data class DailySteps(
    @PrimaryKey val dayStartMillis: Long,
    val steps: Int,
    val updatedAtMillis: Long = System.currentTimeMillis()
)

/** Одна запись выпитой воды. */
@Entity(tableName = "water_entries", indices = [Index("consumedAtMillis")])
data class WaterEntry(
    @PrimaryKey val id: String,
    val amountMl: Int,
    val consumedAtMillis: Long
)

/** Тренировка в зале. */
@Entity(tableName = "workout_sessions", indices = [Index("startedAtMillis")])
data class WorkoutSession(
    @PrimaryKey val id: String,
    val startedAtMillis: Long,
    val endedAtMillis: Long? = null,
    val note: String = ""
)

/** Подход упражнения в рамках тренировки. */
@Entity(
    tableName = "workout_sets",
    foreignKeys = [ForeignKey(
        entity = WorkoutSession::class,
        parentColumns = ["id"],
        childColumns = ["workoutId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("workoutId")]
)
data class WorkoutSet(
    @PrimaryKey val id: String,
    val workoutId: String,
    val exerciseName: String,
    val setNumber: Int,
    val repetitions: Int,
    val weightKg: Double = 0.0
)

/** Интервал сна; время хранится в UTC millis, день привязки рассчитывается в локальной зоне. */
@Entity(tableName = "sleep_sessions", indices = [Index("startedAtMillis")])
data class SleepSession(
    @PrimaryKey val id: String,
    val startedAtMillis: Long,
    val endedAtMillis: Long,
    val quality: Int? = null,
    val note: String = ""
)

/** Замеры веса и параметров тела. */
@Entity(tableName = "body_measurements", indices = [Index("measuredAtMillis")])
data class BodyMeasurement(
    @PrimaryKey val id: String,
    val measuredAtMillis: Long,
    val weightKg: Double? = null,
    val bodyFatPercent: Double? = null,
    val waistCm: Double? = null,
    val chestCm: Double? = null,
    val hipsCm: Double? = null,
    val note: String = ""
)

/** Оценка настроения по шкале 1..5. */
@Entity(tableName = "mood_entries", indices = [Index("recordedAtMillis")])
data class MoodEntry(
    @PrimaryKey val id: String,
    val recordedAtMillis: Long,
    val mood: Int,
    val note: String = ""
)

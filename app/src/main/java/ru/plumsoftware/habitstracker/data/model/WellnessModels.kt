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

@Entity(tableName = "workout_sessions", indices = [Index(value = ["startedAtMillis"])])
data class WorkoutSession(
    @PrimaryKey val id: String,
    val title: String,
    val startedAtMillis: Long,
    val endedAtMillis: Long? = null,
    val notes: String = "",
    val createdAtMillis: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "workout_exercises",
    indices = [Index(value = ["sessionId"])],
    foreignKeys = [ForeignKey(entity = WorkoutSession::class, parentColumns = ["id"], childColumns = ["sessionId"], onDelete = ForeignKey.CASCADE)]
)
data class WorkoutExercise(
    @PrimaryKey val id: String,
    val sessionId: String,
    val name: String,
    val sortOrder: Int
)

@Entity(
    tableName = "workout_sets",
    indices = [Index(value = ["exerciseId"])],
    foreignKeys = [ForeignKey(entity = WorkoutExercise::class, parentColumns = ["id"], childColumns = ["exerciseId"], onDelete = ForeignKey.CASCADE)]
)
data class WorkoutSet(
    @PrimaryKey val id: String,
    val exerciseId: String,
    val setNumber: Int,
    val reps: Int,
    val weightKg: Double,
    val completed: Boolean = true
)

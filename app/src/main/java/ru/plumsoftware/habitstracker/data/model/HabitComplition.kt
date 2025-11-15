package ru.plumsoftware.habitstracker.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "habit_completions",
    foreignKeys = [ForeignKey(
        entity = Habit::class,
        parentColumns = ["id"],
        childColumns = ["habitId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["habitId", "completionDate"])]
)
data class HabitCompletion(
    @PrimaryKey
    val id: String,
    val habitId: String,
    val completionDate: Long, // Дата выполнения (начало дня в timestamp)
    val completed: Boolean = false,
    val completedCount: Int = 0 // Если нужно учитывать количество
)
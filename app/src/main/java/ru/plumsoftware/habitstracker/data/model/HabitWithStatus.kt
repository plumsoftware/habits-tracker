package ru.plumsoftware.habitstracker.data.model

import androidx.room.ColumnInfo
import androidx.room.Embedded

data class HabitWithStatus(
    @Embedded
    val habit: Habit,
    @ColumnInfo(name = "completed")
    val completed: Boolean?,
    // Прогресс к цели в день (для targetCount > 1). 0, если отметок нет.
    @ColumnInfo(name = "count")
    val completedCount: Int = 0
)
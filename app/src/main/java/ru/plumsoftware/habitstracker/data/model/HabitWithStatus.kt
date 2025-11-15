package ru.plumsoftware.habitstracker.data.model

import androidx.room.ColumnInfo
import androidx.room.Embedded

data class HabitWithStatus(
    @Embedded
    val habit: Habit,
    @ColumnInfo(name = "completed")
    val completed: Boolean?
)
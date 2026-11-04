package ru.plumsoftware.habitstracker.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Итог шагомера за локальный календарный день. */
@Entity(tableName = "daily_steps")
data class DailySteps(
    @PrimaryKey val dayStartMillis: Long,
    val steps: Int,
    val updatedAtMillis: Long = System.currentTimeMillis()
)

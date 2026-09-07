package ru.plumsoftware.habitstracker.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "habits")
data class Habit(
    @PrimaryKey
    val id: String,
    val title: String,
    val body: String,
    val periodicity: Periodicity,
    val creationDate: Long,
    val targetCount: Int = 1, // Количество выполнений за период
    val reminderTimeHour: Int,
    val reminderTimeMinute: Int,
    val reminderInterval: Int,
    // Ссылка на категорию (null = без категории). FK не задаём намеренно:
    // удаление категории не должно каскадно трогать привычки.
    val categoryId: String? = null
)

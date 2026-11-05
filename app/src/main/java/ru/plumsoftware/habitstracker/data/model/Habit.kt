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
    val categoryId: String? = null,
    // Эмодзи-иконка привычки (ТЗ §2). Дефолт для старых записей — «🎯».
    val emoji: String = "🎯",
    // Дни недели для режима «По дням» (BY_WEEKDAYS): CSV из Calendar.DAY_OF_WEEK (1=вс..7=сб).
    // Пусто = режим не по дням (используется periodicity).
    val weekdays: String = "",
    // Связывание привычек (ТЗ §7.4.1): id родительской привычки, после которой выполняется эта.
    val chainedAfterId: String? = null,
    // Заморозка стрика (ТЗ §7.4.2): 1 пропуск в неделю не сбрасывает серию.
    val freezeEnabled: Boolean = false,
    // Архив (ТЗ §11.3): привычка скрыта из активных списков, история отметок сохраняется.
    val archived: Boolean = false,
    val activityLink: String? = null
) {
    /** Множество дней недели (Calendar.DAY_OF_WEEK) для BY_WEEKDAYS; пусто если не задано. */
    val weekdaySet: Set<Int>
        get() = weekdays.split(",").mapNotNull { it.trim().toIntOrNull() }.toSet()

    val isByWeekdays: Boolean get() = weekdays.isNotBlank()
}

object HabitActivityLink {
    const val SLEEP = "sleep"
    const val WORKOUT = "workout"
}

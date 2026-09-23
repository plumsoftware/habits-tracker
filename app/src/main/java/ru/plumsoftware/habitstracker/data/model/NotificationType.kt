package ru.plumsoftware.habitstracker.data.model

/**
 * Тип уведомления — определяет иконку и цвет чипа в списке уведомлений.
 *
 * - [HABIT_REMINDER] — локальное напоминание о привычке (через WorkManager)
 * - [HABIT_DONE]     — привычка отмечена выполненной
 * - [STREAK]         — серия выполнений (огонёк)
 * - [WEEKLY_REPORT]  — недельный отчёт по статистике
 * - [PUSH]           — входящий push из Firebase
 * - [GENERAL]        — прочее
 */
enum class NotificationType {
    HABIT_REMINDER,
    HABIT_DONE,
    STREAK,
    WEEKLY_REPORT,
    PUSH,
    FREEZE,
    GENERAL;

    companion object {
        fun fromName(value: String?): NotificationType =
            entries.firstOrNull { it.name == value } ?: GENERAL
    }
}

package ru.plumsoftware.habitstracker.data.model

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromPeriodicity(periodicity: Periodicity): Int {
        return when (periodicity) {
            Periodicity.DAILY -> 0
            Periodicity.WEEKLY -> 1
        }
    }

    @TypeConverter
    fun toPeriodicity(value: Int): Periodicity {
        return when (value) {
            0 -> Periodicity.DAILY
            1 -> Periodicity.WEEKLY
            else -> Periodicity.DAILY
        }
    }

    @TypeConverter
    fun fromNotificationType(type: NotificationType): String = type.name

    @TypeConverter
    fun toNotificationType(value: String?): NotificationType = NotificationType.fromName(value)
}
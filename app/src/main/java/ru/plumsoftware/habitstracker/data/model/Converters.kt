package ru.plumsoftware.habitstracker.data.model

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromPeriodicity(periodicity: Periodicity): String = periodicity.name

    @TypeConverter
    fun toPeriodicity(value: String): Periodicity = Periodicity.valueOf(value)
}
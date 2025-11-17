package ru.plumsoftware.habitstracker.data.model

enum class Periodicity(val days: Int) {
    DAILY(1),
    WEEKLY(7);

    companion object {
        fun fromInt(value: Int): Periodicity {
            return when (value) {
                0 -> DAILY
                1 -> WEEKLY
                else -> DAILY
            }
        }

        // Новый метод для получения числового значения
        fun toInt(periodicity: Periodicity): Int {
            return when (periodicity) {
                DAILY -> 0
                WEEKLY -> 1
            }
        }
    }
}

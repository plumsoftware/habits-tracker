package ru.plumsoftware.habitstracker.data.model

enum class Periodicity {
    DAILY, WEEKLY, MONTHLY;

    companion object {
        fun fromInt(value: Int): Periodicity {
            return when (value) {
                0 -> DAILY
                1 -> WEEKLY
                2 -> MONTHLY
                else -> DAILY
            }
        }
    }
}

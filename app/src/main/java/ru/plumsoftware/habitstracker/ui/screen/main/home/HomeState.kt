package ru.plumsoftware.habitstracker.ui.screen.main.home

import ru.plumsoftware.habitstracker.data.model.Habit

data class HomeState(
    val habits: List<Habit> = emptyList(),
    val isLoading: Boolean = false,
    val selectedDate: Long = 0L,
    val selectedHabit: Habit? = null,
    val selectedDay: Int = 0,
    val selectedMonth: Int = 0,
)
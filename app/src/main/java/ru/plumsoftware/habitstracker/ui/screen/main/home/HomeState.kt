package ru.plumsoftware.habitstracker.ui.screen.main.home

import ru.plumsoftware.habitstracker.data.model.Habit
import ru.plumsoftware.habitstracker.data.model.HabitWithStatus

data class HomeState(
    val habitsWithStatus: List<HabitWithStatus> = emptyList(),
    val isLoading: Boolean = false,
    val selectedDate: Long = 0L,
    val selectedHabit: Habit? = null,
    val selectedDay: Int = 0,
    val selectedMonth: Int = 0,
    val errorMessage: String? = null
)
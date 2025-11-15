package ru.plumsoftware.habitstracker.ui.screen.main.home

import ru.plumsoftware.habitstracker.data.model.Habit

sealed interface HomeEvent {
    data object LoadHabits : HomeEvent
    data class AddHabit(val habit: Habit) : HomeEvent
    data class UpdateHabit(val habit: Habit) : HomeEvent
    data class DeleteHabit(val habit: Habit) : HomeEvent
    data class SelectHabit(val habit: Habit?) : HomeEvent
    data class SelectDay(val day: Int) : HomeEvent
}
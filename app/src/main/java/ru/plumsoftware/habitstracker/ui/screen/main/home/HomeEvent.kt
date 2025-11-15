package ru.plumsoftware.habitstracker.ui.screen.main.home

import ru.plumsoftware.habitstracker.data.model.Habit

sealed interface HomeEvent {
    object LoadHabits : HomeEvent
    data class AddHabit(val habit: Habit) : HomeEvent
    data class UpdateHabit(val habit: Habit) : HomeEvent
    data class DeleteHabit(val habit: Habit) : HomeEvent
    data class SelectHabit(val habit: Habit?) : HomeEvent
    data class SelectDay(val day: Int) : HomeEvent
    data class SelectDate(val date: Long) : HomeEvent
    data class MarkHabitCompleted(val habitId: String) : HomeEvent
    data class UnmarkHabitCompleted(val habitId: String) : HomeEvent
}
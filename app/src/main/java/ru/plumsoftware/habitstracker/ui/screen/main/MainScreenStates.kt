package ru.plumsoftware.habitstracker.ui.screen.main

sealed interface MainScreenStates {
    data object Home : MainScreenStates
    data object Calendar : MainScreenStates
    data object Statistics : MainScreenStates
    data object Notifications : MainScreenStates
    data object Settings : MainScreenStates
}
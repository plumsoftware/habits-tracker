package ru.plumsoftware.habitstracker.ui.screen

import kotlinx.serialization.Serializable


sealed interface Route {
    @Serializable
    data object Main : Route
}
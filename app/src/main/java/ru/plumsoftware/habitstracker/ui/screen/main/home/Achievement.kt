package ru.plumsoftware.habitstracker.ui.screen.main.home

/** Достижение для карточки «Достижения» (ТЗ §10.6). */
data class Achievement(
    val emoji: String,
    val title: String,
    val status: String,   // «получено» или прогресс «ещё N»
    val unlocked: Boolean,
)

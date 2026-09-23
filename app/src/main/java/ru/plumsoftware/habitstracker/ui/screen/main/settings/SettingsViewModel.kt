package ru.plumsoftware.habitstracker.ui.screen.main.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import ru.plumsoftware.habitstracker.data.settings.AppSettings
import ru.plumsoftware.habitstracker.data.settings.SettingsRepository
import ru.plumsoftware.habitstracker.data.settings.ThemeMode
import ru.plumsoftware.habitstracker.data.settings.WeekStart

class SettingsViewModel(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    // null = ещё не прочитано из DataStore (показываем сплэш, а не мигаем онбордингом)
    val onboardingSeen: StateFlow<Boolean?> = settingsRepository.settings
        .map { it.onboardingSeen }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    // Совместимость: используется на других экранах (тёмная тема как булево)
    val isDarkTheme: StateFlow<Boolean> = settingsRepository.settings
        .map { it.themeMode == ThemeMode.DARK }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun setPushEnabled(value: Boolean) = viewModelScope.launch {
        settingsRepository.setPushEnabled(value)
    }

    fun setSoundEnabled(value: Boolean) = viewModelScope.launch {
        settingsRepository.setSoundEnabled(value)
    }

    fun setSeriesEnabled(value: Boolean) = viewModelScope.launch {
        settingsRepository.setSeriesEnabled(value)
    }

    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch {
        settingsRepository.setThemeMode(mode)
    }

    fun setWeekStart(weekStart: WeekStart) = viewModelScope.launch {
        settingsRepository.setWeekStart(weekStart)
    }

    fun setQuietHoursEnabled(value: Boolean) = viewModelScope.launch {
        settingsRepository.setQuietHoursEnabled(value)
    }

    fun setQuietHours(start: Int, end: Int) = viewModelScope.launch {
        settingsRepository.setQuietHours(start, end)
    }

    fun setStreakFreezesPerWeek(value: Int) = viewModelScope.launch {
        settingsRepository.setStreakFreezesPerWeek(value)
    }

    fun setWeeklyGoalDays(value: Int) = viewModelScope.launch {
        settingsRepository.setWeeklyGoalDays(value)
    }

    /** Онбординг пройден (ТЗ §14) — при последующих запусках сразу главный экран. */
    fun completeOnboarding() = viewModelScope.launch {
        settingsRepository.setOnboardingSeen(true)
    }

    /** Показать онбординг снова (кнопка в настройках, ТЗ §11.4) — данные не трогаем. */
    fun resetOnboarding() = viewModelScope.launch {
        settingsRepository.setOnboardingSeen(false)
    }

    /** Быстрое переключение свет/тьма (для обратной совместимости). */
    fun toggleTheme() = viewModelScope.launch {
        val current = settings.value.themeMode
        val next = if (current == ThemeMode.DARK) ThemeMode.LIGHT else ThemeMode.DARK
        settingsRepository.setThemeMode(next)
    }

    companion object {
        val settingsViewModelModule = module {
            viewModel { SettingsViewModel(get()) }
        }
    }
}

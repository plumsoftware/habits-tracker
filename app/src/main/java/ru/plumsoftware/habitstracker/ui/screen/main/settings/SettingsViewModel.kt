package ru.plumsoftware.habitstracker.ui.screen.main.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import ru.plumsoftware.habitstracker.data.settings.ThemeRepository

class SettingsViewModel(
    private val themeRepository: ThemeRepository
) : ViewModel() {

    private val _isDarkTheme = MutableStateFlow(false)
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    init {
        loadThemePreference()
    }

    fun toggleTheme() {
        viewModelScope.launch {
            val newValue = !_isDarkTheme.value
            _isDarkTheme.value = newValue
            themeRepository.setDarkTheme(newValue)
        }
    }

    fun setDarkTheme(enabled: Boolean) {
        viewModelScope.launch {
            _isDarkTheme.value = enabled
            themeRepository.setDarkTheme(enabled)
        }
    }

    private fun loadThemePreference() {
        viewModelScope.launch {
            themeRepository.themeStream.collect { isDarkTheme ->
                _isDarkTheme.value = isDarkTheme
            }
        }
    }

    companion object {
        val settingsViewModelModule = module {
            viewModel { SettingsViewModel(get()) }
        }
    }
}
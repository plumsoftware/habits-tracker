package ru.plumsoftware.habitstracker.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.koin.dsl.module

class ThemeRepository(private val dataStore: DataStore<Preferences>) {

    companion object {
        private val DARK_THEME_KEY = booleanPreferencesKey("dark_theme")

        val themeRepositoryModule = module {
            single<ThemeRepository> {
                ThemeRepository(get())
            }
        }
    }

    val themeStream: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[DARK_THEME_KEY] ?: false
        }

    suspend fun setDarkTheme(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[DARK_THEME_KEY] = enabled
        }
    }
}
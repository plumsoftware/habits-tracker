package ru.plumsoftware.habitstracker.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.koin.dsl.module

/**
 * Единое хранилище пользовательских настроек поверх DataStore Preferences.
 * Питает экран «Настройки», а также используется при построении уведомлений
 * (push/звук/серии) и в календаре (начало недели).
 */
class SettingsRepository(private val dataStore: DataStore<Preferences>) {

    companion object {
        private val PUSH_ENABLED = booleanPreferencesKey("push_enabled")
        private val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        private val SERIES_ENABLED = booleanPreferencesKey("series_enabled")
        private val QUIET_HOURS_ENABLED = booleanPreferencesKey("quiet_hours_enabled")
        private val QUIET_START = intPreferencesKey("quiet_start_hour")
        private val QUIET_END = intPreferencesKey("quiet_end_hour")
        private val THEME_MODE = stringPreferencesKey("theme_mode")
        private val WEEK_START = stringPreferencesKey("week_start")
        private val LANGUAGE = stringPreferencesKey("language")
        private val STREAK_FREEZES = intPreferencesKey("streak_freezes_per_week")
        private val WEEKLY_GOAL_DAYS = intPreferencesKey("weekly_goal_days")
        private val ONBOARDING_SEEN = booleanPreferencesKey("onboarding_seen")
        private val NOTIFIED_FROZEN = stringPreferencesKey("notified_frozen_days")

        // Совместимость со старым ключом ThemeRepository ("dark_theme")
        private val LEGACY_DARK = booleanPreferencesKey("dark_theme")

        val settingsRepositoryModule = module {
            single<SettingsRepository> { SettingsRepository(get()) }
        }
    }

    val settings: Flow<AppSettings> = dataStore.data.map { p ->
        val themeMode = when {
            p[THEME_MODE] != null -> ThemeMode.fromName(p[THEME_MODE])
            // Миграция со старого булева ключа темы
            p[LEGACY_DARK] == true -> ThemeMode.DARK
            else -> ThemeMode.SYSTEM
        }
        AppSettings(
            pushEnabled = p[PUSH_ENABLED] ?: true,
            soundEnabled = p[SOUND_ENABLED] ?: true,
            seriesEnabled = p[SERIES_ENABLED] ?: true,
            quietHoursEnabled = p[QUIET_HOURS_ENABLED] ?: false,
            quietStartHour = (p[QUIET_START] ?: 23).coerceIn(0, 23),
            quietEndHour = (p[QUIET_END] ?: 8).coerceIn(0, 23),
            themeMode = themeMode,
            weekStart = WeekStart.fromName(p[WEEK_START]),
            language = AppLanguage.fromTag(p[LANGUAGE]),
            streakFreezesPerWeek = (p[STREAK_FREEZES] ?: 2).coerceIn(0, 3),
            weeklyGoalDays = (p[WEEKLY_GOAL_DAYS] ?: 5).coerceIn(1, 7),
            onboardingSeen = p[ONBOARDING_SEEN] ?: false
        )
    }

    /** Разовое чтение — для фоновых воркеров/сервисов. */
    suspend fun current(): AppSettings = settings.first()

    suspend fun setPushEnabled(value: Boolean) = edit { it[PUSH_ENABLED] = value }
    suspend fun setSoundEnabled(value: Boolean) = edit { it[SOUND_ENABLED] = value }
    suspend fun setSeriesEnabled(value: Boolean) = edit { it[SERIES_ENABLED] = value }
    suspend fun setQuietHoursEnabled(value: Boolean) = edit { it[QUIET_HOURS_ENABLED] = value }
    suspend fun setQuietHours(start: Int, end: Int) = edit { it[QUIET_START] = start; it[QUIET_END] = end }
    suspend fun setThemeMode(mode: ThemeMode) = edit { it[THEME_MODE] = mode.name }
    suspend fun setWeekStart(weekStart: WeekStart) = edit { it[WEEK_START] = weekStart.name }
    suspend fun setLanguage(language: AppLanguage) = edit { it[LANGUAGE] = language.tag }
    suspend fun setStreakFreezesPerWeek(value: Int) = edit { it[STREAK_FREEZES] = value.coerceIn(0, 3) }
    suspend fun setWeeklyGoalDays(value: Int) = edit { it[WEEKLY_GOAL_DAYS] = value.coerceIn(1, 7) }
    suspend fun setOnboardingSeen(value: Boolean) = edit { it[ONBOARDING_SEEN] = value }

    /** Дни (начало дня, ms), для которых уже показано «Заморозка использована». */
    suspend fun notifiedFrozenDays(): Set<Long> =
        dataStore.data.first()[NOTIFIED_FROZEN]
            ?.split(",")?.mapNotNull { it.trim().toLongOrNull() }?.toSet() ?: emptySet()

    suspend fun setNotifiedFrozenDays(days: Set<Long>) = edit {
        // Храним только последние 60 записей, чтобы не разрастаться
        it[NOTIFIED_FROZEN] = days.sortedDescending().take(60).joinToString(",")
    }

    private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        dataStore.edit { block(it) }
    }
}

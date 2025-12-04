package ru.plumsoftware.habitstracker.data.database

import android.content.Context
import java.util.Calendar
import androidx.core.content.edit
import org.koin.dsl.module

class AppStatsRepository(private val context: Context) {

    companion object {
        private const val PREFS_NAME = "app_stats_prefs"
        private const val KEY_APP_LAUNCH_COUNT = "app_launch_count"
        private const val KEY_MIN_LAUNCHES_FOR_ADS = 3

        val appStatsModule = module {
            single<AppStatsRepository> {
                AppStatsRepository(get())
            }
        }
    }

    private val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    suspend fun recordAppLaunch(): Boolean {
        val currentCount = sharedPreferences.getInt(KEY_APP_LAUNCH_COUNT, 0)

        // Всегда увеличиваем счетчик
        val newCount = currentCount + 1
        sharedPreferences.edit {
            putInt(KEY_APP_LAUNCH_COUNT, newCount)
        }

        // Возвращаем true, если достигли минимума для показа рекламы
        return newCount >= KEY_MIN_LAUNCHES_FOR_ADS
    }

    fun getAppLaunchCount(): Int {
        return sharedPreferences.getInt(KEY_APP_LAUNCH_COUNT, 0)
    }

    fun shouldShowAds(): Boolean {
        return getAppLaunchCount() >= KEY_MIN_LAUNCHES_FOR_ADS
    }

    fun resetLaunchCount() {
        sharedPreferences.edit {
            putInt(KEY_APP_LAUNCH_COUNT, 0)
        }
    }
}
package ru.plumsoftware.habitstracker.data.database

import android.content.Context
import androidx.core.content.edit
import org.koin.dsl.module

class AppStatsRepository(private val context: Context) {

    companion object {
        private const val PREFS_NAME = "app_stats_prefs"
        private const val KEY_APP_LAUNCH_COUNT = "app_launch_count"
        private const val KEY_FIRST_LAUNCH = "first_launch_at"
        private const val MILLIS_PER_HOUR = 60 * 60 * 1_000L

        val appStatsModule = module {
            single<AppStatsRepository> {
                AppStatsRepository(get())
            }
        }
    }

    private val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun recordAppLaunch(): Boolean {
        val now = System.currentTimeMillis()
        val launches = sharedPreferences.getInt(KEY_APP_LAUNCH_COUNT, 0)
        val firstLaunch = sharedPreferences.getLong(KEY_FIRST_LAUNCH, 0L)
        sharedPreferences.edit {
            if (firstLaunch == 0L) {
                // Earlier versions stored the launch count, but not the first launch time.
                val baseline = if (launches > 0) {
                    runCatching { context.packageManager.getPackageInfo(context.packageName, 0).firstInstallTime }
                        .getOrNull()?.takeIf { it in 1L..now } ?: now
                } else now
                putLong(KEY_FIRST_LAUNCH, baseline)
            }
            putInt(KEY_APP_LAUNCH_COUNT, launches + 1)
        }
        return hasUsedForHours(48)
    }

    fun hasUsedForHours(hours: Int): Boolean {
        val firstLaunch = sharedPreferences.getLong(KEY_FIRST_LAUNCH, 0L)
        return firstLaunch > 0L && System.currentTimeMillis() - firstLaunch >= hours * MILLIS_PER_HOUR
    }

    fun resetLaunchCount() {
        sharedPreferences.edit {
            putInt(KEY_APP_LAUNCH_COUNT, 0)
        }
    }
}
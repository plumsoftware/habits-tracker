package ru.plumsoftware.habitstracker

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.dsl.module
import ru.plumsoftware.habitstracker.data.config.PlatformConfig
import ru.plumsoftware.habitstracker.data.config.storePlatformConfig
import ru.plumsoftware.habitstracker.data.database.*
import ru.plumsoftware.habitstracker.data.export.ExportManager
import ru.plumsoftware.habitstracker.data.settings.SettingsRepository
import ru.plumsoftware.habitstracker.data.settings.ThemeRepository
import ru.plumsoftware.habitstracker.ui.screen.main.home.HomeViewModel
import ru.plumsoftware.habitstracker.ui.screen.main.notifications.NotificationsViewModel
import ru.plumsoftware.habitstracker.ui.screen.main.settings.SettingsViewModel

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val dataStoreModule = module {
            single<DataStore<Preferences>> {
                PreferenceDataStoreFactory.create {
                    get<Context>().preferencesDataStoreFile("theme_preferences")
                }
            }
        }
        val wellnessModule = module {
            single { WellnessRepository(get()) }
        }
        startKoin {
            androidContext(this@MyApplication)
            modules(listOf(
                HabitDatabase.databaseModule,
                HabitRepository.repositoryModule,
                HomeViewModel.viewModelModule,
                dataStoreModule,
                ThemeRepository.themeRepositoryModule,
                SettingsRepository.settingsRepositoryModule,
                NotificationRepository.notificationRepositoryModule,
                SettingsViewModel.settingsViewModelModule,
                NotificationsViewModel.viewModelModule,
                AppStatsRepository.appStatsModule,
                CategoryRepository.categoryRepositoryModule,
                ExportManager.exportModule,
                wellnessModule
            ))
        }
        runCatching {
            org.koin.core.context.GlobalContext.get()
                .get<ru.plumsoftware.habitstracker.data.notification.HabitNotificationManager>()
                .scheduleSeriesRiskCheck()
        }
    }

    companion object {
        val platformConfig: PlatformConfig = storePlatformConfig
    }
}

package ru.plumsoftware.habitstracker

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import ru.plumsoftware.habitstracker.data.database.HabitDatabase
import ru.plumsoftware.habitstracker.data.database.HabitRepository
import ru.plumsoftware.habitstracker.ui.screen.main.home.HomeViewModel

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(this@MyApplication)
            modules(
                listOf(
                    HabitDatabase.databaseModule,
                    HabitRepository.repositoryModule,
                    HomeViewModel.viewModelModule
                )
            )
        }
    }
}
package ru.plumsoftware.habitstracker.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import ru.plumsoftware.habitstracker.data.model.Converters
import ru.plumsoftware.habitstracker.data.model.Habit
import ru.plumsoftware.habitstracker.data.model.HabitCompletion
import ru.plumsoftware.habitstracker.data.model.Notification
import ru.plumsoftware.habitstracker.data.model.dao.HabitCompletionDao
import ru.plumsoftware.habitstracker.data.model.dao.HabitDao
import ru.plumsoftware.habitstracker.data.model.dao.NotificationsDao

@Database(
    entities = [Habit::class, HabitCompletion::class, Notification::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class HabitDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitDao
    abstract fun habitCompletionDao(): HabitCompletionDao
    abstract fun notificationsDao(): NotificationsDao

    companion object {
        val databaseModule = module {
            single<HabitDatabase> {
                Room.databaseBuilder(
                    androidContext(),
                    HabitDatabase::class.java,
                    "habit_database"
                )
                    .build()
            }

            single<HabitDao> {
                get<HabitDatabase>().habitDao()
            }

            single<HabitCompletionDao> {
                get<HabitDatabase>().habitCompletionDao()
            }

            single<NotificationsDao> {
                get<HabitDatabase>().notificationsDao()
            }
        }
    }
}
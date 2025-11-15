package ru.plumsoftware.habitstracker.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import ru.plumsoftware.habitstracker.data.model.Habit
import ru.plumsoftware.habitstracker.data.model.dao.HabitDao

@Database(
    entities = [Habit::class],
    version = 1,
    exportSchema = false
)
abstract class HabitDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitDao

    companion object {
        val databaseModule = module {
            single<HabitDatabase> {
                Room.databaseBuilder(
                    androidContext(),
                    HabitDatabase::class.java,
                    "habit_database"
                ).build()
            }

            single<HabitDao> {
                get<HabitDatabase>().habitDao()
            }
        }
    }
}
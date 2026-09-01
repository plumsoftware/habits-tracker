package ru.plumsoftware.habitstracker.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class HabitDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitDao
    abstract fun habitCompletionDao(): HabitCompletionDao
    abstract fun notificationsDao(): NotificationsDao

    companion object {
        // Добавляет колонки type/isRead в таблицу notifications без потери привычек
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE notifications ADD COLUMN type TEXT NOT NULL DEFAULT 'GENERAL'"
                )
                db.execSQL(
                    "ALTER TABLE notifications ADD COLUMN isRead INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        val databaseModule = module {
            single<HabitDatabase> {
                Room.databaseBuilder(
                    androidContext(),
                    HabitDatabase::class.java,
                    "habit_database"
                )
                    .addMigrations(MIGRATION_1_2)
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
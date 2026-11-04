package ru.plumsoftware.habitstracker.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import ru.plumsoftware.habitstracker.data.model.*
import ru.plumsoftware.habitstracker.data.model.dao.*

@Database(
    entities = [
        Habit::class, HabitCompletion::class, Notification::class, Category::class,
        DailySteps::class, WorkoutSession::class, WorkoutExercise::class, WorkoutSet::class
    ],
    version = 9,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class HabitDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitDao
    abstract fun habitCompletionDao(): HabitCompletionDao
    abstract fun notificationsDao(): NotificationsDao
    abstract fun categoryDao(): CategoryDao
    abstract fun wellnessDao(): WellnessDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE notifications ADD COLUMN type TEXT NOT NULL DEFAULT 'GENERAL'")
                db.execSQL("ALTER TABLE notifications ADD COLUMN isRead INTEGER NOT NULL DEFAULT 0")
            }
        }
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""CREATE TABLE IF NOT EXISTS categories (
                    id TEXT NOT NULL PRIMARY KEY, name TEXT NOT NULL, colorArgb INTEGER NOT NULL,
                    isBuiltIn INTEGER NOT NULL DEFAULT 0, sortOrder INTEGER NOT NULL DEFAULT 0)""")
                db.execSQL("ALTER TABLE habits ADD COLUMN categoryId TEXT")
                fun insertBuiltIn(id: String, name: String, color: Long, order: Int) {
                    db.execSQL("INSERT OR IGNORE INTO categories (id,name,colorArgb,isBuiltIn,sortOrder) VALUES (?,?,?,1,?)", arrayOf<Any>(id,name,color,order))
                }
                insertBuiltIn(Category.ID_HEALTH, "Здоровье", Category.COLOR_HEALTH, 0)
                insertBuiltIn(Category.ID_WORK, "Работа", Category.COLOR_WORK, 1)
                insertBuiltIn(Category.ID_STUDY, "Обучение", Category.COLOR_STUDY, 2)
                insertBuiltIn(Category.ID_PERSONAL, "Личное", Category.COLOR_PERSONAL, 3)
            }
        }
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) { db.execSQL("ALTER TABLE habits ADD COLUMN emoji TEXT NOT NULL DEFAULT '🎯'") }
        }
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE habits ADD COLUMN weekdays TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE habits ADD COLUMN chainedAfterId TEXT")
                db.execSQL("ALTER TABLE habits ADD COLUMN freezeEnabled INTEGER NOT NULL DEFAULT 0")
            }
        }
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) { db.execSQL("ALTER TABLE habits ADD COLUMN archived INTEGER NOT NULL DEFAULT 0") }
        }
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) { db.execSQL("ALTER TABLE habit_completions ADD COLUMN completedAtMillis INTEGER NOT NULL DEFAULT 0") }
        }

        /** Создаёт новые таблицы, не затрагивая существующие привычки и отметки. */
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS daily_steps (dayStartMillis INTEGER NOT NULL PRIMARY KEY, steps INTEGER NOT NULL, updatedAtMillis INTEGER NOT NULL)")
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS workout_sessions (id TEXT NOT NULL PRIMARY KEY, title TEXT NOT NULL, startedAtMillis INTEGER NOT NULL, endedAtMillis INTEGER, notes TEXT NOT NULL, createdAtMillis INTEGER NOT NULL)")
            }
        }

        private fun seedBuiltInCategories(db: SupportSQLiteDatabase) {
            fun insertBuiltIn(id: String, name: String, color: Long, order: Int) {
                db.execSQL("INSERT OR IGNORE INTO categories (id,name,colorArgb,isBuiltIn,sortOrder) VALUES (?,?,?,1,?)", arrayOf<Any>(id,name,color,order))
            }
            insertBuiltIn(Category.ID_HEALTH, "Здоровье", Category.COLOR_HEALTH, 0)
            insertBuiltIn(Category.ID_WORK, "Работа", Category.COLOR_WORK, 1)
            insertBuiltIn(Category.ID_STUDY, "Обучение", Category.COLOR_STUDY, 2)
            insertBuiltIn(Category.ID_PERSONAL, "Личное", Category.COLOR_PERSONAL, 3)
        }

        val databaseModule = module {
            single<HabitDatabase> {
                Room.databaseBuilder(androidContext(), HabitDatabase::class.java, "habit_database")
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9)
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            seedBuiltInCategories(db)
                        }
                    }).build()
            }
            single<HabitDao> { get<HabitDatabase>().habitDao() }
            single<HabitCompletionDao> { get<HabitDatabase>().habitCompletionDao() }
            single<NotificationsDao> { get<HabitDatabase>().notificationsDao() }
            single<CategoryDao> { get<HabitDatabase>().categoryDao() }
            single<WellnessDao> { get<HabitDatabase>().wellnessDao() }
        }
    }
}

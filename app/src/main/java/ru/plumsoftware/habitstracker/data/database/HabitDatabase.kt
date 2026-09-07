package ru.plumsoftware.habitstracker.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import ru.plumsoftware.habitstracker.data.model.Category
import ru.plumsoftware.habitstracker.data.model.Converters
import ru.plumsoftware.habitstracker.data.model.Habit
import ru.plumsoftware.habitstracker.data.model.HabitCompletion
import ru.plumsoftware.habitstracker.data.model.Notification
import ru.plumsoftware.habitstracker.data.model.dao.CategoryDao
import ru.plumsoftware.habitstracker.data.model.dao.HabitCompletionDao
import ru.plumsoftware.habitstracker.data.model.dao.HabitDao
import ru.plumsoftware.habitstracker.data.model.dao.NotificationsDao

@Database(
    entities = [Habit::class, HabitCompletion::class, Notification::class, Category::class],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class HabitDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitDao
    abstract fun habitCompletionDao(): HabitCompletionDao
    abstract fun notificationsDao(): NotificationsDao
    abstract fun categoryDao(): CategoryDao

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

        // Добавляет категории и колонку categoryId в привычки, создаёт встроенные категории
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS categories (
                        id TEXT NOT NULL PRIMARY KEY,
                        name TEXT NOT NULL,
                        colorArgb INTEGER NOT NULL,
                        isBuiltIn INTEGER NOT NULL DEFAULT 0,
                        sortOrder INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )
                db.execSQL("ALTER TABLE habits ADD COLUMN categoryId TEXT")

                // Предзаполняем встроенные категории (их нельзя удалять/менять)
                fun insertBuiltIn(id: String, name: String, color: Long, order: Int) {
                    db.execSQL(
                        "INSERT OR IGNORE INTO categories (id, name, colorArgb, isBuiltIn, sortOrder) VALUES (?, ?, ?, 1, ?)",
                        arrayOf<Any>(id, name, color, order)
                    )
                }
                insertBuiltIn(Category.ID_HEALTH, "Здоровье", Category.COLOR_HEALTH, 0)
                insertBuiltIn(Category.ID_WORK, "Работа", Category.COLOR_WORK, 1)
                insertBuiltIn(Category.ID_STUDY, "Обучение", Category.COLOR_STUDY, 2)
                insertBuiltIn(Category.ID_PERSONAL, "Личное", Category.COLOR_PERSONAL, 3)
            }
        }

        // Для чистой установки (version 3 сразу) заполняем встроенные категории после создания БД
        private fun seedBuiltInCategories(db: SupportSQLiteDatabase) {
            fun insertBuiltIn(id: String, name: String, color: Long, order: Int) {
                db.execSQL(
                    "INSERT OR IGNORE INTO categories (id, name, colorArgb, isBuiltIn, sortOrder) VALUES (?, ?, ?, 1, ?)",
                    arrayOf<Any>(id, name, color, order)
                )
            }
            insertBuiltIn(Category.ID_HEALTH, "Здоровье", Category.COLOR_HEALTH, 0)
            insertBuiltIn(Category.ID_WORK, "Работа", Category.COLOR_WORK, 1)
            insertBuiltIn(Category.ID_STUDY, "Обучение", Category.COLOR_STUDY, 2)
            insertBuiltIn(Category.ID_PERSONAL, "Личное", Category.COLOR_PERSONAL, 3)
        }

        val databaseModule = module {
            single<HabitDatabase> {
                Room.databaseBuilder(
                    androidContext(),
                    HabitDatabase::class.java,
                    "habit_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Свежая установка: сразу создаём встроенные категории
                            seedBuiltInCategories(db)
                        }
                    })
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

            single<CategoryDao> {
                get<HabitDatabase>().categoryDao()
            }
        }
    }
}
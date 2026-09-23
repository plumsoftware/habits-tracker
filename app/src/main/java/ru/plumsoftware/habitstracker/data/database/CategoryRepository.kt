package ru.plumsoftware.habitstracker.data.database

import kotlinx.coroutines.flow.Flow
import org.koin.dsl.module
import ru.plumsoftware.habitstracker.data.model.Category
import ru.plumsoftware.habitstracker.data.model.dao.CategoryDao
import java.util.UUID

class CategoryRepository(
    private val categoryDao: CategoryDao
) {
    fun getAll(): Flow<List<Category>> = categoryDao.getAll()

    /** Вставка/замена категории «как есть» (для восстановления из бэкапа). */
    suspend fun upsert(category: Category) = categoryDao.insert(category)

    suspend fun getById(id: String): Category? = categoryDao.getById(id)

    /** Создаёт новую пользовательскую категорию. Ставит её в конец списка. */
    suspend fun create(name: String, colorArgb: Long) {
        categoryDao.insert(
            Category(
                id = UUID.randomUUID().toString(),
                name = name.trim(),
                colorArgb = colorArgb,
                isBuiltIn = false,
                // Пользовательские идут после встроенных
                sortOrder = 100
            )
        )
    }

    /** Обновляет категорию. Встроенные менять нельзя. */
    suspend fun update(category: Category) {
        if (category.isBuiltIn) return
        categoryDao.update(category.copy(name = category.name.trim()))
    }

    /** Удаляет категорию. Встроенные удалять нельзя. */
    suspend fun delete(category: Category) {
        if (category.isBuiltIn) return
        categoryDao.delete(category)
    }

    companion object {
        val categoryRepositoryModule = module {
            single<CategoryRepository> { CategoryRepository(get()) }
        }
    }
}

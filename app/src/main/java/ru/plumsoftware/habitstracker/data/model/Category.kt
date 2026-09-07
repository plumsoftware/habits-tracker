package ru.plumsoftware.habitstracker.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Категория привычки. Встроенные (isBuiltIn = true) нельзя редактировать или удалять —
 * они создаются миграцией и всегда присутствуют.
 */
@Entity(tableName = "categories")
data class Category(
    @PrimaryKey
    val id: String,
    val name: String,
    // Цвет категории в формате ARGB (как Long, чтобы влезал 0xFFxxxxxx)
    val colorArgb: Long,
    val isBuiltIn: Boolean = false,
    // Порядок сортировки в списке (встроенные идут первыми)
    val sortOrder: Int = 0
) {
    companion object {
        // Стабильные id встроенных категорий
        const val ID_HEALTH = "builtin_health"
        const val ID_WORK = "builtin_work"
        const val ID_STUDY = "builtin_study"
        const val ID_PERSONAL = "builtin_personal"

        // Цвета из дизайн-макетов
        const val COLOR_HEALTH = 0xFF3963FAL
        const val COLOR_WORK = 0xFFF5714EL
        const val COLOR_STUDY = 0xFF7C5CFCL
        const val COLOR_PERSONAL = 0xFF22C55EL

        // Палитра для выбора цвета пользовательской категории
        val PALETTE: List<Long> = listOf(
            0xFF3963FAL, // синий
            0xFFF5714EL, // оранжево-красный
            0xFF7C5CFCL, // фиолетовый
            0xFF22C55EL, // зелёный
            0xFFF5A623L, // янтарный
            0xFFEC4899L, // розовый
            0xFF06B6D4L, // бирюзовый
            0xFFEF4444L, // красный
            0xFF64748BL  // серо-синий
        )
    }
}

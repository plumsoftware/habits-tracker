package ru.plumsoftware.habitstracker.data.settings

/** Режим оформления приложения (строка «Оформление» в настройках). */
enum class ThemeMode {
    LIGHT, DARK, SYSTEM;

    companion object {
        fun fromName(value: String?): ThemeMode =
            entries.firstOrNull { it.name == value } ?: SYSTEM
    }
}

/** Начало недели (строка «Начало недели» в настройках). */
enum class WeekStart {
    MONDAY, SUNDAY;

    companion object {
        fun fromName(value: String?): WeekStart =
            entries.firstOrNull { it.name == value } ?: MONDAY
    }
}

/** Язык интерфейса (строка «Язык»). Интерфейс пока только на русском. */
enum class AppLanguage(val tag: String, val title: String) {
    RU("ru", "Русский"),
    EN("en", "English");

    companion object {
        fun fromTag(value: String?): AppLanguage =
            entries.firstOrNull { it.tag == value } ?: RU
    }
}

/** Снимок всех пользовательских настроек. */
data class AppSettings(
    val pushEnabled: Boolean = true,
    val soundEnabled: Boolean = true,
    val seriesEnabled: Boolean = true,
    val quietHoursEnabled: Boolean = false,
    // Интервал тихих часов (час начала/конца), ТЗ §11.2
    val quietStartHour: Int = 23,
    val quietEndHour: Int = 8,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val weekStart: WeekStart = WeekStart.MONDAY,
    val language: AppLanguage = AppLanguage.RU,
    // Кол-во заморозок стрика в неделю (0..3), строка «Заморозки стрика» (ТЗ §11.3)
    val streakFreezesPerWeek: Int = 2,
    // Сколько дней в неделю считается «успешной неделей» (1..7), строка «Цель по дням»
    val weeklyGoalDays: Int = 5,
    // Показан ли онбординг (ТЗ §14: при первом запуске — онбординг, далее — главный)
    val onboardingSeen: Boolean = false
)

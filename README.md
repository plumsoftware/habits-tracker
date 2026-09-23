# Трекер привычек

Android-приложение для отслеживания ежедневных привычек: создание привычек с категориями, отметки выполнения, напоминания, статистика (стрики, тепловая карта, недельная/месячная динамика), экспорт данных и виджеты на домашнем экране.

## Технологии

- **Kotlin** + **Jetpack Compose** (Material 3), single-Activity
- **Koin** — внедрение зависимостей
- **Room** — локальная БД (`habit_database`, версия 3)
- **DataStore Preferences** — настройки и тема
- **WorkManager** — планирование напоминаний
- **Glance** — виджеты на домашнем экране
- **Firebase Cloud Messaging** — push-уведомления
- **Yandex Mobile Ads** — реклама
- **Navigation Compose** — типобезопасная навигация (kotlinx.serialization)
- Gradle **9.4.1**, AGP **8.13.1**, `minSdk` 24, `targetSdk` 37

## Структура

```
app/src/main/java/ru/plumsoftware/habitstracker/
├─ MainActivity.kt / MyApplication.kt   — точка входа, старт Koin
├─ data/
│  ├─ model/            — Room-сущности (Habit, HabitCompletion, Notification, Category) и DAO
│  ├─ database/         — HabitDatabase + репозитории
│  ├─ settings/         — настройки и тема (DataStore)
│  ├─ notification/     — напоминания через WorkManager
│  ├─ export/           — экспорт статистики
│  ├─ service/          — Firebase Messaging
│  └─ config/           — конфигурация площадки/рекламы
└─ ui/
   ├─ screen/main/{home,notifications,settings}/  — экраны (ViewModel + State + Event)
   ├─ components/        — переиспользуемые Compose-компоненты
   ├─ theme/             — тема (цвета, размеры, формы, типографика)
   └─ widget/            — виджеты Glance
```

Экраны (Главная / Статистика / Уведомления / Настройки) переключаются табами внутри `HomeScreen`, а не отдельными destination'ами навигации.

## Флейворы по магазинам

Приложение собирается под три площадки (dimension `store`): `google`, `rustore`, `huawei`. Каждый флейвор переопределяет `src/<flavor>/.../config/StorePlatform.kt` (реклама/площадка). Площадка выбирается **только** через Build Variant — в общем коде хардкодить нельзя.

## Сборка

> ⚠️ Собирать нужно на **JDK 21**. JBR из Android Studio содержит Java 25, на которой сборка падает с ошибкой вида `25.0.2`. В самой Android Studio сборка работает штатно.

```bash
# Компиляция
JAVA_HOME=/opt/homebrew/opt/openjdk@21 sh gradlew :app:compileGoogleDebugKotlin

# Debug-APK (флейвор: google | rustore | huawei)
JAVA_HOME=/opt/homebrew/opt/openjdk@21 sh gradlew :app:assembleGoogleDebug

# Юнит-тесты
JAVA_HOME=/opt/homebrew/opt/openjdk@21 sh gradlew :app:testGoogleDebugUnitTest

# Один тест
JAVA_HOME=/opt/homebrew/opt/openjdk@21 sh gradlew :app:testGoogleDebugUnitTest \
  --tests "ru.plumsoftware.habitstracker.ИмяКласса.имяТеста"

# Инструментальные тесты (нужен девайс/эмулятор)
JAVA_HOME=/opt/homebrew/opt/openjdk@21 sh gradlew :app:connectedGoogleDebugAndroidTest
```

Для сборки нужен `google-services.json` в `app/` (Firebase) и `local.properties` с путём к Android SDK.

## Требования

- Android Studio (актуальная версия)
- JDK 21 (например, `brew install openjdk@21`)
- Android SDK, `compileSdk` 37

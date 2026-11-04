package ru.plumsoftware.habitstracker.ui.screen.main.workouts

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import org.koin.compose.koinInject
import ru.plumsoftware.habitstracker.data.database.WellnessRepository
import ru.plumsoftware.habitstracker.data.model.WorkoutSession
import ru.plumsoftware.habitstracker.data.model.WorkoutSet
import ru.plumsoftware.habitstracker.ui.components.AppScreenTopBar
import ru.plumsoftware.habitstracker.ui.components.PrimaryButton
import ru.plumsoftware.habitstracker.ui.components.PillChip
import ru.plumsoftware.habitstracker.ui.components.clickableNoRipple
import ru.plumsoftware.habitstracker.ui.theme.AppTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class WorkoutRoute { HOME, EDITOR, HISTORY, TEMPLATES, SETTINGS, PROGRESS, EXERCISE_PICKER, DETAIL, REST_TIMER }

private data class WorkoutTemplate(
    val title: String, val subtitle: String, val duration: String, val icon: String,
    val exercises: List<String>,
)

private val workoutTemplates = listOf(
    WorkoutTemplate("Силовая тренировка", "Базовый комплекс для всего тела", "45 мин", "💪",
        listOf("Приседания", "Отжимания", "Планка", "Выпады", "Скручивания", "Подъёмы гантелей")),
    WorkoutTemplate("Кардио тренировка", "Бег, прыжки и активная нагрузка", "30 мин", "👟",
        listOf("Бег на месте", "Прыжки", "Берпи", "Планка", "Выпады", "Скакалка")),
    WorkoutTemplate("Пресс", "Упражнения на пресс и кор", "20 мин", "🏅",
        listOf("Скручивания", "Подъёмы ног", "Планка", "Велосипед", "Русские скручивания")),
    WorkoutTemplate("Растяжка", "Гибкость и мобильность", "15 мин", "🧘",
        listOf("Наклоны", "Растяжка ног", "Повороты корпуса", "Растяжка плеч")),
    WorkoutTemplate("Функциональная", "Комплексная тренировка", "40 мин", "🏋️",
        listOf("Приседания", "Отжимания", "Выпады", "Планка", "Берпи", "Подтягивания")),
)

private val exerciseCatalog = listOf(
    "Приседания", "Отжимания", "Планка", "Выпады", "Скручивания", "Подтягивания",
    "Берпи", "Бег на месте", "Прыжки", "Подъёмы ног", "Жим гантелей", "Тяга гантелей",
    "Разведение рук", "Ягодичный мостик", "Велосипед", "Растяжка",
)

@Composable
fun WorkoutScreen(onBack: () -> Unit) {
    val repository: WellnessRepository = koinInject()
    val scope = rememberCoroutineScope()
    val colors = AppTheme.colors
    val sessions by repository.workouts().collectAsState(initial = emptyList())
    var route by remember { mutableStateOf(WorkoutRoute.HOME) }
    var selectedId by remember { mutableStateOf<String?>(null) }
    var title by remember { mutableStateOf("Моя тренировка") }
    var exerciseName by remember { mutableStateOf("") }
    var search by remember { mutableStateOf("") }
    var showCustomWorkoutDialog by remember { mutableStateOf(false) }
    var showDeleteWorkoutDialog by remember { mutableStateOf(false) }
    var showRenameWorkoutDialog by remember { mutableStateOf(false) }
    var renameValue by remember { mutableStateOf("") }
    var clockTick by remember { mutableStateOf(System.currentTimeMillis()) }
    var filter by remember { mutableStateOf("Все") }

    val active = sessions.firstOrNull { it.endedAtMillis == null }
    LaunchedEffect(active?.id) { while (active != null) { delay(1_000); clockTick = System.currentTimeMillis() } }
    val selected = sessions.firstOrNull { it.id == selectedId }
    val editorSession = selected ?: active
    val exercises by repository.exercises(editorSession?.id.orEmpty()).collectAsState(initial = emptyList())
    val detailExercises by repository.exercises(selectedId.orEmpty()).collectAsState(initial = emptyList())

    fun openEditor(id: String) {
        selectedId = id
        route = WorkoutRoute.EDITOR
    }
    fun startTemplate(template: WorkoutTemplate) {
        scope.launch {
            val id = repository.startWorkout(template.title)
            template.exercises.forEach { repository.addExercise(id, it) }
            selectedId = id
            route = WorkoutRoute.EDITOR
        }
    }

    Column(Modifier.fillMaxSize().background(colors.bgScreen)) {
        AppScreenTopBar(
            title = when (route) {
                WorkoutRoute.HOME -> "Тренировки"
                WorkoutRoute.EDITOR -> editorSession?.title ?: "Создать тренировку"
                WorkoutRoute.HISTORY -> "История"
                WorkoutRoute.TEMPLATES -> "Шаблоны"
                WorkoutRoute.SETTINGS -> "Настройки"
                WorkoutRoute.PROGRESS -> "Прогресс"
                WorkoutRoute.EXERCISE_PICKER -> "Добавить упражнение"
                WorkoutRoute.DETAIL -> selected?.title ?: "Тренировка"
                WorkoutRoute.REST_TIMER -> "Отдых между подходами"
            },
            onBack = {
                when (route) {
                    WorkoutRoute.HOME -> onBack()
                    WorkoutRoute.EDITOR -> if (editorSession?.endedAtMillis != null) route = WorkoutRoute.DETAIL else route = WorkoutRoute.HOME
                    WorkoutRoute.HISTORY, WorkoutRoute.TEMPLATES, WorkoutRoute.SETTINGS, WorkoutRoute.PROGRESS -> route = WorkoutRoute.HOME
                    WorkoutRoute.EXERCISE_PICKER -> route = WorkoutRoute.EDITOR
                    WorkoutRoute.DETAIL -> route = WorkoutRoute.HISTORY
                    WorkoutRoute.REST_TIMER -> route = WorkoutRoute.EDITOR
                }
            },
        )

        when (route) {
            WorkoutRoute.HOME -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item {
                    Text("Записывай упражнения, подходы и прогресс",
                        color = colors.textSecondary, style = AppTheme.type.bodyS)
                }
                if (active != null) item {
                    Column(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp))
                            .background(colors.accentSoft).padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text("🏋️", fontSize = 44.sp)
                        Text("Тренировка продолжается", color = colors.textPrimary, style = AppTheme.type.h2)
                        Text(active.title, color = colors.textSecondary, style = AppTheme.type.bodyS)
                        Text("Время: ${durationAt(active, clockTick)}", color = colors.textSecondary, style = AppTheme.type.caption)
                        PrimaryButton("Продолжить", onClick = { openEditor(active.id) })
                    }
                } else item {
                    Column(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp))
                            .background(colors.accentSoft).padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text("🏋️", fontSize = 48.sp)
                        Text("Готов к тренировке?", color = colors.textPrimary, style = AppTheme.type.h2)
                        Text("Выбери шаблон или собери собственную программу",
                            color = colors.textSecondary, style = AppTheme.type.bodyS)
                        PrimaryButton("＋ Создать тренировку", onClick = {
                            title = "Моя тренировка"; showCustomWorkoutDialog = true
                        })
                    }
                }
                item { SectionHeading("Разделы") }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        NavigationTile("▤", "История", Modifier.weight(1f)) { route = WorkoutRoute.HISTORY }
                        NavigationTile("▦", "Шаблоны", Modifier.weight(1f)) { route = WorkoutRoute.TEMPLATES }
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        NavigationTile("↗", "Прогресс", Modifier.weight(1f)) { route = WorkoutRoute.PROGRESS }
                        NavigationTile("⚙", "Настройки", Modifier.weight(1f)) { route = WorkoutRoute.SETTINGS }
                    }
                }
                item { SectionHeading("Шаблоны тренировок") }
                items(workoutTemplates.take(3)) { template ->
                    TemplateCard(template) { startTemplate(template) }
                }
                item {
                    TextButton(onClick = { route = WorkoutRoute.TEMPLATES }, modifier = Modifier.fillMaxWidth()) {
                        Text("Все шаблоны →")
                    }
                }
                item { SectionHeading("Последние тренировки") }
                val history = sessions.filter { it.endedAtMillis != null }.take(3)
                if (history.isEmpty()) item { EmptyHint("Завершённые тренировки появятся здесь") }
                items(history, key = { "recent_${it.id}" }) { session ->
                    SessionCard(session) { selectedId = session.id; route = WorkoutRoute.DETAIL }
                }
                item { Spacer(Modifier.height(24.dp)) }
            }

            WorkoutRoute.TEMPLATES -> LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item { Text("Выбери готовую программу или начни с пустой тренировки",
                    color = colors.textSecondary, style = AppTheme.type.bodyS) }
                items(workoutTemplates) { template -> TemplateCard(template) { startTemplate(template) } }
                item {
                    OutlinedButton(onClick = {
                        title = "Моя тренировка"; showCustomWorkoutDialog = true
                    }, modifier = Modifier.fillMaxWidth()) { Text("＋ Создать свою") }
                }
            }

            WorkoutRoute.EDITOR -> {
                if (editorSession == null) {
                    EmptyHint("Тренировка не найдена")
                } else LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item {
                        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
                            .background(colors.surface).padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(editorSession.title, modifier = Modifier.weight(1f), color = colors.textPrimary, style = AppTheme.type.h2)
                                if (editorSession.endedAtMillis == null) TextButton(onClick = { renameValue = editorSession.title; showRenameWorkoutDialog = true }) { Text("Изменить") }
                            }
                            Text("Начата · ${formatDate(editorSession.startedAtMillis)}",
                                color = colors.textSecondary, style = AppTheme.type.caption)
                            if (editorSession.endedAtMillis == null) {
                                PrimaryButton("＋ Добавить упражнение", onClick = { route = WorkoutRoute.EXERCISE_PICKER })
                                OutlinedButton(onClick = { showDeleteWorkoutDialog = true },
                                    modifier = Modifier.fillMaxWidth()) { Text("Завершить тренировку") }
                            } else {
                                Text("Тренировка завершена · ${duration(editorSession)}",
                                    color = colors.success, style = AppTheme.type.bodyS)
                            }
                        }
                    }
                    item { SectionHeading("Упражнения · ${exercises.size}") }
                    if (editorSession.endedAtMillis == null) item {
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
                                .background(colors.surface).clickableNoRipple { route = WorkoutRoute.REST_TIMER }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("◷", color = colors.accent, fontSize = 25.sp)
                            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                                Text("Таймер отдыха", color = colors.textPrimary, style = AppTheme.type.bodyL)
                                Text("60 секунд между подходами", color = colors.textSecondary, style = AppTheme.type.caption)
                            }
                            Text("›", color = colors.accent, fontSize = 28.sp)
                        }
                    }
                    if (exercises.isEmpty()) item { EmptyHint("Добавь упражнения в тренировку") }
                    items(exercises, key = { it.id }) { exercise ->
                        val sets by repository.sets(exercise.id).collectAsState(initial = emptyList())
                        ExerciseCard(
                            name = exercise.name, sets = sets,
                            editable = editorSession.endedAtMillis == null,
                            onAddSet = { scope.launch { repository.addSet(exercise.id) } },
                            onUpdate = { scope.launch { repository.updateSet(it) } },
                            onDeleteSet = { scope.launch { repository.deleteSet(it) } },
                            onDeleteExercise = { scope.launch { repository.deleteExercise(exercise.id) } },
                        )
                    }
                    item { Spacer(Modifier.height(24.dp)) }
                }
            }

            WorkoutRoute.EXERCISE_PICKER -> {
                var query by remember { mutableStateOf("") }
                LazyColumn(Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    item {
                        OutlinedTextField(query, { query = it }, modifier = Modifier.fillMaxWidth(),
                            singleLine = true, label = { Text("Поиск упражнений") })
                    }
                    items(exerciseCatalog.filter { it.contains(query, ignoreCase = true) }) { name ->
                        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
                            .background(colors.surface).clickableNoRipple {
                                val sessionId = editorSession?.id ?: return@clickableNoRipple
                                scope.launch { repository.addExercise(sessionId, name); route = WorkoutRoute.EDITOR }
                            }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("🏃", fontSize = 24.sp)
                            Text(name, modifier = Modifier.weight(1f).padding(start = 12.dp),
                                color = colors.textPrimary, style = AppTheme.type.bodyL)
                            Text("＋", color = colors.accent, fontSize = 24.sp)
                        }
                    }
                    item {
                        OutlinedTextField(exerciseName, { exerciseName = it }, modifier = Modifier.fillMaxWidth(),
                            label = { Text("Своё упражнение") }, singleLine = true)
                        PrimaryButton("Добавить своё упражнение", enabled = exerciseName.isNotBlank(),
                            onClick = {
                                val sessionId = editorSession?.id ?: return@PrimaryButton
                                scope.launch { repository.addExercise(sessionId, exerciseName); exerciseName = ""; route = WorkoutRoute.EDITOR }
                            })
                    }
                }
            }

            WorkoutRoute.HISTORY -> {
                val history = sessions.filter { it.endedAtMillis != null }
                Column(Modifier.fillMaxSize()) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Все", "Силовые", "Кардио", "Пресс").forEach { value ->
                            PillChip(value, if (filter == value) colors.accent else colors.surface,
                                if (filter == value) colors.inkOn else colors.textSecondary,
                                onClick = { filter = value })
                        }
                    }
                    LazyColumn(contentPadding = PaddingValues(20.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        val filtered = history.filter {
                            filter == "Все" || when (filter) {
                                "Силовые" -> it.title.contains("сил", true)
                                "Кардио" -> it.title.contains("кардио", true)
                                else -> it.title.contains("пресс", true)
                            }
                        }
                        if (filtered.isEmpty()) item { EmptyHint("Пока нет тренировок в этой категории") }
                        items(filtered, key = { it.id }) { session ->
                            SessionCard(session) { selectedId = session.id; route = WorkoutRoute.DETAIL }
                        }
                    }
                }
            }

            WorkoutRoute.DETAIL -> {
                if (selected == null) EmptyHint("Тренировка не найдена")
                else LazyColumn(Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    item {
                        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
                            .background(colors.accentSoft).padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("🏋️", fontSize = 44.sp)
                            Text(selected.title, color = colors.textPrimary, style = AppTheme.type.h2)
                            Text("${formatDate(selected.startedAtMillis)} · ${duration(selected)}",
                                color = colors.textSecondary, style = AppTheme.type.bodyS)
                            if (selected.notes.isNotBlank()) Text(selected.notes,
                                color = colors.textSecondary, style = AppTheme.type.bodyS)
                        }
                    }
                    item { SectionHeading("Упражнения") }
                    items(detailExercises, key = { it.id }) { exercise ->
                        val sets by repository.sets(exercise.id).collectAsState(initial = emptyList())
                        ExerciseCard(exercise.name, sets, false, {}, {}, {}, {})
                    }
                    item {
                        OutlinedButton(onClick = { showDeleteWorkoutDialog = true },
                            modifier = Modifier.fillMaxWidth()) { Text("Удалить тренировку") }
                    }
                }
            }

            WorkoutRoute.SETTINGS -> LazyColumn(Modifier.fillMaxSize(),
                contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item { SectionHeading("Общие") }
                item { SettingInfoCard("🔔", "Уведомления", "Напоминания о тренировках") }
                item { SettingInfoCard("🌙", "Тема", "Используется тема приложения") }
                item { SectionHeading("Данные") }
                item { SettingInfoCard("💾", "Хранение", "Тренировки сохраняются локально на устройстве") }
                item { SettingInfoCard("🔒", "Приватность", "Данные тренировок не отправляются в облако") }
            }

            WorkoutRoute.REST_TIMER -> RestTimerScreen(onFinish = { route = WorkoutRoute.EDITOR })

            WorkoutRoute.PROGRESS -> {
                val completed = sessions.filter { it.endedAtMillis != null }
                val totalMinutes = completed.sumOf {
                    ((it.endedAtMillis ?: it.startedAtMillis) - it.startedAtMillis).coerceAtLeast(0L) / 60_000L
                }
                var progressPeriod by remember { mutableStateOf("Неделя") }
                val buckets = remember(completed, progressPeriod) { workoutProgressBuckets(completed, progressPeriod) }
                LazyColumn(Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    item { SectionHeading("Твой прогресс") }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            StatCard(completed.size.toString(), "Тренировок", Modifier.weight(1f))
                            StatCard(totalMinutes.toString(), "Минут активности", Modifier.weight(1f))
                        }
                    }
                    item {
                        val average = if (completed.isEmpty()) 0 else totalMinutes / completed.size
                        StatCard(average.toString(), "Средняя длительность, мин", Modifier.fillMaxWidth())
                    }
                    item {
                        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
                            .background(colors.surface).padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Text("Активность", color = colors.textPrimary, style = AppTheme.type.h3)
                            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                                listOf("Неделя", "Месяц", "Год").forEach { period ->
                                    PillChip(period,
                                        if (progressPeriod == period) colors.accent else colors.bgScreen,
                                        if (progressPeriod == period) colors.inkOn else colors.textSecondary,
                                        onClick = { progressPeriod = period })
                                }
                            }
                            WorkoutProgressChart(buckets)
                        }
                    }
                    item { SectionHeading("Последние занятия") }
                    items(completed.take(7), key = { it.id }) { session ->
                        SessionCard(session) { selectedId = session.id; route = WorkoutRoute.DETAIL }
                    }
                    if (completed.isEmpty()) item {
                        EmptyHint("Заверши первую тренировку — здесь появится статистика и график активности")
                    }
                    item { Spacer(Modifier.height(16.dp)) }
                }
            }
        }
    }

    if (showRenameWorkoutDialog) {
        AlertDialog(
            onDismissRequest = { showRenameWorkoutDialog = false },
            title = { Text("Название тренировки") },
            text = { OutlinedTextField(renameValue, { renameValue = it }, singleLine = true, label = { Text("Название") }) },
            confirmButton = {
                TextButton(onClick = {
                    val id = editorSession?.id
                    if (id != null && renameValue.isNotBlank()) scope.launch { repository.renameWorkout(id, renameValue) }
                    showRenameWorkoutDialog = false
                }, enabled = renameValue.isNotBlank()) { Text("Сохранить") }
            },
            dismissButton = { TextButton(onClick = { showRenameWorkoutDialog = false }) { Text("Отмена") } },
        )
    }

    if (showCustomWorkoutDialog) {
        AlertDialog(
            onDismissRequest = { showCustomWorkoutDialog = false },
            title = { Text("Новая тренировка") },
            text = { OutlinedTextField(title, { title = it }, label = { Text("Название") }, singleLine = true) },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        val id = repository.startWorkout(title)
                        selectedId = id
                        showCustomWorkoutDialog = false
                        route = WorkoutRoute.EDITOR
                    }
                }, enabled = title.isNotBlank()) { Text("Создать") }
            },
            dismissButton = { TextButton(onClick = { showCustomWorkoutDialog = false }) { Text("Отмена") } },
        )
    }

    if (showDeleteWorkoutDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteWorkoutDialog = false },
            title = { Text(if (editorSession?.endedAtMillis == null && route == WorkoutRoute.EDITOR) "Завершить тренировку?" else "Удалить тренировку?") },
            text = { Text(if (editorSession?.endedAtMillis == null && route == WorkoutRoute.EDITOR)
                "Тренировка сохранится в истории." else "Тренировка и её упражнения будут удалены.") },
            confirmButton = {
                TextButton(onClick = {
                    val id = selectedId
                    scope.launch {
                        if (id != null) {
                            if (route == WorkoutRoute.EDITOR && editorSession?.endedAtMillis == null) {
                                repository.finishWorkout(id)
                                route = WorkoutRoute.HOME
                            } else {
                                repository.deleteWorkout(id)
                                selectedId = null
                                route = WorkoutRoute.HISTORY
                            }
                        }
                        showDeleteWorkoutDialog = false
                    }
                }) { Text(if (route == WorkoutRoute.EDITOR && editorSession?.endedAtMillis == null) "Завершить" else "Удалить") }
            },
            dismissButton = { TextButton(onClick = { showDeleteWorkoutDialog = false }) { Text("Отмена") } },
        )
    }
}

@Composable
private fun SectionHeading(text: String) {
    Text(text, color = AppTheme.colors.textPrimary, style = AppTheme.type.h3)
}

@Composable
private fun NavigationTile(icon: String, title: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val colors = AppTheme.colors
    Column(modifier.clip(RoundedCornerShape(20.dp)).background(colors.surface)
        .clickableNoRipple(onClick).padding(15.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(icon, color = colors.accent, fontSize = 24.sp)
        Text(title, color = colors.textPrimary, style = AppTheme.type.bodyL, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun TemplateCard(template: WorkoutTemplate, onClick: () -> Unit) {
    val colors = AppTheme.colors
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(colors.surface)
        .clickableNoRipple(onClick).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(13.dp)) {
        Box(Modifier.size(60.dp).clip(RoundedCornerShape(17.dp)).background(colors.accentSoft),
            contentAlignment = Alignment.Center) {
            WorkoutIllustration(when { template.title.contains("Кардио") -> 1; template.title.contains("Пресс") -> 3; else -> 0 }, Modifier.fillMaxSize().padding(2.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(template.title, color = colors.textPrimary, style = AppTheme.type.bodyL, fontWeight = FontWeight.SemiBold)
            Text(template.subtitle, color = colors.textSecondary, style = AppTheme.type.caption)
            Text("${template.duration} · ${template.exercises.size} упр.",
                color = colors.textSecondary, style = AppTheme.type.caption)
        }
        Text("›", color = colors.accent, fontSize = 28.sp)
    }
}

@Composable
private fun SessionCard(session: WorkoutSession, onClick: () -> Unit) {
    val colors = AppTheme.colors
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(colors.surface)
        .clickableNoRipple(onClick).padding(15.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(48.dp).clip(RoundedCornerShape(15.dp)).background(colors.accentSoft),
            contentAlignment = Alignment.Center) { Text("🏋️", fontSize = 25.sp) }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(session.title, color = colors.textPrimary, style = AppTheme.type.bodyL, fontWeight = FontWeight.SemiBold)
            Text("${formatDate(session.startedAtMillis)} · ${duration(session)}",
                color = colors.textSecondary, style = AppTheme.type.caption)
        }
        Text("›", color = colors.accent, fontSize = 28.sp)
    }
}

@Composable
private fun ExerciseCard(
    name: String, sets: List<WorkoutSet>, editable: Boolean,
    onAddSet: () -> Unit, onUpdate: (WorkoutSet) -> Unit,
    onDeleteSet: (String) -> Unit, onDeleteExercise: () -> Unit,
) {
    val colors = AppTheme.colors
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(colors.surface).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            WorkoutIllustration(exerciseIllustrationIndex(name), Modifier.size(54.dp))
            Text(name, modifier = Modifier.weight(1f).padding(start = 8.dp), color = colors.textPrimary, style = AppTheme.type.h4)
            if (editable) TextButton(onClick = onDeleteExercise) { Text("Удалить", color = colors.danger) }
        }
        if (sets.isEmpty()) Text("Подходы ещё не добавлены", color = colors.textSecondary, style = AppTheme.type.caption)
        sets.forEach { set ->
            if (editable) WorkoutSetEditor(set, onUpdate) { onDeleteSet(set.id) }
            else Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(colors.bgScreen).padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Подход ${set.setNumber}", color = colors.textSecondary)
                Text("${set.reps} повт. · ${set.weightKg} кг", color = colors.textPrimary)
            }
        }
        if (editable) OutlinedButton(onClick = onAddSet, modifier = Modifier.fillMaxWidth()) {
            Text("＋ Добавить подход")
        }
    }
}

@Composable
private fun WorkoutSetEditor(set: WorkoutSet, onChange: (WorkoutSet) -> Unit, onDelete: () -> Unit) {
    val colors = AppTheme.colors
    var reps by remember(set.id) { mutableStateOf(set.reps.toString()) }
    var weight by remember(set.id) { mutableStateOf(set.weightKg.toString()) }
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(colors.bgScreen).padding(7.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("${set.setNumber}", color = colors.accent, fontWeight = FontWeight.Bold)
        OutlinedTextField(reps, { value ->
            reps = value.filter(Char::isDigit)
            reps.toIntOrNull()?.let { onChange(set.copy(reps = it)) }
        }, modifier = Modifier.weight(1f), label = { Text("Повторы") }, singleLine = true)
        OutlinedTextField(weight, { value ->
            weight = value.filter { it.isDigit() || it == '.' || it == ',' }.replace(',', '.')
            weight.toDoubleOrNull()?.let { onChange(set.copy(weightKg = it)) }
        }, modifier = Modifier.weight(1f), label = { Text("Кг") }, singleLine = true)
        TextButton(onClick = { onChange(set.copy(completed = !set.completed)) }) {
            Text(if (set.completed) "✓" else "○", color = if (set.completed) colors.success else colors.textSecondary)
        }
        TextButton(onClick = onDelete) { Text("×", color = colors.danger) }
    }
}

@Composable
private fun SettingInfoCard(icon: String, title: String, subtitle: String) {
    val colors = AppTheme.colors
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(colors.surface).padding(15.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(icon, fontSize = 24.sp)
        Column {
            Text(title, color = colors.textPrimary, style = AppTheme.type.bodyL)
            Text(subtitle, color = colors.textSecondary, style = AppTheme.type.caption)
        }
    }
}

@Composable
private fun StatCard(value: String, label: String, modifier: Modifier = Modifier) {
    val colors = AppTheme.colors
    Column(modifier.clip(RoundedCornerShape(18.dp)).background(colors.surface).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(value, color = colors.accent, style = AppTheme.type.h2)
        Text(label, color = colors.textSecondary, style = AppTheme.type.caption)
    }
}

@Composable
private fun EmptyHint(text: String) {
    Text(text, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
        .background(AppTheme.colors.surface).padding(18.dp),
        color = AppTheme.colors.textSecondary, style = AppTheme.type.bodyS)
}

private fun formatDate(time: Long): String =
    SimpleDateFormat("d MMM, HH:mm", Locale("ru")).format(Date(time))

private fun duration(session: WorkoutSession): String {
    val end = session.endedAtMillis ?: System.currentTimeMillis()
    val minutes = ((end - session.startedAtMillis).coerceAtLeast(0L) / 60_000L).toInt()
    return if (minutes >= 60) "${minutes / 60} ч ${minutes % 60} мин" else "$minutes мин"
}

private fun workoutProgressBuckets(sessions: List<WorkoutSession>, period: String): List<Pair<String, Int>> {
    val now = System.currentTimeMillis()
    return when (period) {
        "Месяц" -> (3 downTo 0).map { offset ->
            val c = java.util.Calendar.getInstance().apply {
                timeInMillis = now
                add(java.util.Calendar.WEEK_OF_YEAR, -offset)
                set(java.util.Calendar.DAY_OF_WEEK, firstDayOfWeek)
                set(java.util.Calendar.HOUR_OF_DAY, 0); set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0); set(java.util.Calendar.MILLISECOND, 0)
            }
            val from = c.timeInMillis
            c.add(java.util.Calendar.DAY_OF_YEAR, 7)
            "${4 - offset} нед." to sessions.count { it.startedAtMillis >= from && it.startedAtMillis < c.timeInMillis }
        }
        "Год" -> (11 downTo 0).map { offset ->
            val c = java.util.Calendar.getInstance().apply {
                timeInMillis = now
                add(java.util.Calendar.MONTH, -offset)
                set(java.util.Calendar.DAY_OF_MONTH, 1)
                set(java.util.Calendar.HOUR_OF_DAY, 0); set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0); set(java.util.Calendar.MILLISECOND, 0)
            }
            val from = c.timeInMillis
            c.add(java.util.Calendar.MONTH, 1)
            SimpleDateFormat("LLL", Locale("ru")).format(Date(from)).replaceFirstChar { it.uppercase() } to
                sessions.count { it.startedAtMillis >= from && it.startedAtMillis < c.timeInMillis }
        }
        else -> (6 downTo 0).map { offset ->
            val c = java.util.Calendar.getInstance().apply {
                timeInMillis = now
                add(java.util.Calendar.DAY_OF_YEAR, -offset)
                set(java.util.Calendar.HOUR_OF_DAY, 0); set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0); set(java.util.Calendar.MILLISECOND, 0)
            }
            val from = c.timeInMillis
            c.add(java.util.Calendar.DAY_OF_YEAR, 1)
            SimpleDateFormat("EE", Locale("ru")).format(Date(from)).replaceFirstChar { it.uppercase() } to
                sessions.count { it.startedAtMillis >= from && it.startedAtMillis < c.timeInMillis }
        }
    }
}

@Composable
private fun WorkoutProgressChart(buckets: List<Pair<String, Int>>) {
    val colors = AppTheme.colors
    val maximum = (buckets.maxOfOrNull { it.second } ?: 0).coerceAtLeast(1)
    Row(Modifier.fillMaxWidth().height(174.dp),
        horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.Bottom) {
        buckets.forEach { (label, value) ->
            Column(Modifier.weight(1f).fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom) {
                Text(if (value > 0) value.toString() else "", color = colors.textSecondary,
                    style = AppTheme.type.caption)
                Spacer(Modifier.height(5.dp))
                Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.BottomCenter) {
                    Box(Modifier.fillMaxWidth(0.72f)
                        .fillMaxHeight((value.toFloat() / maximum).coerceAtLeast(if (value > 0) 0.08f else 0f))
                        .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                        .background(colors.accent))
                }
                Spacer(Modifier.height(7.dp))
                Text(label, color = colors.textSecondary, style = AppTheme.type.caption, maxLines = 1)
            }
        }
    }
    if (buckets.all { it.second == 0 }) Text("Пока нет завершённых тренировок за этот период",
        color = colors.textSecondary, style = AppTheme.type.caption)
}

@Composable
private fun RestTimerScreen(onFinish: () -> Unit) {
    val colors = AppTheme.colors
    var remaining by remember { mutableIntStateOf(60) }
    var running by remember { mutableStateOf(true) }
    LaunchedEffect(running, remaining) {
        if (running && remaining > 0) {
            delay(1_000)
            remaining = (remaining - 1).coerceAtLeast(0)
        } else if (remaining == 0) running = false
    }
    Column(
        Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp, Alignment.CenterVertically),
    ) {
        Box(Modifier.size(248.dp).clip(RoundedCornerShape(124.dp)).background(colors.accentSoft),
            contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(String.format(Locale.US, "%02d:%02d", remaining / 60, remaining % 60),
                    color = colors.textPrimary, style = AppTheme.type.displayL)
                Text(if (remaining == 0) "Можно продолжать" else "Осталось",
                    color = colors.textSecondary, style = AppTheme.type.bodyS)
            }
        }
        Text("Отдых между подходами", color = colors.textPrimary, style = AppTheme.type.h2)
        Text("Восстанови дыхание и подготовься к следующему подходу",
            color = colors.textSecondary, style = AppTheme.type.bodyS)
        PrimaryButton(if (running) "Ⅱ  Пауза" else if (remaining == 0) "↻  Повторить" else "▶  Продолжить",
            onClick = { if (remaining == 0) remaining = 60; running = !running })
        TextButton(onClick = { remaining = 60; running = true }) { Text("Сбросить таймер") }
        TextButton(onClick = onFinish) { Text("Вернуться к тренировке") }
    }
}

private fun durationAt(session: WorkoutSession, now: Long): String {
    val end = session.endedAtMillis ?: now
    val minutes = ((end - session.startedAtMillis).coerceAtLeast(0L) / 60_000L).toInt()
    return if (minutes >= 60) "${minutes / 60} ч ${minutes % 60} мин" else "$minutes мин"
}

private fun exerciseIllustrationIndex(name: String): Int = when {
    name.contains("присед", true) || name.contains("выпад", true) -> 2
    name.contains("планк", true) || name.contains("отжим", true) || name.contains("берпи", true) -> 3
    name.contains("бег", true) || name.contains("прыж", true) -> 1
    else -> 0
}

@Composable
private fun WorkoutIllustration(index: Int, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val image = remember(index) {
        val sheet = BitmapFactory.decodeResource(context.resources, ru.plumsoftware.habitstracker.R.drawable.workout_assets)
        val x = (index % 2) * 128
        val y = (index / 2) * 128
        Bitmap.createBitmap(sheet, x, y, 128, 128).asImageBitmap()
    }
    Image(bitmap = image, contentDescription = null, modifier = modifier, contentScale = ContentScale.Fit)
}

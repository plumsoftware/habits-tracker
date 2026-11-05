package ru.plumsoftware.habitstracker.ui.screen.main.workouts

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.saveable.rememberSaveable
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
import ru.plumsoftware.habitstracker.ui.components.PillChip
import ru.plumsoftware.habitstracker.ui.components.clickableNoRipple
import ru.plumsoftware.habitstracker.ui.theme.AppTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.foundation.Canvas
import ru.plumsoftware.habitstracker.R

private enum class WorkoutRoute { HOME, EDITOR, HISTORY, TEMPLATES, TEMPLATE_DETAIL, SETTINGS, PROGRESS, EXERCISE_PICKER, DETAIL, REST_TIMER }

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
fun WorkoutScreen(
    onBack: () -> Unit,
    onWorkoutFinished: () -> Unit = {},
    showCreationAd: suspend () -> Unit = {},
) {
    val repository: WellnessRepository = koinInject()
    val scope = rememberCoroutineScope()
    val screenStates = rememberSaveableStateHolder()
    val colors = AppTheme.colors
    val sessions by repository.workouts().collectAsState(initial = emptyList())
    var route by remember { mutableStateOf(WorkoutRoute.HOME) }
    var previewTemplate by remember { mutableStateOf(workoutTemplates.first()) }
    var templateFilter by rememberSaveable { mutableStateOf("Все") }
    var selectedId by remember { mutableStateOf<String?>(null) }
    var title by remember { mutableStateOf("Моя тренировка") }
    var exerciseName by remember { mutableStateOf("") }
    var showCustomWorkoutDialog by remember { mutableStateOf(false) }
    var creatingWorkout by remember { mutableStateOf(false) }
    var showDeleteWorkoutDialog by remember { mutableStateOf(false) }
    var showRenameWorkoutDialog by remember { mutableStateOf(false) }
    var renameValue by remember { mutableStateOf("") }
    var clockTick by remember { mutableStateOf(System.currentTimeMillis()) }
    var filter by rememberSaveable { mutableStateOf("Все") }

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
    fun createWorkout(name: String, exercises: List<String> = emptyList(), adBeforeCreation: Boolean) {
        if (creatingWorkout) return
        creatingWorkout = true
        scope.launch {
            try {
                if (adBeforeCreation) showCreationAd()
                val id = repository.startWorkout(name)
                exercises.forEach { repository.addExercise(id, it) }
                if (!adBeforeCreation) showCreationAd()
                showCustomWorkoutDialog = false
                openEditor(id)
            } finally {
                creatingWorkout = false
            }
        }
    }

    val navigateBack: () -> Unit = {
        if (!creatingWorkout) {
            when (route) {
                WorkoutRoute.HOME -> onBack()
                WorkoutRoute.EDITOR -> route = if (editorSession?.endedAtMillis != null) WorkoutRoute.DETAIL else WorkoutRoute.HOME
                WorkoutRoute.HISTORY, WorkoutRoute.TEMPLATES, WorkoutRoute.SETTINGS, WorkoutRoute.PROGRESS -> route = WorkoutRoute.HOME
                WorkoutRoute.TEMPLATE_DETAIL -> route = WorkoutRoute.TEMPLATES
                WorkoutRoute.EXERCISE_PICKER -> route = WorkoutRoute.EDITOR
                WorkoutRoute.DETAIL -> route = WorkoutRoute.HISTORY
                WorkoutRoute.REST_TIMER -> route = WorkoutRoute.EDITOR
            }
        }
    }
    BackHandler(onBack = navigateBack)

    Column(Modifier.fillMaxSize().background(colors.bgScreen)
        .windowInsetsPadding(WindowInsets.safeDrawing)) {
        AppScreenTopBar(
            title = when (route) {
                WorkoutRoute.HOME -> "Тренировки"
                WorkoutRoute.EDITOR -> editorSession?.title ?: "Создать тренировку"
                WorkoutRoute.HISTORY -> "История"
                WorkoutRoute.TEMPLATES -> "Шаблоны"
                WorkoutRoute.TEMPLATE_DETAIL -> "Программа тренировки"
                WorkoutRoute.SETTINGS -> "Настройки"
                WorkoutRoute.PROGRESS -> "Прогресс"
                WorkoutRoute.EXERCISE_PICKER -> "Добавить упражнение"
                WorkoutRoute.DETAIL -> selected?.title ?: "Тренировка"
                WorkoutRoute.REST_TIMER -> "Отдых между подходами"
            },
            onBack = navigateBack,
        )

        val screenKey = when (route) {
            WorkoutRoute.EDITOR -> "editor:${editorSession?.id.orEmpty()}"
            WorkoutRoute.DETAIL -> "detail:${selectedId.orEmpty()}"
            WorkoutRoute.TEMPLATE_DETAIL -> "template:${previewTemplate.title}"
            WorkoutRoute.EXERCISE_PICKER -> "picker:${editorSession?.id.orEmpty()}"
            else -> route.name
        }
        screenStates.SaveableStateProvider(screenKey) {
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
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            WorkoutIllustration("equipment_dumbbell_large", Modifier.weight(1f).height(104.dp))
                            WorkoutIllustration("equipment_jump_rope", Modifier.weight(1f).height(104.dp))
                        }
                        Text("Тренировка продолжается", color = colors.textPrimary, style = AppTheme.type.h2)
                        Text(active.title, color = colors.textSecondary, style = AppTheme.type.bodyS)
                        Text("Время: ${durationAt(active, clockTick)}", color = colors.textSecondary, style = AppTheme.type.caption)
                        WorkoutPrimaryButton("Продолжить", onClick = { openEditor(active.id) })
                    }
                } else item {
                    Column(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp))
                            .background(colors.accentSoft).padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            WorkoutIllustration("equipment_dumbbell_large", Modifier.weight(1f).height(104.dp))
                            WorkoutIllustration("equipment_jump_rope", Modifier.weight(1f).height(104.dp))
                        }
                        Text("Готов к тренировке?", color = colors.textPrimary, style = AppTheme.type.h2)
                        Text("Выбери шаблон или собери собственную программу",
                            color = colors.textSecondary, style = AppTheme.type.bodyS)
                        WorkoutPrimaryButton("＋ Создать тренировку", onClick = {
                            title = "Моя тренировка"; showCustomWorkoutDialog = true
                        })
                    }
                }
                item { SectionHeading("Шаблоны тренировок") }
                items(workoutTemplates.take(3)) { template ->
                    TemplateCard(template) { previewTemplate = template; route = WorkoutRoute.TEMPLATE_DETAIL }
                }
                item {
                    TextButton(onClick = { route = WorkoutRoute.TEMPLATES }, modifier = Modifier.fillMaxWidth()) {
                        Text("Все шаблоны →")
                    }
                }
                item {
                    WorkoutOutlinedButton(onClick = { route = WorkoutRoute.HISTORY }) {
                        Text("История тренировок")
                    }
                }
                item {
                    WorkoutOutlinedButton(onClick = { route = WorkoutRoute.SETTINGS }) {
                        Text("Настройки")
                    }
                }
                item {
                    WorkoutOutlinedButton(onClick = { route = WorkoutRoute.PROGRESS }) {
                        Text("Прогресс")
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
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("Все", "Силовые", "Кардио", "Пресс").forEach { value ->
                            PillChip(value, if (templateFilter == value) colors.accent else colors.surface,
                                if (templateFilter == value) colors.inkOn else colors.textSecondary,
                                onClick = { templateFilter = value })
                        }
                    }
                }
                items(workoutTemplates.filter { template ->
                    templateFilter == "Все" || template.title.contains(
                        if (templateFilter == "Силовые") "Силов" else templateFilter, true)
                }) { template -> TemplateCard(template) { previewTemplate = template; route = WorkoutRoute.TEMPLATE_DETAIL } }
                item {
                    WorkoutOutlinedButton(onClick = {
                        title = "Моя тренировка"; showCustomWorkoutDialog = true
                    }, modifier = Modifier.fillMaxWidth()) { Text("＋ Создать свою") }
                }
            }

            WorkoutRoute.TEMPLATE_DETAIL -> LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp))
                        .background(colors.accentSoft).padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        WorkoutIllustration(templateIllustrationAsset(previewTemplate),
                            Modifier.fillMaxWidth().height(140.dp))
                        Text(previewTemplate.title, color = colors.textPrimary, style = AppTheme.type.h2)
                        Text(previewTemplate.subtitle, color = colors.textSecondary, style = AppTheme.type.bodyS)
                        Text("◷ ${previewTemplate.duration} · ${previewTemplate.exercises.size} упр.",
                            color = colors.textSecondary, style = AppTheme.type.bodyS)
                    }
                }
                item { SectionHeading("Упражнения") }
                items(previewTemplate.exercises.withIndex().toList()) { (index, name) ->
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
                        .background(colors.surface).padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("${index + 1}", color = colors.accent, style = AppTheme.type.bodyL)
                        WorkoutIllustration(exerciseIllustrationAsset(name), Modifier.size(64.dp))
                        Text(name, color = colors.textPrimary, style = AppTheme.type.bodyL,
                            modifier = Modifier.weight(1f))
                    }
                }
                item {
                    WorkoutPrimaryButton(if (active == null) "▶ Начать тренировку" else "Продолжить текущую тренировку",
                        enabled = !creatingWorkout,
                        onClick = {
                            if (active != null) openEditor(active.id)
                            else createWorkout(previewTemplate.title, previewTemplate.exercises, adBeforeCreation = true)
                        })
                }
                item { Spacer(Modifier.height(24.dp)) }
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
                                if (editorSession.endedAtMillis == null) {
                                    IconButton(onClick = {
                                        renameValue = editorSession.title
                                        showRenameWorkoutDialog = true
                                    }) {
                                        Icon(Icons.Outlined.Edit, contentDescription = "Редактировать название",
                                            tint = colors.accent)
                                    }
                                }
                            }
                            Text("Начата · ${formatDate(editorSession.startedAtMillis)}",
                                color = colors.textSecondary, style = AppTheme.type.caption)
                            if (editorSession.endedAtMillis == null) {
                                WorkoutPrimaryButton("＋ Добавить упражнение", onClick = { route = WorkoutRoute.EXERCISE_PICKER })
                                WorkoutOutlinedButton(onClick = { showDeleteWorkoutDialog = true },
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
                var query by rememberSaveable { mutableStateOf("") }
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
                            WorkoutIllustration(exerciseIllustrationAsset(name), Modifier.size(64.dp))
                            Text(name, modifier = Modifier.weight(1f).padding(start = 12.dp),
                                color = colors.textPrimary, style = AppTheme.type.bodyL)
                            Text("＋", color = colors.accent, fontSize = 24.sp)
                        }
                    }
                    item {
                        OutlinedTextField(exerciseName, { exerciseName = it }, modifier = Modifier.fillMaxWidth(),
                            label = { Text("Своё упражнение") }, singleLine = true)
                        WorkoutPrimaryButton("Добавить своё упражнение", enabled = exerciseName.isNotBlank(),
                            onClick = {
                                val sessionId = editorSession?.id ?: return@WorkoutPrimaryButton
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
                            WorkoutIllustration(sessionIllustrationAsset(selected.title), Modifier.align(Alignment.End).size(width = 180.dp, height = 112.dp))
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
                        WorkoutOutlinedButton(onClick = { showDeleteWorkoutDialog = true },
                            modifier = Modifier.fillMaxWidth()) { Text("Удалить тренировку") }
                    }
                }
            }

            WorkoutRoute.SETTINGS -> LazyColumn(Modifier.fillMaxSize(),
                contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item { SectionHeading("Общие") }
                item { SettingInfoCard("🔔", "Уведомления", "Напоминания о тренировках") }
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
                var progressPeriod by rememberSaveable { mutableStateOf("Неделя") }
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
            onDismissRequest = { if (!creatingWorkout) showCustomWorkoutDialog = false },
            title = { Text("Новая тренировка") },
            text = {
                OutlinedTextField(title, { title = it }, label = { Text("Название") },
                    enabled = !creatingWorkout, singleLine = true)
            },
            confirmButton = {
                TextButton(
                    onClick = { createWorkout(title, adBeforeCreation = route == WorkoutRoute.TEMPLATES) },
                    enabled = title.isNotBlank() && !creatingWorkout,
                ) { Text("Создать") }
            },
            dismissButton = {
                TextButton(enabled = !creatingWorkout, onClick = { showCustomWorkoutDialog = false }) { Text("Отмена") }
            },
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
                                onWorkoutFinished()
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

// Both variants share the same minimum height and padding. Long labels and
// accessibility font sizes can expand naturally instead of being clipped.
private val workoutButtonMinHeight = 52.dp
private val workoutButtonPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)

@Composable
private fun WorkoutPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().heightIn(min = workoutButtonMinHeight),
        enabled = enabled,
        shape = AppTheme.shapes.button,
        contentPadding = workoutButtonPadding,
        colors = ButtonDefaults.buttonColors(
            containerColor = AppTheme.colors.accent,
            contentColor = AppTheme.colors.inkOn,
        ),
    ) {
        Text(text, style = AppTheme.type.h5, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

@Composable
private fun WorkoutOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().heightIn(min = workoutButtonMinHeight),
        shape = AppTheme.shapes.button,
        contentPadding = workoutButtonPadding,
        colors = ButtonDefaults.outlinedButtonColors(contentColor = AppTheme.colors.accent),
        border = androidx.compose.foundation.BorderStroke(1.dp, AppTheme.colors.accent),
    ) {
        ProvideTextStyle(AppTheme.type.h5) { content() }
    }
}

@Composable
private fun SectionHeading(text: String) {
    Text(text, color = AppTheme.colors.textPrimary, style = AppTheme.type.h3)
}

@Composable
private fun TemplateCard(template: WorkoutTemplate, onClick: () -> Unit) {
    val colors = AppTheme.colors
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(colors.surface)
        .clickableNoRipple(onClick = onClick).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(13.dp)) {
        Box(Modifier.size(60.dp).clip(RoundedCornerShape(17.dp)).background(colors.accentSoft),
            contentAlignment = Alignment.Center) {
            WorkoutIllustration(templateIllustrationAsset(template), Modifier.fillMaxSize().padding(2.dp))
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
        .clickableNoRipple(onClick = onClick).padding(15.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(48.dp).clip(RoundedCornerShape(15.dp)).background(colors.accentSoft),
            contentAlignment = Alignment.Center) {
            WorkoutIllustration(sessionIllustrationAsset(session.title), Modifier.fillMaxSize())
        }
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
            WorkoutIllustration(exerciseIllustrationAsset(name), Modifier.size(54.dp))
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
        if (editable) WorkoutOutlinedButton(onClick = onAddSet, modifier = Modifier.fillMaxWidth()) {
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
        Box(Modifier.size(248.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize().padding(10.dp)) {
                val stroke = androidx.compose.ui.graphics.drawscope.Stroke(12.dp.toPx(), cap = StrokeCap.Round)
                drawArc(colors.accentSoft, -90f, 360f, false, style = stroke)
                drawArc(colors.accent, -90f, 360f * remaining / 60f, false, style = stroke)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(String.format(Locale.US, "%02d:%02d", remaining / 60, remaining % 60),
                    color = colors.textPrimary, style = AppTheme.type.displayS)
                Text(if (remaining == 0) "Можно продолжать" else "Осталось",
                    color = colors.textSecondary, style = AppTheme.type.bodyS)
            }
        }
        WorkoutIllustration("exercise_side_stretch", Modifier.fillMaxWidth().heightIn(min = 80.dp, max = 160.dp).weight(1f, fill = false))
        Text("Отдых между подходами", color = colors.textPrimary, style = AppTheme.type.h2)
        Text("Восстанови дыхание и подготовься к следующему подходу",
            color = colors.textSecondary, style = AppTheme.type.bodyS)
        WorkoutPrimaryButton(if (running) "Ⅱ  Пауза" else if (remaining == 0) "↻  Повторить" else "▶  Продолжить",
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

private fun templateIllustrationAsset(template: WorkoutTemplate): String = when {
    template.title.contains("Кардио", true) -> "equipment_shoes"
    template.title.contains("Пресс", true) -> "equipment_ab_wheel"
    template.title.contains("Растяж", true) -> "equipment_yoga_mat"
    template.title.contains("Функцион", true) -> "equipment_kettlebell"
    else -> "equipment_dumbbell_large"
}

private fun exerciseIllustrationAsset(name: String): String = when {
    name.contains("присед", true) -> "exercise_squat"
    name.contains("выпад", true) -> "exercise_lunge"
    name.contains("отжим", true) -> "exercise_plank_to_pushup"
    name.contains("планк", true) -> "exercise_high_plank"
    name.contains("скручив", true) || name.contains("велосипед", true) -> "exercise_crunch"
    (name.contains("подъём", true) || name.contains("подъем", true)) && name.contains("гантел", true) -> "exercise_biceps_curl"
    name.contains("подъём", true) || name.contains("подъем", true) -> "exercise_leg_raise"
    name.contains("подтяг", true) -> "equipment_weight_plate"
    name.contains("берпи", true) -> "exercise_mountain_climber"
    name.contains("бег", true) -> "exercise_high_knees"
    name.contains("прыж", true) -> "exercise_jump_squat"
    name.contains("жим", true) -> "exercise_overhead_press"
    name.contains("тяга", true) -> "exercise_dumbbell_row"
    name.contains("развед", true) -> "exercise_lateral_raise"
    name.contains("мост", true) -> "exercise_glute_bridge"
    name.contains("скакал", true) -> "equipment_jump_rope"
    name.contains("растяж", true) || name.contains("наклон", true) || name.contains("поворот", true) -> "exercise_side_stretch"
    else -> "exercise_squat_hold"
}

private fun sessionIllustrationAsset(title: String): String = when {
    title.contains("Кардио", true) -> "equipment_shoes"
    title.contains("Пресс", true) -> "equipment_ab_wheel"
    title.contains("Растяж", true) -> "equipment_yoga_mat"
    title.contains("Функцион", true) -> "equipment_kettlebell"
    else -> "equipment_dumbbell_large"
}

// Coordinates refer to the original 1536 × 1024 sprite sheet. The source remains
// intact; crops are decoded once and shared by all workout screens.
private data class PoseRegion(val x: Int, val y: Int, val width: Int, val height: Int)
private val poseRegions = mapOf(
    "exercise_squat" to PoseRegion(24, 330, 192, 254),
    "exercise_lunge" to PoseRegion(220, 330, 194, 218),
    "exercise_high_plank" to PoseRegion(427, 345, 271, 182),
    "exercise_mountain_climber" to PoseRegion(699, 345, 250, 182),
    "exercise_glute_bridge" to PoseRegion(940, 380, 254, 147),
    "exercise_crunch" to PoseRegion(1190, 414, 259, 114),
    "exercise_leg_raise" to PoseRegion(1444, 318, 92, 210),
    "exercise_plank_to_pushup" to PoseRegion(0, 588, 270, 168),
    "exercise_side_plank" to PoseRegion(342, 552, 280, 202),
    "exercise_dumbbell_row" to PoseRegion(619, 536, 145, 234),
    "exercise_overhead_press" to PoseRegion(801, 528, 131, 252),
    "exercise_biceps_curl" to PoseRegion(961, 530, 132, 249),
    "exercise_step_up" to PoseRegion(1386, 536, 150, 240),
    "exercise_squat_hold" to PoseRegion(27, 826, 145, 193),
    "exercise_jump_squat" to PoseRegion(188, 766, 170, 254),
    "exercise_high_knees" to PoseRegion(371, 764, 152, 257),
    "exercise_lateral_raise" to PoseRegion(746, 780, 240, 244),
    "exercise_overhead_triceps" to PoseRegion(1186, 773, 107, 251),
    "exercise_side_stretch" to PoseRegion(1342, 775, 169, 249),
)

private object WorkoutArt {
    private val poses = mutableMapOf<String, ImageBitmap>()
    private var sheet: Bitmap? = null

    @Synchronized
    fun pose(resources: android.content.res.Resources, name: String): ImageBitmap? {
        poses[name]?.let { return it }
        val region = poseRegions[name] ?: return null
        val source = sheet ?: BitmapFactory.decodeResource(resources, R.drawable.workout_pose_sheet,
            BitmapFactory.Options().apply { inScaled = false }).also { sheet = it }
        return Bitmap.createBitmap(source, region.x, region.y, region.width, region.height)
            .asImageBitmap().also { poses[name] = it }
    }
}

private val equipmentResources = mapOf(
    "equipment_dumbbell_large" to R.drawable.equipment_dumbbell_large,
    "equipment_dumbbell_small" to R.drawable.equipment_dumbbell_small,
    "equipment_kettlebell" to R.drawable.equipment_kettlebell,
    "equipment_shoes" to R.drawable.step_sneaker,
    "equipment_yoga_mat" to R.drawable.equipment_yoga_mat,
    "equipment_ab_wheel" to R.drawable.equipment_ab_wheel,
    "equipment_jump_rope" to R.drawable.equipment_jump_rope,
    "equipment_weight_plate" to R.drawable.equipment_weight_plate,
)

@Composable
private fun WorkoutIllustration(assetName: String, modifier: Modifier = Modifier) {
    val equipment = equipmentResources[assetName]
    if (equipment != null) {
        Image(painterResource(equipment), contentDescription = null,
            modifier = modifier, contentScale = ContentScale.Fit)
    } else {
        val resources = LocalContext.current.resources
        val image = remember(resources, assetName) { WorkoutArt.pose(resources, assetName) }
        if (image != null) Image(bitmap = image, contentDescription = null,
            modifier = modifier, contentScale = ContentScale.Fit)
    }
}

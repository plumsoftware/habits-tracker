package ru.plumsoftware.habitstracker.ui.screen.main.workouts

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
import org.koin.compose.koinInject
import ru.plumsoftware.habitstracker.data.database.WellnessRepository
import ru.plumsoftware.habitstracker.data.model.WorkoutSession
import ru.plumsoftware.habitstracker.data.model.WorkoutSet
import ru.plumsoftware.habitstracker.ui.components.AppScreenTopBar
import ru.plumsoftware.habitstracker.ui.components.PrimaryButton
import ru.plumsoftware.habitstracker.ui.components.clickableNoRipple
import ru.plumsoftware.habitstracker.ui.theme.AppTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun WorkoutScreen(onBack: () -> Unit) {
    val repository: WellnessRepository = koinInject()
    val scope = rememberCoroutineScope()
    val colors = AppTheme.colors
    val sessions by repository.workouts().collectAsState(initial = emptyList())
    var selectedId by remember { mutableStateOf<String?>(null) }
    var startDialog by remember { mutableStateOf(false) }
    var exerciseDialog by remember { mutableStateOf(false) }
    var workoutTitle by remember { mutableStateOf("Силовая тренировка") }
    var exerciseName by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    val active = sessions.firstOrNull { it.endedAtMillis == null }
    val selected = sessions.firstOrNull { it.id == (selectedId ?: active?.id) }
    val exercises by repository.exercises(selected?.id.orEmpty()).collectAsState(initial = emptyList())
    LaunchedEffect(selected?.id) { notes = selected?.notes.orEmpty() }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(colors.bgScreen),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 36.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            AppScreenTopBar("Тренировки", onBack)
            Text("Записывай упражнения, подходы и прогресс",
                color = colors.textSecondary, style = AppTheme.type.bodyS)
        }

        if (selected == null) {
            item {
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp))
                        .background(colors.accentSoft).padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text("🏋️", fontSize = 54.sp)
                    Text("Готов к тренировке?", color = colors.textPrimary,
                        style = AppTheme.type.h2)
                    Text("Выбери шаблон или создай свою тренировку. Фиксируй подходы и следи за прогрессом.",
                        color = colors.textSecondary, style = AppTheme.type.bodyS)
                    PrimaryButton("Начать тренировку", onClick = { startDialog = true })
                }
            }
            item { SectionTitle("Шаблоны тренировок") }
            item { TemplateCard("💪", "Силовая тренировка", "45 мин  ·  8 упражнений") { workoutTitle = "Силовая тренировка"; startDialog = true } }
            item { TemplateCard("❤️", "Кардио тренировка", "30 мин  ·  6 упражнений") { workoutTitle = "Кардио тренировка"; startDialog = true } }
            item { TemplateCard("🧘", "Растяжка", "15 мин  ·  6 упражнений") { workoutTitle = "Растяжка"; startDialog = true } }
        } else {
            item {
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp))
                        .background(colors.surface).padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(selected.title, color = colors.textPrimary, style = AppTheme.type.h2)
                    Text("Начата · ${formatDate(selected.startedAtMillis)}",
                        color = colors.textSecondary, style = AppTheme.type.caption)
                    OutlinedTextField(
                        value = notes, onValueChange = { notes = it; scope.launch { repository.updateNotes(selected.id, it) } },
                        modifier = Modifier.fillMaxWidth(), label = { Text("Заметки") }, minLines = 2,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(onClick = { exerciseDialog = true }, modifier = Modifier.weight(1f)) {
                            Text("＋ Упражнение")
                        }
                        OutlinedButton(onClick = {
                            scope.launch { repository.finishWorkout(selected.id); selectedId = null }
                        }, modifier = Modifier.weight(1f)) { Text("Завершить") }
                    }
                }
            }
            item { SectionTitle("Упражнения · ${exercises.size}") }
            if (exercises.isEmpty()) {
                item { Text("Добавь первое упражнение, чтобы начать вести тренировку.",
                    color = colors.textSecondary, style = AppTheme.type.bodyS) }
            }
            items(exercises, key = { it.id }) { exercise ->
                val sets by repository.sets(exercise.id).collectAsState(initial = emptyList())
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp))
                        .background(colors.surface).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(exercise.name, modifier = Modifier.weight(1f),
                            color = colors.textPrimary, style = AppTheme.type.h4)
                        TextButton(onClick = { scope.launch { repository.deleteExercise(exercise.id) } }) {
                            Text("Удалить", color = colors.danger)
                        }
                    }
                    sets.forEach { set ->
                        WorkoutSetEditor(
                            set = set,
                            onChange = { updated -> scope.launch { repository.updateSet(updated) } },
                            onDelete = { scope.launch { repository.deleteSet(set.id) } },
                        )
                    }
                    OutlinedButton(
                        onClick = { scope.launch { repository.addSet(exercise.id) } },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("＋ Добавить подход") }
                }
            }
        }

        item { SectionTitle("История тренировок") }
        val history = sessions.filter { it.endedAtMillis != null }
        if (history.isEmpty()) {
            item { Text("Здесь появятся завершённые тренировки", color = colors.textSecondary) }
        } else {
            items(history, key = { "history_${it.id}" }) { session ->
                TemplateCard("🏋️", session.title,
                    "${formatDate(session.startedAtMillis)}  ·  ${duration(session)}") {
                    selectedId = session.id
                }
            }
        }
    }

    if (startDialog) {
        AlertDialog(
            onDismissRequest = { startDialog = false },
            title = { Text("Новая тренировка") },
            text = {
                OutlinedTextField(value = workoutTitle, onValueChange = { workoutTitle = it },
                    label = { Text("Название") }, singleLine = true)
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { selectedId = repository.startWorkout(workoutTitle); startDialog = false }
                }) { Text("Начать") }
            },
            dismissButton = { TextButton(onClick = { startDialog = false }) { Text("Отмена") } },
        )
    }
    if (exerciseDialog && selected != null) {
        AlertDialog(
            onDismissRequest = { exerciseDialog = false; exerciseName = "" },
            title = { Text("Новое упражнение") },
            text = {
                OutlinedTextField(value = exerciseName, onValueChange = { exerciseName = it },
                    label = { Text("Название упражнения") }, singleLine = true)
            },
            confirmButton = {
                TextButton(enabled = exerciseName.isNotBlank(), onClick = {
                    scope.launch { repository.addExercise(selected.id, exerciseName); exerciseName = ""; exerciseDialog = false }
                }) { Text("Добавить") }
            },
            dismissButton = { TextButton(onClick = { exerciseDialog = false }) { Text("Отмена") } },
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, color = AppTheme.colors.textPrimary, style = AppTheme.type.h3)
}

@Composable
private fun TemplateCard(icon: String, title: String, subtitle: String, onClick: () -> Unit) {
    val colors = AppTheme.colors
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(colors.surface)
            .clickableNoRipple(onClick).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(Modifier.size(54.dp).clip(RoundedCornerShape(17.dp)).background(colors.accentSoft),
            contentAlignment = Alignment.Center) { Text(icon, fontSize = 29.sp) }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, color = colors.textPrimary, style = AppTheme.type.bodyL, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = colors.textSecondary, style = AppTheme.type.caption)
        }
        Text("›", color = colors.accent, fontSize = 30.sp)
    }
}

@Composable
private fun WorkoutSetEditor(set: WorkoutSet, onChange: (WorkoutSet) -> Unit, onDelete: () -> Unit) {
    val colors = AppTheme.colors
    var reps by remember(set.id) { mutableStateOf(set.reps.toString()) }
    var weight by remember(set.id) { mutableStateOf(set.weightKg.toString()) }
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp)).background(colors.bgScreen).padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
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

private fun formatDate(time: Long): String =
    SimpleDateFormat("d MMM, HH:mm", Locale("ru")).format(Date(time))

private fun duration(session: WorkoutSession): String {
    val end = session.endedAtMillis ?: System.currentTimeMillis()
    val minutes = ((end - session.startedAtMillis).coerceAtLeast(0L) / 60_000L).toInt()
    return if (minutes >= 60) "${minutes / 60} ч ${minutes % 60} мин" else "$minutes мин"
}

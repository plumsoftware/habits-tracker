package ru.plumsoftware.habitstracker.ui.screen.main.workouts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import org.koin.compose.koinInject
import ru.plumsoftware.habitstracker.data.database.WellnessRepository
import ru.plumsoftware.habitstracker.data.model.WorkoutSession
import ru.plumsoftware.habitstracker.data.model.WorkoutSet
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun WorkoutScreen(onBack: () -> Unit) {
    val repository: WellnessRepository = koinInject()
    val scope = rememberCoroutineScope()
    val sessions by repository.workouts().collectAsState(initial = emptyList())
    var selectedId by remember { mutableStateOf<String?>(null) }
    var showExerciseDialog by remember { mutableStateOf(false) }
    var exerciseName by remember { mutableStateOf("") }
    var showStartDialog by remember { mutableStateOf(false) }
    var workoutTitle by remember { mutableStateOf("Силовая тренировка") }
    var notes by remember { mutableStateOf("") }

    val active = sessions.firstOrNull { it.endedAtMillis == null }
    val selected = sessions.firstOrNull { it.id == (selectedId ?: active?.id) }
    val exercises by if (selected != null) repository.exercises(selected.id).collectAsState(initial = emptyList())
        else remember { mutableStateOf(emptyList()) }

    LaunchedEffect(selected?.id, selected?.notes) { notes = selected?.notes.orEmpty() }

    Scaffold { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = onBack) { Text("← Назад") }
                    Text("ТРЕНИРОВКИ", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                }
                Text("Тренировки", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("Записывай упражнения, подходы и прогресс", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (selected == null) {
                item {
                    Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Готов к тренировке?", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text("Добавляй упражнения, фиксируй вес и повторения — история останется в приложении.")
                            Button(onClick = { showStartDialog = true }, modifier = Modifier.fillMaxWidth()) { Text("Начать тренировку") }
                        }
                    }
                }
            } else {
                item {
                    Card(shape = RoundedCornerShape(24.dp)) {
                        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(selected.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text("Начата: ${formatDate(selected.startedAtMillis)}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            OutlinedTextField(
                                value = notes,
                                onValueChange = { notes = it; scope.launch { repository.updateNotes(selected.id, it) } },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Заметки к тренировке") },
                                minLines = 2
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { showExerciseDialog = true }, modifier = Modifier.weight(1f)) { Text("＋ Упражнение") }
                                OutlinedButton(onClick = {
                                    scope.launch { repository.finishWorkout(selected.id); selectedId = null }
                                }, modifier = Modifier.weight(1f)) { Text("Завершить") }
                            }
                        }
                    }
                }
                item {
                    Text("Упражнения · ${exercises.size}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
                if (exercises.isEmpty()) {
                    item { Text("Пока нет упражнений. Добавь первое — например, приседания или жим.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
                items(exercises, key = { it.id }) { exercise ->
                    val sets by repository.sets(exercise.id).collectAsState(initial = emptyList())
                    Card(shape = RoundedCornerShape(20.dp)) {
                        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(exercise.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                TextButton(onClick = { scope.launch { repository.deleteExercise(exercise.id) } }) { Text("Удалить") }
                            }
                            if (sets.isEmpty()) Text("Добавь первый подход", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            sets.forEach { set ->
                                WorkoutSetRow(
                                    set = set,
                                    onChange = { updated -> scope.launch { repository.updateSet(updated) } },
                                    onDelete = { scope.launch { repository.deleteSet(set.id) } }
                                )
                            }
                            OutlinedButton(onClick = { scope.launch { repository.addSet(exercise.id) } }, modifier = Modifier.fillMaxWidth()) {
                                Text("＋ Добавить подход")
                            }
                        }
                    }
                }
            }
            item {
                Text("История", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            items(sessions.filter { it.endedAtMillis != null }, key = { "history_${it.id}" }) { session ->
                Card(shape = RoundedCornerShape(18.dp), onClick = { selectedId = session.id }) {
                    Column(Modifier.fillMaxWidth().padding(16.dp)) {
                        Text(session.title, fontWeight = FontWeight.SemiBold)
                        Text("${formatDate(session.startedAtMillis)} · ${durationLabel(session)}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }

    if (showStartDialog) {
        AlertDialog(
            onDismissRequest = { showStartDialog = false },
            title = { Text("Новая тренировка") },
            text = { OutlinedTextField(value = workoutTitle, onValueChange = { workoutTitle = it }, label = { Text("Название") }) },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { selectedId = repository.startWorkout(workoutTitle); showStartDialog = false }
                }) { Text("Начать") }
            },
            dismissButton = { TextButton(onClick = { showStartDialog = false }) { Text("Отмена") } }
        )
    }
    if (showExerciseDialog && selected != null) {
        AlertDialog(
            onDismissRequest = { showExerciseDialog = false; exerciseName = "" },
            title = { Text("Новое упражнение") },
            text = { OutlinedTextField(value = exerciseName, onValueChange = { exerciseName = it }, label = { Text("Например, жим лёжа") }) },
            confirmButton = {
                TextButton(enabled = exerciseName.isNotBlank(), onClick = {
                    scope.launch { repository.addExercise(selected.id, exerciseName); exerciseName = ""; showExerciseDialog = false }
                }) { Text("Добавить") }
            },
            dismissButton = { TextButton(onClick = { showExerciseDialog = false }) { Text("Отмена") } }
        )
    }
}

@Composable
private fun WorkoutSetRow(set: WorkoutSet, onChange: (WorkoutSet) -> Unit, onDelete: () -> Unit) {
    var reps by remember(set.id, set.reps) { mutableStateOf(set.reps.toString()) }
    var weight by remember(set.id, set.weightKg) { mutableStateOf(set.weightKg.toString()) }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("№${set.setNumber}", modifier = Modifier.padding(top = 14.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(value = reps, onValueChange = { reps = it.filter(Char::isDigit); reps.toIntOrNull()?.let { n -> onChange(set.copy(reps = n)) } }, modifier = Modifier.weight(1f), label = { Text("Повторы") }, singleLine = true)
        OutlinedTextField(value = weight, onValueChange = { weight = it.filter { ch -> ch.isDigit() || ch == '.' || ch == ',' }.replace(',', '.'); weight.toDoubleOrNull()?.let { n -> onChange(set.copy(weightKg = n)) } }, modifier = Modifier.weight(1f), label = { Text("Кг") }, singleLine = true)
        TextButton(onClick = { onChange(set.copy(completed = !set.completed)) }) { Text(if (set.completed) "✓" else "○") }
        TextButton(onClick = onDelete) { Text("×") }
    }
}

private fun formatDate(time: Long): String =
    SimpleDateFormat("d MMM, HH:mm", Locale("ru")).format(Date(time))

private fun durationLabel(session: WorkoutSession): String {
    val end = session.endedAtMillis ?: System.currentTimeMillis()
    val minutes = ((end - session.startedAtMillis).coerceAtLeast(0L) / 60_000L).toInt()
    return if (minutes >= 60) "${minutes / 60} ч ${minutes % 60} мин" else "$minutes мин"
}

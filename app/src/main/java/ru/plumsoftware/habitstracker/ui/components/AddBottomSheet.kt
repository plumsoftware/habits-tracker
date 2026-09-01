package ru.plumsoftware.habitstracker.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import ru.plumsoftware.habitstracker.data.model.Habit
import ru.plumsoftware.habitstracker.data.model.Periodicity
import ru.plumsoftware.habitstracker.getStartOfDay
import ru.plumsoftware.habitstracker.ui.theme.Dimen
import java.util.UUID

@Composable
fun AddBottomSheet(
    onSaveHabit: (Habit) -> Unit,
    onUpdateHabit: (Habit) -> Unit = {}, // Новый параметр для обновления
    onDeleteHabit: (() -> Unit)? = null, // Новый параметр для удаления
    editingHabit: Habit? = null // Привычка для редактирования (null = создание)
) {
    val isEditMode = editingHabit != null

    // Инициализируем состояния из editingHabit или пустыми значениями
    var title by remember { mutableStateOf(editingHabit?.title ?: "") }
    val titleFocusRequester = remember { FocusRequester() }
    var isTitleFocused by remember { mutableStateOf(false) }

    var desc by remember { mutableStateOf(editingHabit?.body ?: "") }
    val descFocusRequester = remember { FocusRequester() }
    var isDescFocused by remember { mutableStateOf(false) }

    var showDialog by remember { mutableStateOf(false) }
    var selectedFrequency by remember {
        mutableStateOf(
            when (editingHabit?.periodicity) {
                Periodicity.WEEKLY -> "Каждую неделю"
                else -> "Каждый день"
            }
        )
    }

    var selectedTime by remember {
        mutableStateOf(
            (editingHabit?.reminderTimeHour to editingHabit?.reminderTimeMinute)
        )
    }
    var selectedInterval by remember {
        mutableIntStateOf(editingHabit?.reminderInterval ?: 5)
    }

//    var selectedTime by remember { mutableStateOf(0 to 0) } // (часы, минуты)
//    var selectedInterval by remember { mutableIntStateOf(5) } // минуты

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = {
                Text(
                    text = "Выберите периодичность",
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                Column {
                    listOf("Каждый день", "Каждую неделю").forEach { frequency ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedFrequency = frequency
                                    showDialog = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedFrequency == frequency,
                                onClick = {
                                    selectedFrequency = frequency
                                    showDialog = false
                                }
                            )
                            Text(
                                text = frequency,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showDialog = false }
                ) {
                    Text("Отмена")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(all = Dimen.large),
        verticalArrangement = Arrangement.spacedBy(space = Dimen.large, alignment = Alignment.Top),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Заголовок в зависимости от режима
        Text(
            text = if (isEditMode) "Редактировать привычку" else "Новая привычка",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.fillMaxWidth()
        )

        // Поля формы остаются без изменений
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(
                space = Dimen.smallPlus,
                alignment = Alignment.Top
            ),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "Название",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            ) {
                if (title.isEmpty()) {
                    Text(
                        text = "Придумайте название привычки",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = Dimen.extraLarge,
                                vertical = Dimen.extraLarge
                            )
                    )
                }

                BasicTextField(
                    value = title,
                    onValueChange = { title = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = Dimen.extraLarge,
                            end = if (title.isNotEmpty()) 48.dp else Dimen.extraLarge,
                            top = Dimen.extraLarge,
                            bottom = Dimen.extraLarge
                        )
                        .focusRequester(titleFocusRequester)
                        .onFocusChanged { focusState ->
                            isTitleFocused = focusState.isFocused
                        },
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    decorationBox = { innerTextField ->
                        innerTextField()
                    }
                )

                Column(
                    modifier = Modifier.align(Alignment.CenterEnd)
                ) {
                    AnimatedVisibility(
                        visible = title.isNotEmpty() && isTitleFocused,
                        enter = slideInHorizontally { fullWidth -> fullWidth } + fadeIn(),
                        exit = slideOutHorizontally { fullWidth -> fullWidth } + fadeOut()
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Clear,
                            contentDescription = "Clear",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable(role = Role.Button) { title = "" }
                                .padding(Dimen.mediumPlus)
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(
                space = Dimen.smallPlus,
                alignment = Alignment.Top
            ),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "Описание",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            ) {
                if (desc.isEmpty()) {
                    Text(
                        text = "Напишите описание привычки",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = Dimen.extraLarge,
                                vertical = Dimen.extraLarge
                            )
                    )
                }

                BasicTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = Dimen.extraLarge,
                            end = if (desc.isNotEmpty()) 48.dp else Dimen.extraLarge,
                            top = Dimen.extraLarge,
                            bottom = Dimen.extraLarge
                        )
                        .focusRequester(descFocusRequester)
                        .onFocusChanged { focusState ->
                            isDescFocused = focusState.isFocused
                        },
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    decorationBox = { innerTextField ->
                        innerTextField()
                    }
                )

                Column(
                    modifier = Modifier.align(Alignment.CenterEnd)
                ) {
                    AnimatedVisibility(
                        visible = desc.isNotEmpty() && isDescFocused,
                        enter = slideInHorizontally { fullWidth -> fullWidth } + fadeIn(),
                        exit = slideOutHorizontally { fullWidth -> fullWidth } + fadeOut()
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Clear,
                            contentDescription = "Clear",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable(role = Role.Button) { desc = "" }
                                .padding(Dimen.mediumPlus)
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(
                space = Dimen.smallPlus,
                alignment = Alignment.Top
            ),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "Повторение",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                    .clickable {
                        showDialog = true
                    }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(all = Dimen.large),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = selectedFrequency,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Выбрать периодичность",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Время и «Напомнить за» — две колонки с подписями над полями (как на макете)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(Dimen.mediumPlus)
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Dimen.smallPlus),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "Время",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                TimeCard(
                    initialHour = editingHabit?.reminderTimeHour,
                    initialMinute = editingHabit?.reminderTimeMinute,
                    isClickable = true,
                    onSelectedTime = { hour, minute ->
                        selectedTime = hour to minute
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    contentPadding = PaddingValues(
                        horizontal = Dimen.large,
                        vertical = Dimen.mediumPlus
                    )
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Dimen.smallPlus),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "Напомнить за",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IntervalCard(
                    initialInterval = editingHabit?.reminderInterval,
                    onSelectedTime = { interval ->
                        selectedInterval = interval
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Кнопка сохранения/обновления
        Button(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 14.dp,
                    shape = MaterialTheme.shapes.large,
                    clip = false,
                    ambientColor = MaterialTheme.colorScheme.primary,
                    spotColor = MaterialTheme.colorScheme.primary
                ),
            shape = MaterialTheme.shapes.large,
            contentPadding = PaddingValues(horizontal = Dimen.medium, vertical = Dimen.extraLarge),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
            onClick = {
                val habit = if (isEditMode) {
                    // Режим редактирования - обновляем существующую привычку
                    editingHabit.copy(
                        title = title,
                        body = desc,
                        periodicity = when (selectedFrequency) {
                            "Каждый день" -> Periodicity.DAILY
                            "Каждую неделю" -> Periodicity.WEEKLY
                            else -> Periodicity.DAILY
                        },
                        reminderTimeHour = selectedTime.first ?: 0,
                        reminderTimeMinute = selectedTime.second ?: 0,
                        reminderInterval = selectedInterval
                    )
                } else {
                    // Режим создания - создаем новую привычку
                    Habit(
                        id = UUID.randomUUID().toString(),
                        title = title,
                        body = desc,
                        periodicity = when (selectedFrequency) {
                            "Каждый день" -> Periodicity.DAILY
                            "Каждую неделю" -> Periodicity.WEEKLY
                            else -> Periodicity.DAILY
                        },
                        creationDate = getStartOfDay(System.currentTimeMillis()),
                        targetCount = 1,
                        reminderTimeHour = selectedTime.first ?: 0,
                        reminderTimeMinute = selectedTime.second ?: 0,
                        reminderInterval = selectedInterval
                    )
                }

                if (isEditMode) {
                    onUpdateHabit(habit)
                } else {
                    onSaveHabit(habit)
                }
            }
        ) {
            Text(
                text = if (isEditMode) "Сохранить изменения" else "Сохранить",
                style = MaterialTheme.typography.bodyMedium
            )
        }

        // Кнопка удаления (только в режиме редактирования)
        if (isEditMode && onDeleteHabit != null) {
            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                contentPadding = PaddingValues(
                    horizontal = Dimen.medium,
                    vertical = Dimen.extraLarge
                ),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.error
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                onClick = onDeleteHabit
            ) {
                Text(
                    text = "Удалить привычку",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
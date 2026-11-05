package ru.plumsoftware.habitstracker.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Единая шапка внутренних экранов — использует общий компонент приложения. */
@Composable
fun AppScreenTopBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AppTopBar(title = title, onBack = onBack, modifier = modifier)
}

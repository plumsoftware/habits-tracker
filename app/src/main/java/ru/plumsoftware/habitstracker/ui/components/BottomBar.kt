package ru.plumsoftware.habitstracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import ru.plumsoftware.habitstracker.R
import ru.plumsoftware.habitstracker.ui.screen.main.home.HomeEvent
import ru.plumsoftware.habitstracker.ui.screen.main.home.HomeViewModel
import ru.plumsoftware.habitstracker.ui.theme.Dimen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomBar(
    viewModel: HomeViewModel,
    onAdd: () -> Unit
) {
    var selected by rememberSaveable { mutableIntStateOf(0) }
    val primary = MaterialTheme.colorScheme.primary

    // Отдельная подложка бара (отличается от фона приложения) + линия 1dp сверху
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            HorizontalDivider(
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = Dimen.medium, vertical = Dimen.smallPlus),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                BottomBarIcon(
                    selected = selected == 0,
                    painter = if (selected == 0) painterResource(R.drawable.house_fill)
                    else painterResource(R.drawable.house_outline),
                    onClick = {
                        selected = 0
                        viewModel.onEvent(HomeEvent.OnHome)
                    }
                )

                BottomBarIcon(
                    selected = selected == 1,
                    imageVector = if (selected == 1) Icons.Rounded.Settings else Icons.Outlined.Settings,
                    onClick = {
                        selected = 1
                        viewModel.onEvent(HomeEvent.OnSettings)
                    }
                )

                AddButton(primary = primary, onAdd = onAdd)

                BottomBarIcon(
                    selected = selected == 2,
                    painter = if (selected == 2) painterResource(R.drawable.statistic_filled)
                    else painterResource(R.drawable.statistic_outline),
                    onClick = {
                        selected = 2
                        viewModel.onEvent(HomeEvent.OnStatistic)
                    }
                )

                BottomBarIcon(
                    selected = selected == 3,
                    imageVector = if (selected == 3) Icons.Rounded.Notifications else Icons.Outlined.Notifications,
                    onClick = {
                        selected = 3
                        viewModel.onEvent(HomeEvent.OnNotifications)
                    }
                )
            }
        }
    }
}

@Composable
private fun AddButton(primary: Color, onAdd: () -> Unit) {
    Box(
        modifier = Modifier.size(64.dp),
        contentAlignment = Alignment.Center
    ) {
        // Синее свечение-тень под кнопкой (видно на всех версиях Android)
        Box(
            modifier = Modifier
                .size(56.dp)
                .offset(y = 3.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(primary.copy(alpha = 0.5f), Color.Transparent)
                    ),
                    shape = MaterialTheme.shapes.medium
                )
        )

        Card(
            modifier = Modifier
                .size(44.dp)
                .shadow(
                    elevation = 8.dp,
                    shape = MaterialTheme.shapes.medium,
                    clip = false,
                    ambientColor = primary,
                    spotColor = primary
                ),
            colors = CardDefaults.cardColors(containerColor = primary),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 0.dp,
                pressedElevation = 0.dp,
                focusedElevation = 0.dp
            ),
            shape = MaterialTheme.shapes.medium,
            onClick = onAdd
        ) {
            Icon(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(all = Dimen.medium),
                painter = painterResource(R.drawable.add),
                tint = MaterialTheme.colorScheme.onPrimary,
                contentDescription = null
            )
        }
    }
}

@Composable
private fun BottomBarIcon(
    selected: Boolean,
    onClick: () -> Unit,
    painter: androidx.compose.ui.graphics.painter.Painter? = null,
    imageVector: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    val tint = if (selected) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.onSurfaceVariant

    // clickable снаружи, затем отступ 4dp вокруг иконки, чтобы она не обрезалась
    val modifier = Modifier
        .clip(MaterialTheme.shapes.small)
        .clickable(role = Role.Button, onClick = onClick)
        .padding(Dimen.small)
        .size(Dimen.bottomBarIconSize)

    when {
        painter != null -> Icon(
            modifier = modifier,
            painter = painter,
            tint = tint,
            contentDescription = null
        )

        imageVector != null -> Icon(
            modifier = modifier,
            imageVector = imageVector,
            tint = tint,
            contentDescription = null
        )
    }
}

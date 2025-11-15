package ru.plumsoftware.habitstracker.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import ru.plumsoftware.habitstracker.R
import ru.plumsoftware.habitstracker.ui.theme.Dimen

@Composable
fun BottomBar() {
    var selected by rememberSaveable { mutableIntStateOf(0) }

    BottomAppBar(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = Dimen.medium),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                modifier = Modifier
                    .size(Dimen.bottomBarIconSize)
                    .clip(MaterialTheme.shapes.small)
                    .clickable(
                        enabled = true,
                        role = Role.Button,
                        onClick = {
                            selected = 0
                        }
                    ),
                painter = if (selected == 0) painterResource(R.drawable.house_fill) else painterResource(
                    R.drawable.house_outline
                ),
                contentDescription = null
            )
            Icon(
                modifier = Modifier
                    .size(Dimen.bottomBarIconSize)
                    .clip(MaterialTheme.shapes.small)
                    .clickable(
                        enabled = true,
                        role = Role.Button,
                        onClick = {
                            selected = 1
                        }
                    ),
                imageVector = if (selected == 1) Icons.Rounded.Settings else Icons.Outlined.Settings,
                contentDescription = null
            )

            Card(
                modifier = Modifier.size(44.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                elevation = CardDefaults.elevatedCardElevation(
                    defaultElevation = 0.dp,
                    pressedElevation = 0.dp,
                    focusedElevation = 0.dp
                ),
                shape = MaterialTheme.shapes.medium
            ) {
                Icon(
                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(all = Dimen.medium),
                    painter = painterResource(R.drawable.add),
                    contentDescription = null
                )
            }

            Icon(
                modifier = Modifier
                    .size(Dimen.bottomBarIconSize)
                    .clip(MaterialTheme.shapes.small)
                    .clickable(
                        enabled = true,
                        role = Role.Button,
                        onClick = {
                            selected = 2
                        }
                    ),
                painter = if (selected == 2) painterResource(R.drawable.statistic_filled) else painterResource(
                    R.drawable.statistic_outline
                ),
                contentDescription = null
            )
            Icon(
                modifier = Modifier
                    .size(Dimen.bottomBarIconSize)
                    .clip(MaterialTheme.shapes.small)
                    .clickable(
                        enabled = true,
                        role = Role.Button,
                        onClick = {
                            selected = 3
                        }
                    ),
                imageVector = if (selected == 3) Icons.Rounded.Notifications else Icons.Outlined.Notifications,
                contentDescription = null
            )
        }
    }
}
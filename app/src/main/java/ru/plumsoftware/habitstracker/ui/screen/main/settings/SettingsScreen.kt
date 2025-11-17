package ru.plumsoftware.habitstracker.ui.screen.main.settings

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import ru.plumsoftware.habitstracker.ui.components.IOSSwitch
import ru.plumsoftware.habitstracker.ui.theme.Dimen

@Composable
fun SettingsScreen(
    padding: PaddingValues,
    settingsViewModel: SettingsViewModel,
    isDark: Boolean
) {
    Column(
        modifier = Modifier
            .padding(padding)
            .padding(all = Dimen.medium)
    ) {
        // Строка с переключателем темы
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Тёмная тема",
                style = MaterialTheme.typography.bodyLarge,
            )

            // iOS-style Switch
            IOSSwitch(
                checked = isDark,
                onCheckedChange = { checked ->
                    settingsViewModel.toggleTheme()

                    if (checked) {
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                    } else {
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                    }
                }
            )
        }
    }
}
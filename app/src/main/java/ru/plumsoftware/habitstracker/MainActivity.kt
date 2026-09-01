package ru.plumsoftware.habitstracker

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import org.koin.androidx.compose.koinViewModel
import ru.plumsoftware.habitstracker.data.settings.ThemeMode
import ru.plumsoftware.habitstracker.ui.screen.Route
import ru.plumsoftware.habitstracker.ui.screen.main.HomeScreen
import ru.plumsoftware.habitstracker.ui.screen.main.settings.SettingsViewModel
import ru.plumsoftware.habitstracker.ui.theme.AppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // Запрос разрешений на хранилище (и уведомления) при запуске
            val permissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestMultiplePermissions()
            ) { /* результат не критичен */ }
            LaunchedEffect(Unit) {
                val permissions = buildList {
                    add(Manifest.permission.READ_EXTERNAL_STORAGE)
                    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                        add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        add(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
                permissionLauncher.launch(permissions.toTypedArray())
            }

            val settingsViewModel: SettingsViewModel = koinViewModel()
            val settings by settingsViewModel.settings.collectAsState()
            val isDarkTheme = when (settings.themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            AppTheme(
                darkTheme = isDarkTheme
            ) {
                val navHost = rememberNavController()
                NavHost(
                    navController = navHost,
                    startDestination = Route.Main
                ) {
                    composable<Route.Main> {
                        HomeScreen(
                            settingsViewModel = settingsViewModel,
                            isDarkTheme = isDarkTheme
                        )
                    }
                }
            }
        }
    }
}


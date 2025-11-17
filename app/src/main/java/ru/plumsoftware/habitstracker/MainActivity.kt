package ru.plumsoftware.habitstracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import org.koin.androidx.compose.koinViewModel
import ru.plumsoftware.habitstracker.ui.screen.Route
import ru.plumsoftware.habitstracker.ui.screen.main.HomeScreen
import ru.plumsoftware.habitstracker.ui.screen.main.settings.SettingsViewModel
import ru.plumsoftware.habitstracker.ui.theme.AppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settingsViewModel: SettingsViewModel = koinViewModel()
            val isDarkTheme by settingsViewModel.isDarkTheme.collectAsState()

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


package ru.plumsoftware.habitstracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import ru.plumsoftware.habitstracker.ui.screen.Route
import ru.plumsoftware.habitstracker.ui.screen.main.HomeScreen
import ru.plumsoftware.habitstracker.ui.theme.AppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppTheme {
                val navHost = rememberNavController()
                NavHost(
                    navController = navHost,
                    startDestination = Route.Main
                ) {
                    composable<Route.Main> {
                        HomeScreen()
                    }
                }
            }
        }
    }
}


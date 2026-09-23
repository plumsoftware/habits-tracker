package ru.plumsoftware.habitstracker

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import org.koin.androidx.compose.koinViewModel
import ru.plumsoftware.habitstracker.data.settings.ThemeMode
import ru.plumsoftware.habitstracker.ui.screen.main.HomeScreen
import ru.plumsoftware.habitstracker.ui.screen.onboarding.OnboardingScreen
import ru.plumsoftware.habitstracker.ui.screen.main.settings.SettingsViewModel
import ru.plumsoftware.habitstracker.ui.theme.AppTheme

class MainActivity : ComponentActivity() {
    // Deeplink из виджета (habits://...), наблюдается Compose
    private val deeplinkState = androidx.compose.runtime.mutableStateOf<String?>(null)

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        deeplinkState.value = intent.data?.toString()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        deeplinkState.value = intent?.data?.toString()

        // Телефон — только портрет; планшет/большой экран — автоповорот разрешён
        requestedOrientation = if (resources.getBoolean(R.bool.is_tablet)) {
            android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        } else {
            android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }

        enableEdgeToEdge()
        // Системные бары прозрачные — под них попадает фон приложения (edge-to-edge)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        setContent {
            // Разрешения НЕ запрашиваем на старте — только на экране разрешений в онбординге
            // (ТЗ §14.1), иначе системный диалог всплывает до того, как пользователь туда дошёл.

            val settingsViewModel: SettingsViewModel = koinViewModel()
            val settings by settingsViewModel.settings.collectAsState()
            val onboardingSeen by settingsViewModel.onboardingSeen.collectAsState()
            val isDarkTheme = when (settings.themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            AppTheme(
                darkTheme = isDarkTheme
            ) {
                // ТЗ §14: первый запуск — онбординг, далее — «Сегодня».
                // null = флаг ещё не прочитан из DataStore → короткий сплэш вместо мигания.
                when (onboardingSeen) {
                    null -> SplashBackground()
                    false -> OnboardingScreen(onFinish = { settingsViewModel.completeOnboarding() })
                    true -> HomeScreen(
                        settingsViewModel = settingsViewModel,
                        isDarkTheme = isDarkTheme,
                        deeplink = deeplinkState.value,
                        onDeeplinkHandled = { deeplinkState.value = null },
                    )
                }
            }
        }
    }
}

/** Короткий фон-сплэш, пока читается флаг онбординга из DataStore. */
@Composable
private fun SplashBackground() {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppTheme.colors.bgScreen)
    )
}


package ru.plumsoftware.habitstracker.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class ExtendedColorScheme(
    val graphBarColor: ColorFamily,
    val dangerColor: ColorFamily,
    val goodColor: ColorFamily,
)

private val lightScheme = lightColorScheme(
    primary = primaryLight,
    onPrimary = onPrimaryLight,
    primaryContainer = primaryContainerLight,
    onPrimaryContainer = onPrimaryContainerLight,
    secondary = secondaryLight,
    onSecondary = onSecondaryLight,
    secondaryContainer = secondaryContainerLight,
    onSecondaryContainer = onSecondaryContainerLight,
    tertiary = tertiaryLight,
    onTertiary = onTertiaryLight,
    tertiaryContainer = tertiaryContainerLight,
    onTertiaryContainer = onTertiaryContainerLight,
    error = errorLight,
    onError = onErrorLight,
    errorContainer = errorContainerLight,
    onErrorContainer = onErrorContainerLight,
    background = backgroundLight,
    onBackground = onBackgroundLight,
    surface = surfaceLight,
    onSurface = onSurfaceLight,
    surfaceVariant = surfaceVariantLight,
    onSurfaceVariant = onSurfaceVariantLight,
    outline = outlineLight,
    outlineVariant = outlineVariantLight,
    scrim = scrimLight,
    inverseSurface = inverseSurfaceLight,
    inverseOnSurface = inverseOnSurfaceLight,
    inversePrimary = inversePrimaryLight,
    surfaceDim = surfaceDimLight,
    surfaceBright = surfaceBrightLight,
    surfaceContainerLowest = surfaceContainerLowestLight,
    surfaceContainerLow = surfaceContainerLowLight,
    surfaceContainer = surfaceContainerLight,
    surfaceContainerHigh = surfaceContainerHighLight,
    surfaceContainerHighest = surfaceContainerHighestLight,
)

private val darkScheme = darkColorScheme(
    primary = primaryDark,
    onPrimary = onPrimaryDark,
    primaryContainer = primaryContainerDark,
    onPrimaryContainer = onPrimaryContainerDark,
    secondary = secondaryDark,
    onSecondary = onSecondaryDark,
    secondaryContainer = secondaryContainerDark,
    onSecondaryContainer = onSecondaryContainerDark,
    tertiary = tertiaryDark,
    onTertiary = onTertiaryDark,
    tertiaryContainer = tertiaryContainerDark,
    onTertiaryContainer = onTertiaryContainerDark,
    error = errorDark,
    onError = onErrorDark,
    errorContainer = errorContainerDark,
    onErrorContainer = onErrorContainerDark,
    background = backgroundDark,
    onBackground = onBackgroundDark,
    surface = surfaceDark,
    onSurface = onSurfaceDark,
    surfaceVariant = surfaceVariantDark,
    onSurfaceVariant = onSurfaceVariantDark,
    outline = outlineDark,
    outlineVariant = outlineVariantDark,
    scrim = scrimDark,
    inverseSurface = inverseSurfaceDark,
    inverseOnSurface = inverseOnSurfaceDark,
    inversePrimary = inversePrimaryDark,
    surfaceDim = surfaceDimDark,
    surfaceBright = surfaceBrightDark,
    surfaceContainerLowest = surfaceContainerLowestDark,
    surfaceContainerLow = surfaceContainerLowDark,
    surfaceContainer = surfaceContainerDark,
    surfaceContainerHigh = surfaceContainerHighDark,
    surfaceContainerHighest = surfaceContainerHighestDark,
)

private val mediumContrastLightColorScheme = lightColorScheme(
    primary = primaryLightMediumContrast,
    onPrimary = onPrimaryLightMediumContrast,
    primaryContainer = primaryContainerLightMediumContrast,
    onPrimaryContainer = onPrimaryContainerLightMediumContrast,
    secondary = secondaryLightMediumContrast,
    onSecondary = onSecondaryLightMediumContrast,
    secondaryContainer = secondaryContainerLightMediumContrast,
    onSecondaryContainer = onSecondaryContainerLightMediumContrast,
    tertiary = tertiaryLightMediumContrast,
    onTertiary = onTertiaryLightMediumContrast,
    tertiaryContainer = tertiaryContainerLightMediumContrast,
    onTertiaryContainer = onTertiaryContainerLightMediumContrast,
    error = errorLightMediumContrast,
    onError = onErrorLightMediumContrast,
    errorContainer = errorContainerLightMediumContrast,
    onErrorContainer = onErrorContainerLightMediumContrast,
    background = backgroundLightMediumContrast,
    onBackground = onBackgroundLightMediumContrast,
    surface = surfaceLightMediumContrast,
    onSurface = onSurfaceLightMediumContrast,
    surfaceVariant = surfaceVariantLightMediumContrast,
    onSurfaceVariant = onSurfaceVariantLightMediumContrast,
    outline = outlineLightMediumContrast,
    outlineVariant = outlineVariantLightMediumContrast,
    scrim = scrimLightMediumContrast,
    inverseSurface = inverseSurfaceLightMediumContrast,
    inverseOnSurface = inverseOnSurfaceLightMediumContrast,
    inversePrimary = inversePrimaryLightMediumContrast,
    surfaceDim = surfaceDimLightMediumContrast,
    surfaceBright = surfaceBrightLightMediumContrast,
    surfaceContainerLowest = surfaceContainerLowestLightMediumContrast,
    surfaceContainerLow = surfaceContainerLowLightMediumContrast,
    surfaceContainer = surfaceContainerLightMediumContrast,
    surfaceContainerHigh = surfaceContainerHighLightMediumContrast,
    surfaceContainerHighest = surfaceContainerHighestLightMediumContrast,
)

private val highContrastLightColorScheme = lightColorScheme(
    primary = primaryLightHighContrast,
    onPrimary = onPrimaryLightHighContrast,
    primaryContainer = primaryContainerLightHighContrast,
    onPrimaryContainer = onPrimaryContainerLightHighContrast,
    secondary = secondaryLightHighContrast,
    onSecondary = onSecondaryLightHighContrast,
    secondaryContainer = secondaryContainerLightHighContrast,
    onSecondaryContainer = onSecondaryContainerLightHighContrast,
    tertiary = tertiaryLightHighContrast,
    onTertiary = onTertiaryLightHighContrast,
    tertiaryContainer = tertiaryContainerLightHighContrast,
    onTertiaryContainer = onTertiaryContainerLightHighContrast,
    error = errorLightHighContrast,
    onError = onErrorLightHighContrast,
    errorContainer = errorContainerLightHighContrast,
    onErrorContainer = onErrorContainerLightHighContrast,
    background = backgroundLightHighContrast,
    onBackground = onBackgroundLightHighContrast,
    surface = surfaceLightHighContrast,
    onSurface = onSurfaceLightHighContrast,
    surfaceVariant = surfaceVariantLightHighContrast,
    onSurfaceVariant = onSurfaceVariantLightHighContrast,
    outline = outlineLightHighContrast,
    outlineVariant = outlineVariantLightHighContrast,
    scrim = scrimLightHighContrast,
    inverseSurface = inverseSurfaceLightHighContrast,
    inverseOnSurface = inverseOnSurfaceLightHighContrast,
    inversePrimary = inversePrimaryLightHighContrast,
    surfaceDim = surfaceDimLightHighContrast,
    surfaceBright = surfaceBrightLightHighContrast,
    surfaceContainerLowest = surfaceContainerLowestLightHighContrast,
    surfaceContainerLow = surfaceContainerLowLightHighContrast,
    surfaceContainer = surfaceContainerLightHighContrast,
    surfaceContainerHigh = surfaceContainerHighLightHighContrast,
    surfaceContainerHighest = surfaceContainerHighestLightHighContrast,
)

private val mediumContrastDarkColorScheme = darkColorScheme(
    primary = primaryDarkMediumContrast,
    onPrimary = onPrimaryDarkMediumContrast,
    primaryContainer = primaryContainerDarkMediumContrast,
    onPrimaryContainer = onPrimaryContainerDarkMediumContrast,
    secondary = secondaryDarkMediumContrast,
    onSecondary = onSecondaryDarkMediumContrast,
    secondaryContainer = secondaryContainerDarkMediumContrast,
    onSecondaryContainer = onSecondaryContainerDarkMediumContrast,
    tertiary = tertiaryDarkMediumContrast,
    onTertiary = onTertiaryDarkMediumContrast,
    tertiaryContainer = tertiaryContainerDarkMediumContrast,
    onTertiaryContainer = onTertiaryContainerDarkMediumContrast,
    error = errorDarkMediumContrast,
    onError = onErrorDarkMediumContrast,
    errorContainer = errorContainerDarkMediumContrast,
    onErrorContainer = onErrorContainerDarkMediumContrast,
    background = backgroundDarkMediumContrast,
    onBackground = onBackgroundDarkMediumContrast,
    surface = surfaceDarkMediumContrast,
    onSurface = onSurfaceDarkMediumContrast,
    surfaceVariant = surfaceVariantDarkMediumContrast,
    onSurfaceVariant = onSurfaceVariantDarkMediumContrast,
    outline = outlineDarkMediumContrast,
    outlineVariant = outlineVariantDarkMediumContrast,
    scrim = scrimDarkMediumContrast,
    inverseSurface = inverseSurfaceDarkMediumContrast,
    inverseOnSurface = inverseOnSurfaceDarkMediumContrast,
    inversePrimary = inversePrimaryDarkMediumContrast,
    surfaceDim = surfaceDimDarkMediumContrast,
    surfaceBright = surfaceBrightDarkMediumContrast,
    surfaceContainerLowest = surfaceContainerLowestDarkMediumContrast,
    surfaceContainerLow = surfaceContainerLowDarkMediumContrast,
    surfaceContainer = surfaceContainerDarkMediumContrast,
    surfaceContainerHigh = surfaceContainerHighDarkMediumContrast,
    surfaceContainerHighest = surfaceContainerHighestDarkMediumContrast,
)

private val highContrastDarkColorScheme = darkColorScheme(
    primary = primaryDarkHighContrast,
    onPrimary = onPrimaryDarkHighContrast,
    primaryContainer = primaryContainerDarkHighContrast,
    onPrimaryContainer = onPrimaryContainerDarkHighContrast,
    secondary = secondaryDarkHighContrast,
    onSecondary = onSecondaryDarkHighContrast,
    secondaryContainer = secondaryContainerDarkHighContrast,
    onSecondaryContainer = onSecondaryContainerDarkHighContrast,
    tertiary = tertiaryDarkHighContrast,
    onTertiary = onTertiaryDarkHighContrast,
    tertiaryContainer = tertiaryContainerDarkHighContrast,
    onTertiaryContainer = onTertiaryContainerDarkHighContrast,
    error = errorDarkHighContrast,
    onError = onErrorDarkHighContrast,
    errorContainer = errorContainerDarkHighContrast,
    onErrorContainer = onErrorContainerDarkHighContrast,
    background = backgroundDarkHighContrast,
    onBackground = onBackgroundDarkHighContrast,
    surface = surfaceDarkHighContrast,
    onSurface = onSurfaceDarkHighContrast,
    surfaceVariant = surfaceVariantDarkHighContrast,
    onSurfaceVariant = onSurfaceVariantDarkHighContrast,
    outline = outlineDarkHighContrast,
    outlineVariant = outlineVariantDarkHighContrast,
    scrim = scrimDarkHighContrast,
    inverseSurface = inverseSurfaceDarkHighContrast,
    inverseOnSurface = inverseOnSurfaceDarkHighContrast,
    inversePrimary = inversePrimaryDarkHighContrast,
    surfaceDim = surfaceDimDarkHighContrast,
    surfaceBright = surfaceBrightDarkHighContrast,
    surfaceContainerLowest = surfaceContainerLowestDarkHighContrast,
    surfaceContainerLow = surfaceContainerLowDarkHighContrast,
    surfaceContainer = surfaceContainerDarkHighContrast,
    surfaceContainerHigh = surfaceContainerHighDarkHighContrast,
    surfaceContainerHighest = surfaceContainerHighestDarkHighContrast,
)

val extendedLight = ExtendedColorScheme(
    graphBarColor = ColorFamily(
        graphBarColorLight,
        onGraphBarColorLight,
        graphBarColorContainerLight,
        onGraphBarColorContainerLight,
    ),
    dangerColor = ColorFamily(
        dangerColorLight,
        onDangerColorLight,
        dangerColorContainerLight,
        onDangerColorContainerLight,
    ),
    goodColor = ColorFamily(
        goodColorLight,
        onGoodColorLight,
        goodColorContainerLight,
        onGoodColorContainerLight,
    ),
)

val extendedDark = ExtendedColorScheme(
    graphBarColor = ColorFamily(
        graphBarColorDark,
        onGraphBarColorDark,
        graphBarColorContainerDark,
        onGraphBarColorContainerDark,
    ),
    dangerColor = ColorFamily(
        dangerColorDark,
        onDangerColorDark,
        dangerColorContainerDark,
        onDangerColorContainerDark,
    ),
    goodColor = ColorFamily(
        goodColorDark,
        onGoodColorDark,
        goodColorContainerDark,
        onGoodColorContainerDark,
    ),
)

val extendedLightMediumContrast = ExtendedColorScheme(
    graphBarColor = ColorFamily(
        graphBarColorLightMediumContrast,
        onGraphBarColorLightMediumContrast,
        graphBarColorContainerLightMediumContrast,
        onGraphBarColorContainerLightMediumContrast,
    ),
    dangerColor = ColorFamily(
        dangerColorLightMediumContrast,
        onDangerColorLightMediumContrast,
        dangerColorContainerLightMediumContrast,
        onDangerColorContainerLightMediumContrast,
    ),
    goodColor = ColorFamily(
        goodColorLightMediumContrast,
        onGoodColorLightMediumContrast,
        goodColorContainerLightMediumContrast,
        onGoodColorContainerLightMediumContrast,
    ),
)

val extendedLightHighContrast = ExtendedColorScheme(
    graphBarColor = ColorFamily(
        graphBarColorLightHighContrast,
        onGraphBarColorLightHighContrast,
        graphBarColorContainerLightHighContrast,
        onGraphBarColorContainerLightHighContrast,
    ),
    dangerColor = ColorFamily(
        dangerColorLightHighContrast,
        onDangerColorLightHighContrast,
        dangerColorContainerLightHighContrast,
        onDangerColorContainerLightHighContrast,
    ),
    goodColor = ColorFamily(
        goodColorLightHighContrast,
        onGoodColorLightHighContrast,
        goodColorContainerLightHighContrast,
        onGoodColorContainerLightHighContrast,
    ),
)

val extendedDarkMediumContrast = ExtendedColorScheme(
    graphBarColor = ColorFamily(
        graphBarColorDarkMediumContrast,
        onGraphBarColorDarkMediumContrast,
        graphBarColorContainerDarkMediumContrast,
        onGraphBarColorContainerDarkMediumContrast,
    ),
    dangerColor = ColorFamily(
        dangerColorDarkMediumContrast,
        onDangerColorDarkMediumContrast,
        dangerColorContainerDarkMediumContrast,
        onDangerColorContainerDarkMediumContrast,
    ),
    goodColor = ColorFamily(
        goodColorDarkMediumContrast,
        onGoodColorDarkMediumContrast,
        goodColorContainerDarkMediumContrast,
        onGoodColorContainerDarkMediumContrast,
    ),
)

val extendedDarkHighContrast = ExtendedColorScheme(
    graphBarColor = ColorFamily(
        graphBarColorDarkHighContrast,
        onGraphBarColorDarkHighContrast,
        graphBarColorContainerDarkHighContrast,
        onGraphBarColorContainerDarkHighContrast,
    ),
    dangerColor = ColorFamily(
        dangerColorDarkHighContrast,
        onDangerColorDarkHighContrast,
        dangerColorContainerDarkHighContrast,
        onDangerColorContainerDarkHighContrast,
    ),
    goodColor = ColorFamily(
        goodColorDarkHighContrast,
        onGoodColorDarkHighContrast,
        goodColorContainerDarkHighContrast,
        onGoodColorContainerDarkHighContrast,
    ),
)

@Immutable
data class ColorFamily(
    val color: Color,
    val onColor: Color,
    val colorContainer: Color,
    val onColorContainer: Color
)

val unspecified_scheme = ColorFamily(
    Color.Unspecified, Color.Unspecified, Color.Unspecified, Color.Unspecified
)

// Добавьте этот код в ваш файл с темой
val LocalExtendedColorScheme = staticCompositionLocalOf { extendedLight }

@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Добавьте параметры для контрастности если нужно
    dynamicColor: Boolean = true, // или false, в зависимости от ваших потребностей
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        darkTheme -> darkScheme
        else -> lightScheme
    }

    // Выбираем соответствующую расширенную схему
    val extendedColorScheme = when {
        darkTheme -> extendedDark
        else -> extendedLight
    }

    // Токены дизайн-системы (ТЗ §1): цвета, отступы, тени, формы, типографика, анимации.
    val habitColors = if (darkTheme) DarkHabitColors else LightHabitColors
    val elevation = if (darkTheme) DarkElevation else Elevation()

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = shapes
    ) {
        CompositionLocalProvider(
            LocalExtendedColorScheme provides extendedColorScheme,
            LocalHabitColors provides habitColors,
            LocalHabitShapes provides HabitShapes(),
            LocalSpacing provides Spacing(),
            LocalElevation provides elevation,
            LocalMotion provides Motion(),
            LocalHabitTypography provides HabitTypography(),
        ) {
            content()
        }
    }
}

/**
 * Единая точка доступа к токенам дизайн-системы (ТЗ §1):
 * `AppTheme.colors.accent`, `AppTheme.spacing.screenH`, `AppTheme.shapes.card`,
 * `AppTheme.elevation.card`, `AppTheme.type.h1`, `AppTheme.motion.normal`.
 */
object AppTheme {
    val colors: HabitColors
        @Composable @ReadOnlyComposable get() = LocalHabitColors.current
    val shapes: HabitShapes
        @Composable @ReadOnlyComposable get() = LocalHabitShapes.current
    val spacing: Spacing
        @Composable @ReadOnlyComposable get() = LocalSpacing.current
    val elevation: Elevation
        @Composable @ReadOnlyComposable get() = LocalElevation.current
    val type: HabitTypography
        @Composable @ReadOnlyComposable get() = LocalHabitTypography.current
    val motion: Motion
        @Composable @ReadOnlyComposable get() = LocalMotion.current
}


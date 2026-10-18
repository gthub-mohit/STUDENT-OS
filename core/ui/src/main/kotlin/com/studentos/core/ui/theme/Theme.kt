package com.studentos.core.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = VioletPrimary,
    onPrimary = Color.White,
    primaryContainer = VioletBadgeBg,
    onPrimaryContainer = VioletBright,
    secondary = VioletBright,
    onSecondary = Color(0xFF1E1035),
    secondaryContainer = VioletBadgeBg,
    onSecondaryContainer = VioletBright,
    tertiary = SuccessText,
    onTertiary = SuccessBg,
    tertiaryContainer = SuccessBg,
    onTertiaryContainer = SuccessText,
    background = ZincAppBackground,
    onBackground = ZincTextPrimary,
    surface = ZincCardSurface,
    onSurface = ZincTextPrimary,
    surfaceVariant = ZincInnerSurface,
    onSurfaceVariant = ZincTextSecondary,
    outline = ZincCardBorder,
    outlineVariant = ZincInnerBorder,
    error = DestructiveRed,
    onError = Color(0xFF450A0A),
    errorContainer = DestructiveBg,
    onErrorContainer = DestructiveRed
)

/**
 * Direct access to custom design system tokens:
 * StudentOsTheme.colors.cardSurface
 * StudentOsTheme.colors.primaryCta
 * etc.
 */
object StudentOsTheme {
    val colors: StudentOsColors
        @Composable
        @ReadOnlyComposable
        get() = LocalStudentOsColors.current

    val extendedColors: StudentOsColors
        @Composable
        @ReadOnlyComposable
        get() = LocalStudentOsColors.current
}

@Composable
fun StudentOsTheme(
    darkTheme: Boolean = true, // Default to true: cohesive Linear/Raycast dark theme
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme
    val colors = DarkStudentOsColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = false
            insetsController.isAppearanceLightNavigationBars = false
        }
    }

    CompositionLocalProvider(
        LocalStudentOsColors provides colors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            shapes = StudentOsShapes,
            typography = StudentOsTypography,
            content = content
        )
    }
}

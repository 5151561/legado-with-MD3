package io.legado.app.core.designsystem.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.materialkolor.PaletteStyle

@Immutable
data class LegadoThemeMode(
    val colorScheme: ColorScheme,
    val isDark: Boolean,
    val seedColor: Color,
    val paletteStyle: PaletteStyle,
    val useDynamicColor: Boolean,
)

@Immutable
data class LegadoColorScheme(
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val inversePrimary: Color,
    val secondary: Color,
    val onSecondary: Color,
    val secondaryContainer: Color,
    val onSecondaryContainer: Color,
    val tertiary: Color,
    val onTertiary: Color,
    val tertiaryContainer: Color,
    val onTertiaryContainer: Color,
    val background: Color,
    val onBackground: Color,
    val surface: Color,
    val onSurface: Color,
    val surfaceVariant: Color,
    val onSurfaceVariant: Color,
    val surfaceTint: Color,
    val inverseSurface: Color,
    val inverseOnSurface: Color,
    val error: Color,
    val onError: Color,
    val errorContainer: Color,
    val onErrorContainer: Color,
    val outline: Color,
    val outlineVariant: Color,
    val scrim: Color,
    val surfaceBright: Color,
    val surfaceDim: Color,
    val surfaceContainer: Color,
    val surfaceContainerHigh: Color,
    val surfaceContainerHighest: Color,
    val surfaceContainerLow: Color,
    val surfaceContainerLowest: Color,
    val primaryFixed: Color,
    val primaryFixedDim: Color,
    val onPrimaryFixed: Color,
    val onPrimaryFixedVariant: Color,
    val secondaryFixed: Color,
    val secondaryFixedDim: Color,
    val onSecondaryFixed: Color,
    val onSecondaryFixedVariant: Color,
    val tertiaryFixed: Color,
    val tertiaryFixedDim: Color,
    val onTertiaryFixed: Color,
    val onTertiaryFixedVariant: Color,
    val cardContainer: Color,
    val onCardContainer: Color,
    val onSheetContent: Color,
    val cardPrimaryContainer: Color,
    val surfaceInput: Color,
)

@Immutable
data class LegadoTypography(
    val headlineLarge: TextStyle,
    val headlineLargeEmphasized: TextStyle,
    val headlineMedium: TextStyle,
    val headlineMediumEmphasized: TextStyle,
    val headlineSmall: TextStyle,
    val headlineSmallEmphasized: TextStyle,
    val titleLarge: TextStyle,
    val titleLargeEmphasized: TextStyle,
    val titleMedium: TextStyle,
    val titleMediumEmphasized: TextStyle,
    val titleSmall: TextStyle,
    val titleSmallEmphasized: TextStyle,
    val bodyLarge: TextStyle,
    val bodyLargeEmphasized: TextStyle,
    val bodyMedium: TextStyle,
    val bodyMediumEmphasized: TextStyle,
    val bodySmall: TextStyle,
    val bodySmallEmphasized: TextStyle,
    val labelLarge: TextStyle,
    val labelLargeEmphasized: TextStyle,
    val labelMedium: TextStyle,
    val labelMediumEmphasized: TextStyle,
    val labelSmall: TextStyle,
    val labelSmallEmphasized: TextStyle,
)

@Immutable
data class LegadoSpacing(
    val extraSmall: Dp = 4.dp,
    val small: Dp = 8.dp,
    val medium: Dp = 16.dp,
    val large: Dp = 24.dp,
    val extraLarge: Dp = 32.dp,
)

val LocalLegadoColorScheme = staticCompositionLocalOf<LegadoColorScheme> {
    error("No LegadoColorScheme provided")
}

val LocalLegadoTypography = staticCompositionLocalOf<LegadoTypography> {
    error("No LegadoTypography provided")
}

val LocalLegadoThemeMode = staticCompositionLocalOf {
    LegadoThemeMode(
        colorScheme = lightColorScheme(),
        isDark = false,
        seedColor = Color.Unspecified,
        paletteStyle = PaletteStyle.TonalSpot,
        useDynamicColor = true,
    )
}

val LocalLegadoSpacing = staticCompositionLocalOf { LegadoSpacing() }

object LegadoTheme {
    val colorScheme: LegadoColorScheme
        @Composable
        @ReadOnlyComposable
        get() = LocalLegadoColorScheme.current

    val typography: LegadoTypography
        @Composable
        @ReadOnlyComposable
        get() = LocalLegadoTypography.current

    val spacing: LegadoSpacing
        @Composable
        @ReadOnlyComposable
        get() = LocalLegadoSpacing.current

    val shapes: Shapes
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.shapes

    val isDark: Boolean
        @Composable
        @ReadOnlyComposable
        get() = LocalLegadoThemeMode.current.isDark

    val seedColor: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalLegadoThemeMode.current.seedColor

    val paletteStyle: PaletteStyle
        @Composable
        @ReadOnlyComposable
        get() = LocalLegadoThemeMode.current.paletteStyle

    val useDynamicColor: Boolean
        @Composable
        @ReadOnlyComposable
        get() = LocalLegadoThemeMode.current.useDynamicColor
}

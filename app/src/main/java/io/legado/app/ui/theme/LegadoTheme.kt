package io.legado.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import com.materialkolor.PaletteStyle

/**
 * Phase 1 兼容入口。主题契约已由 :core:designsystem 持有，旧调用方继续通过本包读取同一组
 * CompositionLocal，避免迁移期间出现第二套主题值。
 */
typealias LegadoThemeMode = io.legado.app.core.designsystem.theme.LegadoThemeMode
typealias LegadoColorScheme = io.legado.app.core.designsystem.theme.LegadoColorScheme
typealias LegadoTypography = io.legado.app.core.designsystem.theme.LegadoTypography

val LocalLegadoColorScheme =
    io.legado.app.core.designsystem.theme.LocalLegadoColorScheme
val LocalLegadoTypography =
    io.legado.app.core.designsystem.theme.LocalLegadoTypography
val LocalLegadoThemeColors =
    io.legado.app.core.designsystem.theme.LocalLegadoThemeMode

object LegadoTheme {
    val colorScheme: LegadoColorScheme
        @Composable
        @ReadOnlyComposable
        get() = io.legado.app.core.designsystem.theme.LegadoTheme.colorScheme

    val typography: LegadoTypography
        @Composable
        @ReadOnlyComposable
        get() = io.legado.app.core.designsystem.theme.LegadoTheme.typography

    val isDark: Boolean
        @Composable
        @ReadOnlyComposable
        get() = io.legado.app.core.designsystem.theme.LegadoTheme.isDark

    val seedColor: Color
        @Composable
        @ReadOnlyComposable
        get() = io.legado.app.core.designsystem.theme.LegadoTheme.seedColor

    val paletteStyle: PaletteStyle
        @Composable
        @ReadOnlyComposable
        get() = io.legado.app.core.designsystem.theme.LegadoTheme.paletteStyle

    val useDynamicColor: Boolean
        @Composable
        @ReadOnlyComposable
        get() = io.legado.app.core.designsystem.theme.LegadoTheme.useDynamicColor
}

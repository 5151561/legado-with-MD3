package io.legado.app.core.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember

/**
 * 重设计主题的读取入口。
 *
 * 与既有 [LegadoTheme] 并存：后者服务尚未迁移的旧 UI，前者服务按新设计重做的界面。
 * 各 feature 迁移完成后删除 [LegadoTheme]。
 */
object AppTheme {
    val colorScheme: AppColorScheme
        @Composable @ReadOnlyComposable get() = LocalAppColorScheme.current

    val typography: AppTypography
        @Composable @ReadOnlyComposable get() = LocalAppTypography.current

    val shapes: AppShapes
        @Composable @ReadOnlyComposable get() = LocalAppShapes.current

    val dimens: AppDimens
        @Composable @ReadOnlyComposable get() = LocalAppDimens.current

    /** 正文纸色。独立于 [colorScheme]，由阅读样式抽屉选择（画板 N-04）。 */
    val reading: ReadingPalette
        @Composable @ReadOnlyComposable get() = LocalReadingPalette.current
}

/**
 * 提供重设计主题。
 *
 * 同时把配色映射进 [MaterialTheme]，使直接使用的 Material 3 组件与 kit 组件同色，
 * 避免出现第二套视觉。
 *
 * @param dark 夜墨。日光/夜墨/跟随系统的三选一逻辑由 app shell 解析后传入，
 *   设计系统不读取偏好（画板 N-04 的主题设置）。
 * @param readingPalette 正文纸色，缺省为设计稿默认的「纸」。
 */
@Composable
fun ProvideAppTheme(
    dark: Boolean,
    readingPalette: ReadingPalette = ReadingPaperDefault,
    colorScheme: AppColorScheme = if (dark) AppDarkColorScheme else AppLightColorScheme,
    typography: AppTypography = AppDefaultTypography,
    shapes: AppShapes = AppShapes(),
    dimens: AppDimens = AppDimens(),
    content: @Composable () -> Unit,
) {
    val material = remember(colorScheme, dark) { colorScheme.toMaterialColorScheme(dark) }
    CompositionLocalProvider(
        LocalAppColorScheme provides colorScheme,
        LocalAppTypography provides typography,
        LocalAppShapes provides shapes,
        LocalAppDimens provides dimens,
        LocalReadingPalette provides readingPalette,
    ) {
        MaterialTheme(colorScheme = material, content = content)
    }
}

private fun AppColorScheme.toMaterialColorScheme(dark: Boolean) =
    (if (dark) darkColorScheme() else lightColorScheme()).copy(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = primaryContainer,
        onPrimaryContainer = onPrimaryContainer,
        secondary = secondary,
        onSecondary = onSecondary,
        secondaryContainer = secondaryContainer,
        onSecondaryContainer = onSecondaryContainer,
        tertiary = tertiary,
        onTertiary = onTertiary,
        tertiaryContainer = tertiaryContainer,
        onTertiaryContainer = onTertiaryContainer,
        error = error,
        onError = onError,
        errorContainer = errorContainer,
        onErrorContainer = onErrorContainer,
        surface = surface,
        onSurface = onSurface,
        surfaceVariant = surfaceVariant,
        onSurfaceVariant = onSurfaceVariant,
        surfaceContainerLowest = surfaceContainerLowest,
        surfaceContainerLow = surfaceContainerLow,
        surfaceContainer = surfaceContainer,
        surfaceContainerHigh = surfaceContainerHigh,
        surfaceContainerHighest = surfaceContainerHighest,
        outline = outline,
        outlineVariant = outlineVariant,
        background = surface,
        onBackground = onSurface,
    )
